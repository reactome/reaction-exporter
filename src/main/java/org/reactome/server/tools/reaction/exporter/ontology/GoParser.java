package org.reactome.server.tools.reaction.exporter.ontology;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.reactome.server.tools.reaction.exporter.ontology.model.Obo;
import org.reactome.server.tools.reaction.exporter.ontology.model.Relationship;
import org.reactome.server.tools.reaction.exporter.ontology.model.Term;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.reactome.server.tools.reaction.exporter.ontology.GoTerm.Directionality.OUTGOING;
import static org.reactome.server.tools.reaction.exporter.ontology.RelationshipType.*;


/**
 * Reads Gene Ontology and Cellular Component Ontology resource files and creates the hierarchy of GO terms.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class GoParser {

	private static final String GENE_ONTOLOGY = "/ontologies/go_daily-termdb.obo-xml";

	private GoParser() {}

	private static void addOcelotTerms(Map<String, GoTerm> nodes) {
		final OcelotParser.OcelotElement root = OcelotParser.readOcelot();
		final Map<String, String> index = root.getChildren().stream()
				.collect(toMap(child -> child.getChildren().get(0).getValue(), GoParser::extractGoId));
		root.getChildren().stream()
				.filter(elem1 -> elem1.getValue() == null)  // take elements with children
				.forEach(child -> parseTerm(nodes, index, child.getChildren().get(2)));
	}

	private static void parseTerm(Map<String, GoTerm> terms, Map<String, String> index, OcelotParser.OcelotElement sec) {
		final String goId = sec.getChildren().stream()
				.filter(elem -> elem.getChildren().get(0).getValue().equals("GOID"))
				.findFirst()
				.map(elem -> elem.getChildren().get(1).getValue().replace("\"", "").trim())
				.orElse(null);
		final List<String> surroundedBy = sec.getChildren().stream()
				.filter(elem -> elem.getChildren().get(0).getValue().equals("SURROUNDED-BY"))
				.findFirst()
				.map(elem -> elem.getChildren().stream().skip(1).map(OcelotParser.OcelotElement::getValue).collect(toList()))
				.orElse(Collections.emptyList());
		final List<String> componentOf = sec.getChildren().stream()
				.filter(elem -> elem.getChildren().get(0).getValue().equals("COMPONENT-OF"))
				.findFirst()
				.map(elem -> elem.getChildren().stream().skip(1).map(OcelotParser.OcelotElement::getValue).collect(toList()))
				.orElse(Collections.emptyList());
		if (goId != null) {
			final GoTerm goTerm = terms.get(goId);
			if (goTerm == null) {
//				System.err.println("Go term not found: " + goId);
				return;
			}
			for (String ccoId : surroundedBy) {
				final GoTerm target = terms.get(index.get(ccoId));
				if (target != null)
					goTerm.createRelationship(OUTGOING, surrounded_by, target);
			}
			for (String ccoId : componentOf) {
				final GoTerm target = terms.get(index.get(ccoId));
				if (target != null)
					goTerm.createRelationship(OUTGOING, component_of, target);
			}
		}
	}

	private static String extractGoId(OcelotParser.OcelotElement elem) {
		return elem.getChildren().get(2).getChildren().stream()
				.filter(elem1 -> elem1.getChildren().get(0).getValue().equals("GOID"))
				.findFirst()
				.map(elem1 -> elem1.getChildren().get(1).getValue().replace("\"", "").trim())
				.orElse("");
	}

	public static Map<String, GoTerm> getGoOntology() {
		try {
			final InputStream resource = GoParser.class.getResourceAsStream(GENE_ONTOLOGY);
			final Obo obo = new XmlMapper().readValue(resource, Obo.class);
			final Map<String, GoTerm> index = connect(obo);
			addOcelotTerms(index);
			return index;
		} catch (IOException e) {
			LoggerFactory.getLogger("reaction-exporter").error("Missing resource: " + GENE_ONTOLOGY);
			return Collections.emptyMap();
		}
	}

	private static Map<String, GoTerm> connect(Obo obo) {
		final Map<String, GoTerm> index = new HashMap<>();
		final List<Term> cellularComponents = obo.getTerms().stream()
				.filter(term -> term.getNamespace().equals("cellular_component"))
				.collect(toList());
		// First pass, just create the terms
		for (final Term term : cellularComponents) {
			final GoTerm goTerm = new GoTerm(term.getId());
			goTerm.setName(term.getName());
			goTerm.setObsolete(term.getObsolete());
			index.put(term.getId(), goTerm);
		}
		// Second pass, add the relationships
		for (final Term component : cellularComponents) {
			final GoTerm term = index.get(component.getId());
			for (final Relationship relationship : component.getRelationships()) {
				final GoTerm to = index.get(relationship.getTo());
				final RelationshipType type = valueOf(relationship.getType());
				term.createRelationship(OUTGOING, type, to);
			}
			for (final String isA : component.getIsA()) term.addParent(index.get(isA));
		}
		return index;
	}

}
