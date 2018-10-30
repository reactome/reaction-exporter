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
import org.reactome.server.tools.reaction.exporter.layout.text.TextUtils;

import java.awt.geom.Dimension2D;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe.Translator.getBounds;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe.Translator.move;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.CATALYST;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.INPUT;

public class BreatheAlgorithm implements LayoutAlgorithm {

    /**
     * Length of the backbone of the reaction
     */
    static final double BACKBONE_LENGTH = 20;
    private static final int REGULATOR_SIZE = 4;
    private static final int MIN_SEGMENT = 20;
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
     * Size of reaction glyph
     */
    private static final double REACTION_SIZE = 12;
    /**
     * Size of attachment glyph
     */
    static final double ATTACHMENT_SIZE = REACTION_SIZE;
    /**
     * Padding of attachmente glyphs
     */
    private static final int ATTACHMENT_PADDING = 2;
    /**
     * Precomputed space to allow for attachments
     */
    private static final double BOX_SIZE = ATTACHMENT_SIZE + 2 * ATTACHMENT_PADDING;
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
     * Comparator that puts false elements before true elements.
     */
    private static final Comparator<Boolean> FALSE_FIRST = Comparator.nullsFirst((o1, o2) -> o1.equals(o2) ? 0 : o1 ? 1 : -1);

    private List<EntityGlyph> inputs;
    private List<EntityGlyph> outputs;
    private List<EntityGlyph> catalysts;
    private List<EntityGlyph> regulators;

    @Override
    public void compute(Layout layout) {
        addDuplicates(layout);
        collectParticipants(layout);
        layoutParticipants(layout);
        layoutCompartments(layout);
        layoutReaction(layout);
        layoutCompartments(layout);
        layoutConnectors(layout);
        removeExtracellular(layout);
        computeDimension(layout);
        failedReactions(layout);
        moveToOrigin(layout);
    }

    private void addDuplicates(Layout layout) {
        // We duplicate every entity that has more than one role, except when the input is a catalyst
        final List<EntityGlyph> added = new ArrayList<>();
        for (EntityGlyph entity : layout.getEntities()) {
            final Collection<Role> roles = entity.getRoles();
            if (roles.size() > 1) {
                // Extract the role types
                final ArrayList<Role> roleList = new ArrayList<>(roles);
                for (final Role role : roleList) {
                    if (role.getType() == INPUT
                            || role.getType() == CATALYST
                            || entity.getRoles().size() == 1) continue;
                    final EntityGlyph copy = new EntityGlyph(entity);
                    copy.setRole(role);
                    entity.getRoles().remove(role);
                    added.add(copy);
                    copy.setCompartment(entity.getCompartment());
                    entity.getCompartment().addGlyph(copy);
                }
            }
        }
        for (EntityGlyph entity : added) {
            layout.add(entity);
        }
    }

    private void collectParticipants(Layout layout) {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
        catalysts = new ArrayList<>();
        final List<EntityGlyph> activators = new ArrayList<>();
        final List<EntityGlyph> inhibitors = new ArrayList<>();
        regulators = new ArrayList<>();
        for (EntityGlyph entity : layout.getEntities()) {
            for (Role role : entity.getRoles()) {
                switch (role.getType()) {
                    case INPUT:
                        inputs.add(entity);
                        break;
                    case OUTPUT:
                        outputs.add(entity);
                        break;
                    case CATALYST:
                        catalysts.add(entity);
                        break;
                    case NEGATIVE_REGULATOR:
                        inhibitors.add(entity);
                        break;
                    case POSITIVE_REGULATOR:
                        activators.add(entity);
                        break;
                }
            }
        }
        regulators.addAll(inhibitors);
        regulators.addAll(activators);

    }

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
                    for (EntityGlyph input : inputs) move(input, x - reactionSep, 0);
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
                    for (EntityGlyph output : outputs) move(output, x + reactionSep, 0);
                }

            } else if (canMoveToRegulators(reaction)) {
                double y = 0;
                for (CompartmentGlyph child : clashingCompartments) {
                    if (child.getPosition().getMaxY() > y) y = child.getPosition().getMaxY();
                }
                if (y != 0) {
                    reaction.getPosition().setCenter(0, y + reactionSep);
                    for (EntityGlyph regulator : regulators) move(regulator, 0, y + reactionSep);
                    for (EntityGlyph catalyst : catalysts) move(catalyst, 0, y + reactionSep);
                }
            } else if (canMoveToCatalysts(reaction)) {
                double y = 0;
                for (CompartmentGlyph child : clashingCompartments)
                    if (child.getPosition().getY() < y) y = child.getPosition().getY();
                if (y != 0) {
                    reaction.getPosition().setCenter(0, y - reactionSep);
                    for (EntityGlyph catalyst : catalysts) move(catalyst, 0, y - reactionSep);
                    for (EntityGlyph regulator : regulators) move(regulator, 0, y - reactionSep);
                }

            }
//            // We'll put it at the bottom if the number of regulators is equals or less than catalysts
//            if (regulators.size() <= catalysts.size()) {
//
//            } else {
//            }
        }
        // Add backbones
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getX(), position.getCenterY()),
                new CoordinateImpl(position.getX() - BACKBONE_LENGTH, position.getCenterY())));
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getMaxX(), position.getCenterY()),
                new CoordinateImpl(position.getMaxX() + BACKBONE_LENGTH, position.getCenterY())));
    }

    private Collection<CompartmentGlyph> clashingCompartments(Layout layout, ReactionGlyph reaction) {
        return layout.getCompartments().stream()
                .filter(compartment -> clashes(reaction, compartment))
                .collect(Collectors.toList());
    }

    private boolean clashes(ReactionGlyph reaction, CompartmentGlyph compartment) {
        return compartment != reaction.getCompartment()
                && !isChild(compartment, reaction.getCompartment())
                && compartment.getPosition().intersects(reaction.getPosition());
    }

    private boolean canMove(EntityGlyph entityGlyph, double x, int y) {
        return false;
    }

    private boolean canMoveToInputs(ReactionGlyph reaction) {
        return inputs.stream().noneMatch(glyph -> clashes(reaction, glyph.getCompartment()));
    }

    private boolean canMoveToOutputs(ReactionGlyph reaction) {
        return outputs.stream().noneMatch(glyph -> clashes(reaction, glyph.getCompartment()));

    }

    private boolean canMoveToRegulators(ReactionGlyph reaction) {
        return regulators.stream().noneMatch(glyph -> clashes(reaction, glyph.getCompartment()));
    }

    private boolean canMoveToCatalysts(ReactionGlyph reaction) {
        return catalysts.stream().noneMatch(glyph -> clashes(reaction, glyph.getCompartment()));
    }

    private boolean isChild(CompartmentGlyph compartment, CompartmentGlyph child) {
        CompartmentGlyph parent = child.getParent();
        while (parent != null) {
            if (parent == compartment) return true;
            parent = parent.getParent();
        }
        return false;
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
    private boolean intersect(double r0, double r1, double s0, double s1) {
        return r0 < s0 && s0 < r1 || s0 < r0 && r0 < s1;
    }

    private void layoutParticipants(Layout layout) {
        for (EntityGlyph entity : layout.getEntities()) {
            setSize(entity);
        }
        inputs(layout);
        outputs(layout);
        catalysts(layout);
        regulators(layout);
    }

    private void setSize(ReactionGlyph reaction) {
        reaction.getPosition().setHeight(REACTION_SIZE);
        reaction.getPosition().setWidth(REACTION_SIZE);
    }

    private void setSize(EntityGlyph glyph) {
        final Dimension2D textDimension = TextUtils.textDimension(glyph.getName());
        switch (glyph.getRenderableClass()) {
            case CHEMICAL:
            case CHEMICAL_DRUG:
            case COMPLEX:
            case COMPLEX_DRUG:
            case ENTITY:
            case PROTEIN_DRUG:
            case RNA:
            case RNA_DRUG:
                // exporter padding is 5
                glyph.getPosition().setWidth(10 + textDimension.getWidth());
                glyph.getPosition().setHeight(10 + textDimension.getHeight());
                break;
            case PROTEIN:
                glyph.getPosition().setWidth(10 + textDimension.getWidth());
                glyph.getPosition().setHeight(10 + textDimension.getHeight());
                layoutAttachments(glyph);
                break;
            case ENCAPSULATED_NODE:
            case PROCESS_NODE:
            case ENTITY_SET:
            case ENTITY_SET_DRUG:
                glyph.getPosition().setWidth(15 + textDimension.getWidth());
                glyph.getPosition().setHeight(15 + textDimension.getHeight());
                break;
            case GENE:
                glyph.getPosition().setWidth(6 + textDimension.getWidth());
                glyph.getPosition().setHeight(30 + textDimension.getHeight());
                break;
        }
    }

    private void inputs(Layout layout) {
        inputs.sort(Comparator
                // input/catalysts first
                .comparing((EntityGlyph e) -> e.getRoles().size()).reversed()
                // non trivial then
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                // and by RenderableClass
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));

        double heightPerGlyph = MIN_GLYPH_HEIGHT;
        for (EntityGlyph input : inputs) {
            if (input.getPosition().getHeight() > heightPerGlyph) {
                heightPerGlyph = input.getPosition().getHeight();
            }
        }
        heightPerGlyph += VERTICAL_PADDING;
        final double totalHeight = heightPerGlyph * inputs.size();
        final double yOffset = 0.5 * (totalHeight - heightPerGlyph);
        layoutVerticalEntities(layout.getCompartmentRoot(), inputs, yOffset, heightPerGlyph, (glyph, coord) ->
                placeEntity(glyph, new CoordinateImpl(-coord.getX(), coord.getY())));
    }

    private Stoichiometry getStoichiometry(List<Segment> segments, Role role) {
        if (role.getStoichiometry() == 1)
            return new StoichiometryImpl(1, null);
        final Segment segment = segments.get(segments.size() - 1);
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

    private void outputs(Layout layout) {
        outputs.sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));

        double heightPerGlyph = MIN_GLYPH_HEIGHT;
        for (EntityGlyph input : outputs) {
            if (input.getPosition().getHeight() > heightPerGlyph) {
                heightPerGlyph = input.getPosition().getHeight();
            }
        }
        heightPerGlyph += VERTICAL_PADDING;
        final double totalHeight = heightPerGlyph * outputs.size();
        final double yOffset = 0.5 * (totalHeight - heightPerGlyph);
        layoutVerticalEntities(layout.getCompartmentRoot(), outputs, yOffset, heightPerGlyph, this::placeEntity);
    }

    private void catalysts(Layout layout) {
        // Remove catalysts that are inputs
        catalysts.removeIf(entityGlyph -> entityGlyph.getRoles().stream().anyMatch(role -> role.getType() == INPUT));
        if (catalysts.isEmpty()) return;
        catalysts.sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));

        double widthPerGlyph = MIN_GLYPH_WIDTH;
        for (EntityGlyph input : catalysts) {
            if (input.getPosition().getWidth() > widthPerGlyph) {
                widthPerGlyph = input.getPosition().getWidth();
            }
        }
        widthPerGlyph += HORIZONTAL_PADDING;
        final double totalWidth = widthPerGlyph * catalysts.size();
        final double xOffset = 0.5 * (totalWidth - widthPerGlyph);
        layoutHorizontalEntities(layout.getCompartmentRoot(), catalysts, xOffset, widthPerGlyph, (glyph, coord) ->
                placeEntity(glyph, new CoordinateImpl(coord.getY(), coord.getX())));
    }

    private void regulators(Layout layout) {
        if (regulators.isEmpty()) return;
        regulators.sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass()))
                .thenComparing(EntityGlyph::getName));

        double widthPerGlyph = MIN_GLYPH_WIDTH;
        for (EntityGlyph input : regulators) {
            if (input.getPosition().getWidth() > widthPerGlyph) {
                widthPerGlyph = input.getPosition().getWidth();
            }
        }
        widthPerGlyph += HORIZONTAL_PADDING;
        final double totalWidth = widthPerGlyph * regulators.size();
        final double xOffset = 0.5 * (totalWidth - widthPerGlyph);
        layoutHorizontalEntities(layout.getCompartmentRoot(), regulators, xOffset, widthPerGlyph, (glyph, coord) ->
                placeEntity(glyph, new CoordinateImpl(coord.getY(), -coord.getX())));
    }

    private void layoutConnectors(Layout layout) {
        inputConnectors(layout);
        outputConnectors(layout);
        catalystConnectors(layout);
        regulatorConnectors(layout);
    }

    private void inputConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getX() - BACKBONE_LENGTH;
        final double vRule = port - REACTION_MIN_H_DISTANCE + BACKBONE_LENGTH + reactionPosition.getWidth() + MIN_SEGMENT;
        for (EntityGlyph entity : inputs) {
            final Position position = entity.getPosition();
            // is catalyst and input
            final boolean biRole = entity.getRoles().size() > 1;
            // Catalyst
            double y;
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            entity.setConnector(connector);
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
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
            y = biRole ? position.getCenterY() + 5 : position.getCenterY();
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
            for (Role role : entity.getRoles()) {
                if (role.getType() == INPUT) {
                    final Stoichiometry s = getStoichiometry(segments, role);
                    connector.setStoichiometry(s);
                }
            }
        }
    }

    private void outputConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getMaxX() + BACKBONE_LENGTH;
        final double vRule = port + REACTION_MIN_H_DISTANCE - BACKBONE_LENGTH - reactionPosition.getWidth() - MIN_SEGMENT;
        for (EntityGlyph entity : outputs) {
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
            entity.setConnector(connector);
            final Position position = entity.getPosition();
            for (Role role : entity.getRoles()) {
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getX() - 4, position.getCenterY()),
                        new CoordinateImpl(vRule, position.getCenterY())));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(vRule, position.getCenterY()),
                        new CoordinateImpl(port, reactionPosition.getCenterY())));
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
        for (EntityGlyph entity : catalysts) {
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            entity.setConnector(connector);
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
            final Position position = entity.getPosition();
            for (Role role : entity.getRoles()) {
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getCenterX(), position.getMaxY()),
                        new CoordinateImpl(position.getCenterX(), hRule)));
                segments.add(new SegmentImpl(
                        new CoordinateImpl(position.getCenterX(), hRule),
                        new CoordinateImpl(reactionPosition.getCenterX(), port)));
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setType(role.getType().name());
                connector.setPointer(getConnectorType(role.getType()));
            }
        }
    }

    private void regulatorConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
//        final double port = reactionPosition.getMaxY();
        // an horizontal rule for the first segment of all the regulators
        final double hRule = reactionPosition.getMaxY() + REACTION_MIN_V_DISTANCE;
        // we want to fit all catalysts in a semi-circumference, not using the corners
        final int sectors = regulators.size() + 1;
        // p is the distance between the center of the reaction and the
        final double p = reactionPosition.getHeight() / 2 + REGULATOR_SIZE * sectors / Math.PI;
        int i = 1;
        for (EntityGlyph entity : regulators) {
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            entity.setConnector(connector);
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
            final Position position = entity.getPosition();
//            segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(), reactionPosition.getCenterX(), reactionPosition.getMaxY()));
            segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(), position.getCenterX(), hRule));
            final double x = reactionPosition.getCenterX() - p * Math.cos(Math.PI * i / sectors);
            final double y = reactionPosition.getCenterY() + p * Math.sin(Math.PI * i / sectors);
            segments.add(new SegmentImpl(position.getCenterX(), hRule, x, y));
            // Only one role expected (negative or positive)
            for (Role role : entity.getRoles()) {
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setPointer(getConnectorType(role.getType()));
            }
            i++;
        }

        // This piece of code points to the center of the reaction, but they are oddly distributed
        //   final Position reactionPosition = layout.getReaction().getPosition();
        //        // an horizontal rule for the first segment of all the regulators
        //        final double hRule = reactionPosition.getMaxY() + REACTION_MIN_V_DISTANCE;
        //        // we want to fit all catalysts in a semi-circumference, not using the corners
        //        final int sectors = regulators.size() + 1;
        //        // p is the distance between the center of the reaction and the closer pixel of the shape of the regulator
        //        final double p = REGULATOR_SIZE * sectors / Math.PI;
        //        int i = 1;
        //        for (EntityGlyph entity : regulators) {
        //            final ConnectorImpl connector = new ConnectorImpl();
        //            final List<Segment> segments = new ArrayList<>();
        //            entity.setConnector(connector);
        //            connector.setSegments(segments);
        //            connector.setEdgeId(layout.getReaction().getId());
        //            final Position position = entity.getPosition();
        //            segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(), position.getCenterX(), hRule));
        ////            final double x = reactionPosition.getCenterX() - p *  Math.cos(Math.PI  * i / sectors);
        ////            final double y = reactionPosition.getCenterY() + p * Math.sin(Math.PI * i / sectors);
        //            final double dx = entity.getPosition().getCenterX() - reactionPosition.getCenterX();
        //            final double dy = entity.getPosition().getCenterY() - reactionPosition.getCenterY();
        //            final double mod = Math.sqrt(dx * dx + dy * dy);
        //            final double x = dx / mod * p + reactionPosition.getCenterX();
        //            final double y = dy / mod * p + reactionPosition.getCenterY();
        //            segments.add(new SegmentImpl(position.getCenterX(), hRule, x, y));
        //            // Only one role expected (negative or positive)
        //            for (Role role : entity.getRoles()) {
        //                connector.setStoichiometry(getStoichiometry(segments, role));
        //                connector.setPointer(getConnectorType(role.getType()));
        //            }
        //            i++;
        //        }
    }

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

    private void placeEntity(EntityGlyph entity, Coordinate coordinate) {
        entity.getPosition().setCenter(coordinate.getX(), coordinate.getY());
        if (entity.getAttachments() != null) {
            for (AttachmentGlyph attachment : entity.getAttachments()) {
                attachment.getPosition().move(entity.getPosition().getX(), entity.getPosition().getY());
            }
        }
    }

    private void layoutCompartments(Layout layout) {
        layoutCompartment(layout.getCompartmentRoot());
    }

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
            if (position == null) position = new Position(glyph.getPosition());
            else position.union(glyph.getPosition());
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
            layout.getPosition().union(entity.getPosition());
        }
    }

    private void moveToOrigin(Layout layout) {
        final double dx = -layout.getPosition().getX();
        final double dy = -layout.getPosition().getY();
        final Coordinate delta = new CoordinateImpl(dx, dy);
        layout.getPosition().move(dx, dy);
        move(layout.getCompartmentRoot(), delta, true);
    }

    private void layoutAttachments(EntityGlyph entity) {
        if (entity.getAttachments() != null) {
            final Position position = entity.getPosition();
            position.setX(0.);
            position.setY(0.);
            double width = position.getWidth() - 2 * 8; // rounded rectangles
            double height = position.getHeight() - 2 * 8; // rounded rectangles
            int boxesInWidth = (int) (width / BOX_SIZE);
            int boxesInHeight = (int) (height / BOX_SIZE);
            int maxBoxes = 2 * (boxesInHeight + boxesInWidth);
            while (entity.getAttachments().size() > maxBoxes) {
                position.setWidth(position.getWidth() + BOX_SIZE);
                position.setHeight(position.getWidth() / TextUtils.RATIO);
                width = position.getWidth() - 2 * 8; // rounded rectangles
                height = position.getHeight() - 2 * 8; // rounded rectangles
                boxesInWidth = (int) (width / BOX_SIZE);
                boxesInHeight = (int) (height / BOX_SIZE);
                maxBoxes = 2 * (boxesInHeight + boxesInWidth);
            }

            final ArrayList<AttachmentGlyph> glyphs = new ArrayList<>(entity.getAttachments());
            final double maxWidth = boxesInWidth * BOX_SIZE;
            final double maxHeight = boxesInHeight * BOX_SIZE;
            final double xOffset = 8 + 0.5 * (BOX_SIZE + width - maxWidth);
            final double yOffset = 8 + 0.5 * (BOX_SIZE + height - maxHeight);
            for (int i = 0; i < glyphs.size(); i++) {
                glyphs.get(i).getPosition().setWidth(ATTACHMENT_SIZE);
                glyphs.get(i).getPosition().setHeight(ATTACHMENT_SIZE);
                if (i < boxesInWidth) {
                    // Top line
                    glyphs.get(i).getPosition().setCenter(position.getX() + xOffset + i * BOX_SIZE, position.getY());
                } else if (i < boxesInWidth + boxesInHeight) {
                    // Right line
                    final int pos = i - boxesInWidth;
                    glyphs.get(i).getPosition().setCenter(position.getMaxX(), position.getY() + yOffset + pos * BOX_SIZE);
                } else if (i < 2 * boxesInWidth + boxesInHeight) {
                    // Bottom line
                    final int pos = i - boxesInWidth - boxesInHeight;
                    glyphs.get(i).getPosition().setCenter(position.getMaxX() - xOffset - pos * BOX_SIZE, position.getMaxY());
                } else {
                    // Left line
                    final int pos = i - boxesInHeight - 2 * boxesInWidth;
                    glyphs.get(i).getPosition().setCenter(position.getX(), position.getMaxY() - yOffset - pos * BOX_SIZE);
                }
            }
        }
    }

    private void removeExtracellular(Layout layout) {
        layout.getCompartments().remove(layout.getCompartmentRoot());
    }


    private void failedReactions(Layout layout) {
        if (outputs.isEmpty()) layout.getPosition().setWidth(layout.getPosition().getWidth() + 150);
        if (inputs.isEmpty()) {
            layout.getPosition().setWidth(layout.getPosition().getWidth() + 150);
        }
    }


}
