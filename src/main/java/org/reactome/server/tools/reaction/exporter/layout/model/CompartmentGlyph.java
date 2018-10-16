package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.reactome.server.graph.domain.model.Compartment;
import org.reactome.server.tools.reaction.exporter.goontology.GoTerm;
import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class CompartmentGlyph extends AbstractGlyph {

    private String name;
    private String accession;

    private CompartmentGlyph parent = null;
    private Set<CompartmentGlyph> children = new HashSet<>();

    private Collection<Glyph> containedGlyphs = new HashSet<>();

    private double tx;
    private double ty;

    CompartmentGlyph(Compartment compartment) {
        super();
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
        return new Coordinate((int) tx, (int) ty);
    }

    public void setLabelPosition(double x, double y) {
        tx = x;
        ty = y;
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
