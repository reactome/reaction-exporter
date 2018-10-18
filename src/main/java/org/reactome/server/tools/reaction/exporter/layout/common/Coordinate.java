package org.reactome.server.tools.reaction.exporter.layout.common;

/**
 * Contains the coordinates (x, y)
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class Coordinate {

    private Double x;
    private Double y;

    public Coordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public void move(double dx, double dy) {
        x += dx;
        y += dy;

    }
}
