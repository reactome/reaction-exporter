package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.text.WordUtils.initials;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.GlyphUtils.hasRole;

public class Box implements Div {

    /*
     * (i) inputs, (o) outputs, (c) catalysts, (r) regulators, (x) reaction
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
        // Top down, the only case we need a left right is if 2 subcompartments have both catalyst or regulators
        // TODO: 16/11/18 add horizontal layout
        children.sort(Comparator
                .comparing((CompartmentGlyph c) -> hasRole(c, CATALYST), TRUE_FIRST)
                .thenComparing(c -> hasRole(c, NEGATIVE_REGULATOR), FALSE_FIRST)
                .thenComparing(c -> hasRole(c, POSITIVE_REGULATOR), FALSE_FIRST));
        final List<Box> boxes = new ArrayList<>();
        for (final CompartmentGlyph child : children) boxes.add(new Box(child, index));
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
    }

    Point placeReaction() {
        for (final Glyph glyph : compartment.getContainedGlyphs()) {
            if (glyph instanceof ReactionGlyph) {
                return placeReaction((ReactionGlyph) glyph);
            }
        }
        final AtomicReference<Point> p = new AtomicReference<>();
        divs.forEach((row, map) -> map.forEach((col, div) -> {
            if (div instanceof Box) {
                final Box box = (Box) div;
                final Point point = box.placeReaction();
                if (point != null) p.set(new Point(point.x + col, point.y + row));
            }
        }));
        return p.get();
    }

    private Point placeReaction(ReactionGlyph reactionGlyph) {
        final EnumSet<EntityRole> childrenRoles = EnumSet.noneOf(EntityRole.class);
        for (final Map<Integer, Div> map : divs.values()) {
            for (final Div box : map.values()) {
                childrenRoles.addAll(box.getContainedRoles());
            }
        }
        int row;
        int col;
        final Div reactionDiv = new HorizontalLayout(Collections.singletonList(reactionGlyph));
        reactionDiv.setHorizontalPadding(100);
        reactionDiv.setVerticalPadding(60);
        if (childrenRoles.contains(POSITIVE_REGULATOR)) childrenRoles.add(NEGATIVE_REGULATOR);
        childrenRoles.remove(POSITIVE_REGULATOR);
        // CENTER
        if (childrenRoles.isEmpty()) {
            row = rows / 2;
            col = columns / 2;
        }
        // RIGHT
        else if (childrenRoles.equals(EnumSet.of(INPUT))
                || childrenRoles.equals(EnumSet.of(INPUT, CATALYST))
                || childrenRoles.equals(EnumSet.of(INPUT, NEGATIVE_REGULATOR))
                || childrenRoles.equals(EnumSet.of(INPUT, CATALYST, NEGATIVE_REGULATOR))
                || childrenRoles.equals(EnumSet.of(CATALYST, NEGATIVE_REGULATOR))) {
            row = rows / 2;
            col = columns - 2;
            // LEFT
        } else if (childrenRoles.equals(EnumSet.of(OUTPUT))
                || childrenRoles.equals(EnumSet.of(OUTPUT, CATALYST))
                || childrenRoles.equals(EnumSet.of(OUTPUT, NEGATIVE_REGULATOR))
                || childrenRoles.equals(EnumSet.of(OUTPUT, CATALYST, NEGATIVE_REGULATOR))) {
            row = rows / 2;
            col = 1;
            // BOTTOM
        } else if (childrenRoles.equals(EnumSet.of(CATALYST))
                || childrenRoles.equals(EnumSet.of(INPUT, OUTPUT))
                || childrenRoles.equals(EnumSet.of(CATALYST, INPUT, OUTPUT))) {
            // rule 22: the reaction can be set at the top if this compartment has a catalyst as a rol
            if (hasCatalyst()) {
                row = 1;
            } else {
                row = rows - 2;
            }
            col = columns / 2;
            // TOP
        } else if (childrenRoles.equals(EnumSet.of(NEGATIVE_REGULATOR))
                || childrenRoles.equals(EnumSet.of(NEGATIVE_REGULATOR, INPUT, OUTPUT))) {
            row = 1;
            col = columns / 2;
        } else {
            // the only remaining possibility is that children contain all of them, so there is no way to scape
            // so we insert reaction in the middle
            // WARNING: this will fall inside a subcompartment
            row = rows / 2;
            col = columns / 2;
        }
        set(row, col, reactionDiv);
        return new Point(col, row);
    }

    private boolean hasCatalyst() {
        return compartment.getContainedGlyphs().stream()
                .filter(EntityGlyph.class::isInstance)
                .flatMap(glyph -> ((EntityGlyph) glyph).getRoles().stream())
                .map(Role::getType)
                .anyMatch(CATALYST::equals);
    }

    private void set(int row, int col, Div div) {
        divs.computeIfAbsent(row, r -> new HashMap<>()).put(col, div);
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
        for (final Map<Integer, Div> map : divs.values()) {
            for (final Div box : map.values()) {
                roles.addAll(box.getContainedRoles());
            }
        }
        compartment.getContainedGlyphs().stream()
                .filter(EntityGlyph.class::isInstance)
                .flatMap(e -> ((EntityGlyph) e).getRoles().stream())
                .map(Role::getType)
                .forEach(roles::add);
        return roles;
    }

    /**
     * Set the grid position of each element
     */
    void placeElements(Point reactionPosition) {
        // call sub compartments
        divs.forEach((row, map) -> map.forEach((col, div) -> {
            if (div instanceof Box) {
                final Box box = (Box) div;
                final Point point = new Point(reactionPosition.x - col, reactionPosition.y - row);
                box.placeElements(point);
            }
        }));
        final Div[][] divs = getDivs();
        final List<EntityGlyph> inputs = index.filterInputs(compartment);
        final List<EntityGlyph> outputs = index.filterOutputs(compartment);
        final List<EntityGlyph> catalysts = index.filterCatalysts(compartment);
        final List<EntityGlyph> regulators = index.filterRegulators(compartment);

        if (inputs.size() > 0) {
            final int row = inputs.stream().anyMatch(entityGlyph -> hasRole(entityGlyph, CATALYST))
                    ? getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.y, true, false)
                    : getFreeRow(divs, EnumSet.of(INPUT), reactionPosition.y, false, true);
            final VerticalLayout layout = new VerticalLayout(inputs);
            layout.setLeftPadding(30);
            set(row, 0, layout);
        }
        if (outputs.size() > 0) {
            final int row = getFreeRow(divs, EnumSet.of(OUTPUT), reactionPosition.y, false, true);
            final VerticalLayout layout = new VerticalLayout(outputs);
            layout.setRightPadding(30); // space for compartment
            set(row, columns - 1, layout);
        }
        if (catalysts.size() > 0) {
            final int column = getFreeColumn(divs, EnumSet.of(CATALYST), reactionPosition.x);
            final HorizontalLayout layout = new HorizontalLayout(catalysts);
            layout.setTopPadding(30);
            set(0, column, layout);
        }
        if (regulators.size() > 0) {
            final int column = getFreeColumn(divs, EnumSet.of(NEGATIVE_REGULATOR, POSITIVE_REGULATOR), reactionPosition.x);
            final HorizontalLayout layout = new HorizontalLayout(regulators);
            layout.setBottomPadding(30);
            set(rows - 1, column, layout);
        }
    }

    private int getFreeRow(Div[][] divs, Collection<EntityRole> roles, int reactionRow, boolean up, boolean down) {
        // reaction is under this box
        if (reactionRow > rows) return rows - 2;
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
        if (reactionColumn > columns) return columns - 2;
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
        divs.forEach((row, map) -> map.forEach((col, div) -> {
            if (div instanceof GlyphsLayout) rtn[row][col] = div;
            else if (div instanceof Box) {
                final Box box = (Box) div;
                final Div[][] divs = box.getDivs();
                for (int i = 0; i < box.rows; i++) {
                    System.arraycopy(divs[i], 0, rtn[row + i], col, box.columns);
                }
            }
        }));
        return rtn;
    }

    @Override
    public CompartmentGlyph getCompartment() {
        return compartment;
    }
}
