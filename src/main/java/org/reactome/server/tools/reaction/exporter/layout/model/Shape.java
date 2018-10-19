package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Shape {


    public enum Type {ARROW, BOX, CIRCLE, DOUBLE_CIRCLE, STOP}

    private Coordinate a;

    private Coordinate b;

    private Coordinate c;
    private Integer r;
    private Integer r1;
    private String s;        //symbol e.g. ?, \\, 0-9
    private Type type;
    private Boolean empty = null;

    public Shape(Coordinate a, Coordinate b, Boolean empty, Type type) {
        if (!type.equals(Type.BOX)) {
            throw new RuntimeException("This constructor can only be used for box");
        }
        this.a = a;
        this.b = b;
        if (empty) {
            this.empty = true;
        }
        this.type = type;
        setBoundaries();
    }

    public Shape(Coordinate a, Coordinate b, Coordinate c, Boolean empty, Type type) {
        if (!type.equals(Type.ARROW) && !type.equals(Type.STOP)) {
            throw new RuntimeException("This constructor can only be used for arrow or stop");
        }
        this.a = a;
        this.b = b;
        this.c = c;
        if (empty) {
            this.empty = true;
        }
        this.type = type;
        setBoundaries();
    }

    public Shape(Coordinate c, Integer r, Boolean empty, Type type) {
        if (!type.equals(Type.CIRCLE) && !type.equals(Type.DOUBLE_CIRCLE)) {
            throw new RuntimeException("This constructor can only be used for circles");
        }
        this.c = c;
        this.r = r;
        if (empty) { //Otherwise is left 'null' so it does NOT appear in the serialisation
            this.empty = true;
        }
        this.type = type;
        setBoundaries();
    }

    private transient Double minX;
    private transient Double maxX;
    private transient Double minY;
    private transient Double maxY;

    private void setBoundaries() {
        List<Double> xx = new ArrayList<>();
        List<Double> yy = new ArrayList<>();
        switch (type) {
            case CIRCLE:
            case DOUBLE_CIRCLE:
                xx.add(c.getX() + r);
                yy.add(c.getY() + r);
                xx.add(c.getX() - r);
                yy.add(c.getY() - r);
                break;
            case ARROW:
                xx.add(c.getX());
                yy.add(c.getY());
            default:
                xx.add(a.getX());
                yy.add(a.getY());
                xx.add(b.getX());
                yy.add(b.getY());
        }

        this.minX = Collections.min(xx);
        this.maxX = Collections.max(xx);
        this.minY = Collections.min(yy);
        this.maxY = Collections.max(yy);
    }

    public boolean overlaps(Shape o2) {
        return pointInRectangle(o2.minX, o2.minY) ||
                pointInRectangle(o2.minX, o2.maxY) ||
                pointInRectangle(o2.maxX, o2.minY) ||
                pointInRectangle(o2.maxX, o2.maxY);
    }

    private boolean pointInRectangle(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public void move(double dx, double dy) {
        if (a != null) a.move(dx, dy);
        if (b != null) b.move(dx, dy);
        if (c != null) c.move(dx, dy);
    }

    public Coordinate getA() {
        return a;
    }

    public Coordinate getB() {
        return b;
    }

    public Coordinate getC() {
        return c;
    }

    public Integer getR() {
        return r;
    }

    public Integer getR1() {
        return r1;
    }

    public Type getType() {
        return type;
    }

    public Boolean getEmpty() {
        return empty;
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }
}
