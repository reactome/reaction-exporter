package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * Wrapper class to digest GO obo-xml format.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class TypeDef {

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JacksonXmlProperty(localName = "namespace")
    private String namespace;

    @JacksonXmlProperty(localName = "xref_analog")
    @JacksonXmlElementWrapper(localName = "xref_analog", useWrapping = false)
    private List<XRefAnalog> xrefAnalog;

    @JacksonXmlProperty(localName = "is_a")
    @JacksonXmlElementWrapper(localName = "is_a", useWrapping = false)
    private List<String> isA;

    @JacksonXmlProperty(localName = "transitive_over")
    private String transitiveOver;

    private Boolean metadataTag;
    private Boolean classLevel;
    private Boolean transitive;

    @JacksonXmlProperty(localName = "holds_over_chain")
    private HoldsOverChain holdsOverChain;

    @JacksonXmlProperty(localName = "is_metadata_tag")
    public void setMetadataTag(String isMetadataTag) {
        this.metadataTag = isMetadataTag != null;
    }

    @JacksonXmlProperty(localName = "is_class_level")
    public void setClassLevel(String isClassLevel) {
        this.classLevel = isClassLevel != null;
    }

    @JacksonXmlProperty(localName = "is_transitive")
    public void setTransitive(String isTransitive) {
        this.transitive = isTransitive != null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<XRefAnalog> getXrefAnalog() {
        return xrefAnalog;
    }

    public List<String> getIsA() {
        return isA;
    }

    public String getTransitiveOver() {
        return transitiveOver;
    }

    public Boolean getMetadataTag() {
        return metadataTag;
    }

    public Boolean getClassLevel() {
        return classLevel;
    }

    public Boolean getTransitive() {
        return transitive;
    }

    public HoldsOverChain getHoldsOverChain() {
        return holdsOverChain;
    }
}
