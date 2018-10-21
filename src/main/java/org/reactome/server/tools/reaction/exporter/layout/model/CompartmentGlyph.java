package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.reactome.server.graph.domain.model.Compartment;
import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.ontology.GoTerm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class CompartmentGlyph extends AbstractGlyph {

    private Long dbId;
    private String name;
    private String accession;

    private CompartmentGlyph parent = null;
    private Set<CompartmentGlyph> children = new HashSet<>();

    private Collection<Glyph> containedGlyphs = new HashSet<>();

    private Coordinate labelPosition;

    CompartmentGlyph(Compartment compartment) {
        super();
        dbId = compartment.getDbId();
        name = compartment.getDisplayName();
        accession = compartment.getAccession();
    }

    CompartmentGlyph(GoTerm term) {
        super();
        name = term.getName();
        accession = term.getId().replaceAll("GO:", "");
    }

    public String getAccession() {
        return accession;
    }

    @Override
    public Long getDbId() {
        return dbId;
    }

    @JsonIgnore
    public CompartmentGlyph getParent() {
        return parent;
    }

    @JsonIgnore
    public Set<CompartmentGlyph> getChildren() {
        return children;
    }

    @JsonIgnore
    public Collection<Glyph> getContainedGlyphs() {
        return containedGlyphs;
    }

    void addChild(CompartmentGlyph compartment) {
        children.add(compartment);
    }

    void setParent(CompartmentGlyph compartment) {
        if (parent != null)
            throw new RuntimeException("Trying to assign more than one parent to a compartment tree node");
        parent = compartment;
    }

    public void addGlyph(Glyph glyph){
        containedGlyphs.add(glyph);
    }

    public void print() {
        print(0);
    }

    private void print(int level) {
        for (int i = 0; i < level; i++) System.out.print("\t");
        System.out.println(this);
        for (CompartmentGlyph child : children) child.print(level + 1);
    }

    public Coordinate getLabelPosition() {
        return labelPosition;
    }

    public void setLabelPosition(Coordinate labelPosition) {
        this.labelPosition = labelPosition;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RenderableClass getRenderableClass() {
        return RenderableClass.COMPARTMENT;
    }

    @Override
    public String toString() {
        return "CompartmentGlyph{" +
                "name='" + name + '\'' +
                ", accession='" + accession + '\'' +
                '}';
    }
}
