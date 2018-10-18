package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.ShapeFactory;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;
import org.reactome.server.tools.reaction.exporter.renderer.text.TextRenderer;

import java.awt.*;

public class EntitySetRenderer extends DefaultRenderer {

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
    public void draw(EntityGlyph entity, Graphics2D graphics, DiagramProfile profile) {
        super.draw(entity, graphics, profile);
        final Shape rect = ShapeFactory.roundedRectangle(entity.getPosition(), SET_PADDING);
        border(graphics, profile, entity, rect);
    }

    @Override
    protected void text(EntityGlyph entity, Graphics2D graphics, DiagramProfile profile) {
        graphics.setPaint(getColorProfile(profile).getText());
        final Position position = new Position();
        position.setX(entity.getPosition().getX() + SET_PADDING);
        position.setY(entity.getPosition().getY() + SET_PADDING);
        position.setWidth(entity.getPosition().getWidth() - 2 * SET_PADDING);
        position.setHeight(entity.getPosition().getHeight() - 2 * SET_PADDING);
        TextRenderer.draw(entity.getName(), position, graphics);
    }

}
