package org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic;

import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic.BorderLayout.Place.*;

/**
 * Places elements of a Layout into a BorderLayout.
 */
public class BorderLayoutFactory {

    private BorderLayoutFactory() {
    }

    public static BorderLayout get(Layout layout, LayoutIndex index) {
        return getBorderLayout(layout, layout.getCompartmentRoot(), index);
    }

    private static BorderLayout getBorderLayout(Layout layout, CompartmentGlyph compartment, LayoutIndex index) {
        final BorderLayout borderLayout = new BorderLayout();
        borderLayout.setCompartment(compartment);

        final List<EntityGlyph> inputs = index.filterInputs(compartment);
        final List<EntityGlyph> biRole = inputs.stream().filter(entityGlyph -> entityGlyph.getRoles().stream().anyMatch(role -> role.getType() == EntityRole.CATALYST)).collect(Collectors.toList());
        final List<EntityGlyph> outputs = index.filterOutputs(compartment);
        final List<EntityGlyph> catalysts = index.filterCatalysts(compartment);
        final List<EntityGlyph> regulators = index.filterRegulators(compartment);
        if (!inputs.isEmpty()) borderLayout.set(LEFT, new VerticalLayout(inputs));
        if (!outputs.isEmpty()) borderLayout.set(RIGHT, new VerticalLayout(outputs));
        if (!catalysts.isEmpty()) borderLayout.set(TOP, new HorizontalLayout(catalysts));
        if (!regulators.isEmpty()) borderLayout.set(BOTTOM, new HorizontalLayout(regulators));
        final boolean hasReaction = layout.getReaction().getCompartment() == compartment;
        if (hasReaction) {
            final HorizontalLayout reactionLayout = new HorizontalLayout(Collections.singletonList(layout.getReaction()));
            reactionLayout.setHorizontalPadding(100);
            reactionLayout.setVerticalPadding(60);
            borderLayout.set(CENTER, reactionLayout);
        }
        for (final CompartmentGlyph child : compartment.getChildren()) {
            addChild(borderLayout, getBorderLayout(layout, child, index));
        }
        return borderLayout;
    }

    private static void addChild(BorderLayout parent, BorderLayout child) {
        final Set<BorderLayout.Place> places = child.getBusyPlaces();
        if (places.contains(CENTER)) {
            // Center is always available for reaction
            parent.set(CENTER, child);
        }
        // When we merge, we are adding a whole compartment, see below for the bit table
        if (places.equals(EnumSet.of(TOP))) {
            merge(parent, child, TOP, LEFT, RIGHT);
        } else if (places.equals(EnumSet.of(BOTTOM))) {
            merge(parent, child, BOTTOM, TOP, BOTTOM);
        } else if (places.equals(EnumSet.of(RIGHT))) {
            merge(parent, child, RIGHT, TOP, BOTTOM);
        } else if (places.equals(EnumSet.of(LEFT))) {
            merge(parent, child, LEFT, TOP, BOTTOM);
        } else if (places.equals(EnumSet.of(TOP, BOTTOM))) {
            merge(parent, child, RIGHT, RIGHT, LEFT);
        } else if (places.equals(EnumSet.of(TOP, RIGHT))) {
            merge(parent, child, RIGHT, BOTTOM, TOP);
        } else if (places.equals(EnumSet.of(TOP, LEFT))) {
            merge(parent, child, LEFT, BOTTOM, TOP);
        } else if (places.equals(EnumSet.of(BOTTOM, RIGHT))) {
            merge(parent, child, RIGHT, TOP, BOTTOM);
        } else if (places.equals(EnumSet.of(BOTTOM, LEFT))) {
            merge(parent, child, LEFT, TOP, BOTTOM);
        } else if (places.equals(EnumSet.of(RIGHT, LEFT))) {
            merge(parent, child, BOTTOM, BOTTOM, TOP);
        } else if (places.equals(EnumSet.of(TOP, BOTTOM, RIGHT))) {
            merge(parent, child, RIGHT, RIGHT, LEFT);
        } else if (places.equals(EnumSet.of(TOP, BOTTOM, LEFT))) {
            merge(parent, child, LEFT, LEFT, RIGHT);
        } else if (places.equals(EnumSet.of(TOP, RIGHT, LEFT))) {
            merge(parent, child, TOP, TOP, BOTTOM);
        } else if (places.equals(EnumSet.of(BOTTOM, RIGHT, LEFT))) {
            merge(parent, child, BOTTOM, TOP, BOTTOM);
        } else if (places.equals(EnumSet.of(TOP, BOTTOM, RIGHT, LEFT))) {
            // * This is the hardest case. By now we will use TOP, but we should use any available, when possible
            merge(parent, child, TOP, BOTTOM, TOP);
        } // last else is None, in that case we do nothing
    }
    // /---+---+---\
    // |   | n |   |
    // +---+---+---+
    // | w | c | e |
    // +---+---+---+
    // |   | s |   |
    // \---+---+---/
    //  child  | parent |     sides
    // --------|--------|--------------
    // n s e w |        | parent child
    // --------|--------|--------------
    // 0 0 0 0 |    -   |      -
    // 0 0 0 1 |    w   |     n s
    // 0 0 1 0 |    e   |     n s
    // 0 0 1 1 |    s   |     s n (max horizontal padding)
    // 0 1 0 0 |    s   |     e w (n s is also valid)
    // 0 1 0 1 |    w   |     n s
    // 0 1 1 0 |    e   |     n s
    // 0 1 1 1 |    s   |     n s (max horizontal padding)
    // 1 0 0 0 |    n   |     e w (n s is also valid)
    // 1 0 0 1 |    w   |     s n
    // 1 0 1 0 |    e   |     s n
    // 1 0 1 1 |    n   |     n s
    // 1 1 0 0 |    e   |     e w (max vertical padding)
    // 1 1 0 1 |    w   |     w e
    // 1 1 1 0 |    e   |     e w
    // 1 1 1 1 |    n*  |     s n
    // *an inner compartment has all roles' participants, we should use any coordinate available and put the element
    // as close to the center as possible


    /**
     * Places child into place in parent.
     *
     * @param parent border layout where child will be inserted
     * @param child  border layout to insert into parent
     * @param place  place where we want to put child
     * @param a      where to put the content that is in place in parent
     * @param b      where to put child if parent already has content in place
     */
    private static void merge(BorderLayout parent, BorderLayout child, BorderLayout.Place place, BorderLayout.Place a, BorderLayout.Place b) {
        final Div content = parent.get(place);
        if (content == null) {
            parent.set(place, child);
        } else {
            final BorderLayout aux = new BorderLayout();
            aux.set(a, content);
            aux.set(b, child);
            parent.set(place, aux);
        }
    }

}
