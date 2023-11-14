package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.tools.diagram.data.layout.Connector;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class EntityGlyph extends AbstractGlyph {

    //From the query
    private Collection<Role> roles = new HashSet<>();
    private transient Boolean drug = false;
    private Boolean crossed = false;
    private Boolean dashed = false;

    //Populated in this class
    private Long dbId;
    private String stId;
    private String name;
    private Boolean inDisease;
    private String schemaClass;
    private List<Compartment> compartments;
    private Collection<AttachmentGlyph> attachments = new ArrayList<>();
    private RenderableClass renderableClass;
    private Boolean trivial = false;

    private List<Connector> connector = new ArrayList<>();
    private CompartmentGlyph compartment;

    public EntityGlyph() {
	}

	public EntityGlyph(EntityGlyph entity) {
        dbId = entity.dbId;
        stId = entity.stId;
        name = entity.name;
        inDisease = entity.inDisease;
        schemaClass = entity.schemaClass;
        compartments = entity.compartments;
        compartment = entity.compartment;
        crossed = entity.crossed;
        dashed = entity.dashed;
        drug = entity.drug;
		trivial = entity.trivial;
        if (entity.attachments != null) {
            attachments = new ArrayList<>();
            for (AttachmentGlyph attachment : entity.attachments) {
                attachments.add(new AttachmentGlyph(attachment));
            }
        }
        renderableClass = entity.renderableClass;
    }

    public Collection<AttachmentGlyph> getAttachments() {
        return attachments;
    }

    @JsonIgnore
    List<Compartment> getCompartments() {
        return compartments;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSchemaClass() {
        return schemaClass;
    }

    @Override
    public RenderableClass getRenderableClass() {
        return renderableClass;
    }

    @JsonIgnore
    public Collection<Role> getRoles() {
        return roles;
    }

	@Override
	public Long getDbId() {
		return dbId;
	}

	public String getStId() {
        return stId;
    }

    public Boolean isCrossed() {
        return crossed;
    }

    public Boolean isDashed() {
        return dashed;
    }

    /**
     * @return true for trivial molecules. NULL in any other case
     */
    public Boolean isTrivial() {
        return trivial;
    }

    public Boolean isDisease() {
        return isDashed() ||  inDisease;
    }

    public Boolean isFadeOut(){
        return isCrossed();
    }

    protected void addRole(Role role) {
        roles.add(role);
    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setPhysicalEntity(PhysicalEntity pe) {
        this.dbId = pe.getDbId();
        this.stId = pe.getStId();
        this.name = pe.getName().get(0);
        this.inDisease = pe.getIsInDisease();
        this.compartments = pe.getCompartment();
        this.schemaClass = pe.getSchemaClass();

        ReferenceEntity re = pe.fetchSingleValue("getReferenceEntity");
        if (re instanceof ReferenceMolecule){
            ReferenceMolecule rm = (ReferenceMolecule) re;
            //trivial ONLY true for trivial molecules. NULL in any other case (never false)
            if(rm.getTrivial() != null && rm.getTrivial()) trivial = true;
        }

        Collection<AbstractModifiedResidue> modifiedResidues = pe.fetchMultiValue("getHasModifiedResidue");
        for (AbstractModifiedResidue modifiedResidue : modifiedResidues) {
            if(modifiedResidue instanceof TranslationalModification) {
                attachments.add(new AttachmentGlyph((TranslationalModification) modifiedResidue));
            }
        }

        renderableClass = RenderableClass.getRenderableClass(pe, drug);
    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setDrug(Boolean drug) {
        this.drug = drug;
        if (drug & renderableClass != null) {
            switch (renderableClass){
                case ENTITY_SET:
                    renderableClass = RenderableClass.ENTITY_SET_DRUG;
                    break;
                case COMPLEX:
                    renderableClass = RenderableClass.COMPLEX_DRUG;
                    break;
            }
        }
    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setRole(Role role) {
        roles.add(role);
    }

    public List<Connector> getConnector() {
        return connector;
    }

    public void addConnector(Connector connector) {
        this.connector.add(connector);
    }

    public CompartmentGlyph getCompartment() {
        return compartment;
    }

    public void setCompartment(CompartmentGlyph compartment) {
        this.compartment = compartment;
    }

    /*
    In some cases, due to oddities in the curation, it could happen that the same entity (same stableIdentifier) appears
    more than once in the reaction with different roles AND in some cases crossed or dashed. This method has been put in
    place in order to provide to the layout the data retrieved from the database.
     */
    @JsonIgnore
    String getIdentifier(){
        return String.format("%s:%s:%s", getStId(), isCrossed(), isDashed());
    }

    @Override
    public String toString() {
        return "EntityGlyph{" +
                "pe=" + getName() +
                ", roles=" + roles +
                ", disease=" + isDisease() +
                ", crossed=" + isCrossed() +
                ", dashed=" + isDashed() +
                '}';
    }

    public void setDashed(Boolean dashed) {
      this.dashed = dashed;
    }
}
