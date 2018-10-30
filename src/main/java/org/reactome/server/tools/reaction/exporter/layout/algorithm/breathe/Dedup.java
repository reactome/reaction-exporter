package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.layout.model.Role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.CATALYST;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.INPUT;

class Dedup {

    static void addDuplicates(Layout layout) {
        // We duplicate every entity that has more than one role, except when the input is a catalyst
        final List<EntityGlyph> added = new ArrayList<>();
        for (EntityGlyph entity : layout.getEntities()) {
            final Collection<Role> roles = entity.getRoles();
            if (roles.size() > 1) {
                // Extract the role types
                final ArrayList<Role> roleList = new ArrayList<>(roles);
                for (final Role role : roleList) {
                    if (role.getType() == INPUT
                            || role.getType() == CATALYST
                            || entity.getRoles().size() == 1) continue;
                    final EntityGlyph copy = new EntityGlyph(entity);
                    copy.setRole(role);
                    entity.getRoles().remove(role);
                    added.add(copy);
                    copy.setCompartment(entity.getCompartment());
                    entity.getCompartment().addGlyph(copy);
                }
            }
        }
        for (EntityGlyph entity : added) {
            layout.add(entity);
        }
    }
}
