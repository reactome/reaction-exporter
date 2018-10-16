package org.reactome.server.tools.reaction.exporter.layout.model;

import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;

import java.util.*;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class ReactionGlyph extends AbstractGlyph {

    private ReactionLikeEvent rle;

    private Map<EntityRole, Set<EntityGlyph>> participants = new HashMap<>();

    ReactionLikeEvent getReactionLikeEvent() {
        return rle;
    }

    @Override
    public String getName() {
        return rle.getDisplayName();
    }

    @Override
    public RenderableClass getRenderableClass() {
        return RenderableClass.REACTION;
    }

    public Collection<EntityGlyph> getParticipants(EntityRole... role) {
        Collection<EntityGlyph> rtn = new HashSet<>();
        for (EntityRole entityRole : role) rtn.addAll(participants.get(entityRole));
        return rtn;
    }

    private Map<PhysicalEntity, EntityGlyph> peGlyphMap = new HashMap<>();

    public void setInputs(Collection<EntityGlyph> inputs) {
        participants.computeIfAbsent(EntityRole.INPUT, a -> new HashSet<>()).addAll(inputs);
    }

    public Collection<EntityGlyph> getInputs() {
        return participants.get(EntityRole.INPUT);
    }

    public void setOutputs(Collection<EntityGlyph> outputs) {
        participants.computeIfAbsent(EntityRole.OUTPUT, a -> new HashSet<>()).addAll(outputs);
    }

    public Collection<EntityGlyph> getOutputs() {
        return participants.get(EntityRole.OUTPUT);
    }

    public void setCatalysts(Collection<EntityGlyph> catalysts) {
        participants.computeIfAbsent(EntityRole.CATALYST, a -> new HashSet<>()).addAll(catalysts);
    }

    public Collection<EntityGlyph> getCatalysts() {
        return participants.get(EntityRole.CATALYST);
    }

    public void setPositiveRegulators(Collection<EntityGlyph> positiveRegulators) {
        participants.computeIfAbsent(EntityRole.POSITIVE_REGULATOR, a -> new HashSet<>()).addAll(positiveRegulators);
    }

    public Collection<EntityGlyph> getPositiveRegulators() {
        return participants.get(EntityRole.POSITIVE_REGULATOR);
    }

    public void setNegativeRegulators(Collection<EntityGlyph> negativeRegulators) {
        participants.computeIfAbsent(EntityRole.NEGATIVE_REGULATOR, a -> new HashSet<>()).addAll(negativeRegulators);
    }

    public Collection<EntityGlyph> getNegativeRegulators() {
        return participants.get(EntityRole.NEGATIVE_REGULATOR);
    }

    @Override
    public String toString() {
        return "ReactionGlyph{stId='" + rle.getStId() + "'}";
    }
}
