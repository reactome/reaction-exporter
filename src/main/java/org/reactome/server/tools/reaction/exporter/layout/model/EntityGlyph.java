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
    private PhysicalEntity pe;
    private Collection<Role> roles = new HashSet<>();
    private Boolean drug;
    private Boolean crossed = false;
    private Boolean dashed = false;

    //Populated in this class
    private Collection<AttachmentGlyph> attachments = new ArrayList<>();
    private RenderableClass renderableClass;
    private Boolean trivial = null; //Only true for trivial molecules. Null in any other case

    private Connector connector;
    private CompartmentGlyph compartment;

    public EntityGlyph() {
	    super();
	}

	public EntityGlyph(EntityGlyph entity) {
		super();
		pe = entity.pe;
		trivial = entity.trivial;
		if (entity.attachments != null) {
			attachments = new ArrayList<>();
			for (AttachmentGlyph attachment : entity.attachments) {
				attachments.add(new AttachmentGlyph(attachment));
			}
		}
	}

	public Collection<AttachmentGlyph> getAttachments() {
        return attachments;
    }

    @JsonIgnore
    List<Compartment> getCompartments() {
        return pe.getCompartment();
    }

    @Override
    public String getName() {
        return pe.getName().get(0);
    }

    @Override
    public String getSchemaClass() {
        return pe.getSchemaClass();
    }

    @Override
    public RenderableClass getRenderableClass() {
        if(renderableClass == null) renderableClass = RenderableClass.getRenderableClass(pe, drug);
        return renderableClass;
    }

    @JsonIgnore
    public Collection<Role> getRoles() {
        return roles;
    }

	@Override
	public Long getDbId() {
		return pe.getDbId();
	}

	public String getStId() {
        return pe.getStId();
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
        return isDashed() ? true : pe.getInDisease();
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
        this.pe = pe;

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
    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setRole(Role role) {
        roles.add(role);
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public CompartmentGlyph getCompartment() {
        return compartment;
    }

    public void setCompartment(CompartmentGlyph compartment) {
        this.compartment = compartment;
    }

    @Override
    public String toString() {
        return "EntityGlyph{" +
                "pe=" + getName() +
                ", roles=" + roles +
                ", crossed=" + crossed +
                ", dashed=" + dashed +
                '}';
    }
}
