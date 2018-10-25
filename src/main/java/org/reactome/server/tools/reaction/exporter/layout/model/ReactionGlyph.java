package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class ReactionGlyph extends AbstractGlyph {

    private Long dbId;
    private String stId;
    private String schemaClass;
    private String name;
    private RenderableClass renderableClass;
    private Boolean disease;

    private List<Segment> segments = new ArrayList<>();
    private CompartmentGlyph compartment;

    ReactionGlyph(ReactionLikeEvent rle) {
        super();
        dbId = rle.getDbId();
        stId = rle.getStId();
        schemaClass = rle.getSchemaClass();
        name = rle.getDisplayName();
        disease = rle.getIsInDisease() ? true : null;
        renderableClass = RenderableClass.getRenderableClass(rle);
    }

    public String getStId() {
        return stId;
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

    @Override
    public Long getDbId() {
        return dbId;
    }

    public Boolean isDisease() {
        return disease;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public void setCompartment(CompartmentGlyph compartment) {
        this.compartment = compartment;
    }

    public CompartmentGlyph getCompartment() {
        return compartment;
    }

    @Override
    public String toString() {
        return "ReactionGlyph{stId='" + stId + "'}";
    }
}
