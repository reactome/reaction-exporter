package org.reactome.server.tools.reaction.exporter.layout.algorithm.gridbreathe;

import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;

import java.util.List;

public class Tile {
    private final List<? extends Glyph> glyphs;
    private final CompartmentGlyph compartment;
    private final EntityRole role;

    public Tile(CompartmentGlyph compartment, List<? extends Glyph> glyphs, EntityRole role) {
        this.compartment = compartment;
        this.glyphs = glyphs;
        this.role = role;
    }

    public CompartmentGlyph getCompartment() {
        return compartment;
    }

    public EntityRole getRole() {
        return role;
    }

    public List<? extends Glyph> getGlyphs() {
        return glyphs;
    }
}
