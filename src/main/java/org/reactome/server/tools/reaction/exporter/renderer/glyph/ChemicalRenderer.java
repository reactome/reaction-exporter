package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class ChemicalRenderer extends DefaultEntityRenderer {

    @Override
    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return profile.getChemical();
    }

    @Override
    protected Shape getShape(EntityGlyph entity) {
        final Position position = entity.getPosition();
        return new Ellipse2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }
}
