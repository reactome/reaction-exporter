package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.*;

import static org.apache.commons.lang3.text.WordUtils.initials;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;

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
        for (final Glyph glyph : compartment.getContainedGlyphs()) {
            if (glyph instanceof ReactionGlyph) {
                placeReaction((ReactionGlyph) glyph);
            }
        }
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

    private boolean hasRole(CompartmentGlyph compartment, EntityRole role) {
        for (final Glyph glyph : compartment.getContainedGlyphs()) {
            if (glyph instanceof EntityGlyph) {
                final EntityGlyph entity = (EntityGlyph) glyph;
                for (final Role entityRole : entity.getRoles()) {
                    if (entityRole.getType() == role) return true;
                }
            }
        }
        for (final CompartmentGlyph child : compartment.getChildren()) {
            if (hasRole(child, role)) return true;
        }
        return false;
    }

    private void placeReaction(ReactionGlyph reactionGlyph) {
        final EnumSet<EntityRole> childrenRoles = EnumSet.noneOf(EntityRole.class);
        for (final Map<Integer, Div> map : divs.values()) {
            for (final Div box : map.values()) {
                childrenRoles.addAll(box.getContainedRoles());
            }
        }
        final Div reactionDiv = new HorizontalLayout(Collections.singletonList(reactionGlyph));
        reactionDiv.setHorizontalPadding(100);
        reactionDiv.setVerticalPadding(60);
        if (childrenRoles.isEmpty()) {
            // no children, place in the center
            divs.computeIfAbsent(1, i -> new HashMap<>()).put(1, reactionDiv);
            return;
        }
        if (childrenRoles.contains(POSITIVE_REGULATOR)) childrenRoles.add(NEGATIVE_REGULATOR);
        childrenRoles.remove(POSITIVE_REGULATOR);
        // RIGHT
        if (childrenRoles.equals(EnumSet.of(INPUT))
                || childrenRoles.equals(EnumSet.of(INPUT, CATALYST))
                || childrenRoles.equals(EnumSet.of(INPUT, NEGATIVE_REGULATOR))
                || childrenRoles.equals(EnumSet.of(INPUT, CATALYST, NEGATIVE_REGULATOR))
                || childrenRoles.equals(EnumSet.of(CATALYST, NEGATIVE_REGULATOR))) {
            set(rows / 2, columns - 2, reactionDiv);
            // LEFT
        } else if (childrenRoles.equals(EnumSet.of(OUTPUT))
                || childrenRoles.equals(EnumSet.of(OUTPUT, CATALYST))
                || childrenRoles.equals(EnumSet.of(OUTPUT, NEGATIVE_REGULATOR))
                || childrenRoles.equals(EnumSet.of(OUTPUT, CATALYST, NEGATIVE_REGULATOR))) {
            set(rows / 2, 1, reactionDiv);
            // BOTTOM
        } else if (childrenRoles.equals(EnumSet.of(CATALYST))
                || childrenRoles.equals(EnumSet.of(INPUT, OUTPUT))
                || childrenRoles.equals(EnumSet.of(CATALYST, INPUT, OUTPUT))) {
            // bottom center
            set(rows - 2, columns / 2, reactionDiv);
            // TOP
        } else if (childrenRoles.equals(EnumSet.of(NEGATIVE_REGULATOR))
                || childrenRoles.equals(EnumSet.of(NEGATIVE_REGULATOR, INPUT, OUTPUT))) {
            // top center
            set(1, columns / 2, reactionDiv);
        } else {
            // the only remaining possibility is that children contain all of them, so there is no way to scape
            // so we insert reaction in the middle
            // WARNING: this will fall inside a subcompartment
            set(rows / 2, columns / 2, reactionDiv);
        }
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
    void placeElements() {
        // call sub compartments
        for (final Map<Integer, Div> map : divs.values()) {
            for (final Div div : map.values()) {
                if (div instanceof Box) ((Box) div).placeElements();
            }
        }

        final Div[][] divs = getDivs();
        final List<EntityGlyph> inputs = index.filterInputs(compartment);
        final List<EntityGlyph> outputs = index.filterOutputs(compartment);
        final List<EntityGlyph> catalysts = index.filterCatalysts(compartment);
        final List<EntityGlyph> regulators = index.filterRegulators(compartment);

        // TODO: 16/11/18 shift when subboxes already contain role
        // TODO: 16/11/18 shift closer to reaction

        if (inputs.size() > 0) {
            int row = getFreeRow(divs, EnumSet.of(INPUT));
            final VerticalLayout layout = new VerticalLayout(inputs);
            layout.setLeftPadding(30);
            set(row, 0, layout);
        }
        if (outputs.size() > 0) {
            int row = getFreeRow(divs, EnumSet.of(OUTPUT));
            final VerticalLayout layout = new VerticalLayout(outputs);
            layout.setRightPadding(30); // space for compartment
            set(row, columns - 1, layout);
        }
        if (catalysts.size() > 0) {
            final HorizontalLayout layout = new HorizontalLayout(catalysts);
            layout.setTopPadding(30);
            set(0, getFreeColumn(divs, EnumSet.of(CATALYST)), layout);
        }
        if (regulators.size() > 0) {
            final HorizontalLayout layout = new HorizontalLayout(regulators);
            layout.setBottomPadding(30);
            set(rows - 1, getFreeColumn(divs, EnumSet.of(NEGATIVE_REGULATOR, POSITIVE_REGULATOR)), layout);
        }
    }

    private int getFreeRow(Div[][] divs, Collection<EntityRole> roles) {
        int row = rows / 2;
        for (int i = 0; i < rows - 2; i++) {
            final boolean up = i % 2 == 0;
            final int h = i / 2;
            row = up ? rows / 2 - h : rows / 2 + h;
            boolean busy = false;
            for (int col = 0; col < columns; col++) {
                final Div div = divs[row][col];
                if (div != null && div.getContainedRoles().stream().anyMatch(roles::contains)) {
                    busy = true;
                    break;
                }
            }
            if (!busy) return row;
        }
        return row;
    }

    private int getFreeColumn(Div[][] divs, Collection<EntityRole> roles) {
        int col = columns / 2;
        for (int i = 0; i < columns - 2; i++) {
            final boolean left = i % 2 == 0;
            final int h = i / 2;
            col = left ? columns / 2 - h : columns / 2 + h;
            boolean busy = false;
            for (int row = 0; row < rows; row++) {
                final Div div = divs[row][col];
                if (div != null && div.getContainedRoles().stream().anyMatch(roles::contains)) {
                    busy = true;
                    break;
                }
            }
            if (!busy) break;
        }
        return col;
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


}
