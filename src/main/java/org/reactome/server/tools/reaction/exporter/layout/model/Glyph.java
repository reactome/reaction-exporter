package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;

/**
 * Common methods to all the Glyph objects
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface Glyph extends HasBounds {

    Long getId();

    Long getDbId();

    String getName();

    String getSchemaClass();

    RenderableClass getRenderableClass();

}
