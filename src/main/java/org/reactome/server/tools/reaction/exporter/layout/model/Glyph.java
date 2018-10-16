package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;

/**
 * Common methods to all the Glyph objects
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Glyph extends HasPosition {

    Long getId();

    String getName();

    RenderableClass getRenderableClass();

}
