package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.Stoichiometry;
import org.reactome.server.tools.diagram.data.layout.impl.*;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.LayoutAlgorithm;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe.Dedup.addDuplicates;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe.Transformer.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.INPUT;

/**
 * Standard layout algorithm. It places inputs on the left, outputs on the right, catalysts on top and regulators on the
 * bottom. Every group of participants is placed in a line perpendicular to the edge joining the center of the group to
 * the reaction, i.e. inputs and outputs vertically, catalysts and regulators horizontally. When a group of participants
 * lays into more than one compartment, the are placed into parallel lines, one behind the other, so compartments have
 * space to be drawn. At last, reaction is placed in the centre of the diagram, unless it falls into a compartment it
 * does not belong to. In that case, it is moved to a safer position. In order of priority, it's moved towards inputs,
 * if not possible towards outputs, then catalysts and regulators.
 */
public class BreatheAlgorithm implements LayoutAlgorithm {

    /**
     * Length of the backbone of the reaction
     */
    static final double BACKBONE_LENGTH = 20;
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
    private static final double VERTICAL_PADDING = 12;
    /**
     * Horizontal (x-axis) distance between two glyphs.
     */
    private static final double HORIZONTAL_PADDING = 12;
    /**
     * Minimum vertical distance between any glyph and the reaction glyph
     */
    private static final double REACTION_MIN_V_DISTANCE = 80;
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

    private LayoutIndex index;

    @Override
    public void compute(Layout layout) {
        addDuplicates(layout);
        index = new LayoutIndex(layout);
        layoutParticipants(layout);
        layoutCompartments(layout);
        layoutReaction(layout);
        // we need to recalculate compartment positions if reaction has been moved
        layoutCompartments(layout);
        layoutConnectors(layout);
        removeExtracellular(layout);
        computeDimension(layout);
        failedReactions(layout);
        moveToOrigin(layout);
    }

    private void layoutParticipants(Layout layout) {
        for (EntityGlyph entity : layout.getEntities()) setSize(entity);
        layoutInputs(layout);
        layoutOutputs(layout);
        layoutCatalysts(layout);
        layoutRegulators(layout);
    }

    private void layoutInputs(Layout layout) {
        final List<CompartmentGlyph> sortedCompartments = sortCompartmentsForInputs(layout.getCompartmentRoot());
        Collections.reverse(sortedCompartments);
        index.getInputs().sort(Comparator
                // input/catalysts first
                .comparing((EntityGlyph e) -> e.getRoles().size()).reversed()
                // sorted by compartment
                .thenComparingInt(entity -> sortedCompartments.indexOf(entity.getCompartment()))
                // non trivial then
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                // and sorted by RenderableClass
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                // and finally by name
                .thenComparing(EntityGlyph::getName));
        double heightPerGlyph = MIN_GLYPH_HEIGHT;
        for (EntityGlyph input : index.getInputs()) {
            final Position bounds = Transformer.getBounds(input);
            if (bounds.getHeight() > heightPerGlyph) heightPerGlyph = bounds.getHeight();
        }
        heightPerGlyph += VERTICAL_PADDING;
        final double totalHeight = heightPerGlyph * index.getInputs().size();
        final double yOffset = 0.5 * (totalHeight - heightPerGlyph);
        layoutVerticalEntities(layout.getCompartmentRoot(), index.getInputs(), yOffset, heightPerGlyph, (glyph, coord) ->
                Transformer.center(glyph, new CoordinateImpl(-coord.getX(), coord.getY())));
    }

    private void layoutOutputs(Layout layout) {
        final List<CompartmentGlyph> sortedCompartments = sortCompartmentsForInputs(layout.getCompartmentRoot());
        Collections.reverse(sortedCompartments);
        index.getOutputs().sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                .thenComparingInt(entity -> sortedCompartments.indexOf(entity.getCompartment()))
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));
        double heightPerGlyph = MIN_GLYPH_HEIGHT;
        for (EntityGlyph output : index.getOutputs()) {
            final Position bounds = Transformer.getBounds(output);
            if (bounds.getHeight() > heightPerGlyph) heightPerGlyph = bounds.getHeight();
        }
        heightPerGlyph += VERTICAL_PADDING;
        final double totalHeight = heightPerGlyph * index.getOutputs().size();
        final double yOffset = 0.5 * (totalHeight - heightPerGlyph);
        layoutVerticalEntities(layout.getCompartmentRoot(), index.getOutputs(), yOffset, heightPerGlyph, Transformer::center);
    }

    private void layoutCatalysts(Layout layout) {
        // Remove catalysts that are inputs
        index.getCatalysts().removeIf(entityGlyph -> entityGlyph.getRoles().stream().anyMatch(role -> role.getType() == INPUT));
        if (index.getCatalysts().isEmpty()) return;
        final List<CompartmentGlyph> sortedCompartments = sortCompartmentsForInputs(layout.getCompartmentRoot());
        index.getCatalysts().sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                .thenComparingInt(entity -> sortedCompartments.indexOf(entity.getCompartment()))
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));
        double widthPerGlyph = MIN_GLYPH_WIDTH;
        for (EntityGlyph catalyst : index.getCatalysts()) {
            final Position bounds = Transformer.getBounds(catalyst);
            if (bounds.getWidth() > widthPerGlyph) widthPerGlyph = bounds.getWidth();
        }
        widthPerGlyph += HORIZONTAL_PADDING;

        final double totalWidth = widthPerGlyph * index.getCatalysts().size();
        final double xOffset = 0.5 * (totalWidth - widthPerGlyph);
        layoutHorizontalEntities(layout.getCompartmentRoot(), index.getCatalysts(), xOffset, widthPerGlyph, (glyph, coord) ->
                Transformer.center(glyph, new CoordinateImpl(coord.getY(), coord.getX())));
    }

    private void layoutRegulators(Layout layout) {
        if (index.getRegulators().isEmpty()) return;
        final List<CompartmentGlyph> compartmentGlyphs = sortCompartmentsForInputs(layout.getCompartmentRoot());
        index.getRegulators().sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                .thenComparingInt(entity -> compartmentGlyphs.indexOf(entity.getCompartment()))
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));

        double widthPerGlyph = MIN_GLYPH_WIDTH;
        for (EntityGlyph regulator : index.getRegulators()) {
            final Position bounds = Transformer.getBounds(regulator);
            if (bounds.getWidth() > widthPerGlyph) widthPerGlyph = bounds.getWidth();
        }
        widthPerGlyph += HORIZONTAL_PADDING;

        final double totalWidth = widthPerGlyph * index.getRegulators().size();
        final double xOffset = 0.5 * (totalWidth - widthPerGlyph);
        layoutHorizontalEntities(layout.getCompartmentRoot(), index.getRegulators(), xOffset, widthPerGlyph, (glyph, coord) ->
                Transformer.center(glyph, new CoordinateImpl(coord.getY(), -coord.getX())));
    }

    /**
     * This is a deep first recursive method, so the entities of the inner compartments are rendered first. Entities are
     * placed top to bottom in the order of appearance in the list. All glyph share its <em>x</em> coordinate, depending
     * on the level of the compartment and width of entities. The <em>y</em> coordinated is calculated as
     * <code>y = -yOffset + i * heightPerGlyph</code>. Glyphs are centered at <em>(x,y)</em>
     *
     * @param compartment    current compartment
     * @param entities       list of sorted entities
     * @param yOffset        position of the first entity in the list
     * @param heightPerGlyph height that each glyph occupies, including padding
     * @param apply          operation to apply to entities once its position has been calculated, use this method to
     *                       invert axis or rotate the position.
     */
    private double layoutVerticalEntities(CompartmentGlyph compartment, List<EntityGlyph> entities, double yOffset, double heightPerGlyph, BiConsumer<EntityGlyph, Coordinate> apply) {
        double startX = 0;
        for (CompartmentGlyph child : compartment.getChildren()) {
            startX = Math.max(startX, layoutVerticalEntities(child, entities, yOffset, heightPerGlyph, apply));
        }
        final List<EntityGlyph> glyphs = entities.stream()
                .filter(entityGlyph -> compartment.getContainedGlyphs().contains(entityGlyph))
                .collect(Collectors.toList());
        if (glyphs.isEmpty()) return COMPARTMENT_PADDING + startX;
        double width = MIN_GLYPH_WIDTH;
        for (EntityGlyph entity : entities) {
            final Position bounds = getBounds(entity);
            if (bounds.getWidth() > width) width = bounds.getWidth();
        }
        width += HORIZONTAL_PADDING;
        final double x = REACTION_MIN_H_DISTANCE + startX + 0.5 * width;
        for (EntityGlyph glyph : glyphs) {
            final int i = entities.indexOf(glyph);
            final double y = -yOffset + i * heightPerGlyph;
            apply.accept(glyph, new CoordinateImpl(x, y));
        }
        return COMPARTMENT_PADDING + startX + width;
    }

    /**
     * This is a deep first recursive method, so the entities of the inner compartments are rendered first. Entities are
     * placed left to right in the order of appearance in the list. All glyph share its <em>y</em> coordinate, depending
     * on the level of the compartment and heights of entities. The <em>x</em> coordinated is calculated as
     * <code>x = -xOffset + i * widthPerGlyph</code>. Glyphs are centered at <em>(x,y)</em>
     *
     * @param compartment   current compartment
     * @param entities      list of sorted entities
     * @param xOffset       position of the first entity in the list
     * @param widthPerGlyph height that each glyph occupies, including padding
     * @param apply         operation to apply to entities once its position has been calculated, use this method to
     *                      invert axis or rotate the position.
     */
    private double layoutHorizontalEntities(CompartmentGlyph compartment, List<EntityGlyph> entities, double xOffset, double widthPerGlyph, BiConsumer<EntityGlyph, Coordinate> apply) {
        double startY = COMPARTMENT_PADDING;
        for (CompartmentGlyph child : compartment.getChildren()) {
            startY = Math.max(startY, layoutHorizontalEntities(child, entities, xOffset, widthPerGlyph, apply));
        }
        final List<EntityGlyph> glyphs = entities.stream()
                .filter(entityGlyph -> compartment.getContainedGlyphs().contains(entityGlyph))
                .collect(Collectors.toList());
        if (glyphs.isEmpty()) return COMPARTMENT_PADDING;
        double height = MIN_GLYPH_HEIGHT;
        for (EntityGlyph entity : entities) {
            final Position bounds = getBounds(entity);
            if (bounds.getHeight() > height) height = bounds.getHeight();
        }
        height += VERTICAL_PADDING;
        final double x = REACTION_MIN_V_DISTANCE + startY + 0.5 * height;
        for (EntityGlyph glyph : glyphs) {
            final int i = entities.indexOf(glyph);
            final double y = -xOffset + i * widthPerGlyph;
            apply.accept(glyph, new CoordinateImpl(-x, y));
        }
        return 2 * COMPARTMENT_PADDING + height;
    }

    /**
     * This step should be run after placing the entities.
     */
    private void layoutReaction(Layout layout) {
        final ReactionGlyph reaction = layout.getReaction();
        setSize(reaction);
        // We have to check whether the reaction is in the inner compartment or not to avoid rendering it over a wrong
        // compartment
        final Position position = reaction.getPosition();
        position.setCenter(0., 0.);
        final Collection<CompartmentGlyph> clashingCompartments = clashingCompartments(layout, reaction);
        if (clashingCompartments.size() > 0) {
            // We need to move reaction from center
            //first we have to decide where to move the reaction
            final double reactionSep = COMPARTMENT_PADDING + getBounds(reaction).getWidth();
            if (canMoveToInputs(reaction)) {
                double x = 0;
                for (CompartmentGlyph compartment : clashingCompartments)
                    if (compartment.getPosition().getX() < x) x = compartment.getPosition().getX();
                if (x != 0) {
                    reaction.getPosition().setCenter(x - reactionSep, 0);
                    for (EntityGlyph input : index.getInputs()) move(input, x - reactionSep, 0);
//                    for (EntityGlyph regulator : regulators)
//                        if (canMove(regulator, x - 15, 0))
//                            move(regulator, x - 15, 0);
                }
            } else if (canMoveToOutputs(reaction)) {
                double x = 0;
                for (CompartmentGlyph child : clashingCompartments) {
                    if (child.getPosition().getMaxX() > x) x = child.getPosition().getMaxX();
                }
                if (x != 0) {
                    reaction.getPosition().setCenter(x + reactionSep, 0);
//                    for (EntityGlyph input : inputs) input.getPosition().move(x+15, 0);
                    for (EntityGlyph output : index.getOutputs()) move(output, x + reactionSep, 0);
                }

            } else if (canMoveToRegulators(reaction)) {
                double y = 0;
                for (CompartmentGlyph child : clashingCompartments) {
                    if (child.getPosition().getMaxY() > y) y = child.getPosition().getMaxY();
                }
                if (y != 0) {
                    reaction.getPosition().setCenter(0, y + reactionSep);
                    for (EntityGlyph regulator : index.getRegulators()) move(regulator, 0, y + reactionSep);
                    for (EntityGlyph catalyst : index.getCatalysts()) move(catalyst, 0, y + reactionSep);
                }
            } else if (canMoveToCatalysts(reaction)) {
                double y = 0;
                for (CompartmentGlyph child : clashingCompartments)
                    if (child.getPosition().getY() < y) y = child.getPosition().getY();
                if (y != 0) {
                    reaction.getPosition().setCenter(0, y - reactionSep);
                    for (EntityGlyph catalyst : index.getCatalysts()) move(catalyst, 0, y - reactionSep);
                    for (EntityGlyph regulator : index.getRegulators()) move(regulator, 0, y - reactionSep);
                }
            }
        }
        // Add backbones
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getX(), position.getCenterY()),
                new CoordinateImpl(position.getX() - BACKBONE_LENGTH, position.getCenterY())));
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getMaxX(), position.getCenterY()),
                new CoordinateImpl(position.getMaxX() + BACKBONE_LENGTH, position.getCenterY())));
    }

    /**
     * Get the list of compartments that clash with the reaction.
     */
    private Collection<CompartmentGlyph> clashingCompartments(Layout layout, ReactionGlyph reaction) {
        return layout.getCompartments().stream()
                .filter(compartment -> clashes(reaction, compartment))
                .collect(Collectors.toList());
    }

    /**
     * A compartment clashes with the reaction if they intersect and the reaction does not belong to the compartment or
     * any of its children.
     */
    private boolean clashes(ReactionGlyph reaction, CompartmentGlyph compartment) {
        return compartment != reaction.getCompartment()
                && !isChild(compartment, reaction.getCompartment())
                && compartment.getPosition().intersects(reaction.getPosition());
    }

    /**
     * Test if child is a descendant of compartment.
     */
    private boolean isChild(CompartmentGlyph compartment, CompartmentGlyph child) {
        CompartmentGlyph parent = child.getParent();
        while (parent != null) {
            if (parent == compartment) return true;
            parent = parent.getParent();
        }
        return false;
    }

    private boolean canMoveToInputs(ReactionGlyph reaction) {
        return index.getInputs().stream().noneMatch(glyph -> clashes(reaction, glyph.getCompartment()));
    }

    private boolean canMoveToOutputs(ReactionGlyph reaction) {
        return index.getOutputs().stream().noneMatch(glyph -> clashes(reaction, glyph.getCompartment()));
    }

    private boolean canMoveToRegulators(ReactionGlyph reaction) {
        return index.getRegulators().stream().noneMatch(glyph -> clashes(reaction, glyph.getCompartment()));
    }

    private boolean canMoveToCatalysts(ReactionGlyph reaction) {
        return index.getCatalysts().stream().noneMatch(glyph -> clashes(reaction, glyph.getCompartment()));
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
        final double port = reactionPosition.getX() - BACKBONE_LENGTH;
        final double vRule = port - REACTION_MIN_H_DISTANCE + BACKBONE_LENGTH + reactionPosition.getWidth() + MIN_SEGMENT;
        for (EntityGlyph entity : index.getInputs()) {
            final Position position = entity.getPosition();
            // is catalyst and input
            final boolean biRole = entity.getRoles().size() > 1;
            // Catalyst
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            entity.setConnector(connector);
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
            double y = biRole ? position.getCenterY() + 5 : position.getCenterY();
            // Input
            if (entity.getRenderableClass() == RenderableClass.GENE) {
                segments.add(new SegmentImpl(position.getMaxX() + 8, position.getY(),
                        position.getMaxX() + 30, y));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getMaxX() + 30, y),
                        new CoordinateImpl(vRule, y)));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(vRule, y),
                        new CoordinateImpl(port, reactionPosition.getCenterY())));
            } else {
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getMaxX(), y),
                        new CoordinateImpl(vRule, y)));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(vRule, y),
                        new CoordinateImpl(port, reactionPosition.getCenterY())));
            }
            if (biRole) {
                // Add catalyst segments
                y = position.getCenterY() - 5;
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getMaxX(), y),
                        new CoordinateImpl(vRule + 50, y)));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(vRule + 50, y),
                        new CoordinateImpl(reactionPosition.getCenterX(), reactionPosition.getCenterY())));
                connector.setPointer(ConnectorType.CATALYST);
            } else {
                connector.setPointer(ConnectorType.INPUT);
            }

            for (Role role : entity.getRoles()) {
                if (role.getType() == INPUT) {
                    final Stoichiometry s = getStoichiometry(segments, role);
                    connector.setStoichiometry(s);
                }
            }
        }
    }

    private List<CompartmentGlyph> sortCompartmentsForInputs(CompartmentGlyph compartment) {
        final List<CompartmentGlyph> order = new ArrayList<>();
        order.add(compartment);
        for (final CompartmentGlyph child : compartment.getChildren()) {
            order.addAll(sortCompartmentsForInputs(child));
        }
        return order;
    }

    private void outputConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getMaxX() + BACKBONE_LENGTH;
        final double vRule = port + REACTION_MIN_H_DISTANCE - BACKBONE_LENGTH - reactionPosition.getWidth() - MIN_SEGMENT;
        for (EntityGlyph entity : index.getOutputs()) {
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
            entity.setConnector(connector);
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
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            entity.setConnector(connector);
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
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
        // shapes without touching
        final double radius = reactionPosition.getHeight() / 2 + REGULATOR_SIZE * sectors / Math.PI;
        int i = 1;
        for (EntityGlyph entity : index.getRegulators()) {
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            entity.setConnector(connector);
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
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
     * Creates the stoichiometry box in the last segment.
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
        if (position.getWidth() < textPadding) {
            double diff = textPadding - position.getWidth();
            position.setWidth(textPadding);
            position.setX(position.getX() - 0.5 * diff);
        }
        final Coordinate coordinate = new CoordinateImpl(
                position.getMaxX() - textWidth - 15,
                position.getMaxY() + 0.5 * textHeight - COMPARTMENT_PADDING);
        compartment.setLabelPosition(coordinate);
        compartment.setPosition(position);
    }

    private void computeDimension(Layout layout) {
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            layout.getPosition().union(compartment.getPosition());
        }
        for (EntityGlyph entity : layout.getEntities()) {
            layout.getPosition().union(Transformer.getBounds(entity));
        }
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
     *
     * @param layout
     */
    private void removeExtracellular(Layout layout) {
        layout.getCompartments().remove(layout.getCompartmentRoot());
    }

    private void failedReactions(Layout layout) {
        if (index.getOutputs().isEmpty()) layout.getPosition().setWidth(layout.getPosition().getWidth() + 150);
        if (index.getInputs().isEmpty()) {
            layout.getPosition().setWidth(layout.getPosition().getWidth() + 150);
        }
    }

}
