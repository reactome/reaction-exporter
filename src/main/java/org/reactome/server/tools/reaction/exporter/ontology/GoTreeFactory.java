package org.reactome.server.tools.reaction.exporter.ontology;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.reactome.server.tools.reaction.exporter.ontology.GoTerm.Directionality.OUTGOING;
import static org.reactome.server.tools.reaction.exporter.ontology.RelationshipType.*;

/**
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class GoTreeFactory {

	private static final String ROOT_ID = "GO:0005576";
	private static Map<String, GoTerm> terms = GoParser.readGo().values().stream()
			.filter(term -> term.getNamespace().equals("cellular_component"))
			.collect(Collectors.toMap(GoTerm::getId, Function.identity()));

	private static final List<RelationshipType> RELS = Arrays.asList(surrounded_by, component_of, part_of, is_a);

	/**
	 * Creates a subtree from the components tree that contains the minimum number of terms such as all of the terms in
	 * <em>ids</em> are connected to the tree root.
	 * @param ids a list of GO accession (without GO: prefix)
	 * @return a copy of the components root, with a smaller copy of the tree containing <em>ids</em>
	 */
	public static GoTerm getTree(List<String> ids) {
		// Create a graph wit a copy of the nodes
		final Map<String, GoTerm> subTree = ids.stream()
				.peek(id -> {
					if (!terms.containsKey(id)) System.err.println(id);
				})
				.filter(id -> !id.equals(ROOT_ID))
				.filter(id -> terms.containsKey(id))
				.collect(toMap(Function.identity(), id -> terms.get(id).copy()));

		// extracellular region as root
		final GoTerm root = terms.get(ROOT_ID).copy();
		subTree.values().forEach(goTerm -> {
			// climb parents til reach one of the elements in tree
			// add myself as children
			// if not, add as surrounded by root
			if (!findParent(goTerm, terms.get(goTerm.getId()), subTree)) {
				goTerm.createRelationship(OUTGOING, surrounded_by, root);
			}
		});
		subTree.put(root.getId(), root);
		return root;
	}

	private static boolean findParent(GoTerm goTerm, GoTerm graphTerm, Map<String, GoTerm> tree) {
		// Is any of the elements on the map my parent?
		for (RelationshipType rel : RELS) {
			final Set<GoTerm> relationships = graphTerm.getRelationships(OUTGOING, rel);
			for (GoTerm term : relationships) {
				if (tree.containsKey(term.getId())) {
					goTerm.createRelationship(OUTGOING, rel, tree.get(term.getId()));
					return true;
				}
			}
		}
		// Has any of the ancestors of graphTerm a parent in the map?
		for (RelationshipType rel : RELS) {
			final Set<GoTerm> relationships = graphTerm.getRelationships(OUTGOING, rel);
			for (GoTerm term : relationships)
				if (findParent(goTerm, term, tree)) return true;
		}
		return false;
	}
}
