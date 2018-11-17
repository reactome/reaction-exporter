package org.reactome.server.tools.reaction.exporter.layout.common;

import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Role;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public class GlyphUtils {

    public static boolean hasRole(EntityGlyph entityGlyph, EntityRole... roles) {
        return hasRole(entityGlyph, new HashSet<>(Arrays.asList(roles)));
    }

    public static boolean hasRole(EntityGlyph entityGlyph, Collection<EntityRole> roles) {
        return entityGlyph.getRoles().stream()
                .map(Role::getType)
                .collect(Collectors.toSet())
                .equals(roles);
    }

    public static boolean hasRole(CompartmentGlyph compartment, EntityRole role) {
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

}
