package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.reactome.server.graph.domain.model.*;
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

    //Populated in this class
    private Collection<AttachmentGlyph> attachments = new ArrayList<>();
    private RenderableClass renderableClass;
    private Boolean trivial = null; //Only true for trivial molecules. Null in any other case

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
    public RenderableClass getRenderableClass() {
        if(renderableClass == null) renderableClass = RenderableClass.getRenderableClass(pe, drug);
        return renderableClass;
    }

    @JsonIgnore
    public Collection<Role> getRoles() {
        return roles;
    }

    @JsonIgnore
    public String getStId() {
        return pe.getStId();
    }

    /**
     * @return true for trivial molecules. NULL in any other case
     */
    public Boolean isTrivial() {
        return trivial;
    }

    protected void addRole(Role role) {
        roles.add(role);
    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setPhysicalEntity(PhysicalEntity pe) {
        this.pe = pe;

        ReferenceEntity re = pe.getSingleValue("getReferenceEntity");
        if (re instanceof ReferenceMolecule){
            ReferenceMolecule rm = (ReferenceMolecule) re;
            //trivial ONLY true for trivial molecules. NULL in any other case (never false)
            if(rm.getTrivial() != null && rm.getTrivial()) trivial = true;
        }

        Collection<AbstractModifiedResidue> modifiedResidues = pe.getMultiValue("getHasModifiedResidue");
        for (AbstractModifiedResidue modifiedResidue : modifiedResidues) {
            attachments.add(new AttachmentGlyph(modifiedResidue));
        }
    }
    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setRole(Role role) {
        roles.add(role);
    }
    @Override
    public String toString() {
        return "EntityGlyph{" +
                "pe=" + getName() +
                ", roles=" + roles +
                '}';
    }
}
