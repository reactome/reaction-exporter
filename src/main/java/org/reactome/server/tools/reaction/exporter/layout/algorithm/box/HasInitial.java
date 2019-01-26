package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

/**
 * This interface helps debugging. Classes used in Grid are forced to implement the {@link HasInitial#getInitial()}
 * method, this initial will be used to show the grid as a matrix with all of the elements.
 */
public interface HasInitial {

    Character getInitial();
}
