package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class HoldsOverChain {

    @JacksonXmlProperty(localName = "relation")
    @JacksonXmlElementWrapper(localName = "relation", useWrapping = false)
    private List<String> relations;

    public List<String> getRelations() {
        return relations;
    }
}
