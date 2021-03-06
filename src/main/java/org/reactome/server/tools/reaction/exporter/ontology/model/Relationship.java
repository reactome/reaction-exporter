package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Wrapper class to digest GO obo-xml format.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Relationship {

    @JacksonXmlProperty(localName = "type")
    private String type;

    @JacksonXmlProperty(localName = "to")
    private String to;

    public String getTo() {
        return to;
    }

    public String getType() {
        return type;
    }
}
