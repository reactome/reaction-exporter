package org.reactome.server.tools.reaction.exporter.renderer.glyph.entity;

import org.reactome.server.tools.reaction.exporter.layout.model.AttachmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;
import org.reactome.server.tools.reaction.exporter.renderer.utils.ShapeFactory;
import org.reactome.server.tools.reaction.exporter.renderer.utils.StrokeStyle;

import java.awt.*;

public class ProteinRenderer extends DefaultEntityRenderer {

    @Override
    protected Shape getShape(EntityGlyph entity) {
        return ShapeFactory.getRoundedRectangle(entity.getPosition());
    }

    @Override
    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return profile.getProtein();
    }

    @Override
    public void draw(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        super.draw(entity, canvas, profile);
        attachments(entity, canvas, profile);
    }

    private void attachments(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        if (entity.getAttachments()== null) return;
        final Color textColor = getTextColor(entity, profile);
        final Color borderColor = getBorderColor(entity, profile);
        final Color fillColor = getFillColor(entity, profile);
        for (AttachmentGlyph attachment : entity.getAttachments()) {
            final Shape rectangle = ShapeFactory.rectangle(attachment.getPosition());
            canvas.getAttachmentFill().add(rectangle, fillColor);
            canvas.getAttachmentBorder().add(rectangle, borderColor, StrokeStyle.BORDER.getNormal());
            canvas.getAttachmentText().add(attachment.getName(), entity.getPosition(), textColor, 0);
        }
    }
}
