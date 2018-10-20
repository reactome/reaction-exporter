
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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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
        saveImage(stId, rxn, format, path);
        System.out.println("Done");
    }

    private static Layout getLayout(DatabaseObjectService dos, String stId) {
        final ReactionLikeEvent rle = dos.findById(stId);
        final AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        final LayoutFactory layoutFactory = new LayoutFactory(ads);
        final Layout rxn = layoutFactory.getReactionLikeEventLayout(rle);
        printJson(rxn);
        return rxn;
    }

    private static void printJson(Layout rxn) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            System.out.println(mapper.writeValueAsString(rxn));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private static void saveImage(String stId, Layout rxn, String format, File path) {
        final BufferedImage image = RENDERER.render(new RenderArgs().setQuality(10), rxn);
        final File file = new File(path, stId + "." + format);
        try {
            ImageIO.write(image, format, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Testing reactions
    // disease: R-HSA-9015379, R-HSA-1218824
    // NPE: R-HSA-1362408
    // easy: R-HSA-211734, R-HSA-5205661, R-HSA-5205681, R-HSA-8948146, R-HSA-6787403, R-HSA-68947
    // duplicates: R-HSA-6791223,
    // many inputs: R-HSA-72107, R-HSA-5617820
    // many outputs: R-HSA-6785722, R-HSA-69144 (dissociation)
    // many regulators: R-HSA-6791221
    // autocatalysis: R-HSA-6814559, R-HSA-112381, R-HSA-1362408
    // reaction misplaced: R-HSA-5205661
    // wrong: R-HSA-5205663
    // attachments: R-HSA-140664
    // uncertain

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

}