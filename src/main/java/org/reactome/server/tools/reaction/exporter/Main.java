
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
import org.reactome.server.tools.reaction.exporter.renderer.LayoutRenderer;
import org.reactome.server.tools.reaction.exporter.renderer.RenderArgs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class Main {

    private final static LayoutRenderer RENDERER = new LayoutRenderer();

    public static void main(String[] args) throws JSAPException {

        // Program Arguments -h, -p, -u, -k
        SimpleJSAP jsap = new SimpleJSAP(Main.class.getName(), "Connect to Reactome Graph Database",
                new Parameter[]{
                        new FlaggedOption("host",     JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",     "The neo4j host"),
                        new FlaggedOption("port",     JSAP.STRING_PARSER, "7474",      JSAP.NOT_REQUIRED, 'p', "port",     "The neo4j port"),
                        new FlaggedOption("user",     JSAP.STRING_PARSER, "neo4j",     JSAP.NOT_REQUIRED, 'u', "user",     "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, "neo4j",     JSAP.REQUIRED,     'd', "password", "The neo4j password"),
                        new FlaggedOption("stId",     JSAP.STRING_PARSER,  null,       JSAP.REQUIRED,     's', "stId",     "Reaction stable identifier")
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
//        ReactionLikeEvent rle = dos.findById("R-HSA-1362408");
//        ReactionLikeEvent rle = dos.findById("R-HSA-9015379");
        // R-HSA-72107,R-HSA-5205661,R-HSA-5205663,R-HSA-5205681,R-HSA-5617820,R-HSA-8948146,R-HSA-6814559,R-HSA-6787403,R-HSA-6791221,R-HSA-6785722,R-HSA-112381,R-HSA-6791223
//        ReactionLikeEvent rle = dos.findById("R-HSA-1218824");
        ReactionLikeEvent rle = dos.findById(config.getString("stId"));
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

        final BufferedImage image = RENDERER.render(new RenderArgs().setQuality(10), rxn);
        try {
            ImageIO.write(image, "png", new FileOutputStream("reaction.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done");
    }
}