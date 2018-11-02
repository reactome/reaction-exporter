package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.diagram.data.layout.impl.SegmentImpl;
import org.reactome.server.tools.diagram.data.layout.impl.ShapeImpl;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.*;
import org.reactome.server.tools.reaction.exporter.layout.text.TextUtils;

import java.awt.geom.Dimension2D;
import java.util.ArrayList;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe.BreatheAlgorithm.BACKBONE_LENGTH;

/**
 * Helper class to perform transformations on glyphs: translating, scaling and sizing.
 */
class Transformer {
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
    static void move(Glyph glyph, double dx, double dy) {
        move(glyph, new CoordinateImpl(dx, dy));
    }

    private static void move(Glyph glyph, Coordinate delta) {
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

    static void move(CompartmentGlyph compartment, Coordinate delta) {
        move(compartment, delta, false);
    }

    /**
     * moves the compartment and everything inside it if moveContent is true
     */
    static void move(CompartmentGlyph compartment, Coordinate delta, boolean moveContent) {
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
    static Position getBounds(Glyph glyph) {
        if (glyph instanceof ReactionGlyph) return getBounds((ReactionGlyph) glyph);
        else if (glyph instanceof EntityGlyph) return getBounds((EntityGlyph) glyph);
        return glyph.getPosition();
    }

    /**
     * @return the most outer limits of an entity glyph, taking into account if it contains attachments. In that case
     * the bounds include the position of the attachments on all sides, even if there is only one attachment.
     */
    static Position getBounds(EntityGlyph entityGlyph) {
        final Position position = new Position(entityGlyph.getPosition());
        if (entityGlyph.getAttachments().isEmpty()) return position;
        position.setWidth(position.getWidth() + ATTACHMENT_SIZE);
        position.setHeight(position.getHeight() + ATTACHMENT_SIZE);
        position.move(-0.5 * ATTACHMENT_SIZE, -0.5 * ATTACHMENT_SIZE);
        return position;
    }

    static Position getBounds(ReactionGlyph reaction) {
        final Position position = new Position(reaction.getPosition());
        if (reaction.getSegments() != null) {
            // just the backbone
            // actually, we should perform a for statement with segments, but this is faster
            position.setWidth(position.getWidth() + 2 * BreatheAlgorithm.BACKBONE_LENGTH);
            position.move(-BACKBONE_LENGTH, 0);
        }
        return position;
    }

    private static void center(ReactionGlyph reactionGlyph, Coordinate coordinate) {
        final Coordinate diff = coordinate.minus(new CoordinateImpl(reactionGlyph.getPosition().getCenterX(), reactionGlyph.getPosition().getCenterY()));
        move(reactionGlyph, diff);
    }

    static void center(EntityGlyph entityGlyph, Coordinate coordinate) {
        final Coordinate diff = coordinate.minus(new CoordinateImpl(entityGlyph.getPosition().getCenterX(), entityGlyph.getPosition().getCenterY()));
        move(entityGlyph, diff);
    }

    private static void center(CompartmentGlyph compartmentGlyph, Coordinate coordinate) {
        final Coordinate diff = coordinate.minus(new CoordinateImpl(compartmentGlyph.getPosition().getCenterX(), compartmentGlyph.getPosition().getCenterY()));
        move(compartmentGlyph, diff);
    }

    static void center(Glyph glyph, Coordinate center) {
        if (glyph instanceof CompartmentGlyph)
            center((CompartmentGlyph) glyph, center);
        else if (glyph instanceof ReactionGlyph)
            center((ReactionGlyph) glyph, center);
        else if (glyph instanceof EntityGlyph)
            center((EntityGlyph) glyph, center);
        else throw new UnsupportedOperationException();
    }

    static void setSize(ReactionGlyph reaction) {
        reaction.getPosition().setHeight(REACTION_SIZE);
        reaction.getPosition().setWidth(REACTION_SIZE);
    }

    static void setSize(EntityGlyph glyph) {
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
}
