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

    //Provided by Bijay Jassal
    private static final Set<String> PHOSPHORYLATION = new HashSet<>(Arrays.asList("01931", "01614", "00045", "00046", "00047", "00048", "00043", "00044", "00227", "00330", "00260", "00254", "00251", "00259", "00272", "00261", "00218", "00208", "00232", "00233", "00376", "00377", "00328", "00439", "00435", "01451", "01972", "00042", "00890", "00696", "00541", "01792", "01452", "01163", "01797", "01798"));

    AttachmentGlyph(AbstractModifiedResidue amr) {
        super();
        dbId = amr.getDbId();
        schemaClass = amr.getSchemaClass();
        try {
            Method psiModMethod = amr.getClass().getMethod("getPsiMod");
            PsiMod psiMod = (PsiMod) psiModMethod.invoke(amr);
            name = PHOSPHORYLATION.contains(psiMod.getIdentifier()) ? "P" : "";
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            name = "";
        }
        //name = amr.getDisplayName().contains("-phospho-") ? "P" : "";
    }

    public AttachmentGlyph(AttachmentGlyph attachement) {
        super();
        name = attachement.name;
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
