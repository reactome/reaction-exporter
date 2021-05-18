package org.reactome.server.tools.reaction.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Unit testing.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AppTest extends BaseTest {

    private static final File TEST_IMAGES = new File("test-images");

    @Autowired
    private DatabaseObjectService databaseObjectService;

    @Autowired
    private AdvancedDatabaseObjectService ads;

    private RasterExporter rasterExporter = new RasterExporter();
    private static int total;

    @BeforeAll
    public static void setUpClass() {
        logger.info("Running " + AppTest.class.getName());
        if (!TEST_IMAGES.exists() && !TEST_IMAGES.mkdirs())
            logger.error("Couldn't create test folder " + TEST_IMAGES);
    }

    @AfterAll
    public static void afterClass() {
        if (total > 0) {
            logger.error(String.format("Found %d errors", total));
        }
        try {
            FileUtils.cleanDirectory(TEST_IMAGES);
        } catch (IOException e) {
            logger.error("Could't delete test folder " + TEST_IMAGES);
        }
    }

    @Test
    public void testReactionConverter() {
        Collection<String> identifiers = new LinkedHashSet<>(Arrays.asList(
                "R-HSA-68947", "R-HSA-69144", "R-HSA-70634", "R-HSA-71670", "R-HSA-72107", "R-HSA-74885",
                "R-HSA-75097", "R-HSA-75849",
                "R-HSA-111883", "R-HSA-111930", "R-HSA-112381", "R-HSA-139952", "R-HSA-158419", "R-HSA-194083",
                "R-HSA-194121", "R-HSA-194130", "R-HSA-194153", "R-HSA-194187", "R-HSA-198440", "R-HSA-198513",
                "R-HSA-200424", "R-HSA-203613", "R-HSA-203946", "R-HSA-203977", "R-HSA-265783", "R-HSA-350769",
                "R-HSA-352174", "R-HSA-352182", "R-HSA-352347", "R-HSA-352354", "R-HSA-375342", "R-HSA-376149",
                "R-HSA-376851", "R-HSA-378513", "R-HSA-379415", "R-HSA-379426", "R-HSA-379432", "R-HSA-380869",
                "R-HSA-380901", "R-HSA-382553", "R-HSA-382560", "R-HSA-383190",
                "R-HSA-2012073", "R-HSA-2012074", "R-HSA-2012082", "R-HSA-2012084", "R-HSA-2029984", "R-HSA-2029988",
                "R-HSA-2029989", "R-HSA-2029992", "R-HSA-2033472", "R-HSA-2033474", "R-HSA-2033479", "R-HSA-2033488",
                "R-HSA-2033490", "R-HSA-2067713", "R-HSA-2077424", "R-HSA-5387386", "R-HSA-5387389", "R-HSA-5387392",
                "R-HSA-5483229", "R-HSA-5483238", "R-HSA-5578663", "R-HSA-5602383", "R-HSA-5602549", "R-HSA-5602606",
                "R-HSA-5602885", "R-HSA-5607838", "R-HSA-5610727", "R-HSA-5628807", "R-HSA-5632804", "R-HSA-5632871",
                "R-HSA-5632958", "R-HSA-5632970", "R-HSA-5633241", "R-HSA-5653850", "R-HSA-5654125", "R-HSA-5654748",
                "R-HSA-5655289", "R-HSA-5655295", "R-HSA-5655315", "R-HSA-5655702", "R-HSA-5655733", "R-HSA-5655760",
                "R-HSA-5656219", "R-HSA-5656248", "R-HSA-5656356", "R-HSA-5656438", "R-HSA-5656459", "R-HSA-5658001",
                "R-HSA-6802921", "R-HSA-6802922", "R-HSA-6802925", "R-HSA-6802926", "R-HSA-6802927", "R-HSA-6802930",
                "R-HSA-6802932", "R-HSA-6802933", "R-HSA-6802934", "R-HSA-6802935", "R-HSA-6802938", "R-HSA-6802942",
                "R-HSA-6802943", "R-HSA-8851710", "R-HSA-8853313", "R-HSA-8853315", "R-HSA-8853317", "R-HSA-8853319",
                "R-HSA-8853320", "R-HSA-8853322", "R-HSA-8853325", "R-HSA-8854908", "R-HSA-8855062", "R-HSA-8856808",
                "R-HSA-8863101", "R-HSA-8865275", "R-HSA-8865276", "R-HSA-8866856", "R-HSA-8867876", "R-HSA-8869241",
                "R-HSA-8877620", "R-HSA-8948146", "R-HSA-8948832", "R-HSA-8949609", "R-HSA-8949687", "R-HSA-8949688",
                "R-HSA-8949703", "R-HSA-8951850", "R-HSA-9036056", "R-HSA-9603534", "R-MMU-8851711", "R-MMU-9005872",
                "R-NUL-9005752", "R-NUL-9606338"));
        test(identifiers);
        // TODO: 21/10/18 add resource
    }

    private void test(Collection<String> identifiers) {
        for (String stId : identifiers) convert(stId);

    }

    private void convert(String stId) {
        convert(stId, true, true, false, false, false);
    }

    private void convert(String stId, boolean test, boolean svg, boolean json, boolean pptx, boolean sbgn) {
        try {
            ReactionLikeEvent rle = databaseObjectService.findById(stId);
            final String pStId = rle.getEventOf().isEmpty() ? stId : rle.getEventOf().get(0).getStId();

            final LayoutFactory layoutFactory = new LayoutFactory(ads);
            final Layout layout = layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.BOX);
            final Diagram diagram = ReactionDiagramFactory.get(layout);

            final Graph graph = new ReactionGraphFactory(ads).getGraph(rle, layout);

            if (test) runTest(diagram, stId);
            if (json) printJsons(diagram, graph, layout);
            if (svg) saveSvg(stId, pStId, diagram, graph);
            if (sbgn) saveSbgn(stId, diagram);
            if (pptx) savePptx(diagram);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(stId);
        }
    }

    private void saveSvg(String stId, String pStId, Diagram diagram, Graph graph) {
        try {
            final File file = new File(TEST_IMAGES, String.format("%s.%s", stId, "svg"));
            OutputStream os = new FileOutputStream(file);
            RasterArgs args = new RasterArgs(pStId, "svg").setQuality(8).setMargin(1);
            rasterExporter.export(diagram, graph, args, null, os);
        } catch (IOException | AnalysisException | TranscoderException e) {
            e.printStackTrace();
        }
    }

    private void runTest(Diagram diagram, String stId) {
        final DiagramTest test = new DiagramTest(diagram);
        test.runTests(stId);
        total += test.getLogs().getOrDefault(DiagramTest.Level.ERROR, Collections.emptyList()).size();
    }

    private void savePptx(Diagram diagram) throws DiagramProfileException, DiagramJsonDeserializationException {
        PowerPointExporter.export(diagram, "modern", TEST_IMAGES.getAbsolutePath(), new Decorator(), null);
    }

    private void saveSbgn(String stId, Diagram diagram) throws JAXBException {
        final File sbgn = new File(TEST_IMAGES, String.format("%s.%s", stId, "sbgn"));
        final Sbgn result = new SbgnConverter(diagram).getSbgn();
        SbgnUtil.writeToFile(result, sbgn);

    }

    private void printJsons(Diagram diagram, Graph graph, Layout layout) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(layout));
        System.out.println(mapper.writeValueAsString(diagram));
        System.out.println(mapper.writeValueAsString(graph));
    }

}
