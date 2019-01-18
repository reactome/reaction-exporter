package org.reactome.server.tools.reaction.exporter.ontology.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Source {

    @JacksonXmlProperty(localName = "source_id")
    private String sourceId;

    @JacksonXmlProperty(localName = "source_type")
    private String sourceType;

    @JacksonXmlProperty(localName = "source_fullpath")
    private String sourceFullpath;

    @JacksonXmlProperty(localName = "source_path")
    private String sourcePath;

    @JacksonXmlProperty(localName = "source_md5")
    private String sourceMd5;

    @JacksonXmlProperty(localName = "source_mtime")
    private Long sourceMtime;

    @JacksonXmlProperty(localName = "source_parsetime")
    private Long sourceParsetime;

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceFullpath() {
        return sourceFullpath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getSourceMd5() {
        return sourceMd5;
    }

    public Long getSourceMtime() {
        return sourceMtime;
    }

    public Long getSourceParsetime() {
        return sourceParsetime;
    }
}
