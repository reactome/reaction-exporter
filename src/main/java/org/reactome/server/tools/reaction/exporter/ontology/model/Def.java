package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class Def {

    @JacksonXmlProperty(localName = "defstr")
    private String defstr;

    @JacksonXmlProperty(localName = "dbxref")
    @JacksonXmlElementWrapper(localName = "dbxref", useWrapping = false)
    private List<DbxRef> dbxRefs;

    @JacksonXmlProperty(localName = "comment")
    private String comment;

    public String getDefstr() {
        return defstr;
    }

    public List<DbxRef> getDbxRefs() {
        return dbxRefs;
    }

    public String getComment() {
        return comment;
    }
}
