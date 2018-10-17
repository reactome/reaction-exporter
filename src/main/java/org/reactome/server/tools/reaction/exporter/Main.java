
package org.reactome.server.tools.reaction.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.martiansoftware.jsap.*;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.reaction.exporter.config.ReactomeNeo4jConfig;
import org.reactome.server.tools.reaction.exporter.layout.LayoutFactory;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Main {

    public static void main(String[] args) throws JSAPException {

        // Program Arguments -h, -p, -u, -k
        SimpleJSAP jsap = new SimpleJSAP(Main.class.getName(), "Connect to Reactome Graph Database",
                new Parameter[]{
                        new FlaggedOption("host",     JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",     "The neo4j host"),
                        new FlaggedOption("port",     JSAP.STRING_PARSER, "7474",      JSAP.NOT_REQUIRED, 'p', "port",     "The neo4j port"),
                        new FlaggedOption("user",     JSAP.STRING_PARSER, "neo4j",     JSAP.NOT_REQUIRED, 'u', "user",     "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, "neo4j",     JSAP.REQUIRED,     'd', "password", "The neo4j password")
                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), ReactomeNeo4jConfig.class);

        // Access the data using our service layer.
        DatabaseObjectService dos = ReactomeGraphCore.getService(DatabaseObjectService.class);

//        ReactionLikeEvent rle = dos.findById("R-HSA-6791223");
        //ReactionLikeEvent rle = dos.findById("R-HSA-211734");
        ReactionLikeEvent rle = dos.findById("R-HSA-1362408");

        AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        LayoutFactory layoutFactory = new LayoutFactory(ads);
        Layout rxn = layoutFactory.getReactionLikeEventLayout(rle);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(rxn);
            System.out.println(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        System.out.println("Done");
    }
}