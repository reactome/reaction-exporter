package org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic;

import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;

import java.util.Arrays;
import java.util.List;

public class VerticalLayout implements Div {

    private List<? extends Glyph> glyphs;
    private double horizontalPadding = 10;
    private double verticalPadding = 10;
    private double separation = 5;
    private Position bounds;

    public VerticalLayout(List<? extends Glyph> glyphs) {
        this.glyphs = glyphs;
    }

    public void setSeparation(double separation) {
        this.separation = separation;
    }

    public List<? extends Glyph> getGlyphs() {
        return glyphs;
    }

    @Override
    public String toString() {
        return String.format("vertical (%d)", glyphs.size());
    }

    @Override
    public Position getBounds() {
        if (bounds == null) bounds = vertical();
        return bounds;
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
    public void setPadding(double padding) {
        horizontalPadding = verticalPadding = padding;
    }

    @Override
    public void center(double x, double y) {
        final Position bounds = getBounds();
        move(x - bounds.getCenterX(), y - bounds.getCenterY());
    }

    @Override
    public void move(double dx, double dy) {
        final Position bounds = getBounds();
        final CoordinateImpl delta = new CoordinateImpl(dx, dy);
        for (final Glyph glyph : glyphs) Transformer.move(glyph, delta);
        bounds.move(dx, dy);
    }

    private Position vertical() {
        if (glyphs.isEmpty()) return new Position(0d, 0d, 2 * horizontalPadding, 2 * verticalPadding);
        if (glyphs.size() > 6) return layoutInTwoColumns();
        final double width = glyphs.stream().map(Transformer::getBounds).mapToDouble(Position::getWidth).max().orElse(0);
        final double cx = horizontalPadding + 0.5 * width;
        double y = verticalPadding;
        for (final Glyph glyph : glyphs) {
            final double height = Transformer.getBounds(glyph).getHeight();
            final double cy = y + 0.5 * height;
            Transformer.center(glyph, new CoordinateImpl(cx, cy));
            y += separation + height;
        }
        return new Position(0d, 0d, width + 2 * horizontalPadding, y - separation + verticalPadding);
    }

    private Position layoutInTwoColumns() {
        // the width of each column
        final int columns = 2;
        double[] widths = new double[columns];
        Arrays.fill(widths, 0);
        for (int i = 0; i < glyphs.size(); i++) {
            final double width = Transformer.getBounds(glyphs.get(i)).getWidth();
            final int j = i % columns;
            if (width > widths[j]) widths[j] = width;
        }
        // the x for each column (the center)
        final double[] xs = new double[columns];
        Arrays.fill(xs, 0);
        xs[0] = 0.5 * widths[0];
        for (int i = 1; i < columns; i++) {
            xs[i] = xs[i - 1] + 0.5 * widths[i - 1] + 0.5 * widths[i] + separation;
        }
        final double height = glyphs.stream().map(Transformer::getBounds).mapToDouble(Position::getHeight).max().orElse(0);
        final double step = 0.5 * (height + separation);
        final Position limits = new Position();
        double y = 0;
        for (int i = 0; i < glyphs.size(); i++) {
            final Glyph glyph = glyphs.get(i);
            y += step;
            final double x = xs[i % columns];
            Transformer.center(glyph, new CoordinateImpl(x, y));
            limits.union(Transformer.getBounds(glyph));
        }
        return limits;
    }

}
