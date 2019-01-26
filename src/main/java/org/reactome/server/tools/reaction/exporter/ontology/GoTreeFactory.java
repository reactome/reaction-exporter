package org.reactome.server.tools.reaction.exporter.ontology;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.reactome.server.tools.reaction.exporter.ontology.GoTerm.Directionality.OUTGOING;
import static org.reactome.server.tools.reaction.exporter.ontology.RelationshipType.surrounded_by;

/**
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class GoTreeFactory {

    private static final String EXTRACELLULAR_REGION_ID = "GO:0005576";
    private static final String CELLULAR_COMPONENT_ID = "GO:0005575";

    private static Map<String, GoTerm> masterTree = GoParser.getGoOntology().values().stream()
            .collect(Collectors.toMap(GoTerm::getId, Function.identity()));

    static {
        // NOTE: Reactome diagrams show the cell and any other compartment surrounded by the extracellular region.
        // This is not represented in Gene Ontology. To bypass this behaviour we create the relationship:
        //                 (cellular component)-[surrounded_by]->(extracellular_region)
        final GoTerm cellularComponent = masterTree.get(CELLULAR_COMPONENT_ID);
        final GoTerm extracellularRegion = masterTree.get(EXTRACELLULAR_REGION_ID);
        cellularComponent.createRelationship(OUTGOING, surrounded_by, extracellularRegion);

        // To avoid cycles (extracellular region) is not a (cellular component) anymore
        extracellularRegion.getParents().remove(cellularComponent);
    }

    private GoTreeFactory() {}


    /**
     * Creates a subtree from the GO tree that contains the terms in <em>ids</em> are connected and those in-between
     * them. This method uses a precomputed GO hierarchy.
     *
     * @return a copy of the components root, with a smaller copy of the tree containing <em>ids</em>
     */
    public static GoTerm getTreeWithIntermediateNodes(List<String> goIds) {
        return getTreeWithIntermediateNodes(GoTreeFactory.masterTree, goIds);
    }

    /**
     * Creates a subtree from the GO tree that contains the terms in <em>ids</em> are connected and those in-between
     * them.
     *
     * @param masterTree the tree where to extract the relationships
     * @param ids a list of GO accession (without GO: prefix)
     * @return a copy of the components root, with a smaller copy of the tree containing <em>ids</em>
     */
    public static GoTerm getTreeWithIntermediateNodes(Map<String, GoTerm> masterTree, Collection<String> ids) {
        final Map<String, GoTerm> tree = new HashMap<>();
        final List<GoTerm> terms = ids.stream().map(id -> new GoTerm(masterTree.get(id))).collect(Collectors.toList());
        for (final GoTerm term : terms) {
            tree.put(term.getId(), term);
        }
        for (final GoTerm term : terms)
            addToHierarchy(masterTree, tree, term, terms);
        // Remove upper bound compartments
        GoTerm root = tree.values().stream()
                .filter(goTerm -> goTerm.getOutgoingTerms().isEmpty())
                .findFirst()
                .orElse(null);
        return findRoot(root, terms);
    }

    private static GoTerm findRoot(GoTerm root, List<GoTerm> terms) {
        if (terms.contains(root)) return root;
        final Collection<GoTerm> incomingTerms = root.getIncomingTerms();
        if (incomingTerms.size() > 1) return root;
        if (incomingTerms.isEmpty()) return root;
        return findRoot(incomingTerms.iterator().next(), terms);
    }

    private static void addToHierarchy(Map<String, GoTerm> masterTree, Map<String, GoTerm> tree, GoTerm term, List<GoTerm> terms) {
        Collection<List<GoTerm>> branches = getBranches(term, masterTree);
        final List<GoTerm> best = branches.stream()
                .min(Comparator
                        .comparingLong((List<GoTerm> branch) -> branch.stream().filter(terms::contains).count()).reversed()
                        .thenComparingInt(List::size))
                .orElse(null);

        if (best != null) {
            GoTerm aux = term;
            for (final GoTerm masterParent : best) {
                final GoTerm parent = tree.computeIfAbsent(masterParent.getId(), id -> new GoTerm(masterParent));
                aux.createRelationship(OUTGOING, surrounded_by, parent);
                aux = parent;
            }
        }
    }

    private static Collection<List<GoTerm>> getBranches(GoTerm term, Map<String, GoTerm> masterTree) {
        final Collection<List<GoTerm>> rtn = new ArrayList<>();
        final GoTerm masterTerm = masterTree.get(term.getId());
        for (final GoTerm parent : masterTerm.getOutgoingTerms()) {
            final Collection<List<GoTerm>> branches = getBranches(parent, masterTree);
            for (final List<GoTerm> branch : branches) {
                branch.add(0, parent);
                rtn.add(branch);
            }
            if (branches.isEmpty()) {
                final List<GoTerm> base = new ArrayList<>();
                base.add(parent);
                rtn.add(base);
            }
        }
        return rtn;
    }


}
