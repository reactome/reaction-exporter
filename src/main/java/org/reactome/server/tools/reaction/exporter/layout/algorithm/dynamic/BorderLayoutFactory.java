package org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic;

import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic.BorderLayout.Place.*;

/**
 * Places elements of a Layout into a BorderLayout.
 */
public class BorderLayoutFactory {

    private BorderLayoutFactory() {
    }

    public static BorderLayout get(Layout layout) {
        return getBorderLayout(layout.getCompartmentRoot());
    }

    private static BorderLayout getBorderLayout(CompartmentGlyph compartment) {
        // Don't be afraid of reading this method, it places content of compartment into a new BorderLayout.
        // It places participants in coordinates (NSEW). Every child is wrapped in its own BorderLayout, recursively
        // /-------+-------+-------\
        // |       |       |       |
        // |  NW   |   N   |   NE  |
        // |       |       |       |
        // +-------+-------+-------+
        // |       |       |       |
        // |   W   |   C   |   E   |
        // |       |       |       |
        // +-------+-------+-------+
        // |       |       |       |
        // |   SW  |   S   |   SE  |
        // |       |       |       |
        // \-------+-------+-------/
        final BorderLayout borderLayout = new BorderLayout();
        borderLayout.setCompartment(compartment);

        // This is the easy part, put every participant in its place
        for (Glyph glyph : compartment.getContainedGlyphs()) {
            if (glyph instanceof ReactionGlyph) borderLayout.add(CENTER, glyph);
            else if (glyph instanceof EntityGlyph) {
                final EntityGlyph entity = (EntityGlyph) glyph;
                final Set<EntityRole> roles = entity.getRoles().stream().map(Role::getType).collect(Collectors.toSet());
                if (roles.equals(EnumSet.of(EntityRole.INPUT))) {
                    borderLayout.add(WEST, glyph);
                } else if (roles.equals(EnumSet.of(EntityRole.CATALYST))) {
                    borderLayout.add(NORTH, glyph);
                } else if (roles.equals(EnumSet.of(EntityRole.OUTPUT))) {
                    borderLayout.add(EAST, glyph);
                } else if (roles.equals(EnumSet.of(EntityRole.POSITIVE_REGULATOR))) {
                    borderLayout.add(SOUTH, glyph);
                } else if (roles.equals(EnumSet.of(EntityRole.NEGATIVE_REGULATOR))) {
                    borderLayout.add(SOUTH, glyph);
                } else if (roles.equals(EnumSet.of(EntityRole.INPUT, EntityRole.CATALYST))) {
                    borderLayout.add(NORTH_WEST, glyph);
                } else if (roles.equals(EnumSet.of(EntityRole.INPUT, EntityRole.NEGATIVE_REGULATOR))) {
                    borderLayout.add(SOUTH_WEST, glyph);
                } else if (roles.equals(EnumSet.of(EntityRole.OUTPUT, EntityRole.CATALYST))) {
                    borderLayout.add(NORTH_EAST, glyph);
                } else if (roles.equals(EnumSet.of(EntityRole.OUTPUT, EntityRole.POSITIVE_REGULATOR))) {
                    borderLayout.add(SOUTH_EAST, glyph);
                }
            }
        }
        // Call subcompartments recursively
        for (CompartmentGlyph child : compartment.getChildren()) {
            addChild(borderLayout, getBorderLayout(child));
        }
        return borderLayout;
    }

    private static void addChild(BorderLayout parent, BorderLayout child) {
        // there are 9! * 9! combinations
        final Set<BorderLayout.Place> places = child.getBusyPlaces();
        if (places.equals(EnumSet.of(NORTH))) {
            merge(parent, child, NORTH, WEST, EAST);
        } else if (places.equals(EnumSet.of(SOUTH))) {
            merge(parent, child, SOUTH, WEST, EAST);
        } else if (places.equals(EnumSet.of(EAST))) {
            merge(parent, child, EAST, NORTH, SOUTH);
        } else if (places.equals(EnumSet.of(WEST))) {
            merge(parent, child, WEST, NORTH, SOUTH);
        } else if (places.containsAll(EnumSet.of(WEST, EAST))) {
            final BorderLayout aux = new BorderLayout();
            // clone parent
            for (final BorderLayout.Place place : BorderLayout.Place.values()) {
                aux.set(place, parent.get(place));
                parent.set(place, null);
            }
            parent.set(NORTH, aux);
            parent.set(SOUTH, child);
        } else if (places.containsAll(EnumSet.of(NORTH, SOUTH))) {
            // this one occupies the width of the image
            final BorderLayout aux = new BorderLayout();
            // clone parent
            for (final BorderLayout.Place place : BorderLayout.Place.values()) {
                aux.set(place, parent.get(place));
                parent.set(place, null);
            }
            parent.set(WEST, aux);
            parent.set(EAST, child);
        } else if (places.contains(WEST)) {
            merge(parent, child, CENTER, EAST, WEST);
        } else if (places.contains(NORTH)) {
            merge(parent, child, CENTER, SOUTH, NORTH);
        }
        else {
            merge(parent, child, CENTER, WEST, EAST);
        }
    }

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
        final BorderLayout content = parent.get(place);
        if (content == null) {
            parent.set(place, child);
        } else {
            final BorderLayout aux = new BorderLayout();
            aux.set(a, content);
            aux.set(b, child);
            parent.set(place, aux);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void placeSubBorderLayout(BorderLayout parent, BorderLayout child) {
        boolean n = false;
        boolean s = false;
        boolean e = false;
        boolean w = false;
        boolean c = false;
        if (!child.getGlyphs().isEmpty()) {
            for (Glyph glyph : child.getGlyphs()) {
                if (glyph instanceof ReactionGlyph) c = true;
                else if (glyph instanceof EntityGlyph) {
                    final EntityGlyph entityGlyph = (EntityGlyph) glyph;
                    for (Role role : entityGlyph.getRoles()) {
                        switch (role.getType()) {
                            case INPUT:
                                w = true;
                                break;
                            case OUTPUT:
                                e = true;
                                break;
                            case CATALYST:
                                n = true;
                                break;
                            case NEGATIVE_REGULATOR:
                            case POSITIVE_REGULATOR:
                                s = true;
                                break;
                        }
                    }
                }
            }
        } else {
            n = child.get(NORTH) != null;
            s = child.get(SOUTH) != null;
            e = child.get(EAST) != null;
            w = child.get(WEST) != null;
            c = child.get(CENTER) != null;
            if (child.get(NORTH_EAST) != null) n = e = true;
            if (child.get(NORTH_WEST) != null) n = w = true;
            if (child.get(SOUTH_EAST) != null) s = e = true;
            if (child.get(SOUTH_WEST) != null) s = w = true;
        }

        // MEGA SUPER KARNAUGH MAP (4 inputs -> 8 outputs)
        if (c) setOrMerge(parent, CENTER, child);
            // n s e w
            // 0 0 0 0 error
        else if (!n && !s && !e && !w) System.err.println("Whaaat");
            // 1 1 1 1 c
        else if (n && s && e && w) setOrMerge(parent, CENTER, child);
            // 0 1 0 0 s
        else if (!n && s && !e && !w) setOrMerge(parent, SOUTH, child);
            // 0 1 1 1 s
        else if (!n && s && e && w) setOrMerge(parent, SOUTH, child);
            // 1 0 1 1 n
        else if (n && !s && e && w) setOrMerge(parent, NORTH, child);
            // 1 0 0 0 n
        else if (n && !s && !e && !w) setOrMerge(parent, NORTH, child);
            // 0 0 0 1 w
        else if (!n && !s && !e && w) setOrMerge(parent, WEST, child);
            // 1 1 0 1 w
        else if (n && s && !e && w) setOrMerge(parent, WEST, child);
            // 1 1 1 0 e
        else if (n && s && e && !w) setOrMerge(parent, EAST, child);
            // 0 0 1 0 e
        else if (!n && !s && e && !w) setOrMerge(parent, EAST, child);
            // 0 1 0 1 sw
        else if (!n && s && !e && w) setOrMerge(parent, SOUTH_WEST, child);
            // 1 0 0 1 nw
        else if (n && !s && !e && w) setOrMerge(parent, NORTH_WEST, child);
            // 1 0 1 0 ne
        else if (n && !s && e && !w) setOrMerge(parent, NORTH_EAST, child);
            // 0 1 1 0 se
        else if (!n && s && e && !w) setOrMerge(parent, SOUTH_EAST, child);
            // 1 1 0 0 e (or w)
        else if (n && s && !e && !w) setOrMerge(parent, EAST, child);
            // 0 0 1 1 n (or s)
        else if (!n && !s && e && w) setOrMerge(parent, NORTH, child);
    }

    /**
     * If <em>parent</em> contains already a BorderLayout in position, creates a new BorderLayout containing the border
     * layout and child.
     */
    private static void setOrMerge(BorderLayout parent, BorderLayout.Place place, BorderLayout child) {
        final BorderLayout content = parent.get(place);
        if (content == null) {
            parent.set(place, child);
        } else {
            final BorderLayout auxBorderLayout = new BorderLayout();
            switch (place) {
                case NORTH:
                    auxBorderLayout.set(EAST, content);
                    auxBorderLayout.set(WEST, child);
                    break;
                case SOUTH:
                    auxBorderLayout.set(EAST, content);
                    auxBorderLayout.set(WEST, child);
                    break;
                case CENTER:
                    auxBorderLayout.set(NORTH, content);
                    auxBorderLayout.set(CENTER, child);
                    break;
                case EAST:
                    auxBorderLayout.set(NORTH, content);
                    auxBorderLayout.set(CENTER, child);
                    break;
                case WEST:
                    auxBorderLayout.set(NORTH, content);
                    auxBorderLayout.set(CENTER, child);
                    break;
                case SOUTH_WEST:
                    auxBorderLayout.set(WEST, content);
                    auxBorderLayout.set(SOUTH, child);
                    break;
                case NORTH_WEST:
                    auxBorderLayout.set(NORTH, content);
                    auxBorderLayout.set(WEST, child);
                    break;
                case SOUTH_EAST:
                    auxBorderLayout.set(SOUTH, content);
                    auxBorderLayout.set(EAST, child);
                    break;
                case NORTH_EAST:
                    auxBorderLayout.set(NORTH, content);
                    auxBorderLayout.set(EAST, child);
                    break;
            }
            parent.set(place, auxBorderLayout);
        }
    }
}
