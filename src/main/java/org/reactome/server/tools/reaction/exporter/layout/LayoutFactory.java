package org.reactome.server.tools.reaction.exporter.layout;

import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.layout.model.ReactionGlyph;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a raw Layout object containing the necessary data to apply the layout algorithm that will position each
 * element in the plane
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class LayoutFactory {
    
    private static final String QUERY = "" +
            "MATCH (rle:ReactionLikeEvent{stId:{stId}}) " +
            "OPTIONAL MATCH (rle)-[i:input]->(input:PhysicalEntity) " +
            "WITH rle, COLLECT(DISTINCT CASE input WHEN NULL THEN NULL ELSE {stoichiometry: i.stoichiometry, pe: input} END) AS inputs " +
            "OPTIONAL MATCH (rle)-[o:output]->(output:PhysicalEntity) " +
            "WITH rle, inputs, COLLECT(DISTINCT CASE output WHEN NULL THEN NULL ELSE {stoichiometry: o.stoichiometry, pe: output} END) AS outputs " +
            "OPTIONAL MATCH (rle)-[:catalystActivity|physicalEntity*]->(catalyst:PhysicalEntity) " +
            "WITH rle, inputs, outputs, COLLECT(DISTINCT CASE catalyst WHEN NULL THEN NULL ELSE {stoichiometry: 1, pe: catalyst} END) AS catalysts " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(negative:PhysicalEntity) " +
            "WITH rle, inputs, outputs, catalysts, COLLECT(DISTINCT CASE negative WHEN NULL THEN NULL ELSE  {stoichiometry: 1, pe: negative} END) AS positiveRegulators " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(positive:PhysicalEntity) " +
            "WITH rle, inputs, outputs, catalysts, positiveRegulators, COLLECT(DISTINCT CASE positive WHEN NULL THEN NULL ELSE {stoichiometry: 1, pe: positive} END) AS negativeRegulators " +
            "OPTIONAL MATCH (rle)-[:compartment]->(c:Compartment) " +
            "RETURN rle, " +
            "       inputs, " +
            "       outputs, " +
            "       catalysts, " +
            "       positiveRegulators, " +
            "       negativeRegulators";
    
    public static Layout getReactionLikeEventLayout(AdvancedDatabaseObjectService ads, ReactionLikeEvent rle) {
        Map<String, Object> params = new HashMap<>();
        params.put("stId", rle.getStId());
        try {
            return new Layout(ads.getCustomQueryResult(ReactionGlyph.class, QUERY,  params));
        } catch (CustomQueryException e) {
            //TODO: log -> e.printStackTrace();
            e.printStackTrace();
            return null;
        }
    }
}
