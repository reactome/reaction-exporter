package org.reactome.server.tools.reaction.exporter.renderer.glyph.entity;

import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.glyph.entity.DefaultEntityRenderer;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;
import org.reactome.server.tools.reaction.exporter.renderer.utils.ShapeFactory;

import java.awt.*;

public class EntitySetRenderer extends DefaultEntityRenderer {

    private static double SET_PADDING = 4;

    @Override
    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return profile.getEntitySet();
    }

    @Override
    protected Shape getShape(EntityGlyph entity) {
        return ShapeFactory.roundedRectangle(entity.getPosition());
    }

    @Override
    public void draw(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        super.draw(entity, canvas, profile);
        final Shape rect = ShapeFactory.roundedRectangle(entity.getPosition(), SET_PADDING);
        border(entity, canvas, profile, rect);
    }

    @Override
    protected void text(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        canvas.getNodeTextLayer().add(entity.getName(), entity.getPosition(), getTextColor(entity, profile), SET_PADDING);
    }

}
