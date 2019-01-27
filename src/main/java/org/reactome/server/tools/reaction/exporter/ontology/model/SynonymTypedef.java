package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Wrapper class to digest GO obo-xml format.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class SynonymTypedef {

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JacksonXmlProperty(localName = "scope")
    private String scope;

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getScope() {
        return scope;
    }

}
