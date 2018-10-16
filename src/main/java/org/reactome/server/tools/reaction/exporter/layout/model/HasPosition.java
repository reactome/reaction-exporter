package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;

/**
 * Objects implementing this interface will have to be assigned a position and dimension (x, y, width, height)
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public interface HasPosition {

    Position getPosition();

}
