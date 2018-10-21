package org.reactome.server.tools.reaction.exporter.ontology;

import java.util.*;

import static java.util.stream.Collectors.joining;

/**
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class GoTerm implements Comparable<GoTerm> {

    private String id;
    private String namespace;
    private String name;
    private boolean obsolete;
    private List<String> altIds = new LinkedList<>();
    private List<String> consider = new LinkedList<>();
    /* part_of, is_a, surrounded_by, component_of */
    private Map<Directionality, Map<RelationshipType, Set<GoTerm>>> relationships = new EnumMap<>(Directionality.class);

    public GoTerm(String id) {
        this.id = id;
    }

    public String getAccession(){
        return id.replaceAll("GO:","");
    }

    public String getId() {
        return id;
    }


    public Set<GoTerm> getRelationships(Directionality directionality, RelationshipType type) {
        return relationships
                .getOrDefault(directionality, Collections.emptyMap())
                .getOrDefault(type, Collections.emptySet());
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

    public Set<GoTerm> getChildren(){
        Set<GoTerm> children = new HashSet<>();
        children.addAll(getRelationships(Directionality.INCOMING, RelationshipType.is_a));
        children.addAll(getRelationships(Directionality.INCOMING, RelationshipType.surrounded_by));
        children.addAll(getRelationships(Directionality.INCOMING, RelationshipType.part_of));
        children.addAll(getRelationships(Directionality.INCOMING, RelationshipType.component_of));
        return children;
    }

    public void print() {
        print(0);
    }

    private void print(int level) {
        for (int i = 0; i < level; i++) System.out.print("\t");
        System.out.println(id + " " + name);
        getChildren().forEach(term -> term.print(level + 1));
    }

    enum Directionality {
        OUTGOING, INCOMING;

        public String symbol() {
            return this == INCOMING ? "<-" : "->";
        }
    }
}
