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

@Component
public class ReactionExporter {

    private LayoutFactory layoutFactory;

    private ReactionGraphFactory graphFactory;

    @Autowired
    public ReactionExporter(LayoutFactory layoutFactory, ReactionGraphFactory graphFactory) {
        this.layoutFactory = layoutFactory;
        this.graphFactory = graphFactory;
    }

    public Layout getReactionLayout(ReactionLikeEvent rle){
        return layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.BOX);
    }

    public Layout getReactionLayout(ReactionLikeEvent rle,  LayoutFactory.Style style){
        return layoutFactory.getReactionLikeEventLayout(rle, style);
    }

    public Diagram getReactionDiagram(Layout layout){
        return ReactionDiagramFactory.get(layout);
    }

    public Graph getReactionGraph(ReactionLikeEvent rle, Layout layout){
        return graphFactory.getGraph(rle, layout);
    }
}
