package org.reactome.server.tools.reaction.exporter.layout.algorithm.grid;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.Stoichiometry;
import org.reactome.server.tools.diagram.data.layout.impl.*;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.LayoutAlgorithm;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.FontProperties;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.LayoutIndex;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Dedup.addDuplicates;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;

/**
 * Standard layout algorithm. It places inputs on the left, outputs on the right, catalysts on top and regulators on the
 * bottom. Every group of participants is placed in a line perpendicular to the edge joining the center of the group to
 * the reaction, i.e. inputs and outputs vertically, catalysts and regulators horizontally. When a group of participants
 * lays into more than one compartment, the are placed into parallel lines, one behind the other, so compartments have
 * space to be drawn. At last, reaction is placed in the centre of the diagram, unless it falls into a compartment it
 * does not belong to. In that case, it is moved to a safer position. In order of priority, it's moved towards inputs,
 * if not possible towards outputs, then catalysts and regulators.
 */
@SuppressWarnings("Duplicates")
public class GridAlgorithm implements LayoutAlgorithm {

    private static final int COLUMN_PADDING = 20;
    /**
     * Length of the backbone of the reaction
     */
    private static final double BACKBONE_LENGTH = 20;
    /**
     * Size of the box surrounding regulator and catalysts shapes
     */
    private static final int REGULATOR_SIZE = 6;
    /**
     * Minimum length of segments departing participants.
     */
    private static final int MIN_SEGMENT = 35;
    /**
     * Minimum distance between the compartment border and any of ints contained glyphs.
     */
    private static final double COMPARTMENT_PADDING = 20;
    /**
     * Minimum allocated height for any glyph. Even if glyphs have a lower height, they are placed in the middle of this
     * minimum distance.
     */
    private static final double MIN_GLYPH_HEIGHT = 25;
    /**
     * Minimum allocated width for any glyph. Even if glyphs have a lower width, they are placed in the middle of this
     * minimum distance.
     */
    private static final double MIN_GLYPH_WIDTH = 60;
    /**
     * Vertical (y-axis) distance between two glyphs.
     */
    private static final double VERTICAL_PADDING = 4;
    /**
     * Horizontal (x-axis) distance between two glyphs.
     */
    private static final double HORIZONTAL_PADDING = 12;
    /**
     * Minimum vertical distance between any glyph and the reaction glyph
     */
    private static final double REACTION_MIN_V_DISTANCE = 60;
    /**
     * Minimum horizontal distance between any glyph and the reaction glyph
     */
    private static final double REACTION_MIN_H_DISTANCE = 200;
    /**
     * Order in which nodes should be placed depending on their {@link RenderableClass}
     */
    private static final List<RenderableClass> CLASS_ORDER = Arrays.asList(
            RenderableClass.PROCESS_NODE,
            RenderableClass.ENCAPSULATED_NODE,
            RenderableClass.COMPLEX,
            RenderableClass.ENTITY_SET,
            RenderableClass.PROTEIN,
            RenderableClass.RNA,
            RenderableClass.CHEMICAL,
            RenderableClass.GENE,
            RenderableClass.ENTITY);
    /**
     * Comparator that puts false (and null) elements before true elements.
     */
    private static final Comparator<Boolean> FALSE_FIRST = Comparator.nullsFirst((o1, o2) -> o1.equals(o2) ? 0 : o1 ? 1 : -1);
    private static final Comparator<Boolean> TRUE_FIRST = Comparator.nullsLast((o1, o2) -> o1.equals(o2) ? 0 : o1 ? -1 : 1);
    private static final double ARROW_SIZE = 8;
    private LayoutIndex index;

    @Override
    public void compute(Layout layout) {
        addDuplicates(layout);
        index = new LayoutIndex(layout);
        layoutParticipants(layout);
        layoutCompartments(layout);
        // we need to recalculate compartment positions if reaction has been moved
        // layoutCompartments(layout);
        layoutConnectors(layout);
        removeExtracellular(layout);
        computeDimension(layout);
        moveToOrigin(layout);
    }

    private List<CompartmentGlyph> order(Layout layout) {
        final List<CompartmentGlyph> compartments = flattenCompartments(layout.getCompartmentRoot());
        compartments.sort((o1, o2) -> Comparator
                .comparing(((CompartmentGlyph cg) -> containsRole(cg, CATALYST)), TRUE_FIRST)
                .thenComparing(cg -> containsRole(cg, NEGATIVE_REGULATOR), FALSE_FIRST)
                .thenComparing(cg -> containsRole(cg, POSITIVE_REGULATOR), FALSE_FIRST)
                .compare(o1, o2));
        return compartments;
    }

    private boolean containsRole(CompartmentGlyph compartment, EntityRole role) {
        for (Glyph glyph : compartment.getContainedGlyphs()) {
            if (glyph instanceof EntityGlyph && hasRole((EntityGlyph) glyph, role)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRole(EntityGlyph glyph, EntityRole role) {
        return glyph.getRoles().stream().anyMatch(r -> r.getType() == role);
    }

    private void layoutParticipants(Layout layout) {
        for (EntityGlyph entity : layout.getEntities()) setSize(entity);

        // We are going to use the empty position several times: we can reuse this one
        final Position empty = new Position();

        // We will layout by row, every row is a compartment
        final List<CompartmentGlyph> compartments = order(layout);

        // Keep track of groups of glyphs
        final List<List<List<? extends Glyph>>> matrix = new ArrayList<>();
        // Compute bounds of each group
        final List<List<Position>> bounds = new ArrayList<>();
        for (final CompartmentGlyph compartment : compartments) {
            // top down: CA, IN/R/OUT, REG
            final List<EntityGlyph> catalysts = index.getCatalysts().stream()
                    .filter(entityGlyph -> entityGlyph.getCompartment() == compartment)
                    .collect(Collectors.toList());
            catalysts.removeIf(entityGlyph -> hasRole(entityGlyph, INPUT));
            final Position c = horizontal(catalysts);
            final List<EntityGlyph> inputs = index.getInputs().stream()
                    .filter(entityGlyph -> entityGlyph.getCompartment() == compartment)
                    .collect(Collectors.toList());
            final Position i = vertical(inputs);
            final List<EntityGlyph> outputs = index.getOutputs().stream()
                    .filter(entityGlyph -> entityGlyph.getCompartment() == compartment)
                    .collect(Collectors.toList());
            final Position o = vertical(outputs);
            final List<EntityGlyph> regulators = index.getRegulators().stream()
                    .filter(entityGlyph -> entityGlyph.getCompartment() == compartment)
                    .collect(Collectors.toList());
            final Position reg = horizontal(regulators);
            Position r = new Position();
            final List<Glyph> reaction = new ArrayList<>();
            if (layout.getReaction().getCompartment() == compartment) {
                r = reaction(layout.getReaction());
                reaction.add(layout.getReaction());
            }
            if (!catalysts.isEmpty()) {
                matrix.add(Arrays.asList(Collections.emptyList(), catalysts, Collections.emptyList()));
                bounds.add(Arrays.asList(empty, c, empty));
            }
            matrix.add(Arrays.asList(inputs, reaction, outputs));
            bounds.add(Arrays.asList(i, r, o));
            if (!regulators.isEmpty()) {
                matrix.add(Arrays.asList(Collections.emptyList(), regulators, Collections.emptyList()));
                bounds.add(Arrays.asList(empty, reg, empty));
            }
        }

        // Compute overall widths
        double[] ws = new double[3];
        Arrays.fill(ws, 0);
        for (int i = 0; i < matrix.size(); i++) {
            for (int j = 0; j < 3; j++) {
                final double width = bounds.get(i).get(j).getWidth();
                if (width > ws[j]) ws[j] = width;
            }
        }
        //
        double y = 0;
        for (int i = 0; i < matrix.size(); i++) {
            // Compute max height
            double height = 0;
            for (int j = 0; j < 3; j++) {
                if (bounds.get(i).get(j).getHeight() > height)
                    height = bounds.get(i).get(j).getHeight();
            }
            // Center the 3 groups at height
            height += 2 * VERTICAL_PADDING;
            double cy = y + 0.5 * height;
            double x = 0;
            for (int j = 0; j < 3; j++) {
                final Position position = bounds.get(i).get(j);
                double cx = x + 0.5 * ws[j];
                double dx = cx - position.getCenterX();
                double dy = cy - position.getCenterY();
                final CoordinateImpl delta = new CoordinateImpl(dx, dy);
                for (final Glyph glyph : matrix.get(i).get(j)) {
                    Transformer.move(glyph, delta);
                }
                x += ws[j];
            }
            y += height;
        }
    }

    private Position reaction(ReactionGlyph reaction) {
        Transformer.setSize(reaction);
        // Add backbones
        final Position position = reaction.getPosition();
        reaction.getSegments().add(new SegmentImpl(
                position.getX(), position.getCenterY(),
                position.getX() - BACKBONE_LENGTH, position.getCenterY()));
        reaction.getSegments().add(new SegmentImpl(
                position.getMaxX(), position.getCenterY(),
                position.getMaxX() + BACKBONE_LENGTH, position.getCenterY()));
        return Transformer.padd(Transformer.getBounds(reaction), 200, 100);
    }

    private Position horizontal(List<EntityGlyph> glyphs) {
        if (glyphs.isEmpty()) return new Position();
        final double height = glyphs.stream().map(Transformer::getBounds).mapToDouble(Position::getHeight).max().orElse(MIN_GLYPH_HEIGHT);
        final double y = 0.5 * (height + VERTICAL_PADDING);
        double x = 0;
        for (final EntityGlyph glyph : glyphs) {
            final double width = Transformer.getBounds(glyph).getWidth();
            Transformer.center(glyph, new CoordinateImpl(x + 0.5 * (HORIZONTAL_PADDING + width), y));
            x += HORIZONTAL_PADDING + width;
        }
        return Transformer.padd(new Position(0d, 0d, x, height + VERTICAL_PADDING), COMPARTMENT_PADDING);
    }

    private Position vertical(List<EntityGlyph> glyphs) {
        if (glyphs.isEmpty()) return new Position();
        final double width = glyphs.stream().map(Transformer::getBounds).mapToDouble(Position::getWidth).max().orElse(MIN_GLYPH_WIDTH);
        final double x = 0.5 * (width + HORIZONTAL_PADDING);
        double y = 0;
        for (final EntityGlyph glyph : glyphs) {
            final double height = Transformer.getBounds(glyph).getHeight();
            Transformer.center(glyph, new CoordinateImpl(x, y + 0.5 * (VERTICAL_PADDING + height)));
            y += VERTICAL_PADDING + height;
        }
        return Transformer.padd(new Position(0d, 0d, width + HORIZONTAL_PADDING, y), COMPARTMENT_PADDING);
    }


    private void layoutConnectors(Layout layout) {
        inputConnectors(layout);
        outputConnectors(layout);
        catalystConnectors(layout);
        regulatorConnectors(layout);
    }

    private void inputConnectors(Layout layout) {
        // we have to deal with the case that inputs can be catalysts as well, in that case two connector have to be
        // created. This method supposes that the inputs are on the top left corner of the diagram.
        final Position reactionPosition = layout.getReaction().getPosition();
        final double mx = index.getInputs().stream().map(Transformer::getBounds).mapToDouble(Position::getMaxX).max().orElse(0);
        final double vRule = mx + MIN_SEGMENT;
        final double port = reactionPosition.getX() - BACKBONE_LENGTH;
        for (EntityGlyph entity : index.getInputs()) {
            final Position position = entity.getPosition();
            // is catalyst and input
            final boolean biRole = entity.getRoles().size() > 1;
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            // Input
            if (entity.getRenderableClass() == RenderableClass.GENE) {
                // Genes need an extra segment from the arrow
                segments.add(new SegmentImpl(position.getMaxX() + 8, position.getY(),
                        position.getMaxX() + 30, position.getCenterY()));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getMaxX() + 30, position.getCenterY()),
                        new CoordinateImpl(vRule, position.getCenterY())));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(vRule, position.getCenterY()),
                        new CoordinateImpl(port, reactionPosition.getCenterY())));
            } else {
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getMaxX(), position.getCenterY()),
                        new CoordinateImpl(vRule, position.getCenterY())));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(vRule, position.getCenterY()),
                        new CoordinateImpl(port, reactionPosition.getCenterY())));
            }
            if (biRole) {
                // Add catalyst segments
                final double top = Math.min(position.getY(), reactionPosition.getY()) - 5;
                segments.add(new SegmentImpl(position.getCenterX(), position.getY(), position.getCenterX(), top));
                segments.add(new SegmentImpl(position.getCenterX(), top, vRule + 50, top));
                segments.add(new SegmentImpl(vRule + 50, top, reactionPosition.getCenterX(), reactionPosition.getCenterY()));
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

    private ConnectorImpl createConnector(EntityGlyph entity) {
        final ConnectorImpl connector = new ConnectorImpl();
        final List<Segment> segments = new ArrayList<>();
        connector.setSegments(segments);
        entity.setConnector(connector);
        return connector;
    }

    /**
     * Flattens the compartment tree, putting the outermost ones at the beginning. root is always the first
     */
    private List<CompartmentGlyph> flattenCompartments(CompartmentGlyph compartment) {
        final List<CompartmentGlyph> order = new ArrayList<>();
        order.add(compartment);
        for (final CompartmentGlyph child : compartment.getChildren()) {
            order.addAll(flattenCompartments(child));
        }
        return order;
    }


    private void outputConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getMaxX() + BACKBONE_LENGTH;
        final double mx = index.getOutputs().stream().map(Transformer::getBounds).mapToDouble(Position::getX).min().orElse(0);
        final double vRule = mx - MIN_SEGMENT - ARROW_SIZE;
        for (EntityGlyph entity : index.getOutputs()) {
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            final Position position = entity.getPosition();
            segments.add(new SegmentImpl(
                    new CoordinateImpl(position.getX() - 4, position.getCenterY()),
                    new CoordinateImpl(vRule, position.getCenterY())));
            segments.add(new SegmentImpl(
                    new CoordinateImpl(vRule, position.getCenterY()),
                    new CoordinateImpl(port, reactionPosition.getCenterY())));
            // only one role expected: OUTPUT
            for (Role role : entity.getRoles()) {
                connector.setPointer(getConnectorType(role.getType()));
                connector.setStoichiometry(getStoichiometry(segments, role));
            }
        }
    }

    private ConnectorType getConnectorType(EntityRole type) {
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

    private void catalystConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getCenterY();
        final double hRule = port - REACTION_MIN_V_DISTANCE;
        for (EntityGlyph entity : index.getCatalysts()) {
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            final Position position = entity.getPosition();
            segments.add(new SegmentImpl(
                    new CoordinateImpl(position.getCenterX(), position.getMaxY()),
                    new CoordinateImpl(position.getCenterX(), hRule)));
            segments.add(new SegmentImpl(
                    new CoordinateImpl(position.getCenterX(), hRule),
                    new CoordinateImpl(reactionPosition.getCenterX(), port)));
            // only one role expected: CATALYST
            for (Role role : entity.getRoles()) {
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setType(role.getType().name());
                connector.setPointer(getConnectorType(role.getType()));
            }
        }
    }

    private void regulatorConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double hRule = reactionPosition.getMaxY() + REACTION_MIN_V_DISTANCE;
        // we want to fit all catalysts in a semi-circumference, not using the corners
        final int sectors = index.getRegulators().size() + 1;
        // the semicircle is centered into the reaction, and its length (PI*radius) should be enough to fit all the
        // shapes without touching each other
        final double radius = reactionPosition.getHeight() / 2 + REGULATOR_SIZE * sectors / Math.PI;
        int i = 1;
        final ArrayList<EntityGlyph> regulators = new ArrayList<>(index.getRegulators());
        regulators.sort(Comparator.comparingDouble(v -> v.getPosition().getCenterX()));
        for (EntityGlyph entity : regulators) {
            final ConnectorImpl connector = createConnector(entity);
            final List<Segment> segments = connector.getSegments();
            final Position position = entity.getPosition();
            segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(), position.getCenterX(), hRule));
            final double x = reactionPosition.getCenterX() - radius * Math.cos(Math.PI * i / sectors);
            final double y = reactionPosition.getCenterY() + radius * Math.sin(Math.PI * i / sectors);
            segments.add(new SegmentImpl(position.getCenterX(), hRule, x, y));
            // Only one role expected (negative or positive)
            for (Role role : entity.getRoles()) {
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setPointer(getConnectorType(role.getType()));
            }
            i++;
        }
    }

    /**
     * Creates the stoichiometry box in the first segment.
     */
    private Stoichiometry getStoichiometry(List<Segment> segments, Role role) {
        if (role.getStoichiometry() == 1)
            return new StoichiometryImpl(1, null);
        final Segment segment = segments.get(0);
        final Coordinate center = center(segment);
        final Coordinate a = new CoordinateImpl(center.getX() - 6, center.getY() - 6);
        final Coordinate b = new CoordinateImpl(center.getX() + 6, center.getY() + 6);
        final Shape shape = new BoxImpl(a, b, true, role.getStoichiometry().toString());
        return new StoichiometryImpl(role.getStoichiometry(), shape);
    }

    private Coordinate center(Segment segment) {
        return new CoordinateImpl(
                0.5 * (segment.getFrom().getX() + segment.getTo().getX()),
                0.5 * (segment.getFrom().getY() + segment.getTo().getY())
        );
    }

    private void layoutCompartments(Layout layout) {
        layoutCompartment(layout.getCompartmentRoot());
    }

    /**
     * Calculates the size of the compartments so each of them surrounds all of its contained glyphs and children.
     */
    private void layoutCompartment(CompartmentGlyph compartment) {
        for (CompartmentGlyph child : compartment.getChildren()) {
            layoutCompartment(child);
        }
        Position position = null;
        for (CompartmentGlyph child : compartment.getChildren()) {
            if (position == null) position = new Position(child.getPosition());
            else position.union(child.getPosition());
        }
        for (Glyph glyph : compartment.getContainedGlyphs()) {
            if (position == null) position = new Position(getBounds(glyph));
            else position.union(getBounds(glyph));
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

    private void computeDimension(Layout layout) {
        Position position = null;
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            if (position == null) position = new Position(compartment.getPosition());
            else position.union(compartment.getPosition());
        }
        for (EntityGlyph entity : layout.getEntities()) {
            if (position == null) position = new Position(Transformer.getBounds(entity));
            else position.union(Transformer.getBounds(entity));
        }
        if (position == null) position = new Position(Transformer.getBounds(layout.getReaction()));
        else position.union(Transformer.getBounds(layout.getReaction()));
        layout.getPosition().set(position);
    }

    private void moveToOrigin(Layout layout) {
        final double dx = -layout.getPosition().getX();
        final double dy = -layout.getPosition().getY();
        final Coordinate delta = new CoordinateImpl(dx, dy);
        layout.getPosition().move(dx, dy);
        move(layout.getCompartmentRoot(), delta, true);
    }

    /**
     * This operation should be called in the last steps, to avoid being exported to a Diagram object.
     */
    private void removeExtracellular(Layout layout) {
        layout.getCompartments().remove(layout.getCompartmentRoot());
    }

}
