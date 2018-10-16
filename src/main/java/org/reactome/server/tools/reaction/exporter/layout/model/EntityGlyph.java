package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.reactome.server.graph.domain.model.AbstractModifiedResidue;
import org.reactome.server.graph.domain.model.Compartment;
import org.reactome.server.graph.domain.model.PhysicalEntity;
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
    private Collection<AttachmentGlyph> attachements = null;
    private RenderableClass renderableClass;

    public Collection<AttachmentGlyph> getAttachements() {
        return attachements;
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
        if (renderableClass == null) renderableClass = RenderableClass.getRenderableClass(pe);
        return renderableClass;
    }

    @JsonIgnore
    public Collection<Role> getRoles() {
        return roles;
    }

    @JsonIgnore
    public String getStId(){
        return pe.getStId();
    }

    protected void addRole(Role role){
        roles.add(role);
    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setPhysicalEntity(PhysicalEntity pe) {
        this.pe = pe;
        attachements = new ArrayList<>();
        try {
            Method getHasModifiedResidue = pe.getClass().getMethod("getHasModifiedResidue");
            //noinspection unchecked
            List<AbstractModifiedResidue> modifiedResidues = (List<AbstractModifiedResidue>) getHasModifiedResidue.invoke(pe);
            if (modifiedResidues != null) {
                for (AbstractModifiedResidue modifiedResidue : modifiedResidues) {
                    attachements.add(new AttachmentGlyph(modifiedResidue));
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            //Nothing here
        }
    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setRole(Role role){
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
