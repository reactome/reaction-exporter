package org.reactome.server.tools.reaction.exporter.renderer.glyph.entity;

import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;

public class ChemicalDrugRenderer extends ChemicalRenderer {

    @Override
    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return profile.getChemicalDrug();
    }

    @Override
    public void draw(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile, Layout layout) {
        super.draw(entity, canvas, profile, layout);
        DrugHelper.addDrugText(entity, canvas, getTextColor(entity, profile), 2, 2);
    }
}
