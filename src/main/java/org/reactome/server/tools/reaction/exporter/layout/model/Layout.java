package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.reactome.server.graph.domain.model.Compartment;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.tools.reaction.exporter.layout.common.Bounds;
import org.reactome.server.tools.reaction.exporter.ontology.GoTerm;
import org.reactome.server.tools.reaction.exporter.ontology.GoTreeFactory;
import org.reactome.server.tools.reaction.exporter.ontology.GoTreeFactory.Source;

import java.util.*;

/**
 * Main class aggregating the compartment and the reaction with all its participants
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Layout implements HasBounds {

    private Source goTreeSource = Source.REACTOME;

    private String pathway;

    private Bounds bounds;

    private ReactionGlyph reactionGlyph;

    private Collection<EntityGlyph> entities;

    private CompartmentGlyph compartmentRoot;

    private Map<String, CompartmentGlyph> compartments;


    public Layout() {
        this.bounds = new Bounds();
        this.compartments = new HashMap<>();
    }

  /**
   *
   * @param source
   *          defines source of Go tree hierarchy that should be used when layout
   *          is generated
   */
    public Layout(Source source) {
      this();
      this.goTreeSource=source;
    }

    public void add(EntityGlyph entityGlyph) {
        entities.add(entityGlyph);
    }

    public Collection<CompartmentGlyph> getCompartments() {
        return compartments.values();
    }

    @JsonIgnore
    public CompartmentGlyph getCompartmentRoot() {
        return compartmentRoot;
    }

    public Collection<EntityGlyph> getEntities() {
        return entities;
    }

    @JsonIgnore
    public String getPathway() {
        return pathway;
    }

    public ReactionGlyph getReaction() {
        return reactionGlyph;
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setReactionLikeEvent(ReactionLikeEvent rle) {
        reactionGlyph = new ReactionGlyph(rle);

        //noinspection LoopStatementThatDoesntLoop
        for (Compartment compartment : rle.getCompartment()) {
            String acc = compartment.getAccession();
            CompartmentGlyph cg = compartments.computeIfAbsent(acc, i -> new CompartmentGlyph(compartment));
            cg.addGlyph(reactionGlyph);
            reactionGlyph.setCompartment(cg);
            break; //We only want to assign the reaction to the first compartment in the list
        }

    }

    // This setter is called automatically by the graph-core marshaller
    @SuppressWarnings("unused")
    public void setParticipants(Collection<EntityGlyph> participants) {
        Map<String, EntityGlyph> entities = new HashMap<>();

        for (EntityGlyph participant : participants) {
            EntityGlyph g = entities.get(participant.getIdentifier());
            if (g != null) {
                //In this case participant ONLY has one role
                g.addRole(participant.getRoles().iterator().next());
            } else {
                entities.put(participant.getIdentifier(), participant);
            }
        }

        for (EntityGlyph participant : entities.values()) {
            //noinspection LoopStatementThatDoesntLoop
            for (Compartment compartment : participant.getCompartments()) {
                String acc = compartment.getAccession();
                CompartmentGlyph cg = compartments.computeIfAbsent(acc, i -> new CompartmentGlyph(compartment));
                cg.addGlyph(participant);
                participant.setCompartment(cg);
                break; //We only want to assign the participant to the first compartment in the list
            }
        }
        this.entities = new HashSet<>(entities.values());

        createCompartmentsStructure();
    }

    private void createCompartmentsStructure() {
        List<String> compartments = new ArrayList<>();
        for (CompartmentGlyph compartment : this.compartments.values()) {
            compartments.add("GO:" + compartment.getAccession());
        }

        GoTerm treeRoot = GoTreeFactory.getTreeWithIntermediateNodes(compartments, goTreeSource);
        compartmentRoot = this.compartments.computeIfAbsent(treeRoot.getAccession(), a -> new CompartmentGlyph(treeRoot));

        buildCompartmentHierarchy(compartmentRoot, treeRoot);
    }

    private void buildCompartmentHierarchy(CompartmentGlyph cg, GoTerm term) {
        for (GoTerm goTerm : term.getIncomingTerms()) {
            CompartmentGlyph aux = compartments.computeIfAbsent(goTerm.getAccession(), a -> new CompartmentGlyph(goTerm));
            cg.addChild(aux);
            aux.setParent(cg);
            buildCompartmentHierarchy(aux, goTerm);
        }
    }

    public void setPathway(String pathway) {
        this.pathway = pathway;
    }
}
