package org.reactome.server.tools.reaction.exporter.layout.common;

import com.fasterxml.jackson.annotation.JsonValue;
import org.reactome.server.graph.domain.model.*;

/**
 * For a given DatabaseObject instance, this class returns the RenderableClass
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public enum RenderableClass {

    ATTACHMENT("Attachement"),
    CHEMICAL("Chemical"),
    CHEMICAL_DRUG("ChemicalDrug"),
    COMPARTMENT("Compartment"),
    COMPLEX("Complex"),
    COMPLEX_DRUG("ComplexDrug"),
    ENCAPSULATED_NODE("EncapsulatedNode"),
    ENTITY("Entity"),
    ENTITY_SET("EntitySet"),
    ENTITY_SET_DRUG("EntitySetDrug"),
    FLOW_LINE("FlowLine"),
    GENE("Gene"),
    INTERACTION("Interaction"),
    NOTE("Note"),
    PROCESS_NODE("ProcessNode"),
    PROTEIN("Protein"),
    PROTEIN_DRUG("ProteinDrug"),
    REACTION("Reaction"),
    RNA("RNA"),
    RNA_DRUG("RNADrug"),
    SHADOW("Shadow"),

    ENTITY_SET_AND_MEMBER_LINK("EntitySetAndMemberLink"),
    ENTITY_SET_AND_ENTITY_SET_LINK("EntitySetAndEntitySetLink");

    public final String name;

    RenderableClass(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static RenderableClass getRenderableClass(DatabaseObject databaseObject) {
        return getRenderableClass(databaseObject, false);
    }

    /**
     * @param databaseObject the object associated with the glyph
     * @param drug For entities, the query in {@LayoutFactory} assigns it to true for {@Drug} class instances and for
     *             {@Complexes} or {@EntitySets} class instances containing one of the previous ones
     * @return The associated {@RenderableClass} with the previous parameters
     */
    public static RenderableClass getRenderableClass(DatabaseObject databaseObject, Boolean drug) {
        if (databaseObject instanceof EntitySet) return drug ? ENTITY_SET_DRUG : ENTITY_SET;
        if (databaseObject instanceof Complex) return drug ? COMPLEX_DRUG : COMPLEX;
        if (databaseObject instanceof SimpleEntity) return CHEMICAL;
        if (databaseObject instanceof ChemicalDrug) return CHEMICAL_DRUG;
        if (databaseObject instanceof ProteinDrug) return PROTEIN_DRUG;
        if (databaseObject instanceof RNADrug) return RNA_DRUG;
        if (databaseObject instanceof EntityWithAccessionedSequence) {
            EntityWithAccessionedSequence ewas = (EntityWithAccessionedSequence) databaseObject;
            ReferenceSequence rs = ewas.getReferenceEntity();
            if (rs instanceof ReferenceGeneProduct) return PROTEIN;
            if (rs instanceof ReferenceDNASequence) return GENE;
            if (rs instanceof ReferenceRNASequence) return RNA;
        }
        if (databaseObject instanceof GenomeEncodedEntity) return ENTITY;
        if (databaseObject instanceof OtherEntity) return ENTITY;
        if (databaseObject instanceof Polymer) return ENTITY;

        if (databaseObject instanceof ReactionLikeEvent) return REACTION;
        if (databaseObject instanceof Pathway) return PROCESS_NODE;
        if (databaseObject instanceof Compartment) return COMPARTMENT;

        throw new RuntimeException("No Schema class defined for [" + databaseObject.getDbId() + ":" + databaseObject.getDisplayName() + "]");
    }

    @JsonValue
    @Override
    public String toString() {
        return name;
    }

}
