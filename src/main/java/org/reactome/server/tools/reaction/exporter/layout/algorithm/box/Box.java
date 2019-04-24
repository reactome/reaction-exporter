package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Constants;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.common.Bounds;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.text.WordUtils.initials;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.GlyphUtils.hasRole;

/**
 * This is the main gear of the {@link BoxAlgorithm}. It finds a place for every participant so that every elements
 * falls into its compartment, every compartment falls inside its parent compartment, keeping a left to right approach.
 * The final result is not optimum, with extra rows and columns.
 * <p>
 * A box is a 2d matrix with enough columns and rows to fit its children boxes and elements. This is a simple box:
 * <pre>
 *     +---+
 *     | c |
 * +---+---+---+
 * | i | x | o |
 * +---+---+---+
 *     | r |
 *     +---+
 * (i) inputs, (o) outputs, (c) catalysts, (r) regulators, (x) reaction
 * </pre>
 * When a box contains one box (a child compartment) space is created as follows:
 * <pre>
 *     +-------------------+
 *     | c   c   c   c   c |
 * +---+-------+---+-------+---+
 * | i |       | x |       | o |
 * |   |       +---+       |   |
 * | i |       | c |       | o |
 * |   +---+---+---+---+---+   |
 * | i | x | i | x | o | x | o |
 * |   +---+---+---+---+---+   |
 * | i |       | r |       | o |
 * |   |       +---+       |   |
 * | i |       | x |       | o |
 * +---+-------+---+-------+---+
 *     | r   r   r   r   r |
 *     +-------------------+
 * </pre>
 * The inner box remains as a simple box, the outer box creates 4 new columns and 4 new rows. 1 column for inputs, 1
 * column for outputs, 2 columns for the reaction, 1 row for catalysts, 1 row for regulators and 2 rows for the
 * reaction.
 * <p>
 * Finally, when a box contains more than 1 boxes, they are placed side by side with 1 space between them. If they are
 * placed horizontally, each one can have a different number of columns, but all of them will have the same number of
 * rows (the maximum of them). When they are placed vertically, different heights (rows) and same number of columns.
 * <pre>
 *     +-----------------------------------+
 *     | c   c   c   c   c   c   c   c   c |
 * +---+-------+---+-----------+---+-------+---+
 * | i |       | x |           | x |       | o |
 * |   |       +---+           +---+       |   |
 * | i |       | c |           | c |       | o |
 * |   +---+---+---+---+---+---+---+---+---+   |
 * | i | x | i | x | o | x | i | x | o | x | o |
 * |   +---+---+---+---+---+---+---+---+---+   |
 * | i |       | r |           | r |       | o |
 * |   |       +---+           +---+       |   |
 * | i |       | x |           | x |       | o |
 * +---+-------+---+-----------+---+-------+---+
 *     | r   r   r   r   r   r   r   r   r |
 *     +-----------------------------------+
 * </pre>
 * <pre>
 *     +---------------------------------------------------+
 *     | c   c   c   c   c   c   c   c   c   c   c   c   c |
 * +---+---------------+---+-----------+---+---+---+-------+---+
 * | i |               | x |                   | x |       | o |
 * | i |       +-------+---+-------+           +---+       | o |
 * | i |       | c   c   c   c   c |           | c |       | o |
 * | i |   +---+-------+---+-------+---+       |   |       | o |
 * | i |   | i |       | x |       | o |       |   |       | o |
 * | i |   |   |       +---+       |   |       |   |       | o |
 * | i |   | i |       | c |       | o |       |   |       | o |
 * | i +---+   +---+---+---+---+---+   +---+---+---+---+---+ o |
 * | i | x | i | x | i | x | o | x | o | x | i | x | o | x | o |
 * | i +---+   +---+---+---+---+---+   +---+---+---+---+---+ o |
 * | i |   | i |       | r |       | o |       |   |       | o |
 * | i |   |   |       +---+       |   |       |   |       | o |
 * | i |   | i |       | x |       | o |       |   |       | o |
 * | i |   +---+-------+---+-------+---+       |   |       | o |
 * | i |       | r   r   r   r   r |           | r |       | o |
 * | i |       +-------+---+-------+           +---+       | o |
 * | i |               | x |                   | x |       | o |
 * +---+---------------+---+-----------+---+---+---+-------+---+
 *     | c   c   c   c   c   c   c   c   c   c   c   c   c |
 *     +---------------------------------------------------+
 *  A box with two boxes inside, one of them with another box:
 *   - A contains B and C
 *   - C contains D
 * </pre>
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Box implements Div {

    private final static Comparator<Boolean> TRUE_FIRST = (a, b) -> a == b ? 0 : a ? -1 : 1;
    private final static Comparator<Boolean> FALSE_FIRST = TRUE_FIRST.reversed();
    private static final Logger LOGGER = LoggerFactory.getLogger("reaction-converter");

    private int columns;
    private int rows;
    private CompartmentGlyph compartment;
    private LayoutIndex index;
    private Map<Integer, Map<Integer, Div>> divs = new HashMap<>();
    private double horizontalPadding;
    private double verticalPadding;
    private List<Box> boxes;
    private Box[][] boxOrder;

    Box(CompartmentGlyph compartment, LayoutIndex index) {
        this.compartment = compartment;
        this.index = index;
        placeSubCompartments();
    }

    private void placeSubCompartments() {
        // Placing sub compartments is a combination problem where we must minimize the number of restriction violation.
        // Restrictions are simple:
        //  - inputs must have always an x coordinate (column) lower than the reaction,
        //  - outputs an x coordinate greater than the reaction,
        //  - catalysts lower y coordinate and
        //  - regulators greater y coordinates
        // Derived restrictions are that inputs must have lower x than outputs, and catalysts lower y than regulators.
        // Also, inputs will have lower x than catalysts and regulators, outputs greater x than catalysts and regulator,
        // catalysts lower y than inputs and outputs and regulators greater y than inputs and outputs.
        // As we don't know where the reaction is, we use the derived restrictions.
        // To minimize the number of violations, we must place sub compartments in a grid.
        // Developing an 'n' compartments strategy is complex (doable, but complex). Instead, as we are going to usually
        // have no more than 3 compartments, we decided to hardcode it for 1 and 2 compartments. If there are more than
        // 2 compartments we use a top-down layout.
        final List<CompartmentGlyph> children = new ArrayList<>(compartment.getChildren());
        boxes = new ArrayList<>();
        for (final CompartmentGlyph child : children) boxes.add(new Box(child, index));
        if (boxes.isEmpty()) {
            columns = 3;
            rows = 3;
            boxOrder = new Box[0][0];
        } else if (boxes.size() == 1) {
            final Box box = boxes.get(0);
            columns = 4 + box.columns;
            rows = 4 + box.rows;
            set(2, 2, box);
            boxOrder = new Box[][]{{box}};
        } else if (boxes.size() == 2) {
            final Box a = boxes.get(0);
            final Box b = boxes.get(1);
            Place place = PlacePositioner.getRelativePosition(a.getBusyPlaces(), b.getBusyPlaces());
            if (place == null) {
                // place is null, b has all roles
                place = PlacePositioner.getRelativePosition(a.getBusyPlaces(), EnumSet.noneOf(Place.class));
            }
            if (place == null) {
                // we are doomed, both children have the 4 roles
                LOGGER.error(String.format("(%s) Compartments %s and %s cannot be placed side by side",
                        index.getReaction().getStId(), a.getCompartment().getName(), b.getCompartment().getName()));
                System.err.println("Incompatible siblings: " + compartment.getName());
                place = Place.LEFT;
            }
            if (place == Place.LEFT) placeHorizontal(a, b);
            else if (place == Place.RIGHT) placeHorizontal(b, a);
            else if (place == Place.TOP) placeVertical(a, b);
            else if (place == Place.BOTTOM) placeVertical(b, a);
        } else {
            // TODO: 04/12/18 go for the smart way, use a grid, you coward
            // Top down
            boxes.sort(Comparator
                    .comparing((Box c) -> c.getContainedRoles().contains(CATALYST), TRUE_FIRST)
                    .thenComparing(c -> c.getContainedRoles().contains(NEGATIVE_REGULATOR), FALSE_FIRST)
                    .thenComparing(c -> c.getContainedRoles().contains(POSITIVE_REGULATOR), FALSE_FIRST)
                    .thenComparingInt(c -> c.getChildren().size()));
            int mc = 0;
            int mr = 0;
            for (final Box box : boxes) {
                if (box.columns > mc) mc = box.columns;
                if (box.rows > mr) mr = box.rows;
            }
            for (int i = 0; i < boxes.size(); i++) {
                final Box box = boxes.get(i);
                box.columns = mc;
                box.rows = mr;
                set(2 + i * (mr + 1), 2, box);
            }
            columns = mc == 0 ? 3 : 4 + mc;
            rows = mr == 0 ? 3 : mr * boxes.size() + (boxes.size() - 1) + 4;
            boxOrder = new Box[boxes.size()][1];
            for (int i = 0; i < boxes.size(); i++) {
                boxOrder[i] = new Box[]{boxes.get(i)};
            }
        }
    }

    private void placeHorizontal(Box left, Box right) {
        columns = 5 + left.columns + right.columns;
        final int maxRows = Math.max(left.rows, right.rows);
        left.setNumberOfRows(maxRows);
        right.setNumberOfRows(maxRows);
        rows = 4 + maxRows;
        set(2, 2, left);
        set(2, 3 + left.columns, right);
        boxOrder = new Box[][]{{left, right}};
    }

    private void placeVertical(Box top, Box bottom) {
        rows = 5 + top.rows + bottom.rows;
        final int maxCols = Math.max(top.columns, bottom.columns);
        top.setNumberOfColumns(maxCols);
        bottom.setNumberOfColumns(maxCols);
        columns = 4 + maxCols;
        set(2, 2, top);
        set(3 + top.rows, 2, bottom);
        boxOrder = new Box[][]{{top}, {bottom}};
    }

    private void setNumberOfRows(int maxRows) {
        // If I already have this number of rows, I don't need to rearrange my children
        if (this.rows == maxRows) return;
        this.rows = maxRows;
        if (boxOrder.length == 0) return;
        final int length = boxOrder.length;
        final int spaces = length - 1;
        final int rowsPerBox = (rows - 4 - spaces) / length;
        assert (rows - 4 - spaces) % length == 0;
        int start = 2;
        for (int c = 0; c < boxOrder[0].length; c++) {
            for (int row = 0; row < length; row++) {
                int r = start;
                r += row;
                r += row * rowsPerBox;
                set(r, 2, boxOrder[row][c]);
                boxOrder[row][c].setNumberOfRows(rowsPerBox);
            }
        }
        // TODO: 17/01/19 as down
    }

    private void setNumberOfColumns(int columns) {
        if (this.columns == columns) return;
        this.columns = columns;
        if (boxOrder.length == 0) return;
        for (int r = 0; r < boxOrder.length; r++) {
            final Box[] row = boxOrder[r];
            // 1 how many horizontally placed children do I have?
            final int length = row.length;
            // 2 spaces between boxes
            final int spaces = length - 1;
            // 3 columns per element?
            final int colsPerBox = (columns - 4 - spaces) / length;
            // this should always be exact
            assert (columns - 4 - spaces) % length == 0;
            // resize every sub box
            int start = 2;
            for (int i = 0; i < length; i++) {
                int c = start;
                c += i; // number of before spaces
                c += i * colsPerBox;  // size of before boxes
                set(2, c, row[i]);
                // TODO: 17/01/19 the row may change in the future
                row[i].setNumberOfColumns(colsPerBox);
            }
        }

    }

    private void set(int row, int col, Div div) {
        divs.computeIfAbsent(row, r -> new HashMap<>()).put(col, div);
    }

    private void set(Point point, Div div) {
        set(point.getRow(), point.getCol(), div);
    }

    /**
     * Runs through the compartment tree, locates where the reaction is, places the reaction according to its
     * environment, and return the absolute coordinate
     *
     * @return the absolute coordinate where the reaction was placed
     */
    Point placeReaction() {
        // If the reaction is in this compartment, we calculate its position and return
        for (final Glyph glyph : compartment.getContainedGlyphs()) {
            if (glyph instanceof ReactionGlyph) {
                final Point reactionPosition = getReactionPosition();
                // this will happen if reaction must placed inside an inner compartment
                if (reactionPosition == null) break;
                final Div reactionDiv = new HorizontalLayout(Collections.singletonList(glyph));
                reactionDiv.setHorizontalPadding(Constants.HORIZONTAL_PADDING);
                reactionDiv.setVerticalPadding(Constants.VERTICAL_PADDING);
                set(reactionPosition, reactionDiv);
                return reactionPosition;
            }
        }
        // Otherwise, we search for the reaction in the list of children, when I get the position I add the position of
        // the child that contains the reaction to obtain the absolute position of the reaction
        final AtomicReference<Point> p = new AtomicReference<>();
        forEach((div, point) -> {
            if (div instanceof Box) {
                final Box box = (Box) div;
                final Point reactionPoint = box.placeReaction();
                if (reactionPoint != null)
                    p.set(point.add(reactionPoint));
            }
        });
        return p.get();
    }

    private Point getReactionPosition() {
        final List<Div> children = getChildren().stream().filter(Box.class::isInstance).collect(Collectors.toList());
        if (children.isEmpty()) {
            // center
            return new Point(rows / 2, columns / 2);
        }
        // 1 child: 4 available positions
        if (children.size() == 1) {
            final Place place = PlacePositioner.getRelativePosition(EnumSet.noneOf(Place.class), children.get(0).getBusyPlaces());
            if (place == Place.TOP) return new Point(1, columns / 2);
            if (place == Place.BOTTOM) return new Point(rows - 2, columns / 2);
            if (place == Place.LEFT) return new Point(rows / 2, 1);
            if (place == Place.RIGHT) return new Point(rows / 2, columns - 2);
            // null
            // move reaction to inner compartment
            moveReactionTo(children.get(0));
            return null;
        }
        // n children
        // 1) shrink available positions
        final AtomicInteger maxCol = new AtomicInteger(columns - 1);
        final AtomicInteger minCol = new AtomicInteger(1);
        final AtomicInteger minRow = new AtomicInteger(1);
        final AtomicInteger maxRow = new AtomicInteger(rows - 1);
        forEach((div, point) -> {
            if (div instanceof Box) {
                final Box box = (Box) div;
                final Collection<Place> allowed = PlacePositioner.getAllowances(div.getBusyPlaces());
                if (allowed.isEmpty()) {
                    moveReactionTo(children.get(0));
                    maxCol.set(-1);
                }
                if (!allowed.contains(Place.LEFT)) minCol.set(Math.max(minCol.get(), point.getCol()));
                if (!allowed.contains(Place.RIGHT)) maxCol.set(Math.min(maxCol.get(), point.getCol() + box.columns));
                if (!allowed.contains(Place.TOP)) minRow.set(Math.max(minRow.get(), point.getRow()));
                if (!allowed.contains(Place.BOTTOM)) maxRow.set(Math.min(maxRow.get(), point.getRow() + box.rows));
            }
        });
        // 2) find feasible position
        final AtomicReference<Point> rtn = new AtomicReference<>();
        forEach((div, point) -> {
            if (rtn.get() != null) return;
            if (div instanceof Box) {
                // In each box, try the 4 coordinates
                // this will check 1 point twice, so there is an improvement
                final Box box = (Box) div;
                for (Point p : Arrays.asList(
                        new Point(point.getRow() + box.rows / 2, point.getCol() - 1),  // left
                        new Point(point.getRow() + box.rows / 2, point.getCol() + box.columns),  // right
                        new Point(point.getRow() - 1, point.getCol() + box.columns / 2),   // top
                        new Point(point.getRow() + box.rows, point.getCol() + box.columns / 2)   // bottom
                )) {
                    if (minCol.get() <= p.getCol() && p.getCol() < maxCol.get()
                            && minRow.get() <= p.getRow() && p.getRow() < maxRow.get()) {
                        rtn.set(p);
                        break;
                    }
                }
            }
        });
        return rtn.get();
    }

    private void moveReactionTo(Div div) {
        final ReactionGlyph reaction = (ReactionGlyph) compartment.getContainedGlyphs().stream()
                .filter(ReactionGlyph.class::isInstance).findAny()
                .orElseThrow(RuntimeException::new);
        compartment.getContainedGlyphs().remove(reaction);
        div.getCompartment().getContainedGlyphs().add(reaction);
        LOGGER.info(String.format("(%s) Moving reaction to: %s", reaction.getStId(), div.getCompartment().getName()));
        reaction.setCompartment(compartment);
    }

    @Override
    public Bounds getBounds() {
        return null;
    }

    @Override
    public void setPadding(double padding) {
        verticalPadding = horizontalPadding = padding;
    }

    @Override
    public void setHorizontalPadding(double padding) {
        horizontalPadding = padding;
    }

    @Override
    public void setVerticalPadding(double padding) {
        verticalPadding = padding;
    }

    @Override
    public double getLeftPadding() {
        return 0;
    }

    @Override
    public void setLeftPadding(double padding) {

    }

    @Override
    public double getRightPadding() {
        return 0;
    }

    @Override
    public void setRightPadding(double padding) {

    }

    @Override
    public double getTopPadding() {
        return 0;
    }

    @Override
    public void setTopPadding(double padding) {

    }

    @Override
    public double getBottomPadding() {
        return 0;
    }

    @Override
    public void setBottomPadding(double padding) {

    }

    @Override
    public void center(double x, double y) {

    }

    @Override
    public void move(double dx, double dy) {

    }

    @Override
    public Collection<EntityRole> getContainedRoles() {
        final EnumSet<EntityRole> roles = EnumSet.noneOf(EntityRole.class);
        for (final Div div : getChildren()) {
            roles.addAll(div.getContainedRoles());
        }
        compartment.getContainedGlyphs().stream()
                .filter(EntityGlyph.class::isInstance)
                .flatMap(e -> ((EntityGlyph) e).getRoles().stream())
                .map(Role::getType)
                .forEach(roles::add);
        return roles;
    }

    @Override
    public Collection<Place> getBusyPlaces() {
        final EnumSet<Place> places = EnumSet.noneOf(Place.class);
        for (final Glyph glyph : compartment.getContainedGlyphs()) {
            places.addAll(places(glyph));
        }
        for (final Div child : getChildren())
            places.addAll(child.getBusyPlaces());
        return places;
    }

    private Collection<Place> places(Glyph glyph) {
        if (glyph instanceof ReactionGlyph)
            return EnumSet.of(Place.CENTER);
        final EntityGlyph entityGlyph = (EntityGlyph) glyph;
        return entityGlyph.getRoles().stream()
                .map(Role::getType)
                .map(PlacePositioner::getPlace)
                .collect(Collectors.toSet());
    }

    @Override
    public CompartmentGlyph getCompartment() {
        return compartment;
    }

    private Collection<Div> getChildren() {
        return divs.values().stream()
                .flatMap(map -> map.values().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void forEach(BiConsumer<Div, Point> function) {
        divs.forEach((row, map) -> map.forEach((col, div) -> function.accept(div, new Point(row, col))));
    }

    /**
     * Set the grid position of each element
     */
    void placeElements(Point reactionPosition) {
        // call sub compartments
        forEach((div, point) -> {
            if (div instanceof Box) {
                final Box box = (Box) div;
                final Point newPoint = new Point(reactionPosition.getRow() - point.getRow(), reactionPosition.getCol() - point.getCol());
                box.placeElements(newPoint);
            }
        });
        final Div[][] divs = getDivs();
        final List<EntityGlyph> inputs = index.filterInputs(compartment);
        final List<EntityGlyph> outputs = index.filterOutputs(compartment);
        final List<EntityGlyph> catalysts = index.filterCatalysts(compartment);
        final List<EntityGlyph> regulators = index.filterRegulators(compartment);

        if (inputs.size() > 0) {
            final boolean hasCatalyst = inputs.stream().anyMatch(entityGlyph -> hasRole(entityGlyph, CATALYST));
            final boolean catalystInInputs = index.getInputs().stream().anyMatch(entityGlyph -> hasRole(entityGlyph, CATALYST));
            int row;
            if (hasCatalyst) row = getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), true, false);
            else if (boxes.size() > 0 && reactionPosition.getCol() > columns / 2) {
                // These inputs will probably cause their segments to cross a child, so let's move to the bottom
                final Set<EntityRole> roles = boxes.stream().map(Box::getContainedRoles).map(PlacePositioner::simplify).flatMap(Collection::stream).collect(Collectors.toSet());
                if (!roles.contains(NEGATIVE_REGULATOR)) row = rows - 1;
                else if (!roles.contains(CATALYST)) row = 0;
                else if (catalystInInputs)
                    row = getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, true);
                else row = getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, false);
            } else if (catalystInInputs)
                row = getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, true);
            else row = getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, false);

            final VerticalLayout layout = new VerticalLayout(inputs);
            set(row, 0, layout);
        }
        if (outputs.size() > 0) {
            final boolean catalystInInputs = index.getInputs().stream().anyMatch(entityGlyph -> hasRole(entityGlyph, CATALYST));
            int row;
            if (boxes.size() > 0 && reactionPosition.getCol() < columns / 2) {
                final Set<EntityRole> roles = boxes.stream().map(Div::getContainedRoles).map(PlacePositioner::simplify).flatMap(Collection::stream).collect(Collectors.toSet());
                if (!roles.contains(NEGATIVE_REGULATOR)) row = rows - 1;
                else if (!roles.contains(CATALYST)) row = 0;
                else if (catalystInInputs)
                    row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.getRow(), false, true);
                else row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.getRow(), false, false);
            } else if (catalystInInputs) {
                row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.getRow(), false, true);
            } else row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.getRow(), false, false);


            final VerticalLayout layout = new VerticalLayout(outputs);
            set(row, columns - 1, layout);
        }
        if (catalysts.size() > 0) {
            final int column = getFreeColumn(divs, EnumSet.of(CATALYST), reactionPosition.getCol());
            final HorizontalLayout layout = new HorizontalLayout(catalysts);
            set(0, column, layout);
        }
        if (regulators.size() > 0) {
            final int column = getFreeColumn(divs, EnumSet.of(NEGATIVE_REGULATOR, POSITIVE_REGULATOR), reactionPosition.getCol());
            final HorizontalLayout layout = new HorizontalLayout(regulators);
            set(rows - 1, column, layout);
        }
    }

    private int getFreeRow(Div[][] divs, Collection<EntityRole> roles, int reactionRow, boolean up, boolean down) {
        // reaction is under this box
        if (reactionRow >= rows) return rows - 2;
            // reaction is on top of this compartment
        else if (reactionRow < 0) return 1;
        else if (reactionRow < rows) {
            if (rowIsFree(divs[reactionRow], roles)) return reactionRow;
        }
        // reaction is at the top of this compartment
        else if (reactionRow == 1) return 1;
        if (up) return getFreeRowUp(divs, roles, rows / 2);
        if (down) return getFreeRowDown(divs, roles, rows / 2);
        // By default, return the closest free row from the reaction row
        final int freeRowDown = getFreeRowDown(divs, roles, Math.max(reactionRow, 0));
        final int freeRowUp = getFreeRowUp(divs, roles, Math.min(reactionRow, rows - 1));
        if (reactionRow - freeRowUp > freeRowDown - reactionRow)
            return freeRowDown;
        else return freeRowUp;
    }

    private int getFreeRowUp(Div[][] divs, Collection<EntityRole> roles, int start) {
        for (int i = start; i > 1; i--) {
            if (rowIsFree(divs[i], roles)) return i;
        }
        return 1;
    }

    private int getFreeRowDown(Div[][] divs, Collection<EntityRole> roles, int start) {
        for (int i = start; i < rows - 1; i++) {
            if (rowIsFree(divs[i], roles)) return i;
        }
        return rows - 1;
    }

    private boolean rowIsFree(Div[] divs, Collection<EntityRole> roles) {
        for (int col = 0; col < columns; col++) {
            final Div div = divs[col];
            if (div != null && div.getContainedRoles().stream().anyMatch(roles::contains)) {
                return false;
            }
        }
        return true;
    }

    private int getFreeColumn(Div[][] divs, Collection<EntityRole> roles, int reactionColumn) {
        // reaction is right of this box
        if (reactionColumn >= columns) return columns - 2;
            // reaction is left of this compartment
        else if (reactionColumn < 0) return 1;
        else if (reactionColumn < columns) {
            if (!colIsBusy(divs, roles, reactionColumn)) return reactionColumn;
        }
        int col = columns / 2;
        for (int i = 0; i < columns - 2; i++) {
            final boolean left = i % 2 == 0;
            final int h = i / 2;
            col = left ? columns / 2 - h : columns / 2 + h;
            boolean busy = colIsBusy(divs, roles, col);
            if (!busy) break;
        }
        return col;
    }

    private boolean colIsBusy(Div[][] divs, Collection<EntityRole> roles, int col) {
        for (int row = 0; row < rows; row++) {
            final Div div = divs[row][col];
            if (div != null && div.getContainedRoles().stream().anyMatch(roles::contains)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final StringJoiner builder = new StringJoiner(", ");
        divs.forEach((row, map) -> map.forEach((column, div) -> {
            builder.add(String.format("(%d,%d)->%s", row, column, div));
        }));
        return String.format("%s(%d,%d)[%s]", initials(compartment.getName()), rows, columns, builder.toString());
    }

    Div[][] getDivs() {
        final Div[][] rtn = new Div[rows][columns];
        forEach((div, point) -> {
            if (div instanceof GlyphsLayout) rtn[point.getRow()][point.getCol()] = div;
            else if (div instanceof Box) {
                final Box box = (Box) div;
                final Div[][] divs = box.getDivs();
                for (int i = 0; i < box.rows; i++) {
                    System.arraycopy(divs[i], 0, rtn[point.getRow() + i], point.getCol(), box.columns);
                }
            }
        });
        return rtn;
    }

    @Override
    public Character getInitial() {
        return compartment.getInitial();
    }
}
