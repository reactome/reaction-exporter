package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

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
 */
public class Box implements Div {

    private final static Comparator<Boolean> TRUE_FIRST = (a, b) -> a == b ? 0 : a ? -1 : 1;
    private final static Comparator<Boolean> FALSE_FIRST = TRUE_FIRST.reversed();

    private int columns;
    private int rows;
    private CompartmentGlyph compartment;
    private LayoutIndex index;
    private Map<Integer, Map<Integer, Div>> divs = new HashMap<>();
    private double horizontalPadding;
    private double verticalPadding;

    Box(CompartmentGlyph compartment, LayoutIndex index) {
        this.compartment = compartment;
        this.index = index;
        placeSubCompartments();
    }

    private void placeSubCompartments() {
        // Placing sub compartments is a combination problem where we must minimize the number of restriction violation.
        // Restrictions are simple:
        //  - inputs have to have always an x coordinate (column) lower than the reaction,
        //  - outputs an x coordinate greater than the reaction,
        //  - catalysts lower y coordinate and
        //  - regulators greater y coordinates
        // Derived restrictions are that inputs must have lower x than outputs, and catalysts lower y than regulators.
        // As we don't know where the reaction is, we use the derived restrictions.
        // To minimize the number of violations, we must place sub compartments in a grid.
        // Developing an 'n' compartments strategy is complex (doable, but complex). Instead, as we are going to usually
        // have no more than 3 compartments, we decided to hardcode it for 1 and 2 compartments. If there are more than
        // 2 compartments we use a top-down layout.
        final List<CompartmentGlyph> children = new ArrayList<>(compartment.getChildren());
        final List<Box> boxes = new ArrayList<>();
        for (final CompartmentGlyph child : children) boxes.add(new Box(child, index));
        if (boxes.size() == 1) {
            final Box box = boxes.get(0);
            columns = 4 + box.columns;
            rows = 4 + box.rows;
            set(2, 2, box);
        } else if (boxes.size() == 2) {
            // This method will give us the relative position of the 2 boxes.
            Place place = PlacePositioner.haggle(boxes.get(0).getContainedRoles(), boxes.get(1).getContainedRoles());
            if (place == null) {
                // place is null, boxes.get(1) has all roles
                place = PlacePositioner.haggle(boxes.get(0).getContainedRoles(), EnumSet.noneOf(EntityRole.class));
            }
            if (place == null) {
                // we are doomed, both children have the 4 roles
                System.err.println("Incompatible siblings: " + compartment.getName());
                place = Place.LEFT;
            }
            if (place == Place.LEFT) {
                placeHorizontal(boxes.get(0), boxes.get(1));
            } else if (place == Place.RIGHT) {
                placeHorizontal(boxes.get(1), boxes.get(0));
            } else if (place == Place.TOP) {
                placeVertical(boxes.get(0), boxes.get(1));
            } else if (place == Place.BOTTOM) {
                placeVertical(boxes.get(1), boxes.get(0));
            }
        } else if (boxes.size() > 2) {
            // TODO: 04/12/18 go for the smart way, use a grid, you coward
            // Top down
            boxes.sort(Comparator
                    .comparing((Box c) -> c.getContainedRoles().contains(CATALYST), TRUE_FIRST)
                    .thenComparing(c -> c.getContainedRoles().contains(NEGATIVE_REGULATOR), FALSE_FIRST)
                    .thenComparing(c -> c.getContainedRoles().contains(POSITIVE_REGULATOR), FALSE_FIRST));
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
        } else {
            columns = 3;
            rows = 3;
        }

    }

    private void placeHorizontal(Box left, Box right) {
        columns = 5 + left.columns + right.columns;
        final int maxRows = Math.max(left.rows, right.rows);
        left.rows = right.rows = maxRows;
        rows = 4 + maxRows;
        set(2, 2, left);
        set(2, 3 + left.columns, right);
    }

    private void placeVertical(Box top, Box bottom) {
        rows = 5 + top.rows + bottom.rows;
        final int maxCols = Math.max(top.columns, bottom.columns);
        top.columns = bottom.columns = maxCols;
        columns = 4 + maxCols;
        set(2, 2, top);
        set(3 + top.rows, 2, bottom);
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
                reactionDiv.setHorizontalPadding(100);
                reactionDiv.setVerticalPadding(40);
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
            final Place place = PlacePositioner.haggle(EnumSet.noneOf(EntityRole.class), children.get(0).getContainedRoles());
            if (place == Place.TOP) return new Point(1, columns / 2);
            if (place == Place.BOTTOM) return new Point(rows - 2, columns / 2);
            if (place == Place.LEFT) return new Point(rows / 2, 1);
            if (place == Place.RIGHT) return new Point(rows / 2, columns - 2);
            // null
            // move reaction to inner compartment
            // TODO: 03/12/18 add a warning log
            final ReactionGlyph reaction = (ReactionGlyph) compartment.getContainedGlyphs().stream()
                    .filter(ReactionGlyph.class::isInstance).findAny()
                    .orElseThrow(RuntimeException::new);
            compartment.getContainedGlyphs().remove(reaction);
            children.get(0).getCompartment().getContainedGlyphs().add(reaction);
            System.err.printf("Moving reaction to: %s (%s)%n", children.get(0).getCompartment().getName(), reaction.getStId());
            reaction.setCompartment(compartment);
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
                final Collection<Place> allowed = PlacePositioner.getAllowed(div.getContainedRoles());
                if (allowed.isEmpty()) {
                    // TODO: 03/12/18 add a warning log
                    final ReactionGlyph reaction = (ReactionGlyph) compartment.getContainedGlyphs().stream()
                            .filter(ReactionGlyph.class::isInstance).findAny()
                            .orElseThrow(RuntimeException::new);
                    System.err.printf("Moving reaction to: %s (%s)%n", children.get(0).getCompartment().getName(), reaction.getStId());
                    compartment.getContainedGlyphs().remove(reaction);
                    children.get(0).getCompartment().getContainedGlyphs().add(reaction);
                    reaction.setCompartment(compartment);
                    maxCol.set(-1);
                }
                if (!allowed.contains(Place.LEFT)) minCol.set(Math.max(minCol.get(), point.getCol()));
                if (!allowed.contains(Place.RIGHT)) maxCol.set(Math.min(maxCol.get(), point.getCol() + box.columns));
                if (!allowed.contains(Place.TOP)) minRow.set(Math.max(minRow.get(), point.getRow()));
                if (!allowed.contains(Place.BOTTOM)) maxRow.set(Math.min(maxRow.get(), point.getRow() + box.rows));
            }
        });
        // 2) find feasible position
        final AtomicReference<Point> p = new AtomicReference<>();
        forEach((div, point) -> {
            if (p.get() != null) return;
            // In each box, try the 4 coordinates
            // this will check 1 point twice, so there is an improvement
            if (div instanceof Box) {
                final Box box = (Box) div;
                // left
                int col = point.getCol() - 1;
                int row = point.getRow() + box.rows / 2;
                if (minCol.get() <= col && col < maxCol.get() && minRow.get() <= row && row < maxRow.get()) {
                    p.set(new Point(row, col));
                    return;
                }
                // right
                col = point.getCol() + box.columns;
                row = point.getRow() + box.rows / 2;
                if (minCol.get() <= col && col < maxCol.get() && minRow.get() <= row && row < maxRow.get()) {
                    p.set(new Point(row, col));
                    return;
                }
                // top
                col = point.getCol() + box.columns / 2;
                row = point.getRow() - 1;
                if (minCol.get() <= col && col <= maxCol.get() && minRow.get() <= row && row <= maxRow.get()) {
                    p.set(new Point(row, col));
                    return;
                }
                // bottom
                col = point.getCol() + box.columns / 2;
                row = point.getRow() + box.rows;
                if (minCol.get() <= col && col <= maxCol.get() && minRow.get() <= row && row <= maxRow.get()) {
                    p.set(new Point(row, col));
                }
            }
        });
        return p.get();
    }

    @Override
    public Position getBounds() {
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

    private Collection<Div> getChildren() {
        return divs.values().stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());
    }

    @Override
    public CompartmentGlyph getCompartment() {
        return compartment;
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
            else if (getChildren().size() > 0 && reactionPosition.getCol() > columns / 2) {
                // This inputs will probably cause their segments to cross a child, so let's move to the bottom
                final Set<EntityRole> roles = getChildren().stream().map(Div::getContainedRoles).map(PlacePositioner::simplify).flatMap(Collection::stream).collect(Collectors.toSet());
                if (!roles.contains(NEGATIVE_REGULATOR)) row = rows - 1;
                else if (!roles.contains(CATALYST)) row = 0;
                else if (catalystInInputs)
                    row = getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, true);
                else row = getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, false);
            } else if (catalystInInputs)
                row = getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, true);
            else row = getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, false);

            final VerticalLayout layout = new VerticalLayout(inputs);
            layout.setLeftPadding(30);
            set(row, 0, layout);
        }
        if (outputs.size() > 0) {
            final boolean catalystInInputs = index.getInputs().stream().anyMatch(entityGlyph -> hasRole(entityGlyph, CATALYST));
            int row;
            if (getChildren().size() > 0 && reactionPosition.getCol() < columns / 2) {
                final Set<EntityRole> roles = getChildren().stream().map(Div::getContainedRoles).map(PlacePositioner::simplify).flatMap(Collection::stream).collect(Collectors.toSet());
                if (!roles.contains(NEGATIVE_REGULATOR)) row = rows - 1;
                else if (!roles.contains(CATALYST)) row = 0;
                else if (catalystInInputs)
                    row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.getRow(), false, true);
                else row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.getRow(), false, false);
            } else if (catalystInInputs) {
                row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.getRow(), false, true);
            } else row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.getRow(), false, false);


            final VerticalLayout layout = new VerticalLayout(outputs);
            layout.setRightPadding(30); // space for compartment
            set(row, columns - 1, layout);
        }
        if (catalysts.size() > 0) {
            final int column = getFreeColumn(divs, EnumSet.of(CATALYST), reactionPosition.getCol());
            final HorizontalLayout layout = new HorizontalLayout(catalysts);
            layout.setTopPadding(30);
            set(0, column, layout);
        }
        if (regulators.size() > 0) {
            final int column = getFreeColumn(divs, EnumSet.of(NEGATIVE_REGULATOR, POSITIVE_REGULATOR), reactionPosition.getCol());
            final HorizontalLayout layout = new HorizontalLayout(regulators);
            layout.setBottomPadding(30);
            set(rows - 1, column, layout);
        }
    }

    private int getFreeRow(Div[][] divs, Collection<EntityRole> roles, int reactionRow, boolean up, boolean down) {
        // reaction is under this box
        if (reactionRow >= rows) return rows - 2;
            // reaction is on top of this compartment
        else if (reactionRow < 0) return 1;
        else if (reactionRow < rows) {
            if (!rowIsBusy(divs[reactionRow], roles)) return reactionRow;
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
            if (!rowIsBusy(divs[i], roles)) return i;
        }
        return 1;
    }

    private int getFreeRowDown(Div[][] divs, Collection<EntityRole> roles, int start) {
        for (int i = start; i < rows - 1; i++) {
            if (!rowIsBusy(divs[i], roles)) return i;
        }
        return rows - 1;
    }

    private int getFreeRow(Div[][] divs, Collection<EntityRole> roles, int reactionRow) {
        int row = rows / 2; // by default, center
        for (int i = 0; i < rows - 2; i++) {
            final boolean up = i % 2 == 1;  // first try down
            final int h = i / 2;
            row = up ? rows / 2 - h : rows / 2 + h;
            boolean busy = rowIsBusy(divs[row], roles);
            if (!busy) return row;
        }
        return row;
    }

    private boolean rowIsBusy(Div[] divs, Collection<EntityRole> roles) {
        for (int col = 0; col < columns; col++) {
            final Div div = divs[col];
            if (div != null && div.getContainedRoles().stream().anyMatch(roles::contains)) {
                return true;
            }
        }
        return false;
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
}
