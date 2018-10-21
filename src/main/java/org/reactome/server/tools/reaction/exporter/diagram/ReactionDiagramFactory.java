package org.reactome.server.tools.reaction.exporter.diagram;

import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;

/**
 * Converts a given instance of {@link Layout} to {@link Diagram}
 *
 * The reaction-exporter project uses {@link Layout} objects to ease extraction and layout of
 * reactions. Once the results have to be shared with other projects (such as diagram-exporter)
 * this needs to be shared with a common format to avoid rewriting renderers.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class ReactionDiagramFactory {

    private ReactionDiagramFactory() { }

    public static Diagram get(Layout rxnLayout){
        // TODO: transform Layout to diagram;
        return null;
    }
}
