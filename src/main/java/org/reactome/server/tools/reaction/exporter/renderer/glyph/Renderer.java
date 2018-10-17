package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.StrokeStyle;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;
import org.reactome.server.tools.reaction.exporter.renderer.text.TextRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class Renderer {

    public void draw(EntityGlyph entity, Graphics2D graphics, DiagramProfile profile) {
        final Shape rect = getShape(entity);
        border(graphics, profile, rect);
        fill(graphics, profile, rect);
        text(entity, graphics, profile);
    }

    protected Shape getShape(EntityGlyph entity) {
        final Position position = entity.getPosition();
        return new Rectangle2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }

    protected void border(Graphics2D graphics, DiagramProfile profile, Shape rect) {
        graphics.setPaint(getColorProfile(profile).getStroke());
        graphics.setStroke(StrokeStyle.BORDER.getStroke());
        graphics.draw(rect);
    }

    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return profile.getOtherEntity();
    }

    protected void fill(Graphics2D graphics, DiagramProfile profile, Shape rect) {
        graphics.setPaint(getColorProfile(profile).getFill());
        graphics.fill(rect);
    }

    protected void text(EntityGlyph entity, Graphics2D graphics, DiagramProfile profile) {
        graphics.setPaint(getColorProfile(profile).getText());
        TextRenderer.draw(entity.getName(), entity.getPosition(), graphics);
    }
}
