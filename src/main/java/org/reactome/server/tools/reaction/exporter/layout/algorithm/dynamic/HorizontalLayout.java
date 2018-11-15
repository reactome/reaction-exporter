package org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic;

import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Role;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HorizontalLayout implements Div {

    private List<? extends Glyph> glyphs;
    private double horizontalPadding = 10;
    private double verticalPadding = 10;
    private double separation = 5;
    private Position bounds;

    public HorizontalLayout(List<? extends Glyph> glyphs) {
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
        return String.format("horizontal (%d)", glyphs.size());
    }

    @Override
    public Position getBounds() {
        if (bounds == null) bounds = horizontal();
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

    @Override
    public Set<EntityRole> containedRoles() {
        return glyphs.stream()
                .filter(EntityGlyph.class::isInstance)
                .map(EntityGlyph.class::cast)
                .flatMap(entityGlyph -> entityGlyph.getRoles().stream())
                .map(Role::getType)
                .collect(Collectors.toSet());
    }

    private Position horizontal() {
        if (glyphs.isEmpty()) return new Position(0d, 0d, 2 * horizontalPadding, 2 * verticalPadding);
        final double height = glyphs.stream().map(Transformer::getBounds).mapToDouble(Position::getHeight).max().orElse(0);
        final double cy = 0.5 * height + verticalPadding;
        double x = horizontalPadding;
        for (final Glyph glyph : glyphs) {
            final double width = Transformer.getBounds(glyph).getWidth();
            final double cx = x + 0.5 * width;
            Transformer.center(glyph, new CoordinateImpl(cx, cy));
            x += separation + width;
        }
        return new Position(0d, 0d, x - separation + horizontalPadding, height + 2 * verticalPadding);
    }


}
