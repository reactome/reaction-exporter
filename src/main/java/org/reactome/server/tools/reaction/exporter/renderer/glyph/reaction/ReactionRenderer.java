package org.reactome.server.tools.reaction.exporter.renderer.glyph.reaction;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.ReactionGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.glyph.Renderer;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.utils.StrokeStyle;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class ReactionRenderer implements Renderer<ReactionGlyph> {

    @Override
    public void draw(ReactionGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        final Shape shape = getShape(entity);
        fill(entity, canvas, profile, shape);
        border(entity, canvas, profile, shape);
        text(entity, canvas, profile);
    }

    protected Shape getShape(ReactionGlyph entity) {
        final Position position = entity.getPosition();
        return new Rectangle2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }

    protected void fill(ReactionGlyph entity, ImageCanvas canvas, DiagramProfile profile, Shape shape) {
        canvas.getNodeFillLayer().add(shape, getFillColor(entity, profile));
    }

    protected Paint getFillColor(ReactionGlyph entity, DiagramProfile profile) {
        return profile.getReaction().getFill();
    }

    protected void border(ReactionGlyph entity, ImageCanvas canvas, DiagramProfile profile, Shape shape) {
        canvas.getNodeBorderLayer().add(shape, getBorderColor(entity, profile), StrokeStyle.BORDER.getNormal());
    }

    protected Color getBorderColor(ReactionGlyph entity, DiagramProfile profile) {
        if (entity.isDisease() != null && entity.isDisease()) return profile.getProperties().getDisease();
        return profile.getReaction().getStroke();
    }

    protected void text(ReactionGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        canvas.getNodeTextLayer().add(getText(), entity.getPosition(), getTextColor(entity, profile), 0);
    }

    protected String getText() {
        return "";
    }

    protected Color getTextColor(ReactionGlyph entity, DiagramProfile profile) {
        if (entity.isDisease() != null && entity.isDisease()) return profile.getProperties().getDisease();
        return profile.getReaction().getText();
    }
}
