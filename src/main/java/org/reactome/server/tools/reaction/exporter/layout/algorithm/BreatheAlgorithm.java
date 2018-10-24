package org.reactome.server.tools.reaction.exporter.layout.algorithm;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.Stoichiometry;
import org.reactome.server.tools.diagram.data.layout.impl.*;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.*;
import org.reactome.server.tools.reaction.exporter.layout.text.TextUtils;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;

public class BreatheAlgorithm implements LayoutAlgorithm {

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
    private static final double VERTICAL_PADDING = 5;
    /**
     * Horizontal (x-axis) distance between two glyphs.
     */
    private static final double HORIZONTAL_PADDING = 5;
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
    private static final double ATTACHMENT_SIZE = REACTION_SIZE;
    /**
     * Padding of attachmente glyphs
     */
    private static final int ATTACHMENT_PADDING = 2;
    /**
     * Precomputed space to allow for attachments
     */
    private static final double BOX_SIZE = ATTACHMENT_SIZE + 2 * ATTACHMENT_PADDING;
    /**
     * Length of the backbone of the reaction
     */
    private static final double BACKBONE_LENGTH = 20;
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
    private static final FontMetrics FONT_METRICS;

    static {
        try {
            final Font font = Font.createFont(Font.TRUETYPE_FONT, BreatheAlgorithm.class.getResourceAsStream("/fonts/arialbd.ttf"));
            FONT_METRICS = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    .createGraphics()
                    .getFontMetrics(font.deriveFont(8f));
        } catch (FontFormatException | IOException e) {
            // resources shouldn't throw exceptions
            throw new IllegalArgumentException("/fonts/arialbd.ttf not found", e);
        }
    }

    @Override
    public void compute(Layout layout) {
        addDuplicates(layout);
        layoutReaction(layout);
        layoutParticipants(layout);
        layoutCompartments(layout);
        removeExtracellular(layout);
        computeDimension(layout);
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
                    addCopyToCompartment(layout, entity, copy);
                }
            }
        }
        for (EntityGlyph entity : added) {
            layout.add(entity);
        }
    }

    private void addCopyToCompartment(Layout layout, EntityGlyph entity, EntityGlyph copy) {
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            if (compartment.getContainedGlyphs().contains(entity)) {
                compartment.getContainedGlyphs().add(copy);
                break;
            }
        }
    }

    private void layoutReaction(Layout layout) {
        final ReactionGlyph reaction = layout.getReaction();
        setSize(reaction);
        final Position position = reaction.getPosition();
        position.setCenter(0., 0.);
        // Add backbones
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getX(), position.getCenterY()),
                new CoordinateImpl(position.getX() - BACKBONE_LENGTH, position.getCenterY())));
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getMaxX(), position.getCenterY()),
                new CoordinateImpl(position.getMaxX() + BACKBONE_LENGTH, position.getCenterY())));
    }

    private void layoutParticipants(Layout layout) {
        for (EntityGlyph entity : layout.getEntities()) {
            setSize(entity);
        }
        final Map<EntityRole, Collection<EntityGlyph>> participants = getEntityMap(layout);
        inputs(layout, participants.get(INPUT));
        outputs(layout, participants.get(OUTPUT));
        catalysts(layout, participants.get(CATALYST));
        final ArrayList<EntityGlyph> regulators = new ArrayList<>(participants.getOrDefault(EntityRole.NEGATIVE_REGULATOR, Collections.emptyList()));
        regulators.addAll(participants.getOrDefault(EntityRole.POSITIVE_REGULATOR, Collections.emptyList()));
        regulators(layout, regulators);
    }

    private Map<EntityRole, Collection<EntityGlyph>> getEntityMap(Layout layout) {
        final Map<EntityRole, Collection<EntityGlyph>> participants = new HashMap<>();
        for (EntityGlyph entity : layout.getEntities()) {
            for (Role role : entity.getRoles()) {
                participants.computeIfAbsent(role.getType(), r -> new ArrayList<>()).add(entity);
            }
        }
        return participants;
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

    private void inputs(Layout layout, Collection<EntityGlyph> entities) {
        final ArrayList<EntityGlyph> inputs = new ArrayList<>(entities);
        inputs.sort(Comparator
                // input/catalysts first
                .comparing((EntityGlyph e) -> e.getRoles().size()).reversed()
                // non trivial then
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                // and by RenderableClass
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass())));

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
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getX() - BACKBONE_LENGTH;
        final double vRule = port - REACTION_MIN_H_DISTANCE + BACKBONE_LENGTH;
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
            segments.add(new SegmentImpl(
                    new CoordinateImpl(position.getMaxX(), y),
                    new CoordinateImpl(vRule, y)));
            segments.add(new SegmentImpl(
                    new CoordinateImpl(vRule, y),
                    new CoordinateImpl(port, reactionPosition.getCenterY())));
            for (Role role : entity.getRoles()) {
                if (role.getType() == INPUT) {
                    final Stoichiometry s = getStoichiometry(segments, role);
                    connector.setStoichiometry(s);
                }
            }
        }
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

    private void outputs(Layout layout, Collection<EntityGlyph> entities) {
        final ArrayList<EntityGlyph> outputs = new ArrayList<>(entities);
        outputs.sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass())));

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
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getMaxX() + BACKBONE_LENGTH;
        final double vRule = port + REACTION_MIN_H_DISTANCE - BACKBONE_LENGTH - reactionPosition.getWidth();
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

    private void catalysts(Layout layout, Collection<EntityGlyph> entities) {
        if (entities == null || entities.isEmpty()) return;
        final ArrayList<EntityGlyph> catalysts = new ArrayList<>(entities);
        // Remove catalysts that are inputs
        catalysts.removeIf(entityGlyph -> entityGlyph.getRoles().stream().anyMatch(role -> role.getType() == INPUT));
        catalysts.sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass())));

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
                connector.setPointer(getConnectorType(role.getType()));
            }
        }
    }

    private void regulators(Layout layout, Collection<EntityGlyph> entities) {
        if (entities == null || entities.isEmpty()) return;
        final ArrayList<EntityGlyph> regulators = new ArrayList<>(entities);
        regulators.sort(Comparator
                .comparingInt((EntityGlyph e) -> e.getRoles().size()).reversed()
                .thenComparing(EntityGlyph::isTrivial, FALSE_FIRST)
                .thenComparingInt(e -> CLASS_ORDER.indexOf(e.getRenderableClass())));

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
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getMaxY();
        final double hRule = port + REACTION_MIN_V_DISTANCE;
        for (EntityGlyph entity : regulators) {
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            entity.setConnector(connector);
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
            final Position position = entity.getPosition();
            for (Role role : entity.getRoles()) {
                segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(), position.getCenterX(), hRule));
                segments.add(new SegmentImpl(position.getCenterX(), hRule, reactionPosition.getCenterX(), port));
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setPointer(getConnectorType(role.getType()));
            }
        }
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
        for (EntityGlyph input : entities) {
            if (input.getPosition().getWidth() > width)
                width = input.getPosition().getWidth();
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
        double startY = 0;
        for (CompartmentGlyph child : compartment.getChildren()) {
            startY = Math.max(startY, layoutHorizontalEntities(child, entities, xOffset, widthPerGlyph, apply));
        }
        final List<EntityGlyph> glyphs = entities.stream()
                .filter(entityGlyph -> compartment.getContainedGlyphs().contains(entityGlyph))
                .collect(Collectors.toList());
        if (glyphs.isEmpty()) return COMPARTMENT_PADDING;
        double height = MIN_GLYPH_HEIGHT;
        for (EntityGlyph entity : entities) {
            if (entity.getPosition().getHeight() > height) {
                height = entity.getPosition().getHeight();
            }
        }
        height += VERTICAL_PADDING;
        final double x = REACTION_MIN_V_DISTANCE + startY + 0.5 * height;
        for (EntityGlyph glyph : glyphs) {
            final int i = entities.indexOf(glyph);
            final double y = -xOffset + i * widthPerGlyph;
            apply.accept(glyph, new CoordinateImpl(-x, y));
        }
        return COMPARTMENT_PADDING + height;
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
            else union(position, child.getPosition());
        }
        for (Glyph glyph : compartment.getContainedGlyphs()) {
            if (position == null) position = new Position(glyph.getPosition());
            else union(position, glyph.getPosition());
        }
        position.setX(position.getX() - COMPARTMENT_PADDING);
        position.setY(position.getY() - COMPARTMENT_PADDING);
        position.setWidth(position.getWidth() + 2 * COMPARTMENT_PADDING);
        position.setHeight(position.getHeight() + 2 * COMPARTMENT_PADDING);

        final int textWidth = FONT_METRICS.stringWidth(compartment.getName());
        final int textHeight = FONT_METRICS.getHeight() - FONT_METRICS.getDescent();
        final int textPadding = textWidth + 30;
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

    /**
     * Creates the union between <em>a</em> and <em>b</em> and sets the result into a. The union of two rectangles is
     * defined as the smallest rectangle that contains both rectangles.
     *
     * @param a where the result is to be stored
     * @param b the second position for the union
     */
    private void union(Position a, Position b) {
        final double minX = Math.min(a.getX(), b.getX());
        final double minY = Math.min(a.getY(), b.getY());
        final double maxX = Math.max(a.getMaxX(), b.getMaxX());
        final double maxY = Math.max(a.getMaxY(), b.getMaxY());
        a.setX(minX);
        a.setY(minY);
        a.setWidth(maxX - a.getX());
        a.setHeight(maxY - a.getY());
    }

    private void computeDimension(Layout layout) {
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            union(layout.getPosition(), compartment.getPosition());
        }
        for (EntityGlyph entity : layout.getEntities()) {
            union(layout.getPosition(), entity.getPosition());
        }
    }

    private void moveToOrigin(Layout layout) {
        final double dx = -layout.getPosition().getX();
        final double dy = -layout.getPosition().getY();
        final CoordinateImpl delta = new CoordinateImpl(dx, dy);

        layout.getPosition().move(dx, dy);
        layout.getReaction().getPosition().move(dx, dy);

        for (Segment segment : layout.getReaction().getSegments()) {
            ((SegmentImpl) segment).setFrom(segment.getFrom().add(delta));
            ((SegmentImpl) segment).setTo(segment.getTo().add(delta));
        }

        for (CompartmentGlyph compartment : layout.getCompartments()) {
            compartment.getPosition().move(dx, dy);
            compartment.setLabelPosition(compartment.getLabelPosition().add(delta));
        }

        for (EntityGlyph entity : layout.getEntities()) {
            entity.getPosition().move(dx, dy);
            for (AttachmentGlyph attachment : entity.getAttachments()) {
                attachment.getPosition().move(dx, dy);
            }
            for (Segment segment : entity.getConnector().getSegments()) {
                ((SegmentImpl) segment).setFrom(segment.getFrom().add(delta));
                ((SegmentImpl) segment).setTo(segment.getTo().add(delta));
            }
            moveShape(delta, entity.getConnector().getEndShape());
            if (entity.getConnector().getStoichiometry() != null) {
                moveShape(delta, entity.getConnector().getStoichiometry().getShape());
            }
        }
    }

    private void moveShape(CoordinateImpl delta, Shape s) {
        if (s != null) {
            final ShapeImpl shape = (ShapeImpl) s;
            if (shape.getA() != null) shape.setA(shape.getA().add(delta));
            if (shape.getB() != null) shape.setB(shape.getB().add(delta));
            if (shape.getC() != null) shape.setC(shape.getC().add(delta));
        }
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

}