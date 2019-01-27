package org.reactome.server.tools.reaction.exporter.layout.common;

import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Role;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * These methods extend {@link Glyph} class behaviour without overloading the class.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class GlyphUtils {

    public static boolean hasRole(EntityGlyph entityGlyph, EntityRole... roles) {
        return hasRole(entityGlyph, new HashSet<>(Arrays.asList(roles)));
    }

    public static boolean hasRole(EntityGlyph entityGlyph, Collection<EntityRole> roles) {
        final Set<EntityRole> r = entityGlyph.getRoles().stream().map(Role::getType).collect(Collectors.toSet());
        return r.containsAll(roles);
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

    public static boolean isAncestor(CompartmentGlyph parent, CompartmentGlyph child) {
        if (parent == null || child == null) return false;
        CompartmentGlyph ancestor = child.getParent();
        while (ancestor != null) {
            if (ancestor == parent) return true;
            ancestor = ancestor.getParent();
        }
        return false;
    }

    public static Collection<EntityRole> getContainedRoles(CompartmentGlyph compartmentGlyph) {
        final Set<EntityRole> roles = new HashSet<>();
        for (final Glyph glyph : compartmentGlyph.getContainedGlyphs()) {
            if (glyph instanceof EntityGlyph) {
                roles.addAll(getRoles((EntityGlyph) glyph));
            }
        }
        for (final CompartmentGlyph child : compartmentGlyph.getChildren()) {
            roles.addAll(getContainedRoles(child));
        }
        return roles;
    }

    private static Collection<EntityRole> getRoles(EntityGlyph entityGlyph) {
        return entityGlyph.getRoles().stream().map(Role::getType).collect(Collectors.toSet());
    }
}
