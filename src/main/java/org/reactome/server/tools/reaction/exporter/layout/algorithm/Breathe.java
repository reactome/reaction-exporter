package org.reactome.server.tools.reaction.exporter.layout.algorithm;

import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.*;
import org.reactome.server.tools.reaction.exporter.layout.text.TextUtils;

import java.awt.geom.Dimension2D;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class Breathe implements LayoutAlgorithm {

    /**
     * Minimum distance between the compartment border and any of ints contained glyphs.
     */
    private static final double COMPARTMENT_PADDING = 20;
    /**
     * Minimum allocated height for any glyph. Even if glyphs have a lower height, they are placed in the middle of
     * this minimum distance.
     */
    private static final double MIN_GLYPH_HEIGHT = 40;
    /**
     * Minimum allocated width for any glyph. Even if glyphs have a lower width, they are placed in the middle of
     * this minimum distance.
     */
    private static final double MIN_GLYPH_WIDTH = 80;
    /**
     * Vertical (y-axis) distance between two glyphs.
     */
    private static final double VERTICAL_PADDING = 15;
    /**
     * Horizontal (x-axis) distance between two glyphs.
     */
    private static final double HORIZONTAL_PADDING = 15;
    /**
     * Minimum vertical distance between any glyph and the reaction glyph
     */
    private static final double REACTION_MIN_V_DISTANCE = 120;
    /**
     * Minimum horizontal distance between any glyph and the reaction glyph
     */
    private static final double REACTION_MIN_H_DISTANCE = 200;
    /**
     * Size of reaction glyph
     */
    private static final int REACTION_SIZE = 12;
    /**
     * Size of attachment glyph
     */
    private static final int ATTACHMENT_SIZE = REACTION_SIZE;
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

    @Override
    public void compute(Layout layout) {
        addDuplicates(layout);
        layoutReaction(layout);
        layoutParticipants(layout);
        layoutCompartments(layout);
        computeDimension(layout);
        moveToOrigin(layout);
        removeExtracellular(layout);

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
                    if (role.getType() == EntityRole.INPUT || role.getType() == EntityRole.CATALYST) continue;
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
        position.setCenter(0, 0);
        // Add backbones
        reaction.getSegments().add(new Segment(position.getX(), position.getCenterY(),
                position.getX() - BACKBONE_LENGTH, position.getCenterY()));
        reaction.getSegments().add(new Segment(position.getMaxX(), position.getCenterY(),
                position.getMaxX() + BACKBONE_LENGTH, position.getCenterY()));
    }

    private void layoutParticipants(Layout layout) {
        for (EntityGlyph entity : layout.getEntities()) {
            setSize(entity);
        }
        final Map<EntityRole, Collection<EntityGlyph>> participants = getEntityMap(layout);
        inputs(layout, participants.get(EntityRole.INPUT));
        outputs(layout, participants.get(EntityRole.OUTPUT));
        catalysts(layout, participants.get(EntityRole.CATALYST));
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
                glyph.getPosition().setWidth(6 + textDimension.getWidth());
                glyph.getPosition().setHeight(6 + textDimension.getHeight());
                break;
            case PROTEIN:
                glyph.getPosition().setWidth(6 + textDimension.getWidth());
                glyph.getPosition().setHeight(6 + textDimension.getHeight());
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
                placeEntity(glyph, new Coordinate(-coord.getX(), coord.getY())));
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getX() - BACKBONE_LENGTH;
        final double vRule = port - REACTION_MIN_H_DISTANCE;
        for (EntityGlyph entity : inputs) {
            final Position position = entity.getPosition();
            // is catalyst and input
            final boolean biRole = entity.getRoles().size() > 1;
            // Input
            double y = biRole ? position.getCenterY() : position.getCenterY();
            entity.getConnector().getSegments().add(new Segment(position.getMaxX(), y, vRule, y));
            entity.getConnector().getSegments().add(new Segment(vRule, y, port, reactionPosition.getCenterY()));
            // Catalyst
            if (biRole) {

                y = position.getY() + 10;
                entity.getConnector().getSegments().add(new Segment(position.getMaxX(), y, vRule + 50, y));
                entity.getConnector().getSegments().add(new Segment(vRule + 50, y, reactionPosition.getCenterX(), reactionPosition.getY()));
                entity.getConnector().setPointer(EntityRole.CATALYST);
            }
            for (Role role : entity.getRoles()) {
                // This will take last s > 1
                entity.getConnector().setStoichiometry(role.getStoichiometry());
            }
        }
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
        final double vRule = port + REACTION_MIN_V_DISTANCE;
        for (EntityGlyph entity : outputs) {
            final Position position = entity.getPosition();
            for (Role role : entity.getRoles()) {
                entity.getConnector().getSegments().add(new Segment(position.getX(), position.getCenterY(), vRule, position.getCenterY()));
                entity.getConnector().getSegments().add(new Segment(vRule, position.getCenterY(), port, reactionPosition.getCenterY()));
                entity.getConnector().setPointer(role.getType());
                entity.getConnector().setStoichiometry(role.getStoichiometry());
            }
        }
    }

    private void catalysts(Layout layout, Collection<EntityGlyph> entities) {
        if (entities == null || entities.isEmpty()) return;
        final ArrayList<EntityGlyph> catalysts = new ArrayList<>(entities);
        // Remove catalysts that are inputs
        catalysts.removeIf(entityGlyph -> entityGlyph.getRoles().stream().anyMatch(role -> role.getType() == EntityRole.INPUT));
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
                placeEntity(glyph, new Coordinate(coord.getY(), coord.getX())));
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getY();
        final double hRule = port - REACTION_MIN_V_DISTANCE;
        for (EntityGlyph entity : catalysts) {
            final Position position = entity.getPosition();
            for (Role role : entity.getRoles()) {
                entity.getConnector().getSegments().add(new Segment(position.getCenterX(), position.getMaxY(), position.getCenterX(), hRule));
                entity.getConnector().getSegments().add(new Segment(position.getCenterX(), hRule, reactionPosition.getCenterX(), port));
                entity.getConnector().setStoichiometry(role.getStoichiometry());
                entity.getConnector().setPointer(role.getType());
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
                placeEntity(glyph, new Coordinate(coord.getY(), -coord.getX())));
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getMaxY();
        final double hRule = port + REACTION_MIN_V_DISTANCE;
        for (EntityGlyph entity : regulators) {
            final Position position = entity.getPosition();
            for (Role role : entity.getRoles()) {
                entity.getConnector().getSegments().add(new Segment(position.getCenterX(), position.getMaxY(), position.getCenterX(), hRule));
                entity.getConnector().getSegments().add(new Segment(position.getCenterX(), hRule, reactionPosition.getCenterX(), port));
                entity.getConnector().setStoichiometry(role.getStoichiometry());
                entity.getConnector().setPointer(role.getType());
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
        if (glyphs.isEmpty()) return COMPARTMENT_PADDING;
        double width = MIN_GLYPH_WIDTH;
        for (EntityGlyph input : entities) {
            if (input.getPosition().getWidth() > width)
                width = input.getPosition().getWidth();
        }
        width += HORIZONTAL_PADDING;
        final double x = REACTION_MIN_H_DISTANCE + startX + width;
        for (EntityGlyph glyph : glyphs) {
            final int i = entities.indexOf(glyph);
            final double y = -yOffset + i * heightPerGlyph;
            apply.accept(glyph, new Coordinate(x, y));
        }
        return COMPARTMENT_PADDING + width;
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
        final double height = entities.stream().map(Glyph::getPosition).mapToDouble(Position::getHeight).max().orElse(MIN_GLYPH_HEIGHT) + VERTICAL_PADDING;
        final double x = REACTION_MIN_V_DISTANCE + startY + 0.5 * height;
        for (EntityGlyph glyph : glyphs) {
            final int i = entities.indexOf(glyph);
            final double y = -xOffset + i * widthPerGlyph;
            apply.accept(glyph, new Coordinate(-x, y));
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
        final Position position = compartment.getPosition();
        for (CompartmentGlyph child : compartment.getChildren()) {
            union(position, child.getPosition());
        }
        for (Glyph glyph : compartment.getContainedGlyphs()) {
            union(position, glyph.getPosition());
        }
        position.setX(position.getX() - COMPARTMENT_PADDING);
        position.setY(position.getY() - COMPARTMENT_PADDING);
        position.setWidth(position.getWidth() + 2 * COMPARTMENT_PADDING);
        position.setHeight(position.getHeight() + 2 * COMPARTMENT_PADDING);

        compartment.getLabelPosition().move((int) position.getCenterX(), (int) (position.getY() + COMPARTMENT_PADDING / 2));
    }

    /**
     * Creates the union between <em>a</em> and <em>b</em> and sets the result into a. The union of two
     * rectangles is defined as the smallest rectangle that contains both rectangles.
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
    }

    private void moveToOrigin(Layout layout) {
        final double dx = -layout.getPosition().getX();
        final double dy = -layout.getPosition().getY();

        layout.getPosition().move(dx, dy);

        layout.getReaction().getPosition().move(dx, dy);
        for (Segment segment : layout.getReaction().getSegments()) {
            segment.getFrom().move(dx, dy);
            segment.getTo().move(dx, dy);
        }

        for (CompartmentGlyph compartment : layout.getCompartments()) {
            compartment.getPosition().move(dx, dy);
            compartment.getLabelPosition().move((int) dx, (int) dy);
        }

        for (EntityGlyph entity : layout.getEntities()) {
            entity.getPosition().move(dx, dy);
            for (AttachmentGlyph attachment : entity.getAttachments()) {
                attachment.getPosition().move(dx, dy);
            }
            for (Segment segment : entity.getConnector().getSegments()) {
                segment.getFrom().move(dx, dy);
                segment.getTo().move(dx, dy);
            }
            if (entity.getConnector().getShape() != null) entity.getConnector().getShape().move(dx, dy);
            if (entity.getConnector().getStoichiometry() != null) entity.getConnector().getStoichiometry().move(dx, dy);
        }

    }

    private void layoutAttachments(EntityGlyph entity) {
        if (entity.getAttachments() != null) {
            final Position position = entity.getPosition();
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
