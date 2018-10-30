package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.diagram.data.layout.impl.SegmentImpl;
import org.reactome.server.tools.diagram.data.layout.impl.ShapeImpl;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe.BreatheAlgorithm.ATTACHMENT_SIZE;
import static org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe.BreatheAlgorithm.BACKBONE_LENGTH;

/**
 * Helps moving glyphs and their content.
 */
class Translator {

    private Translator() {
    }

    private static void moveShape(Shape s, Coordinate delta) {
        if (s != null) {
            final ShapeImpl shape = (ShapeImpl) s;
            if (shape.getA() != null) shape.setA(shape.getA().add(delta));
            if (shape.getB() != null) shape.setB(shape.getB().add(delta));
            if (shape.getC() != null) shape.setC(shape.getC().add(delta));
        }
    }

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
            position.move(- BACKBONE_LENGTH, 0);
        }
        return position;
    }

    static void center(ReactionGlyph reactionGlyph, Coordinate coordinate) {
        final Coordinate diff = coordinate.minus(new CoordinateImpl(reactionGlyph.getPosition().getCenterX(), reactionGlyph.getPosition().getCenterY()));
        move(reactionGlyph, diff);
    }

    static void center(EntityGlyph entityGlyph, Coordinate coordinate) {
        final Coordinate diff = coordinate.minus(new CoordinateImpl(entityGlyph.getPosition().getCenterX(), entityGlyph.getPosition().getCenterY()));
        move(entityGlyph, diff);
    }

    static void center(CompartmentGlyph compartmentGlyph, Coordinate coordinate) {
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

}
