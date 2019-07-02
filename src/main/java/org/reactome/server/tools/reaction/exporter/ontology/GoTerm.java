package org.reactome.server.tools.reaction.exporter.ontology;

import org.reactome.server.graph.domain.model.GO_CellularComponent;

import java.util.*;

import static java.util.stream.Collectors.joining;

/**
 * Main in-memory representation of a GO term. It is the most complete and allows adding relationships.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class GoTerm implements Comparable<GoTerm> {

    private String id;
    private String namespace;
    private String name;
    private boolean obsolete;
    private List<String> altIds = new ArrayList<>();
    private List<String> consider = new ArrayList<>();
    /* part_of, is_a, surrounded_by, component_of */
    private Map<Directionality, Map<RelationshipType, Set<GoTerm>>> relationships = new EnumMap<>(Directionality.class);
    private Collection<GoTerm> parents = new ArrayList<>();
    private Collection<GoTerm> children = new HashSet<>();

    public GoTerm(String id) {
        this.id = id;
    }

    public GoTerm(GoTerm that) {
        this.id = that.id;
        this.namespace = that.namespace;
        this.name = that.name;
        this.obsolete = that.obsolete;
        this.altIds = new ArrayList<>(that.altIds);
        this.consider = new ArrayList<>(that.altIds);
    }

    public GoTerm(GO_CellularComponent compartment){
        this.id = compartment.getDatabaseName() + ":" + compartment.getAccession();
        this.namespace = "cellular_component";
        this.name = compartment.getDisplayName();
        this.obsolete = false;
        this.altIds = new ArrayList<>();
        this.consider = new ArrayList<>();
    }

    public String getAccession(){
        return id.replaceAll("GO:","");
    }

    public String getId() {
        return id;
    }


    public Set<GoTerm> getRelationships(Directionality directionality, RelationshipType type) {
        final Set<GoTerm> terms = new HashSet<>(relationships
                .getOrDefault(directionality, Collections.emptyMap())
                .getOrDefault(type, Collections.emptySet()));
        if (directionality == Directionality.OUTGOING) {
            for (final GoTerm parent : parents) {
                terms.addAll(parent.getRelationships(directionality, type));
            }
        } else {
            for (final GoTerm child : children) {
                terms.addAll(child.getRelationships(directionality, type));
            }
            int size;
            do {
                size = terms.size();
                for (final GoTerm term : new ArrayList<>(terms)) {
                    terms.addAll(term.getChildren());
                }
            } while (terms.size() > size);
        }
        return terms;
    }

    public void createRelationship(Directionality directionality, RelationshipType type, GoTerm goTerm) {
        relationships
                .computeIfAbsent(directionality, t -> new EnumMap<>(RelationshipType.class))
                .computeIfAbsent(type, n -> new TreeSet<>())
                .add(goTerm);
        goTerm.relationships
                .computeIfAbsent(opposite(directionality), t -> new EnumMap<>(RelationshipType.class))
                .computeIfAbsent(type, n -> new TreeSet<>())
                .add(this);
    }

    private Directionality opposite(Directionality type) {
        return type == Directionality.INCOMING
                ? Directionality.OUTGOING
                : Directionality.INCOMING;
    }

    @Override
    public int compareTo(GoTerm other) {
        return id.compareTo(other.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GoTerm)) return false;
        return id.equals(((GoTerm) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addAltId(String altId) {
        this.altIds.add(altId);
    }

    public List<String> getAltIds() {
        return altIds;
    }

    public void addConsider(String consider) {
        this.consider.add(consider);
    }

    public List<String> getConsider() {
        return consider;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }


    /**
     * Gets a copy if this term, with empty relationships
     *
     * @return
     */
    public GoTerm copy() {
        final GoTerm goTerm = new GoTerm(id);
        goTerm.setName(name);
        goTerm.setNamespace(namespace);
        goTerm.setObsolete(obsolete);
        return goTerm;
    }

    @Override
    public String toString() {
        final String rels = relationships.keySet().stream().map(directionality -> {
            String val = relationships.get(directionality).keySet().stream().map(type -> {
                final String terms = relationships.get(directionality).get(type).stream().map(GoTerm::getId).collect(joining(","));
                return type + "=" + terms;
            }).collect(joining(", "));
            return directionality.symbol() + "{" + val + "}";
        }).collect(joining(", "));
        return String.format("%s (%s) [%s]", id, name, rels);
    }

    /**
     * @return a collection of terms which have an outgoing relationship to this term.
     */
    public Collection<GoTerm> getIncomingTerms() {
        final Set<GoTerm> children = new HashSet<>();
        for (final RelationshipType type : RelationshipType.values()) {
            children.addAll(getRelationships(Directionality.INCOMING, type));
        }
        return children;
    }

    public Collection<GoTerm> getOutgoingTerms() {
        final Set<GoTerm> children = new HashSet<>();
        for (final RelationshipType type : RelationshipType.values()) {
            children.addAll(getRelationships(Directionality.OUTGOING, type));
        }
        return children;
    }

    public void removeRelationship(GoTerm term) {
        for (final Directionality directionality : Directionality.values()) {
            for (final RelationshipType type : RelationshipType.values()) {
                relationships
                        .getOrDefault(directionality, Collections.emptyMap())
                        .getOrDefault(type, Collections.emptySet())
                        .remove(term);
                term.relationships
                        .getOrDefault(directionality, Collections.emptyMap())
                        .getOrDefault(type, Collections.emptySet())
                        .remove(this);
            }
        }
    }

    public void addParent(GoTerm parent) {
        parents.add(parent);
        parent.children.add(this);
    }

    public Collection<GoTerm> getParents() {
        return parents;
    }

    public Collection<GoTerm> getChildren() {
        return children;
    }

    public enum Directionality {
        OUTGOING, INCOMING;

        public String symbol() {
            return this == INCOMING ? "<-" : "->";
        }
    }
}
