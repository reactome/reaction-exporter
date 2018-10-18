package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.NodeColorProfile;

public class ProteinDrugRenderer extends ProteinRenderer {

    @Override
    protected NodeColorProfile getColorProfile(DiagramProfile profile) {
        return profile.getProteinDrug();
    }

    @Override
    public void draw(EntityGlyph entity, ImageCanvas canvas, DiagramProfile profile) {
        super.draw(entity, canvas, profile);
        DrugHelper.addDrugText(entity, canvas, getTextColor(entity, profile), 0, 0);
    }

}
