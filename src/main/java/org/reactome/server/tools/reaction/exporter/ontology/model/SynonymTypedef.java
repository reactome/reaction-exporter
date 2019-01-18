package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

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
