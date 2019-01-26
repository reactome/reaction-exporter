package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Common class for divs that contain glyph groups.
 */
public abstract class GlyphsLayout implements Div {

    private List<? extends Glyph> glyphs;
    private double leftPadding = 10;
    private double rightPadding = 10;
    private double topPadding = 10;
    private double bottomPadding = 10;
    private double separation = 5;
    private Position bounds;

    GlyphsLayout(List<? extends Glyph> glyphs) {
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
        return String.format("%d glyphs", glyphs.size());
    }

    @Override
    public Position getBounds() {
        if (bounds == null) bounds = layout();
        return bounds;
    }


    @Override
    public void setHorizontalPadding(double padding) {
        leftPadding = rightPadding = padding;
    }

    @Override
    public void setVerticalPadding(double padding) {
        topPadding = bottomPadding = padding;
    }

    @Override
    public void setPadding(double padding) {
        leftPadding = rightPadding = topPadding = bottomPadding = padding;
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
    public Set<EntityRole> getContainedRoles() {
        return glyphs.stream()
                .filter(EntityGlyph.class::isInstance)
                .map(EntityGlyph.class::cast)
                .flatMap(entityGlyph -> entityGlyph.getRoles().stream())
                .map(Role::getType)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<Place> getBusyPlaces() {
        final Glyph glyph = glyphs.get(0);
        if (glyph instanceof ReactionGlyph)
            return EnumSet.of(Place.CENTER);
        return glyphs.stream()
                .map(EntityGlyph.class::cast)
                .flatMap(entiyt -> entiyt.getRoles().stream())
                .map(Role::getType)
                .map(PlacePositioner::getPlace)
                .collect(Collectors.toSet());
    }

    double getSeparation() {
        return separation;
    }

    @Override
    public double getBottomPadding() {
        return bottomPadding;
    }

    @Override
    public double getLeftPadding() {
        return leftPadding;
    }

    @Override
    public double getRightPadding() {
        return rightPadding;
    }

    @Override
    public double getTopPadding() {
        return topPadding;
    }

    @Override
    public void setBottomPadding(double bottomPadding) {
        this.bottomPadding = bottomPadding;
    }

    @Override
    public void setLeftPadding(double leftPadding) {
        this.leftPadding = leftPadding;
    }

    @Override
    public void setRightPadding(double rightPadding) {
        this.rightPadding = rightPadding;
    }

    @Override
    public void setTopPadding(double topPadding) {
        this.topPadding = topPadding;
    }

    abstract Position layout();

    @Override
    public CompartmentGlyph getCompartment() {
        if (glyphs.isEmpty()) return null;
        final Glyph glyph = glyphs.get(0);
        if (glyph instanceof ReactionGlyph)
            return ((ReactionGlyph) glyph).getCompartment();
        else if (glyph instanceof EntityGlyph)
            return ((EntityGlyph) glyph).getCompartment();
        return null;
    }
}
