package org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.FontProperties;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic.BorderLayout.Place.*;

/**
 * Like divs in html. It can only contain glyphs or children, but never both.
 */
final class BorderLayout implements Div {
    /**
     * Minimum distance between the compartment border and any of ints contained glyphs.
     */
    private static final double COMPARTMENT_PADDING = 10;
    private static final int TEXT_PADDING = 10;

    private final Map<Place, Div> layoutMap = new HashMap<>();
    /**
     * This is the compartment surrounding the border layout, in case it is not null
     */
    private CompartmentGlyph compartment;
    private double verticalPadding;
    private double horizontalPadding;
    private Position bounds;

    void set(Place place, Div borderLayout) {
        if (borderLayout == null) layoutMap.remove(place);
        else layoutMap.put(place, borderLayout);
    }

    public CompartmentGlyph getCompartment() {
        return compartment;
    }

    public void setCompartment(CompartmentGlyph compartment) {
        this.compartment = compartment;
    }

    @Override
    public String toString() {
        final StringJoiner joiner = new StringJoiner(",", "{", "}");
        layoutMap.forEach((place, div) -> joiner.add(String.format("%s=%s", place, div)));
        return (compartment == null ? "" : compartment.getName() + ":") + joiner.toString();
    }

    Set<Place> getBusyPlaces() {
        return layoutMap.keySet();
    }

    @Override
    public Position getBounds() {
        if (bounds == null) borderLayout();
        return bounds;
    }

    @Override
    public void setPadding(double padding) {
        verticalPadding = horizontalPadding = padding;
    }

    @Override
    public void setHorizontalPadding(double padding) {
        horizontalPadding = padding;
    }

    @Override
    public void setVerticalPadding(double padding) {
        verticalPadding = padding;
    }

    @Override
    public void center(double x, double y) {
        final Position bounds = getBounds();
        move(x - bounds.getCenterX(), y - bounds.getCenterY());
    }

    @Override
    public void move(double dx, double dy) {
        final Position bounds = getBounds();
        for (final Div div : layoutMap.values()) div.move(dx, dy);
        bounds.move(dx, dy);
        if (compartment != null) Transformer.move(compartment, dx, dy);
    }

    private void borderLayout() {
        bounds = new Position();
        final Div top = get(TOP);
        final Div bottom = get(BOTTOM);
        final Div left = get(LEFT);
        final Div right = get(RIGHT);
        final Div center = get(CENTER);

        final double w1 = left == null ? 0 : left.getBounds().getWidth();
        double w2 = 0;
        if (top != null) w2 = top.getBounds().getWidth();
        if (center != null) w2 = Math.max(w2, center.getBounds().getWidth());
        if (bottom != null) w2 = Math.max(w2, bottom.getBounds().getWidth());
        final double w3 = right == null ? 0 : right.getBounds().getWidth();

        final double cx1 = 0.5 * w1;
        final double cx2 = w1 + 0.5 * w2;
        final double cx3 = w1 + w2 + 0.5 * w3;

        final double h1 = top == null ? 0 : top.getBounds().getHeight();
        double h2 = 0;
        if (left != null) h2 = left.getBounds().getHeight();
        if (center != null) h2 = Math.max(h2, center.getBounds().getHeight());
        if (right != null) h2 = Math.max(h2, right.getBounds().getHeight());
        final double h3 = bottom == null ? 0 : bottom.getBounds().getHeight();

        final double cy1 = 0.5 * h1;
        final double cy2 = h1 + 0.5 * h2;
        final double cy3 = h1 + h2 + 0.5 * h3;

        if (top != null) top.center(cx2, cy1);
        if (bottom != null) bottom.center(cx2, cy3);
        if (right != null) right.center(cx3, cy2);
        if (left != null) left.center(cx1, cy2);
        if (center != null) center.center(cx2, cy2);

        for (final Div div : layoutMap.values()) bounds.union(div.getBounds());
        bounds = Transformer.padd(bounds, horizontalPadding, verticalPadding);
        placeCompartment();
    }

    private void placeCompartment() {
        if (compartment != null) {
            final Position compartmentPosition = Transformer.padd(bounds, COMPARTMENT_PADDING);

            // Place text
            final double textWidth = FontProperties.getTextWidth(compartment.getName());
            final double textHeight = FontProperties.getTextHeight();
            final double textPadding = textWidth + 2 * TEXT_PADDING;
            if (compartmentPosition.getWidth() < textPadding) {
                // Enlarge and center
                double diff = textPadding - compartmentPosition.getWidth();
                compartmentPosition.move(-0.5 * diff, 0);
                compartmentPosition.setWidth(textPadding);
            }
            final Coordinate coordinate;
            if (textWidth > 0.5 * compartmentPosition.getWidth()) {
                // center
                coordinate = new CoordinateImpl(compartmentPosition.getCenterX() - 0.5 * textWidth,
                        compartmentPosition.getMaxY() - textHeight - COMPARTMENT_PADDING / 2);
            } else {
                // southeast
                coordinate = new CoordinateImpl(
                        compartmentPosition.getMaxX() - textWidth - TEXT_PADDING,
                        compartmentPosition.getMaxY() - textHeight - COMPARTMENT_PADDING / 2);
            }
            compartment.setLabelPosition(coordinate);
            compartment.setPosition(compartmentPosition);
            bounds.set(compartmentPosition);
        }
    }

    public Div get(Place place) {
        return layoutMap.get(place);
    }

    enum Place {
        TOP, BOTTOM, CENTER, RIGHT, LEFT
    }


}
