package org.reactome.server.tools.reaction.exporter.layout.common;

/**
 * Contains the coordinates (x, y)
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class Coordinate {

    private Integer x;
    private Integer y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;

    }
}
