
package org.reactome.server.tools.reaction.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.martiansoftware.jsap.*;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.diagram.data.graph.Graph;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.reaction.exporter.config.ReactomeNeo4jConfig;
import org.reactome.server.tools.reaction.exporter.diagram.ReactionDiagramFactory;
import org.reactome.server.tools.reaction.exporter.graph.ReactionGraphFactory;
import org.reactome.server.tools.reaction.exporter.layout.LayoutFactory;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;

import java.io.File;
import java.util.Arrays;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Main {

    public static void main(String[] args) throws JSAPException {

        // Program Arguments -h, -p, -u, -k
        SimpleJSAP jsap = new SimpleJSAP(Main.class.getName(), "Generates an image from a single reaction in reaction. Supports png, jpg, jpeg, gif, svg and pdf.",
                new Parameter[]{
                        new FlaggedOption("host",     JSAP.STRING_PARSER,  "localhost",  JSAP.NOT_REQUIRED, 'h',  "host",      "The neo4j host"),
                        new FlaggedOption("port",     JSAP.STRING_PARSER,  "7474",       JSAP.NOT_REQUIRED, 'p',  "port",      "The neo4j port"),
                        new FlaggedOption("user",     JSAP.STRING_PARSER,  "neo4j",      JSAP.NOT_REQUIRED, 'u',  "user",      "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER,  "neo4j",      JSAP.REQUIRED,     'd',  "password",  "The neo4j password"),
                        new FlaggedOption("stId",     JSAP.STRING_PARSER,  null,         JSAP.REQUIRED,     's',  "stId",      "Reaction stable identifier"),
                        new FlaggedOption("path",     JSAP.STRING_PARSER,  null,         JSAP.REQUIRED,     'o',  "path",      "Output path. File will be named 'path'/'stId'.'format'"),
                        new FlaggedOption("format",   JSAP.STRING_PARSER,  "png",        JSAP.NOT_REQUIRED, 'f',  "format",    "Output format")
                }
        );

        final JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), ReactomeNeo4jConfig.class);

        // Access the data using our service layer.
        final DatabaseObjectService dos = ReactomeGraphCore.getService(DatabaseObjectService.class);

        final String stId = config.getString("stId");
        final Layout rxn = getLayout(dos, stId);

        final String format = config.getString("format").toLowerCase();
        if (!Arrays.asList("png", "jpg", "jpeg", "gif", "svg", "pdf").contains(format)) {
            System.err.println(format + " format not supported");
            return;
        }

        final File path = new File(config.getString("path"));
        if (!path.exists()) {
            if (!path.mkdirs()) {
                System.err.println("Couldn't create path " + path);
                return;
            }
        }
        Diagram diagram = ReactionDiagramFactory.get(rxn);
        printJson(diagram);
        System.out.println("Done");
    }

    private static Layout getLayout(DatabaseObjectService dos, String stId) {
        final ReactionLikeEvent rle = dos.findById(stId);
        final AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        final LayoutFactory layoutFactory = new LayoutFactory(ads);
        final Layout layout = layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.BREATHE);
        printJson(layout);
        final ReactionGraphFactory graphFactory = new ReactionGraphFactory(ads);
        final Graph graph = graphFactory.getGraph(rle, layout);
        printJson(graph);
        return layout;
    }

    private static void printJson(Object object) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            System.out.println(mapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}