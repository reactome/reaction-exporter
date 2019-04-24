package org.reactome.server.tools.reaction.exporter.diagram;

import org.reactome.server.tools.diagram.data.layout.*;
import org.reactome.server.tools.diagram.data.layout.impl.*;
import org.reactome.server.tools.reaction.exporter.layout.common.Bounds;
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
        diagram.setStableId(rxnLayout.getPathway());
        diagram.setDisease(reaction.isDisease());
        diagram.setDisplayName(reaction.getName());
        diagram.setDbId(reaction.getDbId());
        diagram.setCompartments(getCompartments(rxnLayout));
        diagram.setEdges(getEdges(rxnLayout));
        diagram.setMaxX((int) rxnLayout.getBounds().getMaxX());
        diagram.setMaxY((int) rxnLayout.getBounds().getMaxY());
        diagram.setMinX((int) rxnLayout.getBounds().getX());
        diagram.setMinY((int) rxnLayout.getBounds().getY());
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
            final Bounds bounds = comp.getBounds();
            compartment.setProp(getProp(bounds));
            compartment.setTextPosition(comp.getLabelPosition().minus(GWU_CORRECTION));
        }
        return compartments;
    }

    private static void copyGlyphToDatabaseObject(Glyph glyph, DiagramObjectImpl object) {
        final Bounds bounds = glyph.getBounds();
        object.setId(glyph.getId());
        object.setMinX(bounds.getX());
        object.setMinY(bounds.getY());
        object.setMaxX(bounds.getMaxX());
        object.setMaxY(bounds.getMaxY());
        object.setDisplayName(glyph.getName());
        object.setRenderableClass(glyph.getRenderableClass().toString());
        object.setSchemaClass(glyph.getSchemaClass());
        object.setReactomeId(glyph.getDbId());
        object.setPosition(new CoordinateImpl(bounds.getCenterX(), bounds.getCenterY()));
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
        final Bounds bounds = reaction.getBounds();
        final Coordinate a;
        final Coordinate b;
        final Coordinate c;
        switch (reaction.getRenderableClass()) {
            case DISSOCIATION_REACTION:
                c = new CoordinateImpl(bounds.getCenterX(), bounds.getCenterY());
                return new DoubleCircleImpl(c, 6., 4.);
            case OMITTED_REACTION:
                a = new CoordinateImpl(bounds.getX(), bounds.getY());
                b = new CoordinateImpl(bounds.getMaxX(), bounds.getMaxY());
                return new BoxImpl(a, b, true, "\\\\");
            case UNCERTAIN_REACTION:
                a = new CoordinateImpl(bounds.getX(), bounds.getY());
                b = new CoordinateImpl(bounds.getMaxX(), bounds.getMaxY());
                return new BoxImpl(a, b, true, "?");
            case BINDING_REACTION:
                c = new CoordinateImpl(bounds.getCenterX(), bounds.getCenterY());
                return new CircleImpl(c, 6., null, null);
            case TRANSITION_REACTION:
            default:
                a = new CoordinateImpl(bounds.getX(), bounds.getY());
                b = new CoordinateImpl(bounds.getMaxX(), bounds.getMaxY());
                return new BoxImpl(a, b, true, null);
        }
    }

    private static List<Node> getNodes(Layout rxnLayout) {
        List<Node> nodes = new ArrayList<>();
        for (EntityGlyph entity : rxnLayout.getEntities()) {
            final NodeImpl node = new NodeImpl();
            copyGlyphToDatabaseObject(entity, node);
            node.setTrivial(entity.isTrivial() ? true : null);
            node.setDisease(entity.isDisease() ? true : null);
            node.setFadeOut(entity.isFadeOut() ? true : null);
            node.setCrossed(entity.isCrossed() ? true : null);
            node.setNeedDashBorder(entity.isDashed() ? true : null);
            final Bounds bounds = entity.getBounds();
            node.setProp(getProp(bounds));
            final ConnectorImpl connector = (ConnectorImpl) entity.getConnector();
            node.setConnectors(Collections.singletonList(connector));
            connector.setEdgeId(rxnLayout.getReaction().getId());
            connector.setDisease(rxnLayout.getReaction().isDisease() ? true : null);
            connector.setFadeOut(entity.isFadeOut() ? true : null);
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
        final Bounds bounds = attachment.getBounds();
        final Coordinate a = new CoordinateImpl(bounds.getX(), bounds.getY());
        final Coordinate b = new CoordinateImpl(bounds.getMaxX(), bounds.getMaxY());
        return new BoxImpl(a, b, true, attachment.getName());
    }

    private static NodeProperties getProp(Bounds bounds) {
        return NodePropertiesFactory.get(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }
}
