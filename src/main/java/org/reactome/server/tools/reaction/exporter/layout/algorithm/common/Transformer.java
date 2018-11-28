package org.reactome.server.tools.reaction.exporter.layout.algorithm.common;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.diagram.data.layout.impl.SegmentImpl;
import org.reactome.server.tools.diagram.data.layout.impl.ShapeImpl;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.GlyphUtils;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.*;
import org.reactome.server.tools.reaction.exporter.layout.text.TextUtils;

import java.awt.geom.Dimension2D;
import java.util.ArrayList;

/**
 * Helper class to perform transformations on glyphs: translating, scaling and sizing.
 */
public class Transformer {

    private static final double BACKBONE_LENGTH = 20;
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
    private static final int NODE_CORNER_RADIUS = 8;


    private Transformer() {
    }

    /**
     * Moves the given glyph, and content, adding dx and dy to {@link Glyph#getPosition()}
     */
    public static void move(Glyph glyph, double dx, double dy) {
        move(glyph, new CoordinateImpl(dx, dy));
    }

    public static void move(Glyph glyph, Coordinate delta) {
        if (glyph instanceof EntityGlyph)
            move((EntityGlyph) glyph, delta);
        else if (glyph instanceof ReactionGlyph)
            move((ReactionGlyph) glyph, delta);
        else if (glyph instanceof CompartmentGlyph)
            move((CompartmentGlyph) glyph, delta);
        else throw new UnsupportedOperationException();
    }

    /**
     * moves the {@link EntityGlyph#getPosition()} and its connector (segments and shapes)
     */
    private static void move(EntityGlyph entity, Coordinate delta) {
        entity.getPosition().move(delta.getX(), delta.getY());
        for (AttachmentGlyph attachment : entity.getAttachments()) {
            attachment.getPosition().move(delta.getX(), delta.getY());
        }
        if (entity.getConnector() != null) {
            for (Segment segment : entity.getConnector().getSegments()) {
                ((SegmentImpl) segment).setFrom(segment.getFrom().add(delta));
                ((SegmentImpl) segment).setTo(segment.getTo().add(delta));
            }
            moveShape(entity.getConnector().getEndShape(), delta);
            if (entity.getConnector().getStoichiometry() != null) {
                moveShape(entity.getConnector().getStoichiometry().getShape(), delta);
            }
        }
    }

    /**
     * This is for stoichiometries and connectors shapes
     */
    private static void moveShape(Shape s, Coordinate delta) {
        if (s != null) {
            final ShapeImpl shape = (ShapeImpl) s;
            if (shape.getA() != null) shape.setA(shape.getA().add(delta));
            if (shape.getB() != null) shape.setB(shape.getB().add(delta));
            if (shape.getC() != null) shape.setC(shape.getC().add(delta));
        }
    }

    /**
     * moves the reaction and its segments
     */
    private static void move(ReactionGlyph reactionGlyph, Coordinate delta) {
        reactionGlyph.getPosition().move(delta.getX(), delta.getY());

        for (Segment segment : reactionGlyph.getSegments()) {
            ((SegmentImpl) segment).setFrom(segment.getFrom().add(delta));
            ((SegmentImpl) segment).setTo(segment.getTo().add(delta));
        }
    }

    private static void move(CompartmentGlyph compartment, Coordinate delta) {
        move(compartment, delta, false);
    }

    /**
     * moves the compartment and everything inside it if moveContent is true
     */
    public static void move(CompartmentGlyph compartment, Coordinate delta, boolean moveContent) {
        compartment.getPosition().move(delta.getX(), delta.getY());
        if (compartment.getLabelPosition() != null)
            compartment.setLabelPosition(compartment.getLabelPosition().add(delta));
        if (moveContent) {
            for (Glyph glyph : compartment.getContainedGlyphs()) move(glyph, delta);
            for (CompartmentGlyph child : compartment.getChildren()) move(child, delta, true);
        }
    }

    /**
     * returns the outer bounds of a given glyph. This methods takes into account backbone and attachments.
     */
    public static Position getBounds(Glyph glyph) {
        if (glyph instanceof ReactionGlyph) return getBounds((ReactionGlyph) glyph);
        else if (glyph instanceof EntityGlyph) return getBounds((EntityGlyph) glyph);
        return glyph.getPosition();
    }

    /**
     * @return the most outer limits of an entity glyph, taking into account if it contains attachments. In that case
     * the bounds include the position of the attachments on all sides, even if there is only one attachment.
     */
    public static Position getBounds(EntityGlyph entityGlyph) {
        final Position position = new Position(entityGlyph.getPosition());
        if (GlyphUtils.hasRole(entityGlyph, EntityRole.INPUT, EntityRole.CATALYST)) {
            // A catalyst line will overflow 50px over the entity
            position.setY(position.getY() - 50);
        }
        if (entityGlyph.getAttachments().isEmpty()) return position;
        position.setWidth(position.getWidth() + ATTACHMENT_SIZE);
        position.setHeight(position.getHeight() + ATTACHMENT_SIZE);
        position.move(-0.5 * ATTACHMENT_SIZE, -0.5 * ATTACHMENT_SIZE);
        return position;
    }

    public static Position getBounds(ReactionGlyph reaction) {
        final Position position = new Position(reaction.getPosition());
        if (reaction.getSegments() != null) {
            // just the backbone
            // actually, we should perform a for statement with segments, but this is faster
            position.setWidth(position.getWidth() + 2 * BACKBONE_LENGTH);
            position.move(-BACKBONE_LENGTH, 0);
        }
        return position;
    }

    public static void center(Glyph glyph, Coordinate center) {
        final Coordinate diff = center.minus(new CoordinateImpl(glyph.getPosition().getCenterX(), glyph.getPosition().getCenterY()));
        move(glyph, diff);
    }

    public static void setSize(ReactionGlyph reaction) {
        final Position position = reaction.getPosition();
        position.setHeight(REACTION_SIZE);
        position.setWidth(REACTION_SIZE);
        // Add backbones
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getX(), position.getCenterY()),
                new CoordinateImpl(position.getX() - BACKBONE_LENGTH, position.getCenterY())));
        reaction.getSegments().add(new SegmentImpl(
                new CoordinateImpl(position.getMaxX(), position.getCenterY()),
                new CoordinateImpl(position.getMaxX() + BACKBONE_LENGTH, position.getCenterY())));
    }

    public static void setSize(EntityGlyph glyph) {
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
                // exporter padding is 10
                glyph.getPosition().setWidth(20 + textDimension.getWidth());
                glyph.getPosition().setHeight(20 + textDimension.getHeight());
                break;
            case PROTEIN:
                glyph.getPosition().setWidth(20 + textDimension.getWidth());
                glyph.getPosition().setHeight(20 + textDimension.getHeight());
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

    private static void layoutAttachments(EntityGlyph entity) {
        if (entity.getAttachments() != null) {
            final Position position = entity.getPosition();
            position.setX(0.);
            position.setY(0.);
            double width = position.getWidth() - 2 * 8; // rounded rectangles
            double height = position.getHeight() - 2 * 8; // rounded rectangles
            int boxesInWidth = (int) (width / BOX_SIZE);
            int boxesInHeight = (int) (height / BOX_SIZE);
            int maxBoxes = 2 * (boxesInHeight + boxesInWidth);
            // we need to increase the size of the node until it fits the attachments
            while (entity.getAttachments().size() > maxBoxes) {
                // the step is another horizontal box
                position.setWidth(position.getWidth() + BOX_SIZE);
                position.setHeight(position.getWidth() / TextUtils.RATIO);
                width = position.getWidth() - 2 * NODE_CORNER_RADIUS;
                height = position.getHeight() - 2 * NODE_CORNER_RADIUS;
                boxesInWidth = (int) (width / BOX_SIZE);
                boxesInHeight = (int) (height / BOX_SIZE);
                maxBoxes = 2 * (boxesInHeight + boxesInWidth);
            }
            final ArrayList<AttachmentGlyph> glyphs = new ArrayList<>(entity.getAttachments());
            final double maxWidth = boxesInWidth * BOX_SIZE;
            final double maxHeight = boxesInHeight * BOX_SIZE;
            final double xOffset = NODE_CORNER_RADIUS + 0.5 * (BOX_SIZE + width - maxWidth);
            final double yOffset = NODE_CORNER_RADIUS + 0.5 * (BOX_SIZE + height - maxHeight);
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

    public static Position padd(Position position, double padding) {
        return new Position(
                position.getX() - padding,
                position.getY() - padding,
                position.getWidth() + 2 * padding,
                position.getHeight() + 2 * padding);
    }

    public static Position padd(Position position, double horizontal, double vertical ){
        return new Position(
                position.getX() - horizontal,
                position.getY() - vertical,
                position.getWidth() + 2 * horizontal,
                position.getHeight() + 2 * vertical);
    }

    public static void setSize(Glyph glyph) {
        if (glyph instanceof EntityGlyph) setSize((EntityGlyph) glyph);
        if (glyph instanceof ReactionGlyph) setSize((ReactionGlyph) glyph);
    }
}
