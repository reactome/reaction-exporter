package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;

public class Segment {

    private Coordinate from;
    private Coordinate to;

    public Segment(Coordinate from, Coordinate to) {
        this.from = from;
        this.to = to;
    }

    public Coordinate getFrom() {
        return from;
    }

    public Coordinate getTo() {
        return to;
    }
}
