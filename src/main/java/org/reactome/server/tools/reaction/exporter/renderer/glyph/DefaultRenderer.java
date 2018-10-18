package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;
import org.reactome.server.tools.reaction.exporter.renderer.utils.StrokeStyle;
import org.reactome.server.tools.reaction.exporter.renderer.utils.TextRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class DefaultRenderer implements Renderer {

    @Override

    public void draw(EntityGlyph entity, Graphics2D graphics, DiagramProfile profile) {
        final Shape rect = getShape(entity);
        fill(graphics, profile, entity, rect);
        border(graphics, profile, entity, rect);
        text(entity, graphics, profile);
    }

    protected Shape getShape(EntityGlyph entity) {
        final Position position = entity.getPosition();
        return new Rectangle2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }

    protected void border(Graphics2D graphics, DiagramProfile profile, EntityGlyph entity, Shape rect) {
        graphics.setPaint(getBorderColor(profile, entity));
        graphics.setStroke(StrokeStyle.BORDER.getNormal());
        graphics.draw(rect);
    }

    private Color getBorderColor(DiagramProfile profile, EntityGlyph entity) {
        if (entity.isDisease() != null && entity.isDisease())
            return profile.getProperties().getDisease();
        return getColorProfile(profile).getStroke();
    }

    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return profile.getOtherEntity();
    }

    protected void fill(Graphics2D graphics, DiagramProfile profile, EntityGlyph entity, Shape rect) {
        graphics.setPaint(getFillColor(profile));
        graphics.fill(rect);
    }

    private Color getFillColor(DiagramProfile profile) {
        return getColorProfile(profile).getFill();
    }

    protected void text(EntityGlyph entity, Graphics2D graphics, DiagramProfile profile) {
        graphics.setPaint(getTextColor(profile, entity));
        TextRenderer.draw(entity.getName(), entity.getPosition(), graphics, 3);
    }

    protected Color getTextColor(DiagramProfile profile, EntityGlyph entity) {
        return getColorProfile(profile).getText();
    }
}
