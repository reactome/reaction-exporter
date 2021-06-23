package org.reactome.server.tools.reaction.exporter.layout.result;

import org.neo4j.driver.Record;
import org.reactome.server.graph.domain.result.CustomQuery;

import java.util.List;

public class LayoutResult implements CustomQuery {

    private String pathwayStId;
    private String reactionStId;
    private List<LayoutParticipants> participants;

    public String getPathwayStId() {
        return pathwayStId;
    }

    public void setPathwayStId(String pathwayStId) {
        this.pathwayStId = pathwayStId;
    }

    public String getReactionStId() {
        return reactionStId;
    }

    public void setReactionStId(String reactionStId) {
        this.reactionStId = reactionStId;
    }

    public List<LayoutParticipants> getParticipants() {
        return participants;
    }

    public void setParticipants(List<LayoutParticipants> participants) {
        this.participants = participants;
    }

    @Override
    public CustomQuery build(Record r) {
        LayoutResult lr = new LayoutResult();
        lr.setPathwayStId(r.get("pathway").asString());
        lr.setReactionStId(r.get("reactionLikeEvent").asString());
        lr.setParticipants(r.get("participants").asList(LayoutParticipants::build));
        return lr;
    }
}
