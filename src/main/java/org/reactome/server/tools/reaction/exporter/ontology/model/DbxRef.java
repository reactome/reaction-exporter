package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Wrapper class to digest GO obo-xml format.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class DbxRef {

    @JacksonXmlProperty(localName = "acc")
    private String acc;

    @JacksonXmlProperty(localName = "dbname")
    private String dbName;

    public String getAcc() {
        return acc;
    }

    public String getDbName() {
        return dbName;
    }
}
