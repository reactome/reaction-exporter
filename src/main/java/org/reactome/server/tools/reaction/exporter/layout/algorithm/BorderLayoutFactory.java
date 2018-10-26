package org.reactome.server.tools.reaction.exporter.layout.algorithm;

import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.BorderLayout.Place.*;

public class BorderLayoutFactory {

    private BorderLayoutFactory() {
    }

    public static BorderLayout get(Layout layout) {
        return getBorderLayout(layout.getCompartmentRoot());
    }

    private static BorderLayout getBorderLayout(CompartmentGlyph compartment) {
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
        for (Glyph glyph : compartment.getContainedGlyphs())
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

        final List<BorderLayout> layouts = new ArrayList<>();
        for (CompartmentGlyph child : compartment.getChildren()) layouts.add(getBorderLayout(child));

        for (BorderLayout child : layouts) {
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
                                    s = true;
                                    break;
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
                if (child.get(NORTH_EAST) != null) n = e = true;
                if (child.get(NORTH_WEST) != null) n = w = true;
                if (child.get(SOUTH_EAST) != null) s = e = true;
                if (child.get(SOUTH_WEST) != null) s = w = true;
            }

            // MEGA SUPER KARNAUGH MAP (4 inputs -> 8 outputs)
            // n s e w
            // 0 0 0 0 error
            if (c) setOrMerge(borderLayout, CENTER, child);
            else if (!n && !s && !e && !w) System.err.println("Whaaat");
                // 1 1 1 1 c
            else if (n && s && e && w) setOrMerge(borderLayout, CENTER, child);
                // 0 1 0 0 s
                // 0 1 1 1 s
            else if (!n && s && (e && w || !e && !w)) setOrMerge(borderLayout, SOUTH, child);
                // 1 0 1 1 n
                // 1 0 0 0 n
            else if (n && !s && (e && w || !e && !w)) setOrMerge(borderLayout, NORTH, child);
                // 0 0 0 1 w
                // 1 1 0 1 w
            else if ((n && s || !n && !s) && !e && w) setOrMerge(borderLayout, WEST, child);
                // 1 1 1 0 e
                // 0 0 1 0 e
            else if ((n && s || !n && !s) && e && !w) setOrMerge(borderLayout, EAST, child);
                // 0 1 0 1 sw
            else if (!n && s && !e && w) setOrMerge(borderLayout, SOUTH_WEST, child);
                // 1 0 0 1 nw
            else if (n && !s && !e && w) setOrMerge(borderLayout, NORTH_WEST, child);
                // 1 0 1 0 ne
            else if (n && !s && e && !w) setOrMerge(borderLayout, NORTH_EAST, child);
                // 0 1 1 0 se
            else if (!n && s && e && !w) setOrMerge(borderLayout, SOUTH_EAST, child);
                // 1 1 0 0 e (or w)
            else if (n && s && !e && !w) setOrMerge(borderLayout, EAST, child);
                // 0 0 1 1 n (or s)
            else if (!n && !s && e && w) setOrMerge(borderLayout, NORTH, child);
        }
        return borderLayout;
    }

    /**
     * If <em>parent</em> contains already a BorderLayout in position, creates a new BorderLayout containing the border
     * layout and child.
     */
    private static void setOrMerge(BorderLayout parent, BorderLayout.Place place, BorderLayout child) {
        final BorderLayout borderLayout = parent.get(place);
        if (borderLayout == null) {
            parent.set(place, child);
        } else {
            final BorderLayout auxBorderLayout = new BorderLayout();
            switch (place) {
                case NORTH:
                    auxBorderLayout.set(NORTH, borderLayout);
                    auxBorderLayout.set(SOUTH, child);
                    break;
                case SOUTH:
                    auxBorderLayout.set(WEST, borderLayout);
                    auxBorderLayout.set(EAST, child);
                    break;
                case CENTER:
                    auxBorderLayout.set(WEST, borderLayout);
                    auxBorderLayout.set(EAST, child);
                    break;
                case EAST:
                    auxBorderLayout.set(NORTH, borderLayout);
                    auxBorderLayout.set(SOUTH, child);
                    break;
                case WEST:
                    auxBorderLayout.set(NORTH, borderLayout);
                    auxBorderLayout.set(SOUTH, child);
                    break;
                case SOUTH_WEST:
                    auxBorderLayout.set(WEST, borderLayout);
                    auxBorderLayout.set(SOUTH, child);
                    break;
                case NORTH_WEST:
                    auxBorderLayout.set(NORTH, borderLayout);
                    auxBorderLayout.set(WEST, child);
                    break;
                case SOUTH_EAST:
                    auxBorderLayout.set(NORTH, borderLayout);
                    auxBorderLayout.set(EAST, child);
                    break;
                case NORTH_EAST:
                    auxBorderLayout.set(NORTH, borderLayout);
                    auxBorderLayout.set(EAST, child);
                    break;
            }
            parent.set(place, auxBorderLayout);
        }
    }
}
