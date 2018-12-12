package org.reactome.server.tools.reaction.exporter.layout.algorithm.common;

import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LayoutIndex {

    /**
     * Order in which nodes should be placed depending on their {@link RenderableClass}
     */
    private static final List<RenderableClass> CLASS_ORDER = Arrays.asList(
            RenderableClass.PROCESS_NODE,
            RenderableClass.ENCAPSULATED_NODE,
            RenderableClass.COMPLEX,
            RenderableClass.ENTITY_SET,
            RenderableClass.PROTEIN,
            RenderableClass.RNA,
            RenderableClass.CHEMICAL,
            RenderableClass.GENE,
            RenderableClass.ENTITY);
    /**
     * Comparator that puts false (and null) elements before true elements.
     */
    private static final Comparator<Boolean> FALSE_FIRST = Comparator.nullsFirst((o1, o2) -> o1.equals(o2) ? 0 : o1 ? 1 : -1);
    private static final Comparator<Boolean> TRUE_FIRST = Comparator.nullsLast(((o1, o2) -> o1.equals(o2) ? 0 : o1 ? -1 : 1));


    private List<EntityGlyph> inputs = new ArrayList<>();
    private List<EntityGlyph> outputs = new ArrayList<>();
    private List<EntityGlyph> catalysts = new ArrayList<>();
    private List<EntityGlyph> regulators = new ArrayList<>();
    private ReactionGlyph reaction;

    public LayoutIndex(Layout layout) {
        reaction = layout.getReaction();
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

        inputs.sort(Comparator
                // input/catalysts first
                .comparing((EntityGlyph e) -> e.getRoles().size()).reversed()
                // .thenComparing(EntityGlyph::isDashed, TRUE_FIRST)
                // .thenComparing(EntityGlyph::isDisease, TRUE_FIRST)
                // .thenComparing(EntityGlyph::isFadeOut, FALSE_FIRST)
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                // and sorted by RenderableClass
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                // and finally by name
                .thenComparing(EntityGlyph::getName));

        outputs.sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                // .thenComparing(EntityGlyph::isDashed, TRUE_FIRST)
                // .thenComparing(EntityGlyph::isDisease, TRUE_FIRST)
                // .thenComparing(EntityGlyph::isFadeOut, FALSE_FIRST)
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));

        catalysts.sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                // .thenComparing(EntityGlyph::isDashed, TRUE_FIRST)
                // .thenComparing(EntityGlyph::isDisease, TRUE_FIRST)
                // .thenComparing(EntityGlyph::isFadeOut, FALSE_FIRST)
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));

        regulators.sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                // negatives first
                .thenComparing(e -> e.getRoles().stream().anyMatch(role -> role.getType() == EntityRole.NEGATIVE_REGULATOR), TRUE_FIRST)
                // .thenComparing(EntityGlyph::isDashed, TRUE_FIRST)
                // .thenComparing(EntityGlyph::isDisease, TRUE_FIRST)
                // .thenComparing(EntityGlyph::isFadeOut, FALSE_FIRST)
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));

        catalysts.removeIf(entityGlyph -> entityGlyph.getRoles().stream().anyMatch(role -> role.getType() == EntityRole.INPUT));
    }

    public List<EntityGlyph> getCatalysts() {
        return catalysts;
    }

    public List<EntityGlyph> getInputs() {
        return inputs;
    }

    public List<EntityGlyph> getOutputs() {
        return outputs;
    }

    public List<EntityGlyph> getRegulators() {
        return regulators;
    }

    public List<EntityGlyph> filterInputs(CompartmentGlyph compartment) {
        return inputs.stream()
                .filter(entity -> entity.getCompartment() == compartment)
                .collect(Collectors.toList());
    }

    public List<EntityGlyph> filterOutputs(CompartmentGlyph compartment) {
        return outputs.stream()
                .filter(entity -> entity.getCompartment() == compartment)
                .collect(Collectors.toList());
    }
    public List<EntityGlyph> filterCatalysts(CompartmentGlyph compartment) {
        return catalysts.stream()
                .filter(entity -> entity.getCompartment() == compartment)
                .collect(Collectors.toList());
    }
    public List<EntityGlyph> filterRegulators(CompartmentGlyph compartment) {
        return regulators.stream()
                .filter(entity -> entity.getCompartment() == compartment)
                .collect(Collectors.toList());
    }

    public ReactionGlyph getReaction() {
        return reaction;
    }
}
