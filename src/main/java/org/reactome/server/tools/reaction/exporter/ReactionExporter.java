package org.reactome.server.tools.reaction.exporter;

import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.tools.diagram.data.graph.Graph;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.reaction.exporter.diagram.ReactionDiagramFactory;
import org.reactome.server.tools.reaction.exporter.graph.ReactionGraphFactory;
import org.reactome.server.tools.reaction.exporter.layout.LayoutFactory;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public class ReactionExporter {

    @Autowired
    private LayoutFactory layoutFactory;

    @Autowired
    private ReactionGraphFactory graphFactory;

    //public ReactionExporter(AdvancedDatabaseObjectService ads) {
    //    layoutFactory = new LayoutFactory(ads);
    //    graphFactory = new ReactionGraphFactory(ads);
    //}

    public Layout getReactionLayout(ReactionLikeEvent rle){
        return layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.COMPACT);
    }

    public Diagram getReactionDiagram(Layout layout){
        return ReactionDiagramFactory.get(layout);
    }

    public Graph getReactionGraph(ReactionLikeEvent rle, Layout layout){
        return graphFactory.getGraph(rle, layout);
    }
}
