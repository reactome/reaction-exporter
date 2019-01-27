package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * Wrapper class to digest GO obo-xml format.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Obo {

    @JacksonXmlProperty
    private Source source;

    @JacksonXmlProperty
    private Header header;

    @JacksonXmlProperty(localName = "term")
    @JacksonXmlElementWrapper(localName = "term", useWrapping = false)
    private List<Term> terms;

    @JacksonXmlProperty(localName = "typedef")
    @JacksonXmlElementWrapper(localName = "typedef", useWrapping = false)
    private List<TypeDef> typeDefs;

    public Source getSource() {
        return source;
    }

    public Header getHeader() {
        return header;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public List<TypeDef> getTypeDefs() {
        return typeDefs;
    }
}
