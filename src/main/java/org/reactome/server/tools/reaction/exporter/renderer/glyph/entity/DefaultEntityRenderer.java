package org.reactome.server.tools.reaction.exporter.renderer.glyph.entity;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.layout.model.Segment;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.glyph.Renderer;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;
import org.reactome.server.tools.reaction.exporter.renderer.utils.ShapeFactory;
import org.reactome.server.tools.reaction.exporter.renderer.utils.StrokeStyle;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class DefaultEntityRenderer implements Renderer<EntityGlyph> {

    @Override
    public void draw(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile, Layout layout) {
        final Shape rect = getShape(entity);
        fill(entity, canvas, profile, rect);
        border(entity, canvas, profile, rect);
        text(entity, canvas, profile);
        segments(layout, entity, canvas, profile);
    }

    protected Shape getShape(EntityGlyph entity) {
        final Position position = entity.getPosition();
        return new Rectangle2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }

    protected void border(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile, Shape rect) {
        canvas.getNodeBorder().add(rect, getBorderColor(entity, profile), StrokeStyle.BORDER.getNormal());
    }

    protected Color getBorderColor(EntityGlyph entity, DiagramProfile profile) {
        if (entity.isDisease() != null && entity.isDisease())
            return profile.getProperties().getDisease();
        return getColorProfile(profile).getStroke();
    }

    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return profile.getOtherEntity();
    }

    protected void fill(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile, Shape rect) {
        canvas.getNodeFill().add(rect, getFillColor(entity, profile));
    }

    protected Color getFillColor(EntityGlyph entity, DiagramProfile profile) {
        return getColorProfile(profile).getFill();
    }

    protected void text(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        final Color color = getTextColor(entity, profile);
        canvas.getNodeText().add(entity.getName(), entity.getPosition(), color, 3);
    }

    protected Color getTextColor(EntityGlyph entity, DiagramProfile profile) {
        return getColorProfile(profile).getText();
    }

    private void segments(Layout layout, EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        final Color color = layout.getReaction().isDisease() != null && layout.getReaction().isDisease()
                ? profile.getProperties().getDisease()
                : profile.getReaction().getStroke();
        for (Segment segment : entity.getConnector().getSegments()) {
            canvas.getSegments().add(ShapeFactory.getLine(segment), color, StrokeStyle.SEGMENT.getNormal());
        }

    }
}
