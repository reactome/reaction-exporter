package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Dedup;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.FontProperties;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;

import java.awt.*;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer.getBounds;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer.move;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.CATALYST;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.INPUT;
import static org.reactome.server.tools.reaction.exporter.layout.common.GlyphUtils.hasRole;

public class BoxAlgorithm {

    private static final double COMPARTMENT_PADDING = 20;

    private final Layout layout;
    private final LayoutIndex index;

    public BoxAlgorithm(Layout layout) {
        this.layout = layout;
        Dedup.addDuplicates(layout);
        index = new LayoutIndex(layout);
        fixReactionWithNoCompartment(layout);
    }

    /**
     * This fixes reaction R-HSA-9006323 that does not contain a compartment. It can also happen in other species.
     */
    private void fixReactionWithNoCompartment(Layout layout) {
        if (layout.getReaction().getCompartment() == null) {
            layout.getReaction().setCompartment(layout.getCompartmentRoot());
            layout.getCompartmentRoot().getContainedGlyphs().add(layout.getReaction());
        }
    }

    public void compute() {
        final Box box = new Box(layout.getCompartmentRoot(), index);
        final Point reactionPosition = box.placeReaction();
        box.placeElements(reactionPosition);
        final Div[][] divs = box.getDivs();

        final int rows = divs.length;
        final double heights[] = new double[rows];
        final int cols = divs[0].length;
        final double widths[] = new double[cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                final Div div = divs[row][col];
                if (div == null) continue;
                final Position bounds = div.getBounds();
                if (bounds.getWidth() > widths[col]) widths[col] = bounds.getWidth();
                if (bounds.getHeight() > heights[row]) heights[row] = bounds.getHeight();
            }
        }
        for (int i = 0; i < cols / 2; i+= 2) widths[i] += COMPARTMENT_PADDING;
        for (int i = cols - 1; i > cols / 2; i-= 2) widths[i] += COMPARTMENT_PADDING;
        for (int i = 0; i < rows / 2; i+= 2) heights[i] += COMPARTMENT_PADDING;
        for (int i = rows - 1; i > rows / 2; i-= 2) heights[i] += COMPARTMENT_PADDING;
        final double cy[] = new double[rows];
        cy[0] = 0.5 * heights[0];
        for (int i = 1; i < rows; i++) {
            cy[i] = cy[i - 1] + 0.5 * heights[i - 1] + 0.5 * heights[i];
        }
        final double cx[] = new double[cols];
        cx[0] = 0.5 * widths[0];
        for (int i = 1; i < cols; i++) {
            cx[i] = cx[i - 1] + 0.5 * widths[i - 1] + 0.5 * widths[i];
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                final Div div = divs[row][col];
                if (div == null) continue;
                div.center(cx[col], cy[row]);
            }
        }

        ConnectorFactory.addConnectors(layout, index);
        layoutCompartments(layout);
        removeExtracellular(layout);
        computeDimension(layout);
        moveToOrigin(layout);
    }

    private void layoutCompartments(Layout layout) {
        layoutCompartment(layout.getCompartmentRoot());
    }

    /**
     * Calculates the size of the compartments so each of them surrounds all of its contained glyphs and children.
     */
    private void layoutCompartment(CompartmentGlyph compartment) {
        Position position = null;
        for (CompartmentGlyph child : compartment.getChildren()) {
            layoutCompartment(child);
            if (position == null) position = new Position(child.getPosition());
            else position.union(child.getPosition());
        }
        for (Glyph glyph : compartment.getContainedGlyphs()) {
            if (position == null) position = new Position(getBounds(glyph));
            else position.union(getBounds(glyph));
            if (glyph instanceof EntityGlyph) {
                final EntityGlyph entityGlyph = (EntityGlyph) glyph;
                if (hasRole(entityGlyph, CATALYST, INPUT)) {
                    double topy = entityGlyph.getPosition().getY();
                    for (final Segment segment : entityGlyph.getConnector().getSegments()) {
                        if (segment.getFrom().getY() < topy) topy = segment.getFrom().getY();
                    }
                    position.union(new Position(entityGlyph.getPosition().getX(), topy, 1, 1));
                }
            }
        }
        position.setX(position.getX() - COMPARTMENT_PADDING);
        position.setY(position.getY() - COMPARTMENT_PADDING);
        position.setWidth(position.getWidth() + 2 * COMPARTMENT_PADDING);
        position.setHeight(position.getHeight() + 2 * COMPARTMENT_PADDING);

        final double textWidth = FontProperties.getTextWidth(compartment.getName());
        final double textHeight = FontProperties.getTextHeight();
        final double textPadding = textWidth + 30;
        // If the text is too large, we increase the size of the compartment
        if (position.getWidth() < textPadding) {
            double diff = textPadding - position.getWidth();
            position.setWidth(textPadding);
            position.setX(position.getX() - 0.5 * diff);
        }
        // Puts text in the bottom right corner of the compartment
        final Coordinate coordinate = new CoordinateImpl(
                position.getMaxX() - textWidth - 15,
                position.getMaxY() + 0.5 * textHeight - COMPARTMENT_PADDING);
        compartment.setLabelPosition(coordinate);
        compartment.setPosition(position);
    }

    /**
     * This operation should be called in the last steps, to avoid being exported to a Diagram object.
     */
    private void removeExtracellular(Layout layout) {
        layout.getCompartments().remove(layout.getCompartmentRoot());
    }

    private void computeDimension(Layout layout) {
        Position position = null;
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            if (position == null) position = new Position(compartment.getPosition());
            else position.union(compartment.getPosition());
        }
        for (EntityGlyph entity : layout.getEntities()) {
            final Position bounds = Transformer.getBounds(entity);
            if (position == null) position = new Position(bounds);
            else position.union(bounds);
            for (final Segment segment : entity.getConnector().getSegments()) {
                final double minX = Math.min(segment.getFrom().getX(), segment.getTo().getX());
                final double maxX = Math.max(segment.getFrom().getX(), segment.getTo().getX());
                final double minY = Math.min(segment.getFrom().getY(), segment.getTo().getY());
                final double maxY = Math.max(segment.getFrom().getY(), segment.getTo().getY());
                position.union(new Position(minX, minY, maxX - minX, maxY - minY));
            }
        }
        final Position bounds = Transformer.getBounds(layout.getReaction());
        if (position == null) position = new Position(bounds);
        else position.union(bounds);
        layout.getPosition().set(position);
    }

    private void moveToOrigin(Layout layout) {
        final double dx = -layout.getPosition().getX();
        final double dy = -layout.getPosition().getY();
        final Coordinate delta = new CoordinateImpl(dx, dy);
        layout.getPosition().move(dx, dy);
        move(layout.getCompartmentRoot(), delta, true);
    }
}
