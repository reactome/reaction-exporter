package org.reactome.server.tools.reaction.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.tools.diagram.data.graph.Graph;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.diagram.exporter.common.Decorator;
import org.reactome.server.tools.diagram.exporter.common.analysis.AnalysisException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramJsonDeserializationException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramProfileException;
import org.reactome.server.tools.diagram.exporter.pptx.PowerPointExporter;
import org.reactome.server.tools.diagram.exporter.raster.RasterExporter;
import org.reactome.server.tools.diagram.exporter.raster.api.RasterArgs;
import org.reactome.server.tools.diagram.exporter.sbgn.SbgnConverter;
import org.reactome.server.tools.reaction.exporter.diagram.ReactionDiagramFactory;
import org.reactome.server.tools.reaction.exporter.graph.ReactionGraphFactory;
import org.reactome.server.tools.reaction.exporter.layout.DiagramTest;
import org.reactome.server.tools.reaction.exporter.layout.LayoutFactory;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Sbgn;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Unit testing.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppTest extends BaseTest {

    private static final File TEST_IMAGES = new File("test-images");

    @Autowired
    private DatabaseObjectService databaseObjectService;

    @Autowired
    private AdvancedDatabaseObjectService ads;

    private RasterExporter rasterExporter = new RasterExporter();

    @BeforeClass
    public static void setUpClass() {
        logger.info(" --- !!! Running " + AppTest.class.getName() + "!!! --- \n");
        if (!TEST_IMAGES.exists() && !TEST_IMAGES.mkdirs())
            logger.error("Couldn't create test folder " + TEST_IMAGES);
    }

    @AfterClass
    public static void afterClass() {
       try {
           FileUtils.cleanDirectory(TEST_IMAGES);
       } catch (IOException e) {
           logger.error("Could't delete test folder " + TEST_IMAGES);
       }
    }

    @Test
    public void findByDbIdTest() throws CustomQueryException {

        final List<String> identifiers = Arrays.asList(
                "R-HSA-69144",
                "R-HSA-68947",
                "R-HSA-72107",
                "R-HSA-112381",
                "R-HSA-140664",
                "R-HSA-211734",
                "R-HSA-420586",
                "R-HSA-425483",
                "R-HSA-1218824",
                "R-HSA-1247999",
                "R-HSA-1362408",
                "R-HSA-1592238",
                "R-HSA-2993780",
                "R-HSA-5205661",
                "R-HSA-5205663",
                "R-HSA-5205681",
                "R-HSA-5228811",
                "R-HSA-5578663",
                "R-HSA-5602606",
                "R-HSA-5617820",
                "R-HSA-5638137",
                "R-HSA-6785722",
                "R-HSA-6787403",
                "R-HSA-6791221",
                "R-HSA-6791223",
                "R-HSA-6814559",
                "R-HSA-8948146",
                "R-HSA-8948832",
                "R-HSA-9015379"
        );
//        final AnalysisStoredResult result = new TokenUtils("/home/plorente/resources/reactome/v66/analysis").getFromToken("MjAxODEwMDQxMDA3MDhfMw%253D%253D");
        final long start = System.nanoTime();
        for (String stId : identifiers) {

            ReactionLikeEvent rle = databaseObjectService.findById(stId);
            final String pStId = rle.getEventOf().get(0).getStId();
            final String format = "png";


            final LayoutFactory layoutFactory = new LayoutFactory(ads);
            final Layout layout = layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.COMPACT);
            final Diagram diagram = ReactionDiagramFactory.get(layout);

            final ReactionGraphFactory graphFactory = new ReactionGraphFactory(ads);
            final Graph graph = graphFactory.getGraph(rle, layout);


            // runTest(diagram);
            // printJsons(diagram, graph);
            savePng(stId, pStId, format, diagram, graph);
            // saveSbgn(stId, diagram);
            // savePptx(diagram);

        }
        final long elapsed = System.nanoTime() - start;
        System.out.println(elapsed / 1e9);
        System.out.println(elapsed / 1e9 / identifiers.size());
        // TODO: 21/10/18 add resource
    }

    private void runTest(Diagram diagram) {
        new DiagramTest(diagram).printResults(DiagramTest.Level.WARNING);
        System.out.println();
    }

    private void savePptx(Diagram diagram) {
        try {
            PowerPointExporter.export(diagram, "modern", TEST_IMAGES.getAbsolutePath(), new Decorator(), null);
        } catch (DiagramJsonDeserializationException | DiagramProfileException e) {
            e.printStackTrace();
        }
    }

    private void saveSbgn(String stId, Diagram diagram) {
        try {
            final File sbgn = new File(TEST_IMAGES, String.format("%s.%s", stId, "sbgn"));
            final Sbgn result = new SbgnConverter(diagram).getSbgn();
            SbgnUtil.writeToFile(result, sbgn);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private void savePng(String stId, String pStId, String format, Diagram diagram, Graph graph) {
        try {
            final File file = new File(TEST_IMAGES, String.format("%s.%s", stId, format));
            OutputStream os = new FileOutputStream(file);
            RasterArgs args = new RasterArgs(pStId, format).setQuality(8).setMargin(1);
            rasterExporter.export(diagram, graph, args, null, os);
        } catch (IOException | AnalysisException | TranscoderException e) {
            e.printStackTrace();
        }
    }

    private void printJsons(Diagram diagram, Graph graph) {
        try {
            System.out.println(new ObjectMapper().writeValueAsString(diagram));
            System.out.println(new ObjectMapper().writeValueAsString(graph));
        } catch (JsonProcessingException e) {
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
