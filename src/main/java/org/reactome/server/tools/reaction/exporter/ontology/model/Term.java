package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Collections;
import java.util.List;

/**
 * Wrapper class to digest GO obo-xml format.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Term {

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JacksonXmlProperty(localName = "namespace")
    private String namespace;

    @JacksonXmlProperty(localName = "def")
    private Def def;

    @JacksonXmlProperty(localName = "synonym")
    private Synonym synonym;

    @JacksonXmlProperty(localName = "is_a")
    @JacksonXmlElementWrapper(localName = "is_a", useWrapping = false)
    private List<String> isA = Collections.emptyList();

    @JacksonXmlProperty(localName = "alt_id")
    @JacksonXmlElementWrapper(localName = "alt_id", useWrapping = false)
    private List<String> altIds = Collections.emptyList();

    @JacksonXmlProperty(localName = "subset")
    @JacksonXmlElementWrapper(localName = "subset", useWrapping = false)
    private List<String> subset = Collections.emptyList();

    @JacksonXmlProperty(localName = "is_obsolete")
    private Boolean obsolete;

    @JacksonXmlProperty(localName = "xref_analog")
    @JacksonXmlElementWrapper(localName = "xref_analog", useWrapping = false)
    private List<XRefAnalog> xrefAnalog = Collections.emptyList();

    @JacksonXmlProperty(localName = "comment")
    private String comment;

    @JacksonXmlProperty(localName = "consider")
    @JacksonXmlElementWrapper(localName = "consider", useWrapping = false)
    private List<String> consider = Collections.emptyList();

    @JacksonXmlProperty(localName = "relationship")
    @JacksonXmlElementWrapper(localName = "relationship", useWrapping = false)
    private List<Relationship> relationships = Collections.emptyList();

    @JacksonXmlProperty(localName = "replaced_by")
    private String replacedBy;

    @JacksonXmlProperty(localName = "is_root")
    private Boolean root;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public Def getDef() {
        return def;
    }

    public Synonym getSynonym() {
        return synonym;
    }

    public List<String> getIsA() {
        return isA;
    }

    public List<String> getAltIds() {
        return altIds;
    }

    public List<String> getSubset() {
        return subset;
    }

    public Boolean getObsolete() {
        return obsolete != null && obsolete;
    }

    @JacksonXmlProperty(localName = "is_obsolete")
    public void setObsolete(String obsolete) {
        this.obsolete = obsolete != null;
    }

    public List<XRefAnalog> getXrefAnalog() {
        return xrefAnalog;
    }

    public String getComment() {
        return comment;
    }

    public List<String> getConsider() {
        return consider;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public Boolean getRoot() {
        return root;
    }

    @JacksonXmlProperty(localName = "is_root")
    public void setRoot(String isRoot) {
        root = isRoot != null;
    }
}
