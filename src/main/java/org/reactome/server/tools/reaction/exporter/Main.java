
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

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Main {

    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(Main.class.getName(), "Generates an image from a single reaction in reaction. Supports png, jpg, jpeg, gif, svg and pdf.",
                new Parameter[]{
                        new QualifiedSwitch("target",   JSAP.STRING_PARSER, JSAP.NO_DEFAULT,         JSAP.NOT_REQUIRED, 't', "target",   "Target pathways to convert. Use either comma separated IDs, pathways for a given species (e.g. 'Homo sapiens') or 'all' for every pathway").setList(true).setListSeparator(','),
                        new FlaggedOption(  "output",   JSAP.STRING_PARSER, JSAP.NO_DEFAULT,    JSAP.REQUIRED,     'o', "output",   "The directory where the converted files are written to."),
                        new FlaggedOption(  "host",     JSAP.STRING_PARSER,"localhost",  JSAP.NOT_REQUIRED, 'h',  "host",    "The neo4j host"),
                        new FlaggedOption(  "port",     JSAP.STRING_PARSER,  "7474",       JSAP.NOT_REQUIRED, 'p',  "port",    "The neo4j port"),
                        new FlaggedOption(  "user",     JSAP.STRING_PARSER,  "neo4j",      JSAP.NOT_REQUIRED, 'u',  "user",    "The neo4j user"),
                        new FlaggedOption(  "password", JSAP.STRING_PARSER,  "neo4j",      JSAP.REQUIRED,     'd',  "password","The neo4j password")
                }
        );

        final JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), ReactomeNeo4jConfig.class);

        // Access the data using our service layer.
        final DatabaseObjectService dos = ReactomeGraphCore.getService(DatabaseObjectService.class);
        final String stId = config.getString("stId");

        ReactionLikeEvent rle = null;
        try {
            rle = dos.findById("R-HSA-5602549");
        } catch (ClassCastException e) { /* Nothing here */ }

        if (rle == null) {
            System.err.println("Error");
            System.exit(1);
        }

        final File output = new File(config.getString("output"));
        if (!output.exists()) {
            if (!output.mkdirs()) {
                System.err.println("Couldn't create path " + output);
                return;
            }
        }

//        algo(rle);
        algo(dos.findById("R-HSA-5483238"));
        algo(dos.findById("R-HSA-2453818"));
        algo(dos.findById("R-HSA-6802935"));
        algo(dos.findById("R-HSA-2220979"));
        algo(dos.findById("R-HSA-6802930"));
        algo(dos.findById("R-HSA-2206299"));
        algo(dos.findById("R-HSA-2220981"));
        algo(dos.findById("R-HSA-2265534"));
        algo(dos.findById("R-HSA-2220985"));
        algo(dos.findById("R-HSA-5607838"));
        algo(dos.findById("R-HSA-5601843"));
        algo(dos.findById("R-HSA-9035954"));
        algo(dos.findById("R-HSA-3247569"));
        algo(dos.findById("R-HSA-70634"));
        algo(dos.findById("R-HSA-5602549"));

    }

    private static void algo(ReactionLikeEvent rle){
        AdvancedDatabaseObjectService ados = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        LayoutFactory layoutFactory = new LayoutFactory(ados);
        ReactionGraphFactory reactionGraphFactory = new ReactionGraphFactory(ados);

        final Layout layout = layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.BOX);
        final Diagram diagram = ReactionDiagramFactory.get(layout);
        final Graph graph = reactionGraphFactory.getGraph(rle, layout);

        printJson(diagram);
        printJson(graph);
        System.out.println("Done");
    }

//    private static Layout getLayout(DatabaseObjectService dos, String stId) {
//        final ReactionLikeEvent rle = dos.findById(stId);
//        final AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
//        final LayoutFactory layoutFactory = new LayoutFactory(ads);
//        final Layout layout = layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.BREATHE);
//        printJson(layout);
//        final ReactionGraphFactory graphFactory = new ReactionGraphFactory(ads);
//        final Graph graph = graphFactory.getGraph(rle, layout);
//        printJson(graph);
//        return layout;
//    }

    private static void printJson(Object object) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            System.out.println(mapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}