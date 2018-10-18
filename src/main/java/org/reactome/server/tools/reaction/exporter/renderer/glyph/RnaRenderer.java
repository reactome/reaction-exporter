package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.ShapeFactory;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;

import java.awt.*;

public class RnaRenderer extends DefaultRenderer {

    @Override
    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return super.getColorProfile(profile);
    }

    @Override
    protected Shape getShape(EntityGlyph entity) {
        return ShapeFactory.getRnaShape(entity.getPosition());
    }
}
