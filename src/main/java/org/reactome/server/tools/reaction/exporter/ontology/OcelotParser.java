package org.reactome.server.tools.reaction.exporter.ontology;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Consumes a .ocelot file.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
class OcelotParser {

	private static final String CCO_OCELOT = "/ontologies/cco.ocelot";

	static OcelotElement readOcelot() {
		final InputStream resource = OcelotParser.class.getResourceAsStream(CCO_OCELOT);
		final String text = new BufferedReader(new InputStreamReader(resource)).lines().collect(Collectors.joining("\n")).replaceAll("\\s+", " ");
		return parse(text);
	}

	private static OcelotElement parse(String text) {
		text = text.trim();
		if (!text.startsWith("("))
			throw new IllegalArgumentException("OcelotElement should start with (");
		if (!text.endsWith(")"))
			throw new IllegalArgumentException("OcelotElement should end with )");
		text = text.substring(1, text.length() - 1).trim();
		OcelotElement elem = new OcelotElement();
		boolean quoteContext = false;
		int start = 0;
		int openBrackets = 0;
		boolean openSpaces = true;
		for (int i = 0; i < text.length(); i++)
			if (text.charAt(i) == '"') {
				quoteContext = !quoteContext;
			} else if (!quoteContext && text.charAt(i) == '(') {
				if (openBrackets == 0) start = i;
				openSpaces = false;
				openBrackets++;
			} else if (!quoteContext && text.charAt(i) == ')') {
				openBrackets--;
				if (openBrackets == 0) {
					elem.getChildren().add(parse(text.substring(start, i + 1)));
					start = i + 1;
				}
			} else if (!quoteContext && openBrackets == 0 && text.charAt(i) == ' ') {
				if (openSpaces) {
					elem.getChildren().add(new OcelotElement(text.substring(start, i)));
					start = i + 1;
				}
				openSpaces = true;
			}
		if (openSpaces)
			elem.getChildren().add(new OcelotElement(text.substring(start)));
		return elem;
	}

	static class OcelotElement {
		private String value;
		private List<OcelotElement> children = new LinkedList<>();

		OcelotElement() {}

		OcelotElement(String value) {
			this.value = value;
		}

		List<OcelotElement> getChildren() {
			return children;
		}

		String getValue() {
			return value;
		}
	}
}
