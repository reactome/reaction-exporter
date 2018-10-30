package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.layout.model.Role;

import java.util.ArrayList;
import java.util.List;

class LayoutIndex {

    private List<EntityGlyph> inputs = new ArrayList<>();
    private List<EntityGlyph> outputs = new ArrayList<>();
    private List<EntityGlyph> catalysts = new ArrayList<>();
    private List<EntityGlyph> regulators = new ArrayList<>();

    LayoutIndex(Layout layout) {
        final List<EntityGlyph> activators = new ArrayList<>();
        final List<EntityGlyph> inhibitors = new ArrayList<>();
        for (EntityGlyph entity : layout.getEntities()) {
            for (Role role : entity.getRoles()) {
                switch (role.getType()) {
                    case INPUT:
                        inputs.add(entity);
                        break;
                    case OUTPUT:
                        outputs.add(entity);
                        break;
                    case CATALYST:
                        catalysts.add(entity);
                        break;
                    case NEGATIVE_REGULATOR:
                        inhibitors.add(entity);
                        break;
                    case POSITIVE_REGULATOR:
                        activators.add(entity);
                        break;
                }
            }
        }
        regulators.addAll(inhibitors);
        regulators.addAll(activators);
    }

    List<EntityGlyph> getCatalysts() {
        return catalysts;
    }

    List<EntityGlyph> getInputs() {
        return inputs;
    }

    List<EntityGlyph> getOutputs() {
        return outputs;
    }

    List<EntityGlyph> getRegulators() {
        return regulators;
    }
}
