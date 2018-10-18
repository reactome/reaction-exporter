package org.reactome.server.tools.reaction.exporter.renderer.glyph.entity;

import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.glyph.entity.DefaultEntityRenderer;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;
import org.reactome.server.tools.reaction.exporter.renderer.utils.ShapeFactory;

import java.awt.*;

public class RnaRenderer extends DefaultEntityRenderer {

    @Override
    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return super.getColorProfile(profile);
    }

    @Override
    protected Shape getShape(EntityGlyph entity) {
        return ShapeFactory.getRnaShape(entity.getPosition());
    }
}
