
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
        SimpleJSAP jsap = new SimpleJSAP(Main.class.getName(), "Generates an image from a single reaction in reaction. Supports png, jpg, jpeg, gif, svg and pdf.",
                new Parameter[]{
                        new FlaggedOption("host",     JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",     "The neo4j host"),
                        new FlaggedOption("port",     JSAP.STRING_PARSER, "7474",      JSAP.NOT_REQUIRED, 'p', "port",     "The neo4j port"),
                        new FlaggedOption("user",     JSAP.STRING_PARSER, "neo4j",     JSAP.NOT_REQUIRED, 'u', "user",     "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, "neo4j",     JSAP.REQUIRED,     'd', "password", "The neo4j password"),
                        new FlaggedOption("stId",     JSAP.STRING_PARSER,  null,       JSAP.REQUIRED,     's', "stId",     "Reaction stable identifier"),
                        new FlaggedOption("output",   JSAP.STRING_PARSER,  null,       JSAP.REQUIRED,     'o', "output",   "Output file. Format will be detected by extension")
                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), ReactomeNeo4jConfig.class);

        // Access the data using our service layer.
        DatabaseObjectService dos = ReactomeGraphCore.getService(DatabaseObjectService.class);

        // disease: R-HSA-9015379, R-HSA-1218824
        // NPE: R-HSA-1362408
        // easy: R-HSA-211734, R-HSA-5205661, R-HSA-5205681, R-HSA-8948146, R-HSA-6787403, R-HSA-68947
        // duplicates: R-HSA-6791223,
        // many inputs: R-HSA-72107, R-HSA-5617820
        // many outputs: R-HSA-6785722, R-HSA-69144 (dissociation)
        // many regulators: R-HSA-6791221
        // autocatalysis: R-HSA-6814559, R-HSA-112381, R-HSA-1362409
        // reaction misplaced: R-HSA-5205661
        // wrong: R-HSA-5205663
        // attachments: R-HSA-140664

        // Extract elements with more than one role
        // MATCH (rle:ReactionLikeEvent{speciesName:"Homo sapiens"})
        // MATCH (rle)-[:input]->(i:PhysicalEntity)
        // WITH rle, collect(i) AS inputs
        // MATCH (rle)-[:output]->(o:PhysicalEntity)
        // WITH rle, inputs, collect(o) AS outputs
        // OPTIONAL MATCH (rle)-[:catalystActivity|physicalEntity*]->(c:PhysicalEntity)
        // WITH rle, inputs, outputs, collect(c) AS catalysts
        // OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(p:PhysicalEntity)
        // WITH rle, inputs, outputs, catalysts, collect(p) AS positives
        // OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(n:PhysicalEntity)
        // WITH rle, inputs, outputs, catalysts, positives, collect(n) AS negatives
        // WITH rle, inputs, outputs, catalysts, positives, negatives, [x IN inputs WHERE x IN catalysts AND x in outputs | x.stId] AS both
        // WHERE size(both) > 0
        // OPTIONAL MATCH (a)-[:created]->(rle)
        // RETURN rle.stId, rle.displayName, both, a.displayName

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
            ImageIO.write(image, "png", new FileOutputStream(config.getString("output")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done");
    }
}