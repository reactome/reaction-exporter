package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.graph.domain.model.TranslationalModification;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;

/**
 * Small boxes that appear in the proteins with any kind of modification
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class AttachmentGlyph extends AbstractGlyph {

	private Long dbId;
    private String schemaClass;
    private String name;

    AttachmentGlyph(TranslationalModification amr) {
        super();
        dbId = amr.getDbId();
        schemaClass = amr.getSchemaClass();
        name = amr.getLabel();
    }

    AttachmentGlyph(AttachmentGlyph attachment) {
        super();
        dbId = attachment.dbId;
        schemaClass = attachment.schemaClass;
        name = attachment.name;
    }

	@Override
    public String getName() {
        return name;
    }

    @Override
    public String getSchemaClass() {
        return schemaClass;
    }

    @Override
    public RenderableClass getRenderableClass() {
        return RenderableClass.ATTACHMENT;
    }

	@Override
	public Long getDbId() {
		return dbId;
	}

	@Override
    public String toString() {
        return "AttachmentGlyph{" +
                "name='" + name + '\'' +
                '}';
    }
}
