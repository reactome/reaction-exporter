package org.reactome.server.tools.reaction.exporter.layout.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Contains the position and dimension plus extra methods to ease the maths
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class Bounds {

    private double x, y, width, height;
    private double cx, cy;
    private double mx, my;

    public Bounds(){}

    public Bounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.cx = x + 0.5 * width;
        this.cy = y + 0.5 * height;
        this.mx = x + width;
        this.my = y + height;
    }

    public Bounds(Bounds bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;
        this.cx = bounds.cx;
        this.cy = bounds.cy;
        this.mx = bounds.mx;
        this.my = bounds.my;
    }

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

    @JsonIgnore
    public double getMaxX() {
        return mx;
    }

    @JsonIgnore
    public double getMaxY() {
        return my;
    }

    @JsonIgnore
    public double getCenterX() {
        return cx;
    }

    @JsonIgnore
    public double getCenterY() {
        return cy;
    }

    public void move(double dx, double dy) {
        setX(x + dx);
        setY(y + dy);
    }
    /**
     * Creates the union between <em>this</em> and <em>that</em> and sets the result into this. The union of two
     * rectangles is defined as the smallest rectangle that contains both rectangles.
     *
     * @param that the second position for the union
     */
    public void union(Bounds that) {
        if (that == null) return;
        final double minX = Math.min(this.x, that.x);
        final double minY = Math.min(this.y, that.y);
        final double maxX = Math.max(this.mx, that.mx);
        final double maxY = Math.max(this.my, that.my);
        setX(minX);
        setY(minY);
        setWidth(maxX - minX);
        setHeight(maxY - minY);
    }

    public void set(Bounds bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        setWidth(bounds.width);
        setHeight(bounds.height);
    }

    @Override
    public String toString() {
        return "Bounds{" +
                "x=" + x +
                ", y=" + y +
                ", w=" + width +
                ", h=" + height +
                '}';
    }

    public boolean intersects(Bounds bounds) {
        return intersects(x, mx, bounds.x, bounds.mx) && intersects(y, my, bounds.y, bounds.my);
    }

    /**
     * returns true if segments r and s intersect.
     *
     * @param r0 lower bound of r
     * @param r1 upper boud of r
     * @param s0 lower bound of s
     * @param s1 upper bound of s
     * @return true if both segments intersect
     */
    private boolean intersects(double r0, double r1, double s0, double s1) {
        return r0 < s0 && s0 < r1 || s0 < r0 && r0 < s1;
    }
}
