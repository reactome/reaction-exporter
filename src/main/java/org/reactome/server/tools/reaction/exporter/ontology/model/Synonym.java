package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Synonym {

    @JacksonXmlProperty(localName = "synonym_text")
    private String synonymText;

    public String getSynonymText() {
        return synonymText;
    }

    @JacksonXmlProperty(localName = "scope", isAttribute = true)
    private String scope;

    @JacksonXmlProperty(localName = "synonym_type", isAttribute = true)
    private String synonymType;

    @JacksonXmlProperty(localName = "dbxref")
    private DbxRef dbxRef;

    @JacksonXmlProperty(localName = "comment", isAttribute = true)
    private String comment;

    @JacksonXmlProperty(localName = "synonymtypedef", isAttribute = true)
    private String synonymtypedef;

    public String getScope() {
        return scope;
    }

    public String getSynonymType() {
        return synonymType;
    }

    public DbxRef getDbxRef() {
        return dbxRef;
    }

    public String getComment() {
        return comment;
    }

    public String getSynonymtypedef() {
        return synonymtypedef;
    }
}
