package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;
import org.reactome.server.tools.reaction.exporter.renderer.glyph.entity.*;
import org.reactome.server.tools.reaction.exporter.renderer.glyph.reaction.*;

import java.util.EnumMap;
import java.util.Map;

public class RendererFactory {
    private final static Map<RenderableClass, Renderer> RENDERER_MAP = new EnumMap<>(RenderableClass.class);

    static {
        RENDERER_MAP.put(RenderableClass.CHEMICAL, new ChemicalRenderer());
        RENDERER_MAP.put(RenderableClass.CHEMICAL_DRUG, new ChemicalDrugRenderer());
        RENDERER_MAP.put(RenderableClass.COMPARTMENT, new CompartmentRenderer());
        RENDERER_MAP.put(RenderableClass.COMPLEX, new ComplexRenderer());
        RENDERER_MAP.put(RenderableClass.COMPLEX_DRUG, new ComplexDrugRenderer());
        RENDERER_MAP.put(RenderableClass.ENTITY, new EntityRenderer());
        RENDERER_MAP.put(RenderableClass.ENTITY_SET, new EntitySetRenderer());
        RENDERER_MAP.put(RenderableClass.ENTITY_SET_DRUG, new EntitySetDrugRenderer());
        RENDERER_MAP.put(RenderableClass.GENE, new GeneRenderer());
        RENDERER_MAP.put(RenderableClass.PROTEIN, new ProteinRenderer());
        RENDERER_MAP.put(RenderableClass.PROTEIN_DRUG, new ProteinDrugRenderer());
        RENDERER_MAP.put(RenderableClass.RNA, new RnaRenderer());
        RENDERER_MAP.put(RenderableClass.RNA_DRUG, new RnaDrugRenderer());
        RENDERER_MAP.put(RenderableClass.TRANSFORMATION_REACTION, new TransformationReactionRenderer());
        RENDERER_MAP.put(RenderableClass.BINDING_REACTION, new BindingReactionRenderer());
        RENDERER_MAP.put(RenderableClass.UNCERTAIN_REACTION, new UncertainReactionRenderer());
        RENDERER_MAP.put(RenderableClass.OMITTED_REACTION, new OmittedReactionRenderer());
        RENDERER_MAP.put(RenderableClass.DISSOCIATION_REACTION, new DissociationReactionRenderer());
    }

    public static <T extends Glyph> Renderer<T> getRenderer(RenderableClass renderableClass) {
        final Renderer<T> renderer = RENDERER_MAP.get(renderableClass);
        if (renderer == null) {
            throw new IllegalArgumentException("There is no renderer for class " + renderableClass);
        }
        return renderer;
    }
}
