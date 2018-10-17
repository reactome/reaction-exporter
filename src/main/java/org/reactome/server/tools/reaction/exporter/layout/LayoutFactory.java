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
            "OPTIONAL MATCH (rle)-[i:input]->(pei:PhysicalEntity) " +
            "WITH rle, COLLECT(DISTINCT CASE pei WHEN NULL THEN NULL ELSE {physicalEntity: pei, role:{n: i.stoichiometry, type: 'input'}} END) AS ps " +
            "OPTIONAL MATCH (rle)-[o:output]->(peo:PhysicalEntity) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE peo WHEN NULL THEN NULL ELSE {physicalEntity: peo, role:{n: o.stoichiometry, type: 'output'}} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:catalystActivity|physicalEntity*]->(pec:PhysicalEntity) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE pec WHEN NULL THEN NULL ELSE {physicalEntity: pec, role:{n: 1, type: 'catalyst'}} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(pen:PhysicalEntity) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE pen WHEN NULL THEN NULL ELSE {physicalEntity: pen, role:{n: 1, type: 'negative'}} END) AS ps " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pep:PhysicalEntity) " +
            "WITH rle, ps + COLLECT(DISTINCT CASE pep WHEN NULL THEN NULL ELSE {physicalEntity: pep, role:{n: 1, type: 'positive'}} END) AS ps " +
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
