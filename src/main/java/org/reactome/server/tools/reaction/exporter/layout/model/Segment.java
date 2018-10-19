package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;
/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Segment {

    private Coordinate from;
    private Coordinate to;

    public Segment(Coordinate from, Coordinate to) {
        this.from = from;
        this.to = to;
    }

    public Segment(double fx, double fy, double tx, double ty) {
        this.from = new Coordinate(fx, fy);
        this.to = new Coordinate(tx, ty);
    }

    public Coordinate getFrom() {
        return from;
    }

    public Coordinate getTo() {
        return to;
    }
}
