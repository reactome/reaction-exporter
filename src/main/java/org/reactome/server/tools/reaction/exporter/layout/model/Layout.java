package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.reactome.server.graph.domain.model.Compartment;
import org.reactome.server.tools.reaction.exporter.goontology.GoTerm;
import org.reactome.server.tools.reaction.exporter.goontology.GoTreeFactory;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;

import java.util.*;

/**
 * Main class aggregating the compartment and the reaction with all its participants
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Layout implements HasPosition {

    private Position position = new Position();

    private ReactionGlyph reactionGlyph;

    private CompartmentGlyph compartmentRoot;

    private Map<String, CompartmentGlyph> compartments = new HashMap<>();

    public Layout(ReactionGlyph reactionGlyph) {
        this.reactionGlyph = reactionGlyph;

        //noinspection LoopStatementThatDoesntLoop
        for (Compartment compartment : reactionGlyph.getReactionLikeEvent().getCompartment()) {
            String acc = compartment.getAccession();
            CompartmentGlyph cg = compartments.computeIfAbsent(acc, i -> new CompartmentGlyph(compartment));
            cg.addGlyph(reactionGlyph);
            break; //We only want to assign the reaction to the first compartment in the list
        }

        for (EntityGlyph participant : reactionGlyph.getParticipants(EntityRole.values())) {
            //noinspection LoopStatementThatDoesntLoop
            for (Compartment compartment : participant.getCompartments()) {
                String acc = compartment.getAccession();
                CompartmentGlyph cg = compartments.computeIfAbsent(acc, i -> new CompartmentGlyph(compartment));
                cg.addGlyph(participant);
                break; //We only want to assign the participant to the first compartment in the list
            }
        }

        List<String> compartments = new ArrayList<>();
        for (CompartmentGlyph compartment : this.compartments.values()) {
            compartments.add("GO:" + compartment.getAccession());
        }

        GoTerm treeRoot = GoTreeFactory.getTree(compartments);
        compartmentRoot = this.compartments.computeIfAbsent(treeRoot.getAccession(), a -> new CompartmentGlyph(treeRoot));

        buildCompartmentHierarchy(compartmentRoot, treeRoot);
    }


    private void buildCompartmentHierarchy(CompartmentGlyph cg, GoTerm term) {
        for (GoTerm goTerm : term.getChildren()) {
            CompartmentGlyph aux = compartments.computeIfAbsent(goTerm.getAccession(), a -> new CompartmentGlyph(goTerm));
            cg.addChild(aux);
            aux.setParent(cg);
            buildCompartmentHierarchy(aux, goTerm);
        }
    }

    public Collection<CompartmentGlyph> getCompartments() {
        return compartments.values();
    }

    @JsonIgnore
    public CompartmentGlyph getCompartmentRoot() {
        return compartmentRoot;
    }

    public ReactionGlyph getReaction() {
        return reactionGlyph;
    }

    @Override
    public Position getPosition() {
        return position;
    }

}
