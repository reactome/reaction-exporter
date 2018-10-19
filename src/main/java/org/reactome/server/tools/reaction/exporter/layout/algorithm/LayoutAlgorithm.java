package org.reactome.server.tools.reaction.exporter.layout.algorithm;

import org.reactome.server.tools.reaction.exporter.layout.model.Layout;

public interface LayoutAlgorithm {

    /**
     * Computes the positions of the glyphs inside the layout and adds the necessary segments to get a fully functional
     * Reactome like Layout.
     *
     * @param layout the object to calculate its positions.
     */
    void compute(Layout layout);
}
