package org.reactome.server.tools.reaction.exporter.layout.model;

import java.util.ArrayList;
import java.util.List;

public class Connector {

    private List<Segment> segments = new ArrayList<>();

    private Shape shape;
    private Shape stoichiometry;

    public List<Segment> getSegments() {
        return segments;
    }

    public Shape getShape() {
        return shape;
    }
}
