package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.reactome.server.graph.domain.model.AbstractModifiedResidue;
import org.reactome.server.graph.domain.model.Compartment;
import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.graph.domain.model.ReferenceMolecule;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    //Populated in this class
    private Collection<AttachmentGlyph> attachments = null;
    private RenderableClass renderableClass;
    private Boolean trivial = null; //Only true for trivial molecules. Null in any other case

	public EntityGlyph() {
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
    public List<Compartment> getCompartments() {
        return pe.getCompartment();
    }

    @Override
    public String getName() {
        return pe.getName().get(0);
    }

    @Override
    public RenderableClass getRenderableClass() {
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
        renderableClass = RenderableClass.getRenderableClass(pe);
        attachments = new ArrayList<>();

        try {
            Method getReferenceEntity = pe.getClass().getMethod("getReferenceEntity");
            ReferenceMolecule rm = (ReferenceMolecule) getReferenceEntity.invoke(pe);
            //trivial ONLY true for trivial molecules. NULL in any other case (never false)
            if (rm != null && rm.getTrivial() != null && rm.getTrivial()) trivial = true;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            //Nothing here
        }

        try {
            Method getHasModifiedResidue = pe.getClass().getMethod("getHasModifiedResidue");
            //noinspection unchecked
            List<AbstractModifiedResidue> modifiedResidues = (List<AbstractModifiedResidue>) getHasModifiedResidue.invoke(pe);
            if (modifiedResidues != null) {
                for (AbstractModifiedResidue modifiedResidue : modifiedResidues) {
                    attachments.add(new AttachmentGlyph(modifiedResidue));
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            //Nothing here
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
