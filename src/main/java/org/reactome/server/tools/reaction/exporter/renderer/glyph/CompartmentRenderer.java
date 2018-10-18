package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.utils.ShapeFactory;
import org.reactome.server.tools.reaction.exporter.renderer.utils.StrokeStyle;

import java.awt.*;

public class CompartmentRenderer implements Renderer<CompartmentGlyph> {
    @Override
    public void draw(CompartmentGlyph compartment, ImageCanvas canvas, DiagramProfile profile) {
        final Shape rect = ShapeFactory.roundedRectangle(compartment.getPosition());
        canvas.getCompartmentFillLayer().add(rect, profile.getCompartment().getFill());
        canvas.getCompartmentBorderLayer().add(rect, profile.getCompartment().getStroke(), StrokeStyle.BORDER.getNormal());
        canvas.getCompartmentTextLayer().add(compartment.getName(), compartment.getLabelPosition(), profile.getCompartment().getText());
    }
}
