package org.reactome.server.tools.reaction.exporter.layout;

import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.box.BoxAlgorithm;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.layout.model.Role;
import org.reactome.server.tools.reaction.exporter.layout.result.LayoutParticipants;
import org.reactome.server.tools.reaction.exporter.layout.result.LayoutResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Factory for single reaction {@link Layout}. Layout is computed with positions and dimensions already set for each
 * glyph. This factory uses a fixed layout, with an approximate O(number of entities + number of compartments) cost.
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
@Component
public class LayoutFactory {


    private static final String QUERY = "" +
            "MATCH (rle:ReactionLikeEvent{stId:$stId}) " +
            "OPTIONAL MATCH (rle)-[:normalReaction]->(nr:ReactionLikeEvent) " +
            "WHERE (rle:FailedReaction)" +

            "OPTIONAL MATCH (rle)-[i:input]->(pe:PhysicalEntity) " +
            "WHERE NOT (rle:FailedReaction) OR NOT (rle)-[:entityFunctionalStatus]->()-[:diseaseEntity|normalEntity]->(pe) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: i.stoichiometry, type: 'input'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "OPTIONAL MATCH (nr)-[i:input]->(pe:PhysicalEntity) " +
            "WHERE NOT (rle)-[:input]->(pe) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: i.stoichiometry, type: 'input'}, drug: (pe:Drug) OR NOT d IS NULL, crossed:true} END) AS ps " +

            "OPTIONAL MATCH (rle)-[o:output]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: o.stoichiometry, type: 'output'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "OPTIONAL MATCH (nr)-[o:output]->(pe:PhysicalEntity) " +
            "WHERE NOT (rle)-[:output]->(pe) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: o.stoichiometry, type: 'output'}, drug: (pe:Drug) OR NOT d IS NULL, crossed:true} END) AS ps " +

            "OPTIONAL MATCH (rle)-[:catalystActivity|physicalEntity*]->(pe:PhysicalEntity) " +
            "WHERE NOT (rle:FailedReaction) OR NOT (rle)-[:entityFunctionalStatus]->()-[:diseaseEntity|normalEntity]->(pe) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'catalyst'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "OPTIONAL MATCH (nr)-[:catalystActivity|physicalEntity*]->(pe:PhysicalEntity) " +
            "WHERE NOT (rle)-[:catalystActivity|physicalEntity*]->(pe) AND NOT (rle)-[:entityFunctionalStatus|diseaseEntity*]->(pe) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'catalyst'}, drug: (pe:Drug) OR NOT d IS NULL, crossed:true} END) AS ps " +

            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(pe:PhysicalEntity) " +
            "WHERE NOT (rle:FailedReaction) OR NOT (rle)-[:entityFunctionalStatus]->()-[:diseaseEntity|normalEntity]->(pe) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'negative'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "OPTIONAL MATCH (nr)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(pe:PhysicalEntity) " +
            "WHERE NOT (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(pe) AND NOT (rle)-[:entityFunctionalStatus|normalEntity*]->(pe) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'negative'}, drug: (pe:Drug) OR NOT d IS NULL, crossed:true} END) AS ps " +

            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pe:PhysicalEntity) " +
            "WHERE NOT (rle:FailedReaction) OR NOT (rle)-[:entityFunctionalStatus]->()-[:diseaseEntity|normalEntity]->(pe) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'positive'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "OPTIONAL MATCH (nr)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pe:PhysicalEntity) " +
            "WHERE NOT (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pe) AND NOT (rle)-[:entityFunctionalStatus|normalEntity*]->(pe) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'positive'}, drug: (pe:Drug) OR NOT d IS NULL, crossed:true} END) AS ps " +


            "OPTIONAL MATCH (nr)-[:input]->(:PhysicalEntity)<-[:normalEntity]-(efs:EntityFunctionalStatus)<-[:entityFunctionalStatus]-(rle) " +
            "WHERE (rle:FailedReaction) AND NOT (rle)-[:input]->(:PhysicalEntity)<-[:diseaseEntity|normalEntity]-(efs) " +
            "OPTIONAL MATCH (efs)-[:entityFunctionalStatus|diseaseEntity*]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'input'}, drug: (pe:Drug) OR NOT d IS NULL, dashed:true} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:input]->(:PhysicalEntity)<-[:diseaseEntity|normalEntity]-(efs:EntityFunctionalStatus)<-[:entityFunctionalStatus]-(rle) " +
            "WHERE (rle:FailedReaction) " +
            "OPTIONAL MATCH (efs)-[:entityFunctionalStatus|diseaseEntity*]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'input'}, drug: (pe:Drug) OR NOT d IS NULL, dashed:true} END) AS ps " +

            "OPTIONAL MATCH (nr)-[:catalystActivity|physicalEntity*]->(:PhysicalEntity)<-[:normalEntity]-(efs:EntityFunctionalStatus)<-[:entityFunctionalStatus]-(rle) " +
            "WHERE (rle:FailedReaction) AND NOT (rle)-[:catalystActivity|physicalEntity*]->(:PhysicalEntity)<-[:diseaseEntity|normalEntity]-(efs) " +
            "OPTIONAL MATCH (efs)-[:entityFunctionalStatus|diseaseEntity*]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'catalyst'}, drug: (pe:Drug) OR NOT d IS NULL, dashed:true} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:catalystActivity|physicalEntity*]->(:PhysicalEntity)<-[:diseaseEntity|normalEntity]-(efs:EntityFunctionalStatus)<-[:entityFunctionalStatus]-(rle) " +
            "WHERE (rle:FailedReaction) " +
            "OPTIONAL MATCH (efs)-[:entityFunctionalStatus|diseaseEntity*]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'catalyst'}, drug: (pe:Drug) OR NOT d IS NULL, dashed:true} END) AS ps " +

            "OPTIONAL MATCH (nr)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(:PhysicalEntity)<-[:normalEntity]-(efs:EntityFunctionalStatus)<-[:entityFunctionalStatus]-(rle) " +
            "WHERE (rle:FailedReaction) AND NOT (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(:PhysicalEntity)<-[:diseaseEntity|normalEntity]-(efs) " +
            "OPTIONAL MATCH (efs)-[:entityFunctionalStatus|diseaseEntity*]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'negative'}, drug: (pe:Drug) OR NOT d IS NULL, dashed:true} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(:PhysicalEntity)<-[:diseaseEntity|normalEntity]-(efs:EntityFunctionalStatus)<-[:entityFunctionalStatus]-(rle) " +
            "WHERE (rle:FailedReaction) " +
            "OPTIONAL MATCH (efs)-[:entityFunctionalStatus|diseaseEntity*]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, nr, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'negative'}, drug: (pe:Drug) OR NOT d IS NULL, dashed:true} END) AS ps " +

            "OPTIONAL MATCH (nr)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(:PhysicalEntity)<-[:normalEntity]-(efs:EntityFunctionalStatus)<-[:entityFunctionalStatus]-(rle) " +
            "WHERE (rle:FailedReaction) AND NOT (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(:PhysicalEntity)<-[:diseaseEntity|normalEntity]-(efs) " +
            "OPTIONAL MATCH (efs)-[:entityFunctionalStatus|diseaseEntity*]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'positive'}, drug: (pe:Drug) OR NOT d IS NULL, dashed:true} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(:PhysicalEntity)<-[:diseaseEntity|normalEntity]-(efs:EntityFunctionalStatus)<-[:entityFunctionalStatus]-(rle) " +
            "WHERE (rle:FailedReaction) " +
            "OPTIONAL MATCH (efs)-[:entityFunctionalStatus|diseaseEntity*]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe.stId, role:{n: 1, type: 'positive'}, drug: (pe:Drug) OR NOT d IS NULL, dashed:true} END) AS ps " +

            "OPTIONAL MATCH path=(p:Pathway{hasDiagram:true})-[:hasEvent*]->(rle) " +
            "WHERE SINGLE(x IN NODES(path) WHERE (x:Pathway) AND x.hasDiagram) " +
            "RETURN p.stId AS pathway, rle.stId AS reactionLikeEvent, ps AS participants " +
            "LIMIT 1"; // no matter how many lines, only the first one is used. Parse one line then.

    private final AdvancedDatabaseObjectService ads;
    private final DatabaseObjectService ds;

    public LayoutFactory(AdvancedDatabaseObjectService ads, DatabaseObjectService ds) {
        this.ads = ads;
        this.ds = ds;
    }

    /**
     * Gets the {@link Layout} of rle
     *
     * @param rle a ReactionLikeEvent
     * @return the corresponding layout of the rle
     * @throws NullPointerException if rle is null
     */
    public Layout getReactionLikeEventLayout(Event rle, Style style) {
        if (rle == null) throw new NullPointerException("rle cannot be null");

//        DatabaseObjectService ds = ReactomeGraphCore.getService(DatabaseObjectService.class);

        Map<String, Object> params = new HashMap<>();
        params.put("stId", rle.getStId());
        try {
            // Query returns simple values rather than full objects
            final LayoutResult layoutResult = ads.getCustomQueryResult(LayoutResult.class, QUERY, params);

            // Create Layout and load minimum necessary information and let Lazy-fetch take care of the rest.
            Layout layout = new Layout();
            layout.setPathway(layoutResult.getPathwayStId());
            layout.setReactionLikeEvent(ds.findByIdNoRelations(layoutResult.getReactionStId()));

            Collection<EntityGlyph> participants = new ArrayList<>();
            for (LayoutParticipants layoutParticipant : layoutResult.getParticipants()) {
                EntityGlyph a = new EntityGlyph();
                a.setDrug(layoutParticipant.isDrug());
                a.setDashed(layoutParticipant.isDashed());
                a.setPhysicalEntity(ds.findByIdNoRelations(layoutParticipant.getPhysicalEntity()));
                a.setRole(new Role(layoutParticipant.getRole().getType(), layoutParticipant.getRole().getStoichiometry()));
                participants.add(a);
            }
            layout.setParticipants(participants);
            style.apply(layout);

            return layout;
        } catch (CustomQueryException e) {
            e.printStackTrace();
            return null;
        }
    }

    public enum Style {
        BOX(layout -> new BoxAlgorithm(layout).compute()),
        BRUTE_FORCE(layout -> new BruteForce(layout).compute());

        private final Consumer<Layout> consumer;

        Style(Consumer<Layout> consumer) {
            this.consumer = consumer;
        }

        void apply(Layout layout) {
            consumer.accept(layout);
        }

    }

}
