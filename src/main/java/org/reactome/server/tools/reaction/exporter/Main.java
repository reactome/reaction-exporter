
package org.reactome.server.tools.reaction.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.martiansoftware.jsap.*;
import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.util.DatabaseObjectUtils;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.diagram.data.graph.Graph;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.reaction.exporter.config.ReactomeNeo4jConfig;
import org.reactome.server.tools.reaction.exporter.diagram.ReactionDiagramFactory;
import org.reactome.server.tools.reaction.exporter.graph.ReactionGraphFactory;
import org.reactome.server.tools.reaction.exporter.layout.LayoutFactory;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.util.ProgressBar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Generates the layout and graph json files for the specified reactions
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class Main {

    private static NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(Main.class.getName(), "Generates an image from a single reaction in reaction. Supports png, jpg, jpeg, gif, svg and pdf.",
                new Parameter[]{
                        // QualifiedSwitch example -t:"R-HSA-70994"
                        new QualifiedSwitch("target",   JSAP.STRING_PARSER, JSAP.NO_DEFAULT,            JSAP.NOT_REQUIRED, 't', "target",   "Target rles to convert. Use either comma separated IDs, rles for a given species (e.g. 'Homo sapiens') or 'all' for every pathway").setList(true).setListSeparator(','),
                        new FlaggedOption(  "output",   JSAP.STRING_PARSER, JSAP.NO_DEFAULT,            JSAP.REQUIRED,     'o', "output",   "The directory where the converted files are written to."),
                        new FlaggedOption(  "host",     JSAP.STRING_PARSER,"bolt://localhost:7687", JSAP.NOT_REQUIRED, 'h',  "host",    "The neo4j host"),
                        new FlaggedOption(  "user",     JSAP.STRING_PARSER,  "neo4j",               JSAP.NOT_REQUIRED, 'u',  "user",    "The neo4j user"),
                        new FlaggedOption(  "password", JSAP.STRING_PARSER,  "neo4j",               JSAP.REQUIRED,     'd',  "password","The neo4j password")
                }
        );

        final JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("user"), config.getString("password"), ReactomeNeo4jConfig.class);

        final File output = new File(config.getString("output"));
        if (!output.exists()) {
            if (!output.mkdirs()) {
                System.err.println("Couldn't create path " + output);
                return;
            }
        }

        //Check if target rles are specified
        String[] target = config.getStringArray("target");
        AdvancedDatabaseObjectService ados = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        DatabaseObjectService dos = ReactomeGraphCore.getService(DatabaseObjectService.class);

        Collection<? extends ReactionLikeEvent> rles = getTargets(target);
        if (rles != null && !rles.isEmpty()) {
            long start = System.currentTimeMillis();
            int i = 0, tot = rles.size();
            System.out.printf("\r· Reaction exporter started:\n\t> Targeting %s reactions.\n%n", numberFormat.format(tot));
            for (ReactionLikeEvent rle : rles) {
                ProgressBar.updateProgressBar(rle.getStId(), i++, tot);
                generateJsonFiles(rle, ados, dos, output);
            }
            long time = System.currentTimeMillis() - start;
            ProgressBar.done(tot);
            System.out.printf("· Conversion finished: %s reactions have been successfully converted (%s)\n%n", numberFormat.format(tot), getTimeFormatted(time));
        } else {
            System.err.println("No targets found. Please check the parameters.");
        }

        System.exit(0);
    }

    private static void generateJsonFiles(Event rle, AdvancedDatabaseObjectService ados, DatabaseObjectService dos, File dir) {
        LayoutFactory layoutFactory = new LayoutFactory(ados, dos);
        ReactionGraphFactory reactionGraphFactory = new ReactionGraphFactory(ados);
        final Layout layout = layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.BOX);

        final Diagram diagram = ReactionDiagramFactory.get(layout);
        File rxnLayout = new File(dir.getPath() + "/" + rle.getStId() + ".json");
        File rxnLinkedLayout = new File(dir.getPath() + "/" + rle.getDbId() + ".json");
        saveJson(diagram, rxnLayout, rxnLinkedLayout);

        final Graph graph = reactionGraphFactory.getGraph(rle, layout);
        File rxnGraph = new File(dir.getPath() + "/" + rle.getStId() + ".graph.json");
        File rxnLinkedGraph = new File(dir.getPath() + "/" + rle.getDbId() + ".graph.json");
        saveJson(graph, rxnGraph, rxnLinkedGraph);
    }

    private static void saveJson(Object object, File file, File linkedFile) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mapper.writeValue(byteArrayOutputStream, object);

            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            byteArrayOutputStream.writeTo(fileOutputStream);

            //Create symbolicLink
            if (!Files.exists(Paths.get(linkedFile.getAbsolutePath()))) {
                Files.createSymbolicLink(Paths.get(linkedFile.getAbsolutePath()), Paths.get(file.getName()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Collection<? extends ReactionLikeEvent> getTargets(String[] target) {
        AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        String query;
        Map<String, Object> parametersMap = new HashMap<>();
        if (target.length > 1) {
            query = "MATCH (rle:ReactionLikeEvent) " +
                    "WHERE rle.dbId IN $dbIds OR rle.stId IN $stIds " +
                    "RETURN DISTINCT rle " +
                    "ORDER BY rle.dbId";
            List<Long> dbIds = new ArrayList<>();
            List<String> stIds = new ArrayList<>();
            for (String identifier : target) {
                String id = DatabaseObjectUtils.getIdentifier(identifier);
                if (DatabaseObjectUtils.isStId(id)) {
                    stIds.add(id);
                } else if (DatabaseObjectUtils.isDbId(id)) {
                    dbIds.add(Long.parseLong(id));
                }
            }
            parametersMap.put("dbIds", dbIds);
            parametersMap.put("stIds", stIds);
        } else {
            String aux = target[0];
            if (aux.equalsIgnoreCase("all")) {
                query = "MATCH (rle:ReactionLikeEvent) " +
                        "RETURN DISTINCT rle " +
                        "ORDER BY rle.dbId";
            } else if (DatabaseObjectUtils.isStId(aux)) {
                query = "MATCH (rle:ReactionLikeEvent{stId:$stId}) RETURN DISTINCT rle";
                parametersMap.put("stId", DatabaseObjectUtils.getIdentifier(aux));
            } else if (DatabaseObjectUtils.isDbId(aux)) {
                query = "MATCH (rle:ReactionLikeEvent{dbId:$dbId}) RETURN DISTINCT rle";
                parametersMap.put("dbId", DatabaseObjectUtils.getIdentifier(aux));
            } else {
                query = "MATCH (rle:ReactionLikeEvent{speciesName:$speciesName}) " +
                        "RETURN DISTINCT rle " +
                        "ORDER BY rle.dbId";
                parametersMap.put("speciesName", aux);
            }
        }

        System.out.print("· Retrieving target reactions...");
        Collection<ReactionLikeEvent> rles = null;
        try {
            rles = ads.getCustomQueryResults(ReactionLikeEvent.class, query, parametersMap);
        } catch (CustomQueryException e) {
            e.printStackTrace();
        }
        return rles;
    }

    private static String getTimeFormatted(Long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }
}