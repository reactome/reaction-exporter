package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.Stoichiometry;
import org.reactome.server.tools.diagram.data.layout.impl.*;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Constants;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.Bounds;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.*;

import static java.lang.Math.*;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Constants.MIN_SEGMENT;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;

/**
 * Helper class to add connectors to diagram via {@link ConnectorFactory#addConnectors(Point, Grid, double[], double[],
 * Layout, LayoutIndex)}. This class relies in that reaction layout is properly placed: inputs on the left, outputs on
 * the right, catalysts on top, and regulators at the bottom.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class ConnectorFactory {

    private ConnectorFactory() {
    }

    public static void addConnectors(Point reactionPosition, Grid<Div> grid, double[] widths, double[] heights, Layout layout, LayoutIndex index) {
        // Points of interest
        double x1 = 0;
        double y1 = 0;
        int i = reactionPosition.getCol() - 1;
        while (i >= 0 && !hasRole(grid.getColumn(i), INPUT)) i--;
        for (int j = 0; j <= i; j++) x1 += widths[j];

        i = reactionPosition.getRow() - 1;
        while (i >= 0 && !hasRole(grid.getRow(i), CATALYST)) i--;
        for (int j = 0; j <= i; j++) y1 += heights[j];

        double x2 = 0;
        i = 0;
        while (i < grid.getColumns() && !(hasRole(grid.getColumn(i), OUTPUT))) {
            x2 += widths[i++];
        }
        double y2 = 0;
        i = 0;
        while (i < grid.getRows() && !(hasRole(grid.getRow(i), POSITIVE_REGULATOR) || hasRole(grid.getRow(i), NEGATIVE_REGULATOR))) {
            y2 += heights[i++];
        }

        double cx = layout.getReaction().getBounds().getCenterX();
        double cy = layout.getReaction().getBounds().getCenterY();
        double rx1 = layout.getReaction().getBounds().getX() - Constants.BACKBONE_LENGTH;
        double rx2 = layout.getReaction().getBounds().getMaxX() + Constants.BACKBONE_LENGTH;
        /*
         *      x1 rx1 cx  rx2  x2
         * y1 ---------------------
         *      |   |       |   |
         * cy   |   |---o---|   |
         *      |   |       |   |
         * y2 ---------------------
         */
        inputs(index, x1, rx1, cy, cx);
        outputs(index, x2, rx2, cy);
        catalysts(index, grid, reactionPosition, cx, y1, cy);
        regulators(index, grid, reactionPosition, x1, x2, cx, y2, cy);
    }

    private static ConnectorImpl createConnector(EntityGlyph entity) {
        final ConnectorImpl connector = new ConnectorImpl();
        final List<Segment> segments = new ArrayList<>();
        connector.setSegments(segments);
        entity.addConnector(connector);
        return connector;
    }

    private static ConnectorType getConnectorType(EntityRole type) {
        switch (type) {
            case OUTPUT:
                return ConnectorType.OUTPUT;
            case CATALYST:
                return ConnectorType.CATALYST;
            case NEGATIVE_REGULATOR:
                return ConnectorType.INHIBITOR;
            case POSITIVE_REGULATOR:
                return ConnectorType.ACTIVATOR;
            case INPUT:
            default:
                return ConnectorType.INPUT;
        }
    }

    /**
     * Creates the stoichiometry box in the first segment.
     */
    private static Stoichiometry getStoichiometry(List<Segment> segments, Role role) {
        if (role.getStoichiometry() == 1)
            return new StoichiometryImpl(1, null);
        final Segment segment = segments.get(0);
        final Coordinate center = getCenter(segment);
        final Coordinate a = new CoordinateImpl(center.getX() - 6, center.getY() - 6);
        final Coordinate b = new CoordinateImpl(center.getX() + 6, center.getY() + 6);
        final Shape shape = new BoxImpl(a, b, true, role.getStoichiometry().toString());
        return new StoichiometryImpl(role.getStoichiometry(), shape);
    }

    private static Coordinate getCenter(Segment segment) {
        return new CoordinateImpl(
                0.5 * (segment.getFrom().getX() + segment.getTo().getX()),
                0.5 * (segment.getFrom().getY() + segment.getTo().getY())
        );
    }

    private static boolean hasRole(Div[] divs, EntityRole role) {
        return Arrays.stream(divs).filter(Objects::nonNull).anyMatch(div -> div.getContainedRoles().contains(role));
    }

    private static void inputs(LayoutIndex index, double x, double rx, double cy, double cx) {
        for (final EntityGlyph entity : index.getInputs()) {
            final Bounds bounds = Transformer.getBounds(entity);
            ConnectorImpl connector;
            List<Segment> segments;
            // We expect to have stoichiometry only in input role
            for (Role role : entity.getRoles()) {
                switch (role.getType()) {
                    case INPUT:
                        connector = createConnector(entity);
                        segments = connector.getSegments();
                        if (entity.getRenderableClass() == RenderableClass.GENE) {
                            // Genes need an extra segment from the arrow
                            segments.add(new SegmentImpl(bounds.getMaxX() + 8, bounds.getY(),
                                    bounds.getMaxX() + Constants.GENE_SEGMENT_LENGTH, bounds.getCenterY()));
                            segments.add(new SegmentImpl(
                                    new CoordinateImpl(bounds.getMaxX() + Constants.GENE_SEGMENT_LENGTH, bounds.getCenterY()),
                                    new CoordinateImpl(x, bounds.getCenterY())));
                            segments.add(new SegmentImpl(
                                    new CoordinateImpl(x, bounds.getCenterY()),
                                    new CoordinateImpl(rx, cy)));
                        } else {
                            segments.add(new SegmentImpl(bounds.getMaxX(), bounds.getCenterY(), x, bounds.getCenterY()));
                            segments.add(new SegmentImpl(x, bounds.getCenterY(), rx, cy));
                        }
                        connector.setPointer(ConnectorType.INPUT);
                        connector.setStoichiometry(getStoichiometry(segments, role));
                        break;
                    case CATALYST:
                        // Add catalyst segments
                        connector = createConnector(entity);
                        segments = connector.getSegments();
                        final double top = min(bounds.getY() - 10, cy - 50);
                        final double catalystPosition = cx - 20;
                        segments.add(new SegmentImpl(bounds.getCenterX(), bounds.getY(), bounds.getCenterX(), top));
                        segments.add(new SegmentImpl(bounds.getCenterX(), top, catalystPosition, top));
                        segments.add(new SegmentImpl(catalystPosition, top, cx, cy));
                        connector.setPointer(ConnectorType.CATALYST);
                        break;
                }
            }

        }
    }

    private static void outputs(LayoutIndex index, double x2, double rx2, double cy) {
        for (final EntityGlyph entity : index.getOutputs()) {
            final Bounds bounds = Transformer.getBounds(entity);
            final List<Segment> segments = Arrays.asList(
                    new SegmentImpl(bounds.getX() - 4, bounds.getCenterY(), x2, bounds.getCenterY()),
                    new SegmentImpl(x2, bounds.getCenterY(), rx2, cy));
            createConnector(entity, segments);
        }
    }

    private static void catalysts(LayoutIndex index, Grid<Div> grid, Point reactionPosition, double cx, double y1, double cy) {
        // Instead of going through catalysts in index, we are going to add connectors to every HorizontalLayout over
        // the reaction
        final List<EntityGlyph> catalysts = index.getCatalysts();
        for (int c = 0; c < grid.getColumns(); c++) {
            for (int r = 0; r < reactionPosition.getRow(); r++) {
                final Div div = grid.get(r, c);
                if (div instanceof HorizontalLayout) {
                    final HorizontalLayout box = (HorizontalLayout) div;
                    if (c == reactionPosition.getCol() || catalysts.size() > 1) {
                        addConnectorsToCenterCatalysts(cx, y1, cy, box.getGlyphs());
                    } else if (c < reactionPosition.getCol()) {
                        addConnectorToLeftCatalysts(cx, cy, box.getGlyphs());
                    } else {
                        addConnectorsToRightCatalysts(cx, cy, box.getGlyphs());
                    }
                }
            }
        }
    }

    private static void addConnectorsToCenterCatalysts(double cx, double y1, double cy, List<? extends Glyph> glyphs) {
        for (final Glyph glyph : glyphs) {
            final EntityGlyph entity = (EntityGlyph) glyph;
            final Bounds bounds = Transformer.getBounds(entity);
            createConnector(entity, Arrays.asList(
                    new SegmentImpl(bounds.getCenterX(), bounds.getMaxY(), bounds.getCenterX(), y1),
                    new SegmentImpl(bounds.getCenterX(), y1, cx, cy)));
        }
    }

    private static void addConnectorToLeftCatalysts(double cx, double cy, List<? extends Glyph> glyphs) {
        // Behaviour as inputs
        for (final Glyph glyph : glyphs) {
            final EntityGlyph entity = (EntityGlyph) glyph;
            final Bounds bounds = Transformer.getBounds(entity);
            final double x1 = bounds.getMaxX() + MIN_SEGMENT;
            createConnector(entity, Arrays.asList(
                    new SegmentImpl(bounds.getMaxX(), bounds.getCenterY(), x1, bounds.getCenterY()),
                    new SegmentImpl(x1, bounds.getCenterY(), cx, cy)));
        }
    }

    private static void addConnectorsToRightCatalysts(double cx, double cy, List<? extends Glyph> glyphs) {
        // Behaviour as outputs
        for (final Glyph glyph : glyphs) {
            final EntityGlyph entity = (EntityGlyph) glyph;
            final Bounds bounds = Transformer.getBounds(entity);
            final double x2 = bounds.getX() - MIN_SEGMENT;
            createConnector(entity, Arrays.asList(
                    new SegmentImpl(bounds.getX(), bounds.getCenterY(), x2, bounds.getCenterY()),
                    new SegmentImpl(x2, bounds.getCenterY(), cx, cy)));
        }
    }

    private static void createConnector(EntityGlyph entity, List<Segment> segments) {
        final ConnectorImpl connector = new ConnectorImpl();
        connector.setSegments(new ArrayList<>(segments));
        entity.addConnector(connector);
        for (Role role : entity.getRoles()) {
            connector.setStoichiometry(getStoichiometry(segments, role));
            connector.setPointer(getConnectorType(role.getType()));
        }
    }

    private static void regulators(LayoutIndex index, Grid<Div> grid, Point reactBox, double x1, double x2, double cx, double y2, double cy) {
        // this is following the previous approach
        final List<EntityGlyph> regulators = new ArrayList<>();
        for (int c = 0; c < grid.getColumns(); c++) {
            for (int r = reactBox.getRow() + 1; r < grid.getRows(); r++) {
                final Div div = grid.get(r, c);
                if (div instanceof HorizontalLayout) {
                    final HorizontalLayout horizontalLayout = (HorizontalLayout) div;
                    regulators.addAll((Collection<EntityGlyph>) horizontalLayout.getGlyphs());
                }
            }
        }
        final HorizontalLayout layout = (HorizontalLayout) grid.get(reactBox.getRow(), reactBox.getCol());
        final ReactionGlyph reactionGlyph = (ReactionGlyph) layout.getGlyphs().iterator().next();
        final Bounds reactionBounds = reactionGlyph.getBounds();
        double my = index.getRegulators().stream().map(Transformer::getBounds).mapToDouble(Bounds::getY).min().orElse(0);
        final double hRule = my - MIN_SEGMENT;
        // we want to fit all catalysts in a semi-circumference, not using the corners
        final int sectors = index.getRegulators().size() + 1;
        // the semi-circumference is centered into the reaction, and its length (PI*radius) should be enough to fit all
        // the shapes without touching each other
        int i = 1;
        regulators.sort(Comparator.comparingDouble(v -> v.getBounds().getCenterX()));
        final boolean left = regulators.stream().noneMatch(e -> e.getBounds().getMaxX() > reactionBounds.getX());
        final boolean right = regulators.stream().noneMatch(e -> e.getBounds().getX() < reactionBounds.getMaxX());
        double startAngle = 0;
        double totalAngle = PI;
        if (left) {
            totalAngle = 0.5 * PI;
        }
        if (right) {
            startAngle = 0.5 * PI;
            totalAngle = 0.5 * PI;
        }
        double radius = reactionBounds.getHeight() / 2 + Constants.REGULATOR_SIZE * sectors / totalAngle;
        for (EntityGlyph entity : regulators) {
            final Bounds bounds = Transformer.getBounds(entity);
            final double x = reactionBounds.getCenterX() - radius * cos((startAngle + totalAngle) * i / sectors);
            final double y = reactionBounds.getCenterY() + radius * sin((startAngle + totalAngle) * i / sectors);
            final List<Segment> segments = Arrays.asList(
                    new SegmentImpl(bounds.getCenterX(), bounds.getY(), bounds.getCenterX(), hRule),
                    new SegmentImpl(bounds.getCenterX(), hRule, x, y));
            createConnector(entity, segments);
            i++;
        }
    }
}
