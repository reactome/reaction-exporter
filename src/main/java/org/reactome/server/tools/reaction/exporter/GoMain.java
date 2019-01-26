package org.reactome.server.tools.reaction.exporter;

import com.martiansoftware.jsap.*;
import org.reactome.server.tools.reaction.exporter.ontology.GoParser;
import org.reactome.server.tools.reaction.exporter.ontology.GoTerm;
import org.reactome.server.tools.reaction.exporter.ontology.RelationshipType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GoMain {

    public static void main(String[] args) throws JSAPException {

        final SimpleJSAP jsap = new SimpleJSAP(GoMain.class.getName(), "Exports GO hierarchy into a csv file",
                new Parameter[]{
                        new FlaggedOption("output", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'o', "path", "Output path")
                });
        final JSAPResult result = jsap.parse(args);
        final File output = new File(result.getString("output"));
        if (!output.exists() && !output.mkdirs()) {
            throw new IllegalArgumentException(output + " cannot be created");
        }
        final File file = new File(output, "go_hierarchy.tsv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            final Map<String, GoTerm> termMap = GoParser.getGoOntology();
            final List<GoTerm> terms = termMap.keySet().stream().sorted()
                    .map(termMap::get)
                    .collect(Collectors.toList());
            for (final GoTerm term : terms) {
                export(term, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void export(GoTerm term, BufferedWriter writer) throws IOException {
        for (final RelationshipType type : RelationshipType.values()) {
            for (final GoTerm other : term.getRelationships(GoTerm.Directionality.OUTGOING, type)) {
                writer.write(getLine(term, type, other));
                writer.newLine();
            }
        }
    }

    private static String getLine(GoTerm from, RelationshipType type, GoTerm to) {
        return String.join("\t", "GO:" + from.getAccession(), from.getName(), type.name(), "GO:" + to.getAccession(), to.getName());
    }
}
