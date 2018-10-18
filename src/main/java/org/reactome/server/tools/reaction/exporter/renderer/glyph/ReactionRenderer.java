package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.ReactionGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.utils.StrokeStyle;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class ReactionRenderer implements Renderer<ReactionGlyph> {

    @Override
    public void draw(ReactionGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        final Shape shape = getShape(entity);
        fill(entity, canvas, profile, shape);
        border(entity, canvas, profile, shape);
        text(entity, canvas, profile);
    }

    private Shape getShape(ReactionGlyph entity) {
        final Position position = entity.getPosition();
        return new Rectangle2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }

    private void fill(ReactionGlyph entity, ImageCanvas canvas, DiagramProfile profile, Shape shape) {
        canvas.getNodeFillLayer().add(shape, getFillColor(entity, profile));
    }

    private Paint getFillColor(ReactionGlyph entity, DiagramProfile profile) {
        return profile.getReaction().getFill();
    }

    private void border(ReactionGlyph entity, ImageCanvas canvas, DiagramProfile profile, Shape shape) {
        canvas.getNodeBorderLayer().add(shape, getBorderColor(entity, profile), StrokeStyle.BORDER.getNormal());
    }

    private Color getBorderColor(ReactionGlyph entity, DiagramProfile profile) {
        if (entity.isDisease()) return profile.getProperties().getDisease();
        return profile.getReaction().getStroke();
    }

    private void text(ReactionGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        canvas.getNodeTextLayer().add("?", entity.getPosition(), getTextColor(entity, profile), 0);
    }

    private Color getTextColor(ReactionGlyph entity, DiagramProfile profile) {
        if (entity.isDisease()) return profile.getProperties().getDisease();
        return profile.getReaction().getText();
    }
}
