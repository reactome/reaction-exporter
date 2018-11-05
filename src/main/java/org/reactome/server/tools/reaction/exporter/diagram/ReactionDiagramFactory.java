package org.reactome.server.tools.reaction.exporter.diagram;

import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.tools.diagram.data.layout.*;
import org.reactome.server.tools.diagram.data.layout.impl.*;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts a given instance of {@link Layout} to {@link Diagram}
 * <p>
 * The reaction-exporter project uses {@link Layout} objects to ease extraction and layout of reactions. Once the
 * results have to be shared with other projects (such as diagram-exporter) this needs to be shared with a common format
 * to avoid rewriting renderers.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class ReactionDiagramFactory {

    /**
     * We need to cause the same error than the standard diagrams
     */
    private static final Coordinate GWU_CORRECTION = CoordinateFactory.get(14, 18);

    private ReactionDiagramFactory() {
    }

    public static Diagram get(Layout rxnLayout) {
        final DiagramImpl diagram = new DiagramImpl();
        final ReactionGlyph reaction = rxnLayout.getReaction();
        Pathway pwy = rxnLayout.getPathway();
        diagram.setStableId(pwy == null ? null : pwy.getStId());
        diagram.setDisease(reaction.isDisease());
        diagram.setDisplayName(reaction.getName());
        diagram.setDbId(reaction.getDbId());
        diagram.setCompartments(getCompartments(rxnLayout));
        diagram.setEdges(getEdges(rxnLayout));
        diagram.setMaxX((int) rxnLayout.getPosition().getMaxX());
        diagram.setMaxY((int) rxnLayout.getPosition().getMaxY());
        diagram.setMinX((int) rxnLayout.getPosition().getX());
        diagram.setMinY((int) rxnLayout.getPosition().getY());
        diagram.setNodes(getNodes(rxnLayout));
        diagram.setNotes(Collections.emptyList());
        diagram.setShadows(Collections.emptyList());
        diagram.setLinks(Collections.emptyList());
        return diagram;
    }

    private static List<Compartment> getCompartments(Layout rxnLayout) {
        final List<Compartment> compartments = new ArrayList<>();
        for (CompartmentGlyph comp : rxnLayout.getCompartments()) {
            final List<Long> ids = new ArrayList<>();
            for (Glyph glyph : comp.getContainedGlyphs()) {
                ids.add(glyph.getId());
            }
            for (final CompartmentGlyph child : comp.getChildren()) {
                ids.add(child.getId());
            }
            final CompartmentImpl compartment = new CompartmentImpl(ids);
            compartments.add(compartment);
            copyGlyphToDatabaseObject(comp, compartment);
            final Position position = comp.getPosition();
            compartment.setProp(getProp(position));
            compartment.setTextPosition(comp.getLabelPosition().minus(GWU_CORRECTION));
        }
        return compartments;
    }

    private static void copyGlyphToDatabaseObject(Glyph glyph, DiagramObjectImpl object) {
        final Position position = glyph.getPosition();
        object.setId(glyph.getId());
        object.setMinX(position.getX());
        object.setMinY(position.getY());
        object.setMaxX(position.getMaxX());
        object.setMaxY(position.getMaxY());
        object.setDisplayName(glyph.getName());
        object.setRenderableClass(glyph.getRenderableClass().toString());
        object.setSchemaClass(glyph.getSchemaClass());
        object.setReactomeId(glyph.getDbId());
        object.setPosition(new CoordinateImpl(position.getCenterX(), position.getCenterY()));
    }

    /**
     * Actually, get 'edge' :P
     */
    private static List<Edge> getEdges(Layout rxnLayout) {
        final ReactionGlyph reaction = rxnLayout.getReaction();
        final EdgeImpl edge = new EdgeImpl();
        copyGlyphToDatabaseObject(reaction, edge);
        edge.setReactionShape(getReactionShape(reaction));
        edge.setSegments(reaction.getSegments());
        edge.setDisease(reaction.isDisease());
        final List<ReactionPart> activators = new ArrayList<>();
        final List<ReactionPart> catalyst = new ArrayList<>();
        final List<ReactionPart> inhibitors = new ArrayList<>();
        final List<ReactionPart> inputs = new ArrayList<>();
        final List<ReactionPart> outputs = new ArrayList<>();
        for (EntityGlyph entity : rxnLayout.getEntities()) {
            if (entity.isCrossed() != null && entity.isCrossed()) continue;
            for (Role role : entity.getRoles()) {
                final ReactionPartImpl reactionPart = new ReactionPartImpl();
                reactionPart.setId(entity.getId());
                reactionPart.setStoichiometry(role.getStoichiometry());
                switch (role.getType()) {
                    case INPUT:
                        inputs.add(reactionPart);
                        break;
                    case OUTPUT:
                        outputs.add(reactionPart);
                        break;
                    case CATALYST:
                        catalyst.add(reactionPart);
                        break;
                    case NEGATIVE_REGULATOR:
                        inhibitors.add(reactionPart);
                        break;
                    case POSITIVE_REGULATOR:
                        activators.add(reactionPart);
                        break;
                }
            }

        }
        edge.setActivators(activators);
        edge.setCatalysts(catalyst);
        edge.setInhibitors(inhibitors);
        edge.setInputs(inputs);
        edge.setOutputs(outputs);
        edge.setRenderableClass("Reaction");
        return Collections.singletonList(edge);
    }

    private static Shape getReactionShape(ReactionGlyph reaction) {
        final Position position = reaction.getPosition();
        final Coordinate a;
        final Coordinate b;
        final Coordinate c;
        switch (reaction.getRenderableClass()) {
            case DISSOCIATION_REACTION:
                c = new CoordinateImpl(position.getCenterX(), position.getCenterY());
                return new DoubleCircleImpl(c, 6., 4.);
            case OMITTED_REACTION:
                a = new CoordinateImpl(position.getX(), position.getY());
                b = new CoordinateImpl(position.getMaxX(), position.getMaxY());
                return new BoxImpl(a, b, true, "\\\\");
            case UNCERTAIN_REACTION:
                a = new CoordinateImpl(position.getX(), position.getY());
                b = new CoordinateImpl(position.getMaxX(), position.getMaxY());
                return new BoxImpl(a, b, true, "?");
            case BINDING_REACTION:
                c = new CoordinateImpl(position.getCenterX(), position.getCenterY());
                return new CircleImpl(c, 6., null, null);
            case TRANSITION_REACTION:
            default:
                a = new CoordinateImpl(position.getX(), position.getY());
                b = new CoordinateImpl(position.getMaxX(), position.getMaxY());
                return new BoxImpl(a, b, true, null);
        }
    }

    private static List<Node> getNodes(Layout rxnLayout) {
        List<Node> nodes = new ArrayList<>();
        for (EntityGlyph entity : rxnLayout.getEntities()) {
            final NodeImpl node = new NodeImpl();
            copyGlyphToDatabaseObject(entity, node);
            node.setTrivial(entity.isTrivial());
            node.setDisease(entity.isDisease());
            node.setFadeOut(entity.isCrossed());
            node.setCrossed(entity.isCrossed());
            final Position position = entity.getPosition();
            node.setProp(getProp(position));
            node.setConnectors(Collections.singletonList(entity.getConnector()));
            List<NodeAttachment> attachments = new ArrayList<>();
            for (AttachmentGlyph attachment : entity.getAttachments()) {
                final NodeAttachmentImpl nodeAttachment = new NodeAttachmentImpl();
                nodeAttachment.setLabel(attachment.getName());
                nodeAttachment.setReactomeId(attachment.getDbId());
                nodeAttachment.setShape(getAttachmentShape(attachment));
                attachments.add(nodeAttachment);
            }
            node.setNodeAttachments(attachments);
            nodes.add(node);
        }
        return nodes;
    }

    private static Shape getAttachmentShape(AttachmentGlyph attachment) {
        final Position position = attachment.getPosition();
        final Coordinate a = new CoordinateImpl(position.getX(), position.getY());
        final Coordinate b = new CoordinateImpl(position.getMaxX(), position.getMaxY());
        return new BoxImpl(a, b, true, attachment.getName());
    }

    private static NodeProperties getProp(Position position) {
        return NodePropertiesFactory.get(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }
}
