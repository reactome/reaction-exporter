package org.reactome.server.tools.reaction.exporter.ontology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.reactome.server.tools.reaction.exporter.ontology.GoTerm.Directionality.OUTGOING;
import static org.reactome.server.tools.reaction.exporter.ontology.RelationshipType.*;


/**
 * Reads go-basic.obo and cco.ocelot files and creates the hierarchy of GoTerms,
 * using
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
class GoParser {

	private final static Pattern ID_PATTERN = Pattern.compile("id:\\s+(GO:\\d+).*");
	private final static Pattern IS_A = Pattern.compile("is_a:\\s+(GO:\\d+).*");
	private final static Pattern REL2 = Pattern.compile("relationship:\\s*(\\S+)\\s+(GO:\\d+).*");
	// node -> IN|OUT -> type -> ids
	private static final Map<String, Map<GoTerm.Directionality, Map<RelationshipType, Set<String>>>> relationships = new TreeMap<>();

	private GoParser() {}

	static Map<String, GoTerm> readGo() {
		final InputStream resource = GoParser.class.getResourceAsStream("go-basic.obo");
		final Map<String, GoTerm> nodes = new TreeMap<>();
		final AtomicReference<GoTerm> current = new AtomicReference<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
			reader.lines().forEach(line -> {
				if (line.startsWith("id:")) {
					final Matcher matcher = ID_PATTERN.matcher(line);
					if (matcher.matches()) {
						final GoTerm goTerm = new GoTerm(matcher.group(1));
						nodes.put(goTerm.getId(), goTerm);
						current.set(goTerm);
					}
				} else if (line.startsWith("namespace:"))
					current.get().setNamespace(line.split(": ")[1].trim());
				else if (line.startsWith("name:"))
					current.get().setName(line.split(": ")[1].trim());
				else if (line.startsWith("is_obsolete:"))
					current.get().setObsolete(true);
				else if (line.startsWith("alt_id:"))
					current.get().addAltId(line.split(": ")[1].trim());
				else if (line.startsWith("consider:"))
					current.get().addConsider(line.split(": ")[1].trim());
				else if (line.startsWith("is_a: ")) {
					final Matcher matcher = IS_A.matcher(line);
					if (matcher.matches())
						relationships.computeIfAbsent(current.get().getId(), s -> new EnumMap<>(GoTerm.Directionality.class))
								.computeIfAbsent(OUTGOING, d -> new TreeMap<>())
								.computeIfAbsent(is_a, t -> new TreeSet<>())
								.add(matcher.group(1));
				} else if (line.startsWith("relationship: part_of ")) {
					Matcher matcher = REL2.matcher(line);
					if (matcher.matches()) {
						final String id = matcher.group(2);
						relationships.computeIfAbsent(current.get().getId(), s -> new EnumMap<>(GoTerm.Directionality.class))
								.computeIfAbsent(OUTGOING, d -> new TreeMap<>())
								.computeIfAbsent(part_of, t -> new TreeSet<>())
								.add(id);
					}
				}
			});
		} catch (IOException e) {
			// Should never happen, it's a resource
			e.printStackTrace();
		}
		// Lazy assignment of nodes in relationships
		relationships.forEach((termId, map) -> map.forEach((directionality, map1) -> map1.forEach((type, ids) -> ids.forEach(id -> {
			nodes.get(termId).createRelationship(directionality, type, nodes.get(id));
		}))));
		addOcelotTerms(nodes);

		return nodes.values().stream()
				.filter(term -> term.getNamespace().equals("cellular_component"))
				.collect(Collectors.toMap(GoTerm::getId, Function.identity()));
	}

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

}
