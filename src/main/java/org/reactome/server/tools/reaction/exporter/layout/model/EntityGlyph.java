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
import java.util.List;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class EntityGlyph extends AbstractGlyph {

    private Integer stoichiometry;
    private PhysicalEntity pe;

    private RenderableClass renderableClass;

    private Collection<AttachmentGlyph> attachements = null;


    @Override
    public String getName() {
        return pe.getName().get(0);
    }

    public Integer getStoichiometry() {
        return stoichiometry;
    }

    public Collection<AttachmentGlyph> getAttachements() {
        return attachements;
    }

    @JsonIgnore
    public List<Compartment> getCompartments() {
        return pe.getCompartment();
    }

    @Override
    public RenderableClass getRenderableClass() {
        if (renderableClass == null) renderableClass = RenderableClass.getRenderableClass(pe);
        return renderableClass;
    }

    public void setPe(PhysicalEntity pe) {
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

    @Override
    public String toString() {
        return "EntityGlyph{" +
                "n=" + stoichiometry +
                ", pe=" + pe.getStId() +
                '}';
    }
}
