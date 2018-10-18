package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;

public interface Renderer<T extends Glyph> {

    void draw(T glyph, ImageCanvas canvas, DiagramProfile profile);
}
