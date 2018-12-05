package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.Stoichiometry;
import org.reactome.server.tools.diagram.data.layout.impl.*;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Constants;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.*;

import static java.lang.Math.*;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Constants.MIN_SEGMENT;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;

class ConnectorFactory {

    private ConnectorFactory() {
    }

    private static ConnectorImpl createConnector(EntityGlyph entity) {
        final ConnectorImpl connector = new ConnectorImpl();
        final List<Segment> segments = new ArrayList<>();
        connector.setSegments(segments);
        entity.setConnector(connector);
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

    static void addConnectors(Point reactionPosition, Grid<Div> grid, double[] widths, double[] heights, Layout layout, LayoutIndex index) {
        // Points of interest
        double x1 = 0;
        double y1 = 0;
        int i = reactionPosition.getCol() - 1;
        while (i >= 0 && !hasRole(grid.getColumn(i), INPUT)) i--;
        for (int j = 0; j <= i; j++) x1 += widths[j];

        i = reactionPosition.getRow() - 1;
        while (i>= 0 && !hasRole(grid.getRow(i), CATALYST)) i--;
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

        double cx = layout.getReaction().getPosition().getCenterX();
        double cy = layout.getReaction().getPosition().getCenterY();
        double rx1 = layout.getReaction().getPosition().getX() - Constants.BACKBONE_LENGTH;
        double rx2 = layout.getReaction().getPosition().getMaxX() + Constants.BACKBONE_LENGTH;
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

    private static boolean hasRole(Div[] divs, EntityRole role) {
        return Arrays.stream(divs).filter(Objects::nonNull).anyMatch(div -> div.getContainedRoles().contains(role));
    }

    private static void inputs(LayoutIndex index, double x, double rx, double cy, double cx) {
        for (final EntityGlyph entity : index.getInputs()) {
            final Position position = Transformer.getBounds(entity);
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            if (entity.getRenderableClass() == RenderableClass.GENE) {
                // Genes need an extra segment from the arrow
                segments.add(new SegmentImpl(position.getMaxX() + 8, position.getY(),
                        position.getMaxX() + Constants.GENE_SEGMENT_LENGTH, position.getCenterY()));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getMaxX() + Constants.GENE_SEGMENT_LENGTH, position.getCenterY()),
                        new CoordinateImpl(x, position.getCenterY())));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(x, position.getCenterY()),
                        new CoordinateImpl(rx, cy)));
            } else {
                segments.add(new SegmentImpl(position.getMaxX(), position.getCenterY(), x, position.getCenterY()));
                segments.add(new SegmentImpl(x, position.getCenterY(), rx, cy));
            }
            if (entity.getRoles().size() > 1) {
                // Add catalyst segments
                final double top = min(position.getY() - 10, cy - 50);
                final double catalystPosition = cx - 20;
                segments.add(new SegmentImpl(position.getCenterX(), position.getY(), position.getCenterX(), top));
                segments.add(new SegmentImpl(position.getCenterX(), top, catalystPosition, top));
                segments.add(new SegmentImpl(catalystPosition, top, cx, cy));
                connector.setPointer(ConnectorType.CATALYST);
            } else {
                connector.setPointer(ConnectorType.INPUT);
            }
            // We expect to have stoichiometry only in input role
            for (Role role : entity.getRoles())
                if (role.getType() == INPUT) {
                    connector.setStoichiometry(getStoichiometry(segments, role));
                    break;
                }
        }
    }

    private static void outputs(LayoutIndex index, double x2, double rx2, double cy) {
        for (final EntityGlyph entity : index.getOutputs()) {
            final Position position = Transformer.getBounds(entity);
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            segments.add(new SegmentImpl(position.getX() - 4, position.getCenterY(), x2, position.getCenterY()));
            segments.add(new SegmentImpl(x2, position.getCenterY(),rx2, cy));
            // only one role expected: OUTPUT
            for (Role role : entity.getRoles()) {
                connector.setPointer(getConnectorType(role.getType()));
                connector.setStoichiometry(getStoichiometry(segments, role));
            }
        }
    }

    private static void catalysts(LayoutIndex index, Grid<Div> grid, Point reactionPosition, double cx, double y1, double cy) {
        for (int c = 0; c < grid.getColumns(); c++) {
            for (int r = 0; r < reactionPosition.getRow(); r++) {
                final Div div = grid.get(r, c);
                if (div instanceof HorizontalLayout) {
                    final HorizontalLayout box = (HorizontalLayout) div;
                    if (c == reactionPosition.getCol()) {
                        for (final Glyph glyph : box.getGlyphs()) {
                            final EntityGlyph entity = (EntityGlyph) glyph;
                            final Position position = Transformer.getBounds(entity);
                            final ConnectorImpl connector = createConnector(entity);
                            final List<Segment> segments = connector.getSegments();
                            segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(), position.getCenterX(), y1));
                            segments.add(new SegmentImpl(position.getCenterX(), y1, cx, cy));
                            // only one role expected: CATALYST
                            for (Role role : entity.getRoles()) {
                                connector.setStoichiometry(getStoichiometry(segments, role));
                                connector.setPointer(getConnectorType(role.getType()));
                            }
                        }
                    } else if (c < reactionPosition.getCol()) {
                        // Behaviour as inputs
                        for (final Glyph glyph : box.getGlyphs()) {
                            final EntityGlyph entity = (EntityGlyph) glyph;
                            final Position position = Transformer.getBounds(entity);
                            final ConnectorImpl connector = createConnector(entity);
                            final List<Segment> segments = connector.getSegments();
                            final double x1 = position.getMaxX() + MIN_SEGMENT;
                            segments.add(new SegmentImpl(position.getMaxX(), position.getCenterY(), x1, position.getCenterY()));
                            segments.add(new SegmentImpl(x1, position.getCenterY(), cx, cy));
                            // only one role expected: CATALYST
                            for (Role role : entity.getRoles()) {
                                connector.setStoichiometry(getStoichiometry(segments, role));
                                connector.setPointer(getConnectorType(role.getType()));
                            }
                        }
                    } else {
                        // As outputs
                        for (final Glyph glyph : box.getGlyphs()) {
                            final EntityGlyph entity = (EntityGlyph) glyph;
                            final Position position = Transformer.getBounds(entity);
                            final ConnectorImpl connector = createConnector(entity);
                            final List<Segment> segments = connector.getSegments();
                            final double x2 = position.getX() - MIN_SEGMENT;
                            segments.add(new SegmentImpl(position.getX(), position.getCenterY(), x2, position.getCenterY()));
                            segments.add(new SegmentImpl(x2, position.getCenterY(), cx, cy));
                            // only one role expected: CATALYST
                            for (Role role : entity.getRoles()) {
                                connector.setStoichiometry(getStoichiometry(segments, role));
                                connector.setPointer(getConnectorType(role.getType()));
                            }
                        }
                    }
                }
            }
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
        final Position reactionPosition = reactionGlyph.getPosition();
        double my = index.getRegulators().stream().map(Transformer::getBounds).mapToDouble(Position::getY).min().orElse(0);
        final double hRule = my - MIN_SEGMENT;
        // we want to fit all catalysts in a semi-circumference, not using the corners
        final int sectors = index.getRegulators().size() + 1;
        // the semi-circumference is centered into the reaction, and its length (PI*radius) should be enough to fit all
        // the shapes without touching each other
        int i = 1;
        regulators.sort(Comparator.comparingDouble(v -> v.getPosition().getCenterX()));
        final boolean left = regulators.stream().noneMatch(e -> e.getPosition().getMaxX() > reactionPosition.getX());
        final boolean right = regulators.stream().noneMatch(e -> e.getPosition().getX() < reactionPosition.getMaxX());
        double startAngle = 0;
        double totalAngle = PI;
        if (left) {
            totalAngle = 0.5 * PI;
        }
        if (right) {
            startAngle = 0.5 * PI;
            totalAngle = 0.5 * PI;
        }
        double radius = reactionPosition.getHeight() / 2 + Constants.REGULATOR_SIZE * sectors / totalAngle;
        for (EntityGlyph entity : regulators) {
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            final Position position = Transformer.getBounds(entity);
            segments.add(new SegmentImpl(position.getCenterX(), position.getY(), position.getCenterX(), hRule));
            final double x = reactionPosition.getCenterX() - radius * cos((startAngle + totalAngle) * i / sectors);
            final double y = reactionPosition.getCenterY() + radius * sin((startAngle + totalAngle) * i / sectors);
            segments.add(new SegmentImpl(position.getCenterX(), hRule, x, y));
            // Only one role expected (negative or positive)
            for (Role role : entity.getRoles()) {
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setPointer(getConnectorType(role.getType()));
            }
            i++;
        }
    }
}
