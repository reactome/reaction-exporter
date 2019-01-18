package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class Header {

    @JacksonXmlProperty(localName = "format-version")
    private String formatVersion;

    @JacksonXmlProperty(localName = "data-version")
    private String dataVersion;

    @JacksonXmlProperty(localName = "subsetdef")
    @JacksonXmlElementWrapper(localName = "subsetdef", useWrapping = false)
    private List<SubsetDef> subsetDefs;

    @JacksonXmlProperty(localName = "synonymtypedef")
    @JacksonXmlElementWrapper(localName = "synonymtypedef", useWrapping = false)
    private List<SynonymTypedef> synonymtypedefs;

    @JacksonXmlProperty(localName = "default-namespace")
    private String defautlNamespace;

    @JacksonXmlProperty(localName = "remark")
    @JacksonXmlElementWrapper(localName = "remark", useWrapping = false)
    private List<String> remarks;

    @JacksonXmlProperty
    private String ontology;

    public String getFormatVersion() {
        return formatVersion;
    }

    public List<String> getRemarks() {
        return remarks;
    }

    public List<SubsetDef> getSubsetDefs() {
        return subsetDefs;
    }

    public List<SynonymTypedef> getSynonymtypedefs() {
        return synonymtypedefs;
    }

    public String getDataVersion() {
        return dataVersion;
    }

    public String getDefautlNamespace() {
        return defautlNamespace;
    }

    public String getOntology() {
        return ontology;
    }

}
