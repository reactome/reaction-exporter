package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.graph.domain.model.AbstractModifiedResidue;
import org.reactome.server.graph.domain.model.PsiMod;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    AttachmentGlyph(AbstractModifiedResidue amr) {
        super();
        dbId = amr.getDbId();
        schemaClass = amr.getSchemaClass();
        PsiMod psiMod = amr.fetchSingleValue("getPsiMod");
        name = psiMod == null ? null : psiMod.getAbbreviation();
    }

    AttachmentGlyph(AttachmentGlyph attachment) {
        super();
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
