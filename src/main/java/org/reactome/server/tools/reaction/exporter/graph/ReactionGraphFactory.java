package org.reactome.server.tools.reaction.exporter.graph;


import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.tools.diagram.data.graph.EntityNode;
import org.reactome.server.tools.diagram.data.graph.EventNode;
import org.reactome.server.tools.diagram.data.graph.Graph;
import org.reactome.server.tools.diagram.data.graph.impl.EntityNodeImpl;
import org.reactome.server.tools.diagram.data.graph.impl.EventNodeImpl;
import org.reactome.server.tools.diagram.data.graph.impl.GraphImpl;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.layout.model.ReactionGlyph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * For a given reaction like event this class produces a graph with the underlying physical entities and their children.
 * This information will be sent to the client in a second batch so the graph can be kept
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
@Component
public class ReactionGraphFactory {

    private final AdvancedDatabaseObjectService ads;

    @Autowired
    public ReactionGraphFactory(AdvancedDatabaseObjectService ads) {
        this.ads = ads;
    }

    public Graph getGraph(ReactionLikeEvent rle, Layout layout) {
        return new GraphImpl(
                rle.getDbId(),
                rle.getStId(),
                rle.getDisplayName(),
                rle.getSpeciesName(),
                getGraphNodes(rle, layout.getEntities()),
                getGraphEdges(rle, layout.getReaction()),
                new ArrayList<>() //A RLE does not have subpathways
        );
    }

    private List<EntityNode> getGraphNodes(ReactionLikeEvent rle, Collection<EntityGlyph> entityGlyphs) {
        String query = "" +
                "MATCH (rle:ReactionLikeEvent{dbId:{dbId}})-[:input|output|catalystActivity|physicalEntity|entityFunctionalStatus|diseaseEntity|regulatedBy|regulator|hasComponent|hasMember|hasCandidate|repeatedUnit*]->(pe:PhysicalEntity) " +
                "WITH COLLECT(DISTINCT pe) AS pes " +
                "UNWIND pes AS pe " +
                "OPTIONAL MATCH (pe)-[:hasComponent|hasMember|hasCandidate|repeatedUnit]->(children:PhysicalEntity) " +
                "OPTIONAL MATCH (parent:PhysicalEntity)-[:hasComponent|hasMember|hasCandidate|repeatedUnit]->(pe) " +
                "WHERE parent IN pes " +
                "OPTIONAL MATCH (pe)-[:referenceEntity]->(re:ReferenceEntity) " +
                "OPTIONAL MATCH (pe)-[:species]->(s:Species) " +
                "RETURN DISTINCT pe.dbId AS dbId, pe.stId AS stId, pe.displayName AS displayName, pe.schemaClass AS schemaClass, " +
                "       s.dbId AS speciesID, " +
                "       COLLECT(DISTINCT children.dbId) AS children, " +
                "       COLLECT(DISTINCT parent.dbId) AS parents, " +
                "       CASE WHEN re.variantIdentifier IS NULL THEN re.identifier ELSE re.variantIdentifier END AS identifier, " +
                "       re.geneName AS geneNames";
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put("dbId", rle.getDbId());
        try {
            Collection<EntityNodeImpl> rtn = ads.getCustomQueryResults(EntityNodeImpl.class, query, parametersMap);
            Map<Long, List<Long>> map = getMap(entityGlyphs);
            rtn.forEach(node -> node.setDiagramIds(map.get(node.getDbId())));
            return new ArrayList<>(rtn);
        } catch (CustomQueryException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<EventNode> getGraphEdges(ReactionLikeEvent rle, ReactionGlyph rxnGlyph) {
        String query = "" +
                "MATCH (rle:ReactionLikeEvent{dbId:{dbId}}) " +
                "OPTIONAL MATCH (rle)-[:input]->(i:PhysicalEntity) " +
                "OPTIONAL MATCH (rle)-[:output]->(o:PhysicalEntity) " +
                "OPTIONAL MATCH (rle)-[:catalystActivity|physicalEntity*]->(c:PhysicalEntity) " +
                "OPTIONAL MATCH (rle)-[:entityFunctionalStatus|diseaseEntity*]->(e:PhysicalEntity) " +
                "OPTIONAL MATCH (rle)-[:regulatedBy]->(reg:Regulation)-[:regulator]->(r:PhysicalEntity) " +
                "OPTIONAL MATCH prep=(p)-[:hasEvent*]->(pre:ReactionLikeEvent)<-[:precedingEvent]-(rle) " +
                "WHERE SINGLE(x IN NODES(prep) WHERE (x:Pathway) AND x.hasDiagram) " +
                "OPTIONAL MATCH folp=(p)-[:hasEvent*]->(fol:ReactionLikeEvent)-[:precedingEvent]->(rle) " +
                "WHERE SINGLE(x IN NODES(folp) WHERE (x:Pathway) AND x.hasDiagram) " +
                "RETURN rle.dbId AS dbId, rle.stId as stId, rle.displayName AS displayName, rle.schemaClass AS schemaClass, " +
                "       COLLECT(DISTINCT i.dbId) AS inputs, " +
                "       COLLECT(DISTINCT o.dbId) AS outputs, " +
                "       COLLECT(DISTINCT c.dbId) AS catalysts, " +
                "       COLLECT(DISTINCT e.dbId) AS efs, " +
                "       CASE WHEN reg IS NULL THEN [] ELSE COLLECT(DISTINCT {type: reg.schemaClass, dbId: r.dbId}) END AS regulations, " +
                "       COLLECT(DISTINCT pre.dbId) AS preceding, " +
                "       COLLECT(DISTINCT fol.dbId) AS following";
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put("dbId", rle.getDbId());
        try {
            EventNodeImpl rxn = ads.getCustomQueryResult(EventNodeImpl.class, query, parametersMap);
            List<Long> diagramIds = Collections.singletonList(rxnGlyph.getDbId());
            rxn.setDiagramIds(diagramIds);
            return Collections.singletonList(rxn);
        } catch (CustomQueryException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param entityGlyphs a list of entity glyphs present in the layout
     * @return a map from the entity glyph dbId (reactomeId) to the glyph(s) identifier(s) representing it in the layout
     */
    private Map<Long, List<Long>> getMap(Collection<EntityGlyph> entityGlyphs){
        Map<Long, List<Long>> map = new HashMap<>();
        for (EntityGlyph entity : entityGlyphs) {
            map.computeIfAbsent(entity.getDbId(), k -> new ArrayList<>()).add(entity.getId());
        }
        return map;
    }
}
