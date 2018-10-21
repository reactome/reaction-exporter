package org.reactome.server.tools.reaction.exporter.ontology;

/**
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
enum RelationshipType {
	ancestor, equals, is_a, part_of, regulates, positively_regulates,
	negatively_regulates, replaced_by, consider, has_part, occurs_in,
	used_in, capable_of, capable_of_part_of, surrounded_by, component_of
}
