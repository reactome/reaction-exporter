package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

import org.reactome.server.tools.diagram.data.layout.Connector;
import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.*;
import org.reactome.server.tools.reaction.exporter.layout.common.Bounds;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.GlyphUtils;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.box.Place.*;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer.getBounds;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer.move;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.GlyphUtils.hasRole;
import static org.reactome.server.tools.reaction.exporter.layout.common.GlyphUtils.isAncestor;

/**
 * This is the crown jewel of the algorithms. Though it is still under high development, its results outperform any
 * other developed algorithm. The algorithm is divided in 3 steps: matrix generation, compaction and positioning.
 * <p><p>
 * <b>1. Matrix generation</b>
 * <p>
 * This part is performed by the {@link Box} class itself. A box is created by each compartment. A box can contain other
 * boxes (representing sub compartments) and glyphs. Boxes are designed to have enough space to layout all of its
 * content. A description of how it works can be found inside {@link Box} class.
 * <p><p>
 * <b>2. Compaction</b>
 * This step is performed by a dozen of short methods that remove empty columns and rows in the matrix, and move
 * elements in short steps closer to the reaction so that they do not break any rule and represent a more compact view.
 * <p><p>
 * <b>3. Positioning</b>
 * Finally, we transform rows and columns into 'x's and 'y's, by computing columns and rows max widths and height and
 * centering each box into its 'x' and 'y'. Special attention to compartments in this step, as they can cause an
 * enlargement of widths when their name is too long.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
@SuppressWarnings("Duplicates")
public class BoxAlgorithm {

    private static final String EXTRACELLULAR_REGION_ACC = "0005576";


    private final Layout layout;
    private final LayoutIndex index;

    /**
     * Creates a BoxAlgorithm and prepares it to compute a layout. Use only one {@link BoxAlgorithm} per layout and call
     * {@link BoxAlgorithm#compute()} only once.
     */
    public BoxAlgorithm(Layout layout) {
        this.layout = layout;
        DuplicateManager.addDuplicates(layout);
        index = new LayoutIndex(layout);
        fixReactionWithNoCompartment(layout);
    }

    /**
     * This fixes reaction R-HSA-9006323 that does not contain a compartment. It can also happen in other species.
     */
    private void fixReactionWithNoCompartment(Layout layout) {
        if (layout.getReaction().getCompartment() == null) {
            layout.getReaction().setCompartment(layout.getCompartmentRoot());
            layout.getCompartmentRoot().getContainedGlyphs().add(layout.getReaction());
        }
    }

    /**
     * Computes the position (dimension and coordinate) of every element in the Layout.
     */
    public void compute() {
        // This is the main algorithm, divided into 3 steps: positioning, compaction and sizing
        // Due to the high dependence of variables between the steps, everything happens int this method

        // 1. POSITIONING

        // a) compartments are placed based on their content and, of course, their hierarchy
        final Box box = new Box(layout.getCompartmentRoot(), index);
        // b) reaction is placed, also based on its surrounding content
        Point reactionPosition = box.placeReaction();
        // c) participants are placed, they have to avoid being in the same row or column as similar participants
        box.placeElements(reactionPosition);

        // 2. COMPACTION

        final Div[][] preDivs = box.getDivs();
        final Grid<Div> grid = new Grid<>(Div.class, preDivs);

        // a) empty rows and columns are removed
        removeEmptyRows(grid);
        removeEmptyCols(grid);

        // artifact: having participants with the same role in the same row or column
        reactionPosition = getReactionPosition(grid);
        expandSameRoleSameColumn(grid, reactionPosition);

        reactionPosition = getReactionPosition(grid);
        expandSameRoleSameRow(grid, reactionPosition);

        // b) some helper methods to fix artifacts
        reactionPosition = getReactionPosition(grid);

        // these 2 methods force elements to be divided by imaginary limits
        //     | C |
        //  ---+---+---
        //   I | X | O
        //  ---+---+---
        //     | R |
        // although previous steps are supposed to avoid this, sometimes it happens
        forceDiagonalTopDown(grid, reactionPosition);
        forceDiagonalLeftRight(grid, reactionPosition);

        // c) elements are moved closer to the center
        // this part has serious problems when the reaction is not in the center
        reactionPosition = getReactionPosition(grid);
        compactLeft(grid, reactionPosition);
        compactRight(grid, reactionPosition);
        compactTop(grid, reactionPosition);
        compactBottom(grid, reactionPosition);

        removeEmptyRows(grid);
        removeEmptyCols(grid);

        // 3. SIZING

        // a) compute the size and padding of each column and row
        // Each square from grid will now have integer sizes. For that, we associate 4 properties to each one:
        // width and height represent the size of the square,
        // vertical and horizontal padding the position where elements are placed inside the square
        // padding is necessary when participants are close to a compartment border (very often)
        final double[] heights = new double[grid.getRows()];
        final double[] verticalPadding = new double[grid.getRows()];
        final double[] widths = new double[grid.getColumns()];
        final double[] horizontalPadding = new double[grid.getColumns()];

        // sizing is done through compartment tree, since every compartment size depends also on its sub-compartment
        size(layout.getCompartmentRoot(), grid, heights, widths, horizontalPadding, verticalPadding);

        // b) Add extra spaces around the reaction
        reactionPosition = getReactionPosition(grid);
        for (int r = reactionPosition.getRow() - 1; r >= 0; r--) {
            if (containsRole(grid.getRow(r), Collections.singletonList(CATALYST))) {
                heights[r] += Constants.VERTICAL_PADDING;
                verticalPadding[r] -= Constants.VERTICAL_PADDING;
                break;
            }
        }
        for (int r = reactionPosition.getRow() + 1; r < grid.getRows(); r++) {
            if (containsRole(grid.getRow(r), Arrays.asList(NEGATIVE_REGULATOR, POSITIVE_REGULATOR))) {
                heights[r] += Constants.VERTICAL_PADDING;
                verticalPadding[r] += Constants.VERTICAL_PADDING;
                break;
            }
        }
        for (int c = reactionPosition.getCol() - 1; c >= 0; c--) {
            if (containsRole(grid.getColumn(c), Collections.singletonList(INPUT))) {
                widths[c] += Constants.HORIZONTAL_PADDING;
                horizontalPadding[c] -= Constants.HORIZONTAL_PADDING;
                break;
            }
        }
        for (int c = reactionPosition.getCol() + 1; c < grid.getColumns(); c++) {
            if (containsRole(grid.getColumn(c), Collections.singletonList(OUTPUT))) {
                widths[c] += Constants.HORIZONTAL_PADDING;
                horizontalPadding[c] += Constants.HORIZONTAL_PADDING;
                break;
            }
        }

        // c) place things (wheeeee!!)
        final double[] cy = getCenters(heights, verticalPadding);
        final double[] cx = getCenters(widths, horizontalPadding);

        for (int row = 0; row < grid.getRows(); row++) {
            for (int col = 0; col < grid.getColumns(); col++) {
                final Div div = grid.get(row, col);
                if (div == null) continue;
                div.center(cx[col], cy[row]);
            }
        }

        // 4. CONNECTORS
        ConnectorFactory.addConnectors(reactionPosition, grid, widths, heights, layout, index);

        // APPENDIX
        // TODO: 17/01/19 can this be done with step 3?
        layoutCompartments();
        removeExtracellular();
        computeDimension();
        moveToOrigin();
    }

    /**
     * Returns the absolute center of each element. The absolute center depends on the previous sizes, the size of the
     * current element and its padding.
     *
     * @param sizes    size of each element (widths or heights)
     * @param paddings padding of each element (vertical or horizontal)
     * @return an array containing the exact center for each element
     */
    private double[] getCenters(double[] sizes, double[] paddings) {
        final double[] cy = new double[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            for (int j = 0; j < i; j++) cy[i] += sizes[j];
            cy[i] += 0.5 * (paddings[i] + sizes[i]);
        }
        return cy;
    }

    private void compactLeft(Grid<Div> grid, Point reactionPosition) {
        boolean hasMoved;
        do {
            hasMoved = false;
            for (int col = reactionPosition.getCol() - 1; col >= 0; col--) {
                final int to = col + 1;
                for (int row = 0; row < grid.getRows(); row++) {

                    final Div div = grid.get(row, col);
                    if (div == null || grid.get(row, to) != null) continue;

                    final Collection<Place> places = div.getBusyPlaces();
                    // Inputs cannot share column with reaction

                    if (places.contains(LEFT) && to == reactionPosition.getCol()) continue;

                    final Div[] column = grid.getColumn(to);
                    // Inputs cannot share column with catalysts neither regulators
                    if (places.contains(LEFT) && (hasPlace(column, TOP) || hasPlace(column, BOTTOM))) continue;

                    // Two catalysts cannot share column
                    if (places.contains(TOP) && hasPlace(column, TOP)) continue;
                    // Two regulators cannot share column
                    if (places.contains(BOTTOM) && hasPlace(column, BOTTOM)) continue;

                    if (canMove(grid, new Point(row, col), new Point(row, to))) {
                        grid.set(row, to, div);
                        grid.set(row, col, null);
                        hasMoved = true;
                    }
                }
            }
        } while (hasMoved);
    }

    private void compactRight(Grid<Div> grid, Point reactionPosition) {
        // From reaction to border
        boolean hasMoved;
        do {
            hasMoved = false;
            for (int c = reactionPosition.getCol() + 1; c < grid.getColumns(); c++) {
                final int to = c - 1;
                for (int r = 0; r < grid.getRows(); r++) {

                    final Div div = grid.get(r, c);
                    if (div == null || grid.get(r, to) != null) continue;

                    final Collection<Place> places = div.getBusyPlaces();

                    // Outputs cannot share column with reaction
                    if (places.contains(RIGHT) && reactionPosition.getCol() == to) continue;

                    // Outputs cannot share column with catalysts or regulators
                    final Div[] column = grid.getColumn(to);
                    if (places.contains(RIGHT) && (hasPlace(column, TOP) || hasPlace(column, BOTTOM))) continue;

                    // Two catalysts cannot share column
                    if (places.contains(TOP) && hasPlace(column, TOP)) continue;
                    // Two regulators cannot share column
                    if (places.contains(BOTTOM) && hasPlace(column, BOTTOM)) continue;

                    if (canMove(grid, new Point(r, c), new Point(r, to))) {
                        grid.set(r, to, div);
                        grid.set(r, c, null);
                        hasMoved = true;
                    }
                }
            }
        } while (hasMoved);
    }

    private void compactTop(Grid<Div> grid, Point reactionPosition) {
        boolean hasMoved;
        do {  // We perform several rounds till nothing can be moved
            hasMoved = false;
            for (int r = reactionPosition.getRow() - 1; r >= 0; r--) {
                final int to = r + 1;
                for (int c = 0; c < grid.getColumns(); c++) {

                    final Div div = grid.get(r, c);
                    if (div == null || grid.get(to, c) != null) continue;

                    final Collection<Place> places = div.getBusyPlaces();

                    // Catalysts cannot share row with reaction
                    if (places.contains(TOP) && reactionPosition.getRow() == to) continue;

                    // Catalysts cannot share row with inputs or outputs
                    final Div[] row = grid.getRow(to);
                    if (places.contains(TOP) && (hasPlace(row, LEFT) || hasPlace(row, RIGHT))) continue;

                    // Two inputs cannot share row
                    if (places.contains(LEFT) && hasPlace(row, LEFT)) continue;
                    // Two outputs cannot share column
                    if (places.contains(RIGHT) && hasPlace(row, RIGHT)) continue;

                    if (canMove(grid, new Point(r, c), new Point(to, c))) {
                        grid.set(to, c, div);
                        grid.set(r, c, null);
                        hasMoved = true;
                    }
                }
            }
        } while (hasMoved);
    }

    private void compactBottom(Grid<Div> grid, Point reactionPosition) {
        boolean hasMoved;
        do {
            hasMoved = false;
            for (int r = reactionPosition.getRow() + 1; r < grid.getRows(); r++) {
                final int to = r - 1;
                for (int c = 0; c < grid.getColumns(); c++) {

                    final Div div = grid.get(r, c);
                    if (div == null || grid.get(to, c) != null) continue;

                    // Regulators cannot share row with reaction
                    final Collection<Place> places = div.getBusyPlaces();
                    if (places.contains(BOTTOM) && reactionPosition.getRow() == to) continue;

                    // Regulators cannot share row with inputs or outputs
                    final Div[] row = grid.getRow(to);
                    if (places.contains(BOTTOM) && (hasPlace(row, LEFT) || hasPlace(row, RIGHT))) continue;

                    // Two inputs cannot share row
                    if (places.contains(LEFT) && hasPlace(row, LEFT)) continue;
                    // Two outputs cannot share column
                    if (places.contains(RIGHT) && hasPlace(row, RIGHT)) continue;

                    if (canMove(grid, new Point(r, c), new Point(to, c))) {
                        grid.set(to, c, div);
                        grid.set(r, c, null);
                        hasMoved = true;
                    }
                }
            }
        } while (hasMoved);
    }

    private boolean hasPlace(Div[] divs, Place place) {
        return Arrays.stream(divs).filter(Objects::nonNull).anyMatch(div -> div.getBusyPlaces().contains(place));
    }

    /**
     * Tests if the content in source can be moved to target without breaking the diagram. Returns false as soon as it
     * meets the false condition.
     *
     * @param grid   current grid
     * @param source position where the current content is
     * @param target desired position where to move the content
     * @return true if, and only if, moving the content from source to target generated a valid diagram
     */
    private boolean canMove(Grid<Div> grid, Point source, Point target) {
        if (grid.get(target.getRow(), target.getCol()) != null) return false;
        final Grid<Div> gridCopy = new Grid<>(grid);
        gridCopy.set(target.getRow(), target.getCol(), grid.get(source.getRow(), source.getCol()));
        gridCopy.set(source.getRow(), source.getCol(), null);
        final Grid<CompartmentGlyph> compartmentGridCopy = new Grid<>(CompartmentGlyph.class, grid.getRows(), grid.getColumns());
        if (!computeCompartment(layout.getCompartmentRoot(), gridCopy, compartmentGridCopy)) return false;
        return elementsInCompartments(gridCopy, compartmentGridCopy);
    }

    /**
     * Tests if elements in <em>grid</em> match compartments in <em>compartmentGrid</em>. Matches are exact, the
     * compartment of {@link Grid#get(int, int)} must be the same.
     *
     * @param grid            grid representing the diagram
     * @param compartmentGrid grid of compartments
     * @return true if, and only if, for every position in grid, the content in grid has the same compartment as in
     * compartmentGrid
     */
    private boolean elementsInCompartments(Grid<Div> grid, Grid<CompartmentGlyph> compartmentGrid) {
        for (int r = 0; r < grid.getRows(); r++) {
            for (int c = 0; c < grid.getColumns(); c++) {
                final Div div = grid.get(r, c);
                if (div != null) {
                    final CompartmentGlyph compartment = div.getCompartment();
                    final CompartmentGlyph compartmentGlyph = compartmentGrid.get(r, c);
                    if (compartment != compartmentGlyph) return false;
                }
            }
        }
        return true;
    }

    private boolean containsRole(Div[] divs, Collection<EntityRole> roles) {
        for (final Div div : divs) {
            if (div != null) {
                final Collection<EntityRole> containedRoles = PlacePositioner.simplify(div.getContainedRoles());
                for (final EntityRole role : roles) {
                    if (containedRoles.contains(role)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Avoid having catalysts in the same row as inputs or outputs
     */
    private void forceDiagonalTopDown(Grid<Div> grid, Point reactionPosition) {
        int r = 0;
        while (r < reactionPosition.getRow()) {
            boolean hasVertical = false;
            boolean hasHorizontal = false;
            for (int c = 0; c < grid.getColumns(); c++) {
                if (grid.get(r, c) instanceof VerticalLayout) hasVertical = true;
                if (grid.get(r, c) instanceof HorizontalLayout) hasHorizontal = true;
            }
            if (hasHorizontal && hasVertical) {
                grid.insertRows(r, 1);
                reactionPosition.setRow(reactionPosition.getRow() + 1);
                for (int c = 0; c < grid.getColumns(); c++) {
                    if (grid.get(r + 1, c) instanceof HorizontalLayout) {
                        grid.set(r, c, grid.get(r + 1, c));
                        grid.set(r + 1, c, null);
                    }
                }
            }
            r++;
        }
        r = reactionPosition.getRow() + 1;
        while (r < grid.getRows()) {
            boolean hasVertical = false;
            boolean hasHorizontal = false;
            for (int c = 0; c < grid.getColumns(); c++) {
                if (grid.get(r, c) instanceof VerticalLayout) hasVertical = true;
                if (grid.get(r, c) instanceof HorizontalLayout) hasHorizontal = true;
            }
            if (hasHorizontal && hasVertical) {
                grid.insertRows(r + 1, 1);
                for (int c = 0; c < grid.getColumns(); c++) {
                    if (grid.get(r, c) instanceof HorizontalLayout) {
                        grid.set(r + 1, c, grid.get(r, c));
                        grid.set(r, c, null);
                    }
                }
            }
            r++;
        }
    }

    private void forceDiagonalLeftRight(Grid<Div> grid, Point reactionPosition) {
        // left/right
        int c = 0;
        while (c < reactionPosition.getCol()) {
            boolean hasVertical = false;
            boolean hasHorizontal = false;
            for (int r = 0; r < grid.getRows(); r++) {
                if (grid.get(r, c) instanceof VerticalLayout) hasVertical = true;
                if (grid.get(r, c) instanceof HorizontalLayout) hasHorizontal = true;
            }
            if (hasHorizontal && hasVertical) {
                grid.insertColumns(c, 1);
                reactionPosition.setCol(reactionPosition.getCol() + 1);
                for (int r = 0; r < grid.getRows(); r++) {
                    if (grid.get(r, c + 1) instanceof VerticalLayout) {
                        grid.set(r, c, grid.get(r, c + 1));
                        grid.set(r, c + 1, null);
                    }
                }
            }
            c++;
        }
        c = reactionPosition.getCol() + 1;
        while (c < grid.getColumns()) {
            boolean hasVertical = false;
            boolean hasHorizontal = false;
            for (int r = 0; r < grid.getRows(); r++) {
                if (grid.get(r, c) instanceof VerticalLayout) hasVertical = true;
                if (grid.get(r, c) instanceof HorizontalLayout) hasHorizontal = true;
            }
            if (hasHorizontal && hasVertical) {
                grid.insertColumns(c + 1, 1);
                for (int r = 0; r < grid.getRows(); r++) {
                    if (grid.get(r, c) instanceof VerticalLayout) {
                        grid.set(r, c + 1, grid.get(r, c));
                        grid.set(r, c, null);
                    }
                }
            }
            c++;
        }
    }


    /**
     * Computes the positions in grid occupied by this compartment, but first calls this method with all of its
     * subcompartments.
     *
     * @param compartment current compartment
     * @param grid        grid of elements
     * @param comps       grid of compartments
     * @return true if all compartments could be placed without violating the hierarchy.
     */
    private boolean computeCompartment(CompartmentGlyph compartment, Grid<Div> grid, Grid<CompartmentGlyph> comps) {
        for (final CompartmentGlyph child : compartment.getChildren()) {
            if (!computeCompartment(child, grid, comps)) return false;
        }
        // Find all elements (divs and sub-compartments) that fall into this compartment and compute the limits of the
        // compartment
        int minCol = Integer.MAX_VALUE;
        int maxCol = 0;
        int minRow = Integer.MAX_VALUE;
        int maxRow = 0;
        for (int r = 0; r < grid.getRows(); r++) {
            for (int c = 0; c < grid.getColumns(); c++) {
                final Div div = grid.get(r, c);
                if (div == null) continue;
                if (compartment == div.getCompartment() || GlyphUtils.isAncestor(compartment, div.getCompartment())) {
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                }
            }
        }
        // Mark all squares in the range [minCol, maxCol], [minRow, maxRow]. If there is a compartment in that position
        // do nothing. If the compartment in that position is not a sub-compartment, return false.
        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                if (comps.get(r, c) == null) comps.set(r, c, compartment);
                else if (!isAncestor(compartment, comps.get(r, c))) return false;
            }
        }
        return true;
    }

    /**
     * Calculates the absolute position of the reaction in the grid
     */
    private Point getReactionPosition(Grid<Div> grid) {
        for (int r = 0; r < grid.getRows(); r++) {
            for (int c = 0; c < grid.getColumns(); c++) {
                if (grid.get(r, c) instanceof GlyphsLayout) {
                    final GlyphsLayout layout = (GlyphsLayout) grid.get(r, c);
                    if (layout.getGlyphs().iterator().next() instanceof ReactionGlyph) {
                        return new Point(r, c);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Set the size of rows and cols where this compartment spans.
     *
     * @param compartment current compartment
     * @param grid        grid of elements
     * @param heights     array with heights
     * @param widths      array with widths
     * @param horPad      array with horizontal paddings
     * @param verPad      array with vertical paddings
     */
    private void size(CompartmentGlyph compartment, Grid<Div> grid, double[] heights, double[] widths, double[] horPad, double[] verPad) {
        for (final CompartmentGlyph child : compartment.getChildren()) {
            size(child, grid, heights, widths, horPad, verPad);
        }
        int minCol = Integer.MAX_VALUE;
        int maxCol = 0;
        int minRow = Integer.MAX_VALUE;
        int maxRow = 0;
        for (int r = 0; r < grid.getRows(); r++) {
            for (int c = 0; c < grid.getColumns(); c++) {
                final Div div = grid.get(r, c);
                if (div == null) continue;
                // This compartment spans through participants on it or other sub-compartments
                if (compartment == div.getCompartment() || GlyphUtils.isAncestor(compartment, div.getCompartment())) {
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    // Only compute sizes of elements that belong to this compartment, otherwise, we will compute the
                    // same bounds more than once
                    if (compartment == div.getCompartment()) {
                        final Bounds bounds = div.getBounds();
                        widths[c] = Math.max(widths[c], bounds.getWidth());
                        heights[r] = Math.max(heights[r], bounds.getHeight());
                    }
                }
            }
        }
        // Test if compartment text fits into the width of the compartment
        // otherwise, expand all of them proportionally
        double width = 0;
        for (int i = minCol; i <= maxCol; i++) {
            width += widths[i];
        }
        final double minWidth = FontProperties.getTextWidth(compartment.getName());
        if (width < minWidth) {
            final double factor = minWidth / width;
            for (int i = minCol; i <= maxCol; i++) {
                widths[i] *= factor;
            }
        }
        widths[minCol] += Constants.COMPARTMENT_PADDING;
        horPad[minCol] += Constants.COMPARTMENT_PADDING;
        widths[maxCol] += Constants.COMPARTMENT_PADDING;
        horPad[maxCol] -= Constants.COMPARTMENT_PADDING;
        heights[minRow] += Constants.COMPARTMENT_PADDING;
        verPad[minRow] += Constants.COMPARTMENT_PADDING;
        heights[maxRow] += Constants.COMPARTMENT_PADDING;
        verPad[maxRow] -= Constants.COMPARTMENT_PADDING;
    }

    private void removeEmptyRows(Grid<Div> divs) {
        int r = 0;
        while (r < divs.getRows()) {
            if (Arrays.stream(divs.getRow(r)).allMatch(Objects::isNull)) {
                divs.removeRows(r, 1);
            } else r++;
        }
    }

    private void removeEmptyCols(Grid<Div> divs) {
        int c = 0;
        while (c < divs.getColumns()) {
            if (Arrays.stream(divs.getColumn(c)).allMatch(Objects::isNull)) {
                divs.removeColumns(c, 1);
            } else c++;
        }
    }

    private void expandSameRoleSameRow(Grid<Div> grid, Point reactionPosition) {
        // Now I know exactly why this happens:
        // 2 compartments side by side, both have the same role (input f. e.).
        // Now imagine both are small (3x3), both have only 1 row for the input. This causes both inputs to be in the
        // same row
        // Case 2: Now imagine that one of them has 3 rows available for inputs. If it is rendered first, it will use
        // the center position; when the other one is rendered, as it only contains 1 position for the input, both will
        // be in the same row.
        // Now we are using this piece of code, but in the future we should use a more convenient strategy
        // This always happens in the center row
        final int r = reactionPosition.getRow();
        for (final Place place : Arrays.asList(LEFT, RIGHT)) {
            final Div[] row = grid.getRow(r);
            long count = count(row, place);
            while (count > 1) {
                grid.insertRow(r + 1);
                for (int col = 0; col < row.length; col++) {
                    final Div div = row[col];
                    if (div != null && div.getBusyPlaces().contains(place)) {
                        grid.set(r + 1, col, div);
                        grid.set(r, col, null);
                        break;
                    }
                }
                count = count(row, place);
            }
        }
    }

    private void expandSameRoleSameColumn(Grid<Div> grid, Point reactionPosition) {
        // this is something that happens in the reaction column
        // When the compartment of the reaction has children with regulators, more than one regulator appear in this
        // column (R-HSA-425661)
        final int c = reactionPosition.getCol();
        for (final Place place : Arrays.asList(TOP, BOTTOM)) {
            Div[] column = grid.getColumn(c);
            long count = count(column, place);
            while (count > 1) {
                grid.insertColumn(c + 1);
                for (int row = 0; row < column.length; row++) {
                    final Div div = column[row];
                    if (div != null && div.getBusyPlaces().contains(place)) {
                        grid.set(row, c + 1, div);
                        grid.set(row, c, null);
                        break;
                    }
                }
                column = grid.getColumn(c);  // Column must be generated again
                count = count(column, place);
            }
        }
    }

    private long count(Div[] row, Place place) {
        return Arrays.stream(row).filter(Objects::nonNull).filter(div -> div.getBusyPlaces().contains(place)).count();
    }

    private int countRegulators(Point reactionPosition, Div[] column) {
        int regulators = 0;
        for (int r = reactionPosition.getRow() + 1; r < column.length; r++) {
            if (column[r] != null && PlacePositioner.simplify(column[r].getContainedRoles()).contains(NEGATIVE_REGULATOR))
                regulators++;
        }
        return regulators;
    }

    private void layoutCompartments() {
        layoutCompartment(layout.getCompartmentRoot());
    }

    /**
     * Calculates the size of the compartments so each of them surrounds all of its contained glyphs and children.
     */
    private void layoutCompartment(CompartmentGlyph compartment) {
        Bounds b = null;
        for (CompartmentGlyph child : compartment.getChildren()) {
            layoutCompartment(child);
            if (b == null) b = new Bounds(child.getBounds());
            else b.union(child.getBounds());
        }
        for (Glyph glyph : compartment.getContainedGlyphs()) {
            final Bounds bounds = glyph instanceof ReactionGlyph
                    ? Transformer.padd(getBounds(glyph), 60, 40)
                    : getBounds(glyph);
            if (b == null) b = new Bounds(bounds);
            else b.union(bounds);
            // If there is an input that is a catalyst, it will have a connector on top of it. That will be part of the
            // compartment
            if (glyph instanceof EntityGlyph) {
                final EntityGlyph entityGlyph = (EntityGlyph) glyph;
                if (hasRole(entityGlyph, CATALYST, INPUT)) {
                    double topy = entityGlyph.getBounds().getY();
                    for (Connector connector : entityGlyph.getConnector()) {
                        for (final Segment segment : connector.getSegments()) {
                            if (segment.getFrom().getY() < topy) topy = segment.getFrom().getY();
                        }
                    }
                    b.union(new Bounds(entityGlyph.getBounds().getX(), topy, 1, 1));
                }
            }
        }
        b.setX(b.getX() - Constants.COMPARTMENT_PADDING);
        b.setY(b.getY() - Constants.COMPARTMENT_PADDING);
        b.setWidth(b.getWidth() + 2 * Constants.COMPARTMENT_PADDING);
        b.setHeight(b.getHeight() + 2 * Constants.COMPARTMENT_PADDING);

        final double textWidth = FontProperties.getTextWidth(compartment.getName());
        final double textHeight = FontProperties.getTextHeight();
        final double textPadding = textWidth + 30;
        // If the text is too large, we increase the size of the compartment
        if (b.getWidth() < textPadding) {
            double diff = textPadding - b.getWidth();
            b.setWidth(textPadding);
            b.setX(b.getX() - 0.5 * diff);
        }
        // Puts text in the bottom right corner of the compartment
        boolean center = textWidth > 0.5 * b.getWidth();
        final Collection<EntityRole> roles = GlyphUtils.getContainedRoles(compartment);
        final double x = center
                ? b.getCenterX() - 0.5 * textWidth
                : b.getMaxX() - textWidth - 15;
        final double y = roles.contains(CATALYST)
                ? b.getY() + 0.5 * textHeight
                : b.getMaxY() + 0.5 * textHeight - Constants.COMPARTMENT_PADDING;
        final Coordinate coordinate = new CoordinateImpl(x, y);
        compartment.setLabelPosition(coordinate);
        compartment.setBounds(b);
    }

    /**
     * This operation should be called in the last steps, to avoid being exported to a Diagram object.
     */
    private void removeExtracellular() {
        if (layout.getCompartmentRoot().getAccession().equals(EXTRACELLULAR_REGION_ACC))
            layout.getCompartments().remove(layout.getCompartmentRoot());
    }

    /**
     * Compute the absolute dimension of the layout.
     */
    private void computeDimension() {
        Bounds b = null;
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            if (b == null) b = new Bounds(compartment.getBounds());
            else b.union(compartment.getBounds());
        }
        for (EntityGlyph entity : layout.getEntities()) {
            final Bounds bounds = Transformer.getBounds(entity);
            if (b == null) b = new Bounds(bounds);
            else b.union(bounds);
            for (Connector connector : entity.getConnector()) {
                for (final Segment segment : connector.getSegments()) {
                    final double minX = Math.min(segment.getFrom().getX(), segment.getTo().getX());
                    final double maxX = Math.max(segment.getFrom().getX(), segment.getTo().getX());
                    final double minY = Math.min(segment.getFrom().getY(), segment.getTo().getY());
                    final double maxY = Math.max(segment.getFrom().getY(), segment.getTo().getY());
                    b.union(new Bounds(minX, minY, maxX - minX, maxY - minY));
                }
            }
        }
        final Bounds bounds = Transformer.getBounds(layout.getReaction());
        if (b == null) b = new Bounds(bounds);
        else b.union(bounds);
        layout.getBounds().set(b);
    }

    /**
     * Modify all positions so the layout.position is (x,y)=(0,0)
     */
    private void moveToOrigin() {
        final double dx = -layout.getBounds().getX();
        final double dy = -layout.getBounds().getY();
        final Coordinate delta = new CoordinateImpl(dx, dy);
        layout.getBounds().move(dx, dy);
        move(layout.getCompartmentRoot(), delta, true);
    }
}
