package org.reactome.server.tools.reaction.exporter.layout;

import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a raw Layout object containing the necessary data to apply the layout algorithm that will position each
 * element in the plane
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
@Component
public class LayoutFactory {

    private AdvancedDatabaseObjectService ads;

    @Autowired
    public LayoutFactory(AdvancedDatabaseObjectService ads) {
        this.ads = ads;
    }

    private static final String QUERY = "" +
            "MATCH (rle:ReactionLikeEvent{stId:{stId}}) " +
            "OPTIONAL MATCH (rle)-[i:input]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe, role:{n: i.stoichiometry, type: 'input'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "OPTIONAL MATCH (rle)-[o:output]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe, role:{n: o.stoichiometry, type: 'output'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:catalystActivity|physicalEntity*]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe, role:{n: 1, type: 'catalyst'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe, role:{n: 1, type: 'negative'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pe:PhysicalEntity) " +
            "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate*]->(d:Drug) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE pe WHEN NULL THEN NULL ELSE {physicalEntity: pe, role:{n: 1, type: 'positive'}, drug: (pe:Drug) OR NOT d IS NULL} END) AS ps " +
            "RETURN rle AS reactionLikeEvent, ps AS participants";

    public Layout getReactionLikeEventLayout(ReactionLikeEvent rle) {
        Map<String, Object> params = new HashMap<>();
        params.put("stId", rle.getStId());
        try {
            return ads.getCustomQueryResult(Layout.class, QUERY, params);
        } catch (CustomQueryException e) {
            //TODO: log -> e.printStackTrace();
            e.printStackTrace();
            return null;
        }
    }
}
