package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.GlyphUtils;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.text.WordUtils.initials;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.GlyphUtils.hasRole;

public class Box implements Div {

    /*
     * (i) inputs, (o) outputs, (c) catalysts, (r) regulators, (x) reaction
     *     +---+
     *     | c |
     * +---+---+---+
     * | i | x | o |
     * +---+---+---+
     *     | r |
     *     +---+
     *  a single compartment, left to right approach
     *
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
     * a compartment inside a compartment. size of inner compartments is 3,
     * for every external compartment we add extra 4 columns and rows
     *
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
     * if this compartment has more than 1 subcompartment, then a space is added between subcompartments,
     * suitable for adding the reaction
     */

    private final static Comparator<Boolean> TRUE_FIRST = (a, b) -> a == b ? 0 : a ? -1 : 1;
    private final static Comparator<Boolean> FALSE_FIRST = TRUE_FIRST.reversed();

    private int columns;
    private int rows;
    private CompartmentGlyph compartment;
    private LayoutIndex index;
    private Map<Integer, Map<Integer, Div>> divs = new HashMap<>();
    private double horizontalPadding;
    private double verticalPadding;

    public Box(CompartmentGlyph compartment, LayoutIndex index) {
        this.compartment = compartment;
        this.index = index;
        placeSubCompartments();
    }

    private void placeSubCompartments() {
        final List<CompartmentGlyph> children = new ArrayList<>(compartment.getChildren());
        // I can do this because there are never more than 3 sub compartments
        final List<Box> boxes = new ArrayList<>();
        for (final CompartmentGlyph child : children) boxes.add(new Box(child, index));
        if (boxes.size() == 1) {
            final Box box = boxes.get(0);
            columns = 4 + box.columns;
            rows = 4 + box.rows;
            set(2, 2, box);
        } else if (boxes.size() == 2) {
            final Place place = PlacePositioner.haggle(boxes.get(0).getContainedRoles(), boxes.get(1).getContainedRoles());
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
            // Top down, the only case we need a left right is if 2 subcompartments have both catalyst or regulators
            // TODO: 16/11/18 add horizontal layout
            children.sort(Comparator
                    .comparing((CompartmentGlyph c) -> hasRole(c, CATALYST), TRUE_FIRST)
                    .thenComparing(c -> hasRole(c, NEGATIVE_REGULATOR), FALSE_FIRST)
                    .thenComparing(c -> hasRole(c, POSITIVE_REGULATOR), FALSE_FIRST));
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

    // TODO: 30/11/18 this is extremely hard
    // private void subCompartments() {
    //     final List<CompartmentGlyph> children = new ArrayList<>(compartment.getChildren());
    //     final List<Box> boxes = new ArrayList<>();
    //     for (final CompartmentGlyph child : children) boxes.add(new Box(child, index));
    //     final List<List<Box>> boxGrid = getBoxGrid(boxes);
    //
    // }
    //
    // private List<List<Box>> getBoxGrid(List<Box> boxes) {
    //     //     |   3
    //     //     | 2 1 5 4
    //     // ----|---------
    //     // 1 2 | 2 1
    //     // 3   |   3
    //     // 4 5 |     5 4
    //     final List<List<Box>> horizontalGroups = sortAndGroup(boxes, PlacePositioner.HORIZONTAL);
    //     final List<List<Box>> verticalGroups = sortAndGroup(boxes, PlacePositioner.VERTICAL);
    //     final List<List<Box>>
    //     return null;
    // }
    //
    // private List<List<Box>> sortAndGroup(List<Box> boxes, Comparator<Box> comparator) {
    //     if (boxes.isEmpty()) return Collections.emptyList();
    //     boxes.sort(comparator);
    //     final List<List<Box>> groups = new ArrayList<>();
    //     List<Box> list = new ArrayList<>();
    //     groups.add(list);
    //     list.add(boxes.get(0));
    //     for (int i = 1; i < boxes.size(); i++) {
    //         if (comparator.compare(boxes.get(i - 1), boxes.get(i)) == 0) {
    //             list.add(boxes.get(i));
    //         } else {
    //             list = new ArrayList<>();
    //             list.add(boxes.get(i));
    //         }
    //     }
    //     return groups;
    // }


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
        for (final Glyph glyph : compartment.getContainedGlyphs()) {
            if (glyph instanceof ReactionGlyph) {
                final Point reactionPosition = getReactionPosition();
                final Div reactionDiv = new HorizontalLayout(Collections.singletonList(glyph));
                reactionDiv.setHorizontalPadding(100);
                reactionDiv.setVerticalPadding(60);
                set(reactionPosition, reactionDiv);
                return reactionPosition;
            }
        }
        final AtomicReference<Point> p = new AtomicReference<>();

        forEach((div, point) -> {
            if (div instanceof Box) {
                final Box box = (Box) div;
                final Point reactionPoint = box.placeReaction();
                if (reactionPoint != null)
                    p.set(new Point(point.getRow() + reactionPoint.getRow(), point.getCol() + reactionPoint.getCol()));
            }
        });
        return p.get();
    }

    private Point getReactionPosition() {
        // Ok, first of all, where can I place the reaction?
        //  1) any of the four borders
        //  2) if I contain more than 1 child, in the middle of two of them
        final Collection<Div> children = getChildren().stream().filter(Box.class::isInstance).collect(Collectors.toList());
        if (children.isEmpty()) {
            // center
            return new Point(rows / 2, columns / 2);
        }
        boolean left = true;
        boolean right = true;
        boolean top = true;
        boolean bottom = true;

        for (final CompartmentGlyph child : compartment.getChildren()) {
            final Collection<EntityRole> roles = GlyphUtils.getContainedRoles(child);
            if (roles.contains(INPUT)) left = false;
            if (roles.contains(OUTPUT)) right = false;
            if (roles.contains(CATALYST)) top = false;
            if (roles.contains(POSITIVE_REGULATOR) || roles.contains(NEGATIVE_REGULATOR)) bottom = false;
        }
        // TODO: 29/11/18 generalize to n children
        if (children.size() == 2) {
            return new Point(rows / 2, columns / 2);
        } else {
            if (left) return new Point(rows / 2, 1);
            if (right) return new Point(rows / 2, columns - 2);
            if (top) return new Point(1, columns / 2);
            if (bottom) return new Point(rows - 2, columns / 2);
        }
        return new Point(rows / 2, columns / 2);
    }

    private boolean hasCatalyst() {
        return compartment.getContainedGlyphs().stream()
                .filter(EntityGlyph.class::isInstance)
                .flatMap(glyph -> ((EntityGlyph) glyph).getRoles().stream())
                .map(Role::getType)
                .anyMatch(CATALYST::equals);
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
            final int row = hasCatalyst
                    ? getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), true, false)
                    : catalystInInputs
                    ? getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, true)
                    : getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.getRow(), false, false);
            final VerticalLayout layout = new VerticalLayout(inputs);
            layout.setLeftPadding(30);
            set(row, 0, layout);
        }
        if (outputs.size() > 0) {
            final int row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.getRow(), false, false);
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
