package org.reactome.server.tools.reaction.exporter.layout.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Contains the position and dimension plus extra methods to ease the maths
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class Position {

    private double x, y, width, height;
    private double cx, cy;
    private double mx, my;

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        this.mx = x + width;
        this.cx = x + 0.5 * width;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        this.my = y + height;
        this.cy = y + 0.5 * height;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
        this.mx = x + width;
        this.cx = x + 0.5 * width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
        this.my = y + height;
        this.cy = y + 0.5 * height;
    }

    public void setCenter(double x, double y) {
        this.x = x - 0.5 * this.width;
        this.y = y - 0.5 * this.height;
        this.cx = x;
        this.cy = y;
        this.mx = this.x + width;
        this.my = this.y + height;
    }

//    private void setSize() {
//        final Dimension2D dimension = TextUtils.splitText(displayName);
//        switch (renderableClass) {
//            case CHEMICAL:
//            case CHEMICAL_DRUG:
//            case COMPLEX:
//            case ENTITY:
//            case PROTEIN:
//            case RNA:
//                setWidth(6 + dimension.getWidth());
//                setHeight(6 + dimension.getHeight());
//                break;
//            case ENCAPSULATED_NODE:
//            case PROCESS_NODE:
//            case ENTITY_SET:
//                setWidth(15 + dimension.getWidth());
//                setHeight(15 + dimension.getHeight());
//                break;
//            case REACTION:
//                setHeight(12);
//                setWidth(12);
//                break;
//            case GENE:
//                setWidth(6 + dimension.getWidth());
//                setHeight(30 + dimension.getHeight());
//        }
//    }

    @JsonIgnore
    public double getMaxX() {
        return mx;
    }

    @JsonIgnore
    public double getMaxY() {
        return my;
    }

//    public Point2D getCenter() {
//        return new Point2D.Double(cx, cy);
//    }

    @JsonIgnore
    public double getCenterX() {
        return cx;
    }

    @JsonIgnore
    public double getCenterY() {
        return cy;
    }

//
//    public Rectangle2D getBoundingBox() {
//        return new Rectangle2D.Double(x,y,width,height);
//    }
}
