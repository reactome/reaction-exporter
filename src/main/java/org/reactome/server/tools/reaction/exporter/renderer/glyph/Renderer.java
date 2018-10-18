package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;

import java.awt.*;

public interface Renderer {

    void draw(EntityGlyph entity, Graphics2D graphics, DiagramProfile profile);
}
