package org.reactome.server.tools.reaction.exporter.ontology;

import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag.*;
import static org.reactome.server.tools.reaction.exporter.ontology.GoTerm.Directionality.OUTGOING;
import static org.reactome.server.tools.reaction.exporter.ontology.RelationshipType.component_of;
import static org.reactome.server.tools.reaction.exporter.ontology.RelationshipType.surrounded_by;


/**
 * Reads Gene Ontology and Cellular Component Ontology resource files and creates the hierarchy of GO terms.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class GoParser {
    private static final String GENE_ONTOLOGY = "/ontologies/go-basic.obo";

    private GoParser() {
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

    public static Map<String, GoTerm> getGoOntology() {
        try {
            final URL resource = GoParser.class.getResource(GENE_ONTOLOGY);
            OBODoc obo = new OBOFormatParser().parse(Objects.requireNonNull(resource));
            final Map<String, GoTerm> index = connect(obo);
            addOcelotTerms(index);
            return index;
        } catch (IOException | NullPointerException e) {
            LoggerFactory.getLogger("reaction-exporter").error("Missing resource: " + GENE_ONTOLOGY);
            return Collections.emptyMap();
        }
    }

    private static Map<String, GoTerm> connect(OBODoc obo) {
        final Map<String, GoTerm> index = new HashMap<>();
        final List<Frame> cellularComponents = obo.getTermFrames().stream()
                .filter(term -> {
                    String namespace = term.getTagValue(TAG_NAMESPACE, String.class);
                    return namespace != null && namespace.equals("cellular_component");
                })
                .collect(toList());


        // First pass, just create the terms
        for (final Frame term : cellularComponents) {
            final GoTerm goTerm = new GoTerm(term.getId());
            goTerm.setName(term.getTagValue(TAG_NAME, String.class));
            goTerm.setObsolete(term.getTagValue(TAG_IS_OBSELETE, Boolean.class) == Boolean.TRUE);
            index.put(term.getId(), goTerm);
        }
//		// Second pass, add the relationships
        for (final Frame component : cellularComponents) {
            final GoTerm term = index.get(component.getId());

            for (Clause relationship : component.getClauses(TAG_RELATIONSHIP)) {
                final RelationshipType type = RelationshipType.valueOf(relationship.getValue(String.class));
                final GoTerm to = index.get(relationship.getValue2(String.class));
                term.createRelationship(OUTGOING, type, to);
            }
            for (String isA : component.getTagValues(TAG_IS_A, String.class)) {
                term.addParent(index.get(isA));
            }

        }
        return index;
    }

}
