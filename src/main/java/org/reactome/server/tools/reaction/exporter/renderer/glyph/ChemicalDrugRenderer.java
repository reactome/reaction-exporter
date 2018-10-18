package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;

import java.awt.*;

public class ChemicalDrugRenderer extends ChemicalRenderer {

    @Override
    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return profile.getChemicalDrug();
    }

    @Override
    public void draw(EntityGlyph entity, Graphics2D graphics, DiagramProfile profile) {
        super.draw(entity, graphics, profile);
        DrugHelper.addDrugText(entity, graphics, 2,2);
        // TODO: 17/10/18 add Rx
    }
}
