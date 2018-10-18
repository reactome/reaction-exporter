package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.utils.StrokeStyle;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class CompartmentRenderer implements Renderer<CompartmentGlyph> {
    @Override
    public void draw(CompartmentGlyph compartment, ImageCanvas canvas, DiagramProfile profile) {
        final Stroke stroke = StrokeStyle.BORDER.getNormal();
        final Color fill = profile.getCompartment().getFill();
        final Color border = profile.getCompartment().getStroke();
        final Color text = profile.getCompartment().getText();
        final Position position = compartment.getPosition();
        final Rectangle2D.Double rect = new Rectangle2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight());
        canvas.getCompartmentFillLayer().add(rect, fill);
        canvas.getNodeBorderLayer().add(rect, border, stroke);
        canvas.getCompartmentTextLayer().add(compartment.getName(), compartment.getLabelPosition(), text);
    }
}
