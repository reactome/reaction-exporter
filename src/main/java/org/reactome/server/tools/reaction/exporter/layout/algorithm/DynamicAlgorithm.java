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

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.BorderLayout.Place.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.CATALYST;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.INPUT;

@SuppressWarnings("Duplicates")
public class DynamicAlgorithm implements LayoutAlgorithm {
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
    private static final double REACTION_MIN_V_DISTANCE = 0;
    /**
     * Minimum horizontal distance between any glyph and the reaction glyph
     */
    private static final double REACTION_MIN_H_DISTANCE = 0;
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
            final Font font = Font.createFont(Font.TRUETYPE_FONT, DynamicAlgorithm.class.getResourceAsStream("/fonts/arialbd.ttf"));
            FONT_METRICS = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    .createGraphics()
                    .getFontMetrics(font.deriveFont(8f));
        } catch (FontFormatException | IOException e) {
            // resources shouldn't throw exceptions
            throw new IllegalArgumentException("/fonts/arialbd.ttf not found", e);
        }
    }

    private List<Glyph> inputs;
    private List<Glyph> outputs;
    private List<Glyph> catalysts;
    private List<Glyph> regulators;


    @Override
    public void compute(Layout layout) {
        addDuplicates(layout);
        collectParticipants(layout);
        for (EntityGlyph entity : layout.getEntities()) setSize(entity);
        setSize(layout.getReaction());
        final BorderLayout borderLayout = BorderLayoutFactory.get(layout);
//        borderLayout.print();
        setPositions(borderLayout, Orientation.HORIZONTAL);
        layoutConnectors(layout);
        computeDimension(layout);
//        moveToOrigin(layout);

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


    private void setPositions(BorderLayout borderLayout, Orientation orientation) {
        if (borderLayout == null) return;
        if (borderLayout.getGlyphs().isEmpty()) {
            setPositions(borderLayout.get(CENTER), Orientation.HORIZONTAL);
            setPositions(borderLayout.get(NORTH), Orientation.HORIZONTAL);
            setPositions(borderLayout.get(SOUTH), Orientation.HORIZONTAL);
            setPositions(borderLayout.get(WEST), Orientation.VERTICAL);
            setPositions(borderLayout.get(EAST), Orientation.VERTICAL);
            setPositions(borderLayout.get(SOUTH_WEST), Orientation.VERTICAL);
            setPositions(borderLayout.get(SOUTH_EAST), Orientation.VERTICAL);
            setPositions(borderLayout.get(NORTH_EAST), Orientation.VERTICAL);
            setPositions(borderLayout.get(NORTH_WEST), Orientation.VERTICAL);

            final Position north = nonNull(borderLayout.get(NORTH));
            final Position south = nonNull(borderLayout.get(SOUTH));
            final Position west = nonNull(borderLayout.get(WEST));
            final Position east = nonNull(borderLayout.get(EAST));
            final Position center = nonNull(borderLayout.get(CENTER));
            final Position northWest = nonNull(borderLayout.get(NORTH_WEST));
            final Position northEast = nonNull(borderLayout.get(NORTH_EAST));
            final Position southEast = nonNull(borderLayout.get(SOUTH_EAST));
            final Position southWest = nonNull(borderLayout.get(SOUTH_WEST));
            final double westWidth = max(northWest.getWidth(), west.getWidth(), southWest.getWidth()) + HORIZONTAL_PADDING;
            final double centerWidth = max(north.getWidth(), center.getWidth(), south.getWidth()) + HORIZONTAL_PADDING;
            final double eastWidth = max(northEast.getWidth(), east.getWidth(), southEast.getWidth()) + HORIZONTAL_PADDING;

            final double cx1 = 0.5 * westWidth;
            final double cx2 = westWidth + 0.5 * centerWidth;
            final double cx3 = westWidth + centerWidth + 0.5 * eastWidth;

            final double northHeight = max(northEast.getHeight(), north.getHeight(), northWest.getHeight()) + VERTICAL_PADDING;
            final double centerHeight = max(west.getHeight(), center.getHeight(), east.getHeight()) + VERTICAL_PADDING;
            final double southHeight = max(southEast.getHeight(), south.getHeight(), southWest.getHeight()) + VERTICAL_PADDING;
            final double cy1 = 0.5 * northHeight;
            final double cy2 = northHeight + 0.5 * centerHeight;
            final double cy3 = northHeight + centerHeight + 0.5 * southHeight;

            northWest.setCenter(cx1, cy1);
            north.setCenter(cx2, cy1);
            northEast.setCenter(cx3, cy1);
            west.setCenter(cx1, cy2);
            center.setCenter(cx2, cy2);
            east.setCenter(cx3, cy2);
            southWest.setCenter(cx1, cy3);
            south.setCenter(cx2, cy3);
            southEast.setCenter(cx3, cy3);

            borderLayout.union(center);
            borderLayout.union(north);
            borderLayout.union(south);
            borderLayout.union(west);
            borderLayout.union(east);
            borderLayout.union(southEast);
            borderLayout.union(southWest);
            borderLayout.union(northEast);
            borderLayout.union(northWest);
        } else {
            if (orientation == Orientation.VERTICAL) vertical(borderLayout);
            else horizontal(borderLayout);
        }
        placeCompartment(borderLayout);
    }

    private Position nonNull(Position position) {
        return position == null ? new Position() : position;
    }

    private void vertical(BorderLayout borderLayout) {
        double h = 0;
        double w = 0;
        for (Glyph glyph : borderLayout.getGlyphs()) {
            if (glyph.getPosition().getHeight() > h) h = glyph.getPosition().getHeight();
            if (glyph.getPosition().getWidth() > w) w = glyph.getPosition().getWidth();
            if (glyph instanceof ReactionGlyph) {
                if (glyph.getPosition().getWidth() + 2 * BACKBONE_LENGTH > w)
                    w = glyph.getPosition().getWidth() + 2 * BACKBONE_LENGTH;
            }
        }
        h += VERTICAL_PADDING;
        final double x = 0.5 * w;
        final double y = 0.5 * h;
        for (int i = 0; i < borderLayout.getGlyphs().size(); i++) {
            final Glyph glyph = borderLayout.getGlyphs().get(i);
            final double x1 = glyph.getPosition().getX();
            final double y1 = glyph.getPosition().getY();
            glyph.getPosition().setCenter(x, y + i * h);
            if (glyph instanceof EntityGlyph)
                moveAttachments((EntityGlyph) glyph, glyph.getPosition().getX() - x1, glyph.getPosition().getY() - y1);
            borderLayout.union(glyph.getPosition());
        }
    }

    private void horizontal(BorderLayout borderLayout) {
        double h = 0;
        double w = 0;
        for (Glyph glyph : borderLayout.getGlyphs()) {
            if (glyph.getPosition().getHeight() > h) h = glyph.getPosition().getHeight();
            if (glyph.getPosition().getWidth() > w) w = glyph.getPosition().getWidth();
            if (glyph instanceof ReactionGlyph) {
                if (glyph.getPosition().getWidth() + 2 * BACKBONE_LENGTH > w)
                    w = glyph.getPosition().getWidth() + 2 * BACKBONE_LENGTH;
            }
        }
        w += HORIZONTAL_PADDING;
        final double x = 0.5 * w;
        final double y = 0.5 * h;
        for (int i = 0; i < borderLayout.getGlyphs().size(); i++) {
            final Glyph glyph = borderLayout.getGlyphs().get(i);
            final double x1 = glyph.getPosition().getX();
            final double y1 = glyph.getPosition().getY();
            glyph.getPosition().setCenter(x + i * w, y);
            if (glyph instanceof EntityGlyph)
                moveAttachments((EntityGlyph) glyph, glyph.getPosition().getX() - x1, glyph.getPosition().getY() - y1);
            borderLayout.union(glyph.getPosition());
        }
    }

    private void placeCompartment(BorderLayout borderLayout) {
        if (borderLayout.getCompartment() != null) {
            // enlarge layout with centering everything inside
            borderLayout.move(COMPARTMENT_PADDING, COMPARTMENT_PADDING);
            borderLayout.setX(0);
            borderLayout.setY(0);
            borderLayout.setWidth(borderLayout.getWidth() + 2 * COMPARTMENT_PADDING);
            borderLayout.setHeight(borderLayout.getHeight() + 2 * COMPARTMENT_PADDING);

            // Place text
            final int textWidth = FONT_METRICS.stringWidth(borderLayout.getCompartment().getName());
            final int textHeight = FONT_METRICS.getHeight() - FONT_METRICS.getDescent();
            final int textPadding = textWidth + 30;
            if (borderLayout.getWidth() < textPadding) {
                // Enlarge and center
                double diff = textPadding - borderLayout.getWidth();
                borderLayout.move(0.5 * diff, 0);
                borderLayout.setWidth(textPadding);
                borderLayout.setX(0);
            }
            final Coordinate coordinate;
            if (textPadding > 0.5 * borderLayout.getWidth()) {
                // center
                coordinate = new CoordinateImpl(borderLayout.getCenterX() - 0.5 * textPadding,
                        borderLayout.getMaxY() - textHeight - COMPARTMENT_PADDING / 2);
            } else {
                // southeast
                coordinate = new CoordinateImpl(
                        borderLayout.getMaxX() - textWidth - 15,
                        borderLayout.getMaxY() - textHeight - COMPARTMENT_PADDING / 2);
            }
            borderLayout.getCompartment().setLabelPosition(coordinate);
            borderLayout.getCompartment().setPosition(new Position(borderLayout));
        }
    }

    private void moveAttachments(EntityGlyph glyph, double dx, double dy) {
        if (glyph.getAttachments().isEmpty()) return;
        for (AttachmentGlyph attachment : glyph.getAttachments()) {
            attachment.getPosition().move(dx, dy);
        }
    }

    private void setSize(ReactionGlyph reaction) {
        final Position position = reaction.getPosition();
        position.setHeight(REACTION_SIZE);
        position.setWidth(REACTION_SIZE);
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

    private void layoutConnectors(Layout layout) {
        reactionSegments(layout);
        inputConnectors(layout);
        outputConnectors(layout);
        catalystConnectors(layout);
        regulatorConnectors(layout);
    }

    private void reactionSegments(Layout layout) {
        final ReactionGlyph reaction = layout.getReaction();
        final Position position = reaction.getPosition();
        // Add backbones
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getX(), position.getCenterY()),
                new CoordinateImpl(position.getX() - BACKBONE_LENGTH, position.getCenterY())));
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getMaxX(), position.getCenterY()),
                new CoordinateImpl(position.getMaxX() + BACKBONE_LENGTH, position.getCenterY())));

    }

    private void inputConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getX() - BACKBONE_LENGTH;
        final double vRule = port - REACTION_MIN_H_DISTANCE + BACKBONE_LENGTH + reactionPosition.getWidth() + MIN_SEGMENT;
        for (Glyph g : inputs) {
            final EntityGlyph entity = (EntityGlyph) g;
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
                segments.add(new SegmentImpl(position.getMaxX(), y, reactionPosition.getCenterX(), reactionPosition.getY()));
//                segments.add(new SegmentImpl(
//                        new CoordinateImpl(position.getMaxX(), y),
//                        new CoordinateImpl(vRule + 50, y)));
//                segments.add(new SegmentImpl(
//                        new CoordinateImpl(vRule + 50, y),
//                        new CoordinateImpl(reactionPosition.getCenterX(), reactionPosition.getCenterY())));
                connector.setPointer(ConnectorType.CATALYST);
            } else {
                connector.setPointer(ConnectorType.INPUT);
            }
            y = biRole ? position.getCenterY() + 5 : position.getCenterY();
            // Input
            segments.add(new SegmentImpl(position.getMaxX(), y, port, reactionPosition.getCenterY()));
//            segments.add(new SegmentImpl(
//                    new CoordinateImpl(position.getMaxX(), y),
//                    new CoordinateImpl(vRule, y)));
//            segments.add(new SegmentImpl(
//                    new CoordinateImpl(vRule, y),
//                    new CoordinateImpl(port, reactionPosition.getCenterY())));
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
        for (Glyph g : outputs) {
            final EntityGlyph entity = (EntityGlyph) g;
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
            entity.setConnector(connector);
            final Position position = entity.getPosition();
            for (Role role : entity.getRoles()) {
                segments.add(new SegmentImpl(position.getX() - 4, position.getCenterY(),
                        port, reactionPosition.getCenterY()));
//                segments.add(new SegmentImpl(
//                        new CoordinateImpl(position.getX() - 4, position.getCenterY()),
//                        new CoordinateImpl(vRule, position.getCenterY())));
//                segments.add(new SegmentImpl(
//                        new CoordinateImpl(vRule, position.getCenterY()),
//                        new CoordinateImpl(port, reactionPosition.getCenterY())));
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
        for (Glyph g : catalysts) {
            final EntityGlyph entity = (EntityGlyph) g;
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            entity.setConnector(connector);
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
            final Position position = entity.getPosition();
            for (Role role : entity.getRoles()) {
                segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(),
                        reactionPosition.getCenterX(), port));
//                segments.add(new SegmentImpl(
//                        new CoordinateImpl(position.getCenterX(), position.getMaxY()),
//                        new CoordinateImpl(position.getCenterX(), hRule)));
//                segments.add(new SegmentImpl(
//                        new CoordinateImpl(position.getCenterX(), hRule),
//                        new CoordinateImpl(reactionPosition.getCenterX(), port)));
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setPointer(getConnectorType(role.getType()));
            }
        }
    }

    private void regulatorConnectors(Layout layout) {
        final Position reactionPosition = layout.getReaction().getPosition();
        final double port = reactionPosition.getMaxY();
        final double hRule = port + REACTION_MIN_V_DISTANCE;
        for (Glyph g : regulators) {
            final EntityGlyph entity = (EntityGlyph) g;
            final ConnectorImpl connector = new ConnectorImpl();
            final List<Segment> segments = new ArrayList<>();
            entity.setConnector(connector);
            connector.setSegments(segments);
            connector.setEdgeId(layout.getReaction().getId());
            final Position position = entity.getPosition();
            for (Role role : entity.getRoles()) {
                segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(), reactionPosition.getCenterX(), port));
//                segments.add(new SegmentImpl(position.getCenterX(), position.getMaxY(), position.getCenterX(), hRule));
//                segments.add(new SegmentImpl(position.getCenterX(), hRule, reactionPosition.getCenterX(), port));
                connector.setStoichiometry(getStoichiometry(segments, role));
                connector.setPointer(getConnectorType(role.getType()));
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

    private void computeDimension(Layout layout) {
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            layout.getPosition().union(compartment.getPosition());
        }
        for (EntityGlyph entity : layout.getEntities()) {
            layout.getPosition().union(entity.getPosition());
        }
    }

    private double max(Double... values) {
        return Collections.max(Arrays.asList(values));
    }
}
