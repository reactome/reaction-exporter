package org.reactome.server.tools.reaction.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.batik.transcoder.TranscoderException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
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
import java.util.HashSet;

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
        // try {
        //     FileUtils.cleanDirectory(TEST_IMAGES);
        // } catch (IOException e) {
        //     logger.error("Could't delete test folder " + TEST_IMAGES);
        // }
    }

    @Test
    public void findByDbIdTest() {
        final Collection<String> identifiers = new HashSet<>(Arrays.asList(
                "R-HSA-68947",
                "R-HSA-69144",
                "R-HSA-72107",
                "R-HSA-112381",
                "R-HSA-140664",
                "R-HSA-211734",
                "R-HSA-420586",
                "R-HSA-425483",
                "R-HSA-1218824",
                "R-HSA-1218833",
                "R-HSA-1220614",
                "R-HSA-1247999",
                "R-HSA-1362408",
                "R-HSA-1592238",
                "R-HSA-1839031",
                "R-HSA-1839039",
                "R-HSA-1839065",
                "R-HSA-1839067",
                "R-HSA-1839078",
                "R-HSA-1839095",
                "R-HSA-1839102",
                "R-HSA-1839110",
                "R-HSA-1839112",
                "R-HSA-1839114",
                "R-HSA-1888198",
                "R-HSA-1982065",
                "R-HSA-1982066",
                "R-HSA-2012073",
                "R-HSA-2012074",
                "R-HSA-2012082",
                "R-HSA-2012084",
                "R-HSA-2023456",
                "R-HSA-2023460",
                "R-HSA-2023462",
                "R-HSA-2029983",
                "R-HSA-2029984",
                "R-HSA-2029988",
                "R-HSA-2029989",
                "R-HSA-2029992",
                "R-HSA-2033472",
                "R-HSA-2033474",
                "R-HSA-2033476",
                "R-HSA-2033479",
                "R-HSA-2033485",
                "R-HSA-2033486",
                "R-HSA-2033488",
                "R-HSA-2033490",
                "R-HSA-2038386",
                "R-HSA-2038387",
                "R-HSA-2046363",
                "R-HSA-2067713",
                "R-HSA-2077420",
                "R-HSA-2077424",
                "R-HSA-2220966",
                "R-HSA-2220979",
                "R-HSA-2220981",
                "R-HSA-2220985",
                "R-HSA-2263490",
                "R-HSA-2263492",
                "R-HSA-2265534",
                "R-HSA-2666278",
                "R-HSA-2993780",
                "R-HSA-3215391",
                "R-HSA-3274540",
                "R-HSA-3791349",
                "R-HSA-3814838",
                "R-HSA-3828061",
                "R-HSA-3858506",
                "R-HSA-5205661",
                "R-HSA-5205663",
                "R-HSA-5205681",
                "R-HSA-5228811",
                "R-HSA-5358460",
                "R-HSA-5362450",
                "R-HSA-5387386",
                "R-HSA-5387389",
                "R-HSA-5387392",
                "R-HSA-5483229",
                "R-HSA-5483238",
                "R-HSA-5578663",
                "R-HSA-5602383",
                "R-HSA-5602549",
                "R-HSA-5602606",
                "R-HSA-5617820",
                "R-HSA-5638137",
                "R-HSA-5654544",
                "R-HSA-5654545",
                "R-HSA-5683209",
                "R-HSA-6785722",
                "R-HSA-6787403",
                "R-HSA-6791221",
                "R-HSA-6791223",
                "R-HSA-6802912",
                "R-HSA-6802914",
                "R-HSA-6802925",
                "R-HSA-6802927",
                "R-HSA-6802927",
                "R-HSA-6802930",
                "R-HSA-6802932",
                "R-HSA-6802934",
                "R-HSA-6802938",
                "R-HSA-6802942",
                "R-HSA-6814559",
                "R-HSA-8851710",
                "R-HSA-8853309",
                "R-HSA-8853313",
                "R-HSA-8853317",
                "R-HSA-8853319",
                "R-HSA-8853320",
                "R-HSA-8853322",
                "R-HSA-8853325",
                "R-HSA-8948146",
                "R-HSA-8948832",
                "R-HSA-9013533",
                "R-HSA-9015379",
                "R-HSA-9036025",
                "R-HSA-9036056",
                "R-MMU-8851711",
                "R-MMU-9005872",
                "R-NUL-9005752",
                "R-NUL-9606338"
        ));
//        final AnalysisStoredResult result = new TokenUtils("/home/plorente/resources/reactome/v66/analysis").getFromToken("MjAxODEwMDQxMDA3MDhfMw%253D%253D");
        final long start = System.nanoTime();
        for (String stId : identifiers) convert(stId);
        final long elapsed = System.nanoTime() - start;
        System.out.println(formatTime(elapsed));
        System.out.println(formatTime(elapsed / identifiers.size()));
        // TODO: 21/10/18 add resource
    }

    private void convert(String stId) {
        try {
            ReactionLikeEvent rle = databaseObjectService.findById(stId);
            final String pStId = rle.getEventOf().get(0).getStId();

            final LayoutFactory layoutFactory = new LayoutFactory(ads);
            final Layout layout = layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.COMPACT);
            final Diagram diagram = ReactionDiagramFactory.get(layout);

            final ReactionGraphFactory graphFactory = new ReactionGraphFactory(ads);
            final Graph graph = graphFactory.getGraph(rle, layout);

            // runTest(diagram);
            // printJsons(diagram, graph, layout);
            savePng(stId, pStId, diagram, graph);
            // saveSbgn(stId, diagram);
            // savePptx(diagram);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(stId);
        }
    }

    private void savePng(String stId, String pStId, Diagram diagram, Graph graph) {
        try {
            final File file = new File(TEST_IMAGES, String.format("%s.%s", stId, "png"));
            OutputStream os = new FileOutputStream(file);
            RasterArgs args = new RasterArgs(pStId, "png").setQuality(8).setMargin(1);
            rasterExporter.export(diagram, graph, args, null, os);
        } catch (IOException | AnalysisException | TranscoderException e) {
            e.printStackTrace();
        }
    }

    private void runTest(Diagram diagram) {
        new DiagramTest(diagram).printResults(DiagramTest.Level.WARNING);
        System.out.println();
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

    // https://reactomedev.oicr.on.ca/download/current/reactome.graphdb.tgz
    private static String formatTime(long nanoSeconds) {
        final long hours = nanoSeconds / 3_600_000_000_000L;
        nanoSeconds = nanoSeconds - hours * 3_600_000_000_000L;
        final long minutes = nanoSeconds / 60_000_000_000L;
        nanoSeconds = nanoSeconds - minutes * 60_000_000_000L;
        final long seconds = nanoSeconds / 1_000_000_000L;
        nanoSeconds = nanoSeconds - minutes * 60_000_000_000L;
        final long millis = nanoSeconds / 1_000_000L;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }
}
