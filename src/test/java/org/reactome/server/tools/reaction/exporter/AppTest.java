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
import java.util.*;

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
    private int total;

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
    public void testOne() {
        convert("R-HSA-70634");
    }

    @Test
    public void testAllReactome() {
        Collection<String> identifiers = new ArrayList<>();
        try {
            identifiers = ads.getCustomQueryResults(String.class, "MATCH (rle:ReactionLikeEvent{speciesName:\"Homo sapiens\"}) RETURN rle.stId");
        } catch (CustomQueryException e) {
            e.printStackTrace();
        }
        System.out.println(identifiers.size() + " reactions");
        final long start = System.nanoTime();
        for (String stId : identifiers) convert(stId);
        final long elapsed = System.nanoTime() - start;
        System.out.println();
        System.out.println(formatTime(elapsed));
        System.out.println(formatTime(elapsed / identifiers.size()));
        System.out.println("Total errors = " + total);
    }

    @Test
    public void testAll() {
        Collection<String> identifiers = new LinkedHashSet<>(Arrays.asList(
                "R-HSA-68947", "R-HSA-69144", "R-HSA-70634", "R-HSA-72107", "R-HSA-74986", "R-HSA-75849",
                "R-HSA-111883", "R-HSA-112381", "R-HSA-139952", "R-HSA-140664", "R-HSA-162683", "R-HSA-162721",
                "R-HSA-162730", "R-HSA-162857", "R-HSA-163214", "R-HSA-163215", "R-HSA-163217", "R-HSA-164651",
                "R-HSA-164834", "R-HSA-170026", "R-HSA-170835", "R-HSA-186785", "R-HSA-193362", "R-HSA-194079",
                "R-HSA-194083", "R-HSA-194121", "R-HSA-194130", "R-HSA-194153", "R-HSA-194187", "R-HSA-198440",
                "R-HSA-198513", "R-HSA-200424", "R-HSA-203946", "R-HSA-210404", "R-HSA-210426", "R-HSA-210430",
                "R-HSA-211734", "R-HSA-265166", "R-HSA-265682", "R-HSA-265783", "R-HSA-352174", "R-HSA-352182",
                "R-HSA-352347", "R-HSA-352354", "R-HSA-352364", "R-HSA-352371", "R-HSA-352379", "R-HSA-352385",
                "R-HSA-372448", "R-HSA-372449", "R-HSA-372480", "R-HSA-372529", "R-HSA-372843", "R-HSA-374899",
                "R-HSA-374909", "R-HSA-374922", "R-HSA-375342", "R-HSA-376149", "R-HSA-376851", "R-HSA-378513",
                "R-HSA-379415", "R-HSA-379426", "R-HSA-379432", "R-HSA-380869", "R-HSA-380901", "R-HSA-382553",
                "R-HSA-382560", "R-HSA-383190", "R-HSA-392513", "R-HSA-416320", "R-HSA-420586", "R-HSA-420883",
                "R-HSA-425482", "R-HSA-425483", "R-HSA-425577", "R-HSA-425661", "R-HSA-425678", "R-HSA-425822",
                "R-HSA-425994", "R-HSA-426015", "R-HSA-427666", "R-HSA-427910", "R-HSA-428015", "R-HSA-428052",
                "R-HSA-428127", "R-HSA-428625", "R-HSA-432162", "R-HSA-434650", "R-HSA-444393", "R-HSA-444419",
                "R-HSA-446187", "R-HSA-446191", "R-HSA-446195", "R-HSA-446200", "R-HSA-446207", "R-HSA-446208",
                "R-HSA-446214", "R-HSA-446218", "R-HSA-446277", "R-HSA-446278", "R-HSA-447074", "R-HSA-449718",
                "R-HSA-450971", "R-HSA-452838", "R-HSA-452894", "R-HSA-480301", "R-HSA-480520", "R-HSA-549533",
                "R-HSA-560491", "R-HSA-561041", "R-HSA-561059", "R-HSA-561253", "R-HSA-597628", "R-HSA-727807",
                "R-HSA-735702", "R-HSA-741450", "R-HSA-744230", "R-HSA-744231", "R-HSA-888589", "R-HSA-917744",
                "R-HSA-917979", "R-HSA-936802", "R-HSA-936883", "R-HSA-936895", "R-HSA-936897", "R-HSA-937311",
                "R-HSA-975608", "R-HSA-983144",
                "R-HSA-1218824", "R-HSA-1218833", "R-HSA-1220614", "R-HSA-1222738", "R-HSA-1236949", "R-HSA-1237038",
                "R-HSA-1247665", "R-HSA-1247999", "R-HSA-1362408", "R-HSA-1368140", "R-HSA-1369028", "R-HSA-1369052",
                "R-HSA-1369065", "R-HSA-1454916", "R-HSA-1454928", "R-HSA-1467457", "R-HSA-1592234", "R-HSA-1592238",
                "R-HSA-1592252", "R-HSA-1614546", "R-HSA-1839031", "R-HSA-1839039", "R-HSA-1839065", "R-HSA-1839067",
                "R-HSA-1839078", "R-HSA-1839091", "R-HSA-1839095", "R-HSA-1839098", "R-HSA-1839102", "R-HSA-1839107",
                "R-HSA-1839110", "R-HSA-1839112", "R-HSA-1839114", "R-HSA-1888198", "R-HSA-1982065", "R-HSA-1982066",
                "R-HSA-2012073", "R-HSA-2012074", "R-HSA-2012082", "R-HSA-2012084", "R-HSA-2023456", "R-HSA-2023460",
                "R-HSA-2023462", "R-HSA-2029475", "R-HSA-2029983", "R-HSA-2029984", "R-HSA-2029988", "R-HSA-2029989",
                "R-HSA-2029992", "R-HSA-2033472", "R-HSA-2033474", "R-HSA-2033476", "R-HSA-2033479", "R-HSA-2033485",
                "R-HSA-2033486", "R-HSA-2033488", "R-HSA-2033490", "R-HSA-2038386", "R-HSA-2038387", "R-HSA-2046363",
                "R-HSA-2067713", "R-HSA-2077420", "R-HSA-2077424", "R-HSA-2134519", "R-HSA-2160932", "R-HSA-2161506",
                "R-HSA-2161538", "R-HSA-2220966", "R-HSA-2220979", "R-HSA-2220981", "R-HSA-2220985", "R-HSA-2263490",
                "R-HSA-2263492", "R-HSA-2265534", "R-HSA-2316352", "R-HSA-2396007", "R-HSA-2404137", "R-HSA-2429643",
                "R-HSA-2453855", "R-HSA-2466710", "R-HSA-2485180", "R-HSA-2514891", "R-HSA-2581474", "R-HSA-2584246",
                "R-HSA-2666278", "R-HSA-2682349", "R-HSA-2872444", "R-HSA-2889070", "R-HSA-2993780", "R-HSA-3000122",
                "R-HSA-3095901", "R-HSA-3149432", "R-HSA-3215391", "R-HSA-3229118", "R-HSA-3257122", "R-HSA-3274540",
                "R-HSA-3325546", "R-HSA-3656523", "R-HSA-3702153", "R-HSA-3713560", "R-HSA-3791349", "R-HSA-3814838",
                "R-HSA-3828061", "R-HSA-3858506", "R-HSA-4419979", "R-HSA-4419986", "R-HSA-4549334", "R-HSA-4549368",
                "R-HSA-4549382", "R-HSA-4551297", "R-HSA-4717406", "R-HSA-4719354", "R-HSA-4719375", "R-HSA-4755572",
                "R-HSA-4755600", "R-HSA-5205661", "R-HSA-5205663", "R-HSA-5205681", "R-HSA-5213462", "R-HSA-5228811",
                "R-HSA-5251989", "R-HSA-5357845", "R-HSA-5358460", "R-HSA-5362450", "R-HSA-5362459", "R-HSA-5387386",
                "R-HSA-5387389", "R-HSA-5387392", "R-HSA-5483229", "R-HSA-5483238", "R-HSA-5578663", "R-HSA-5602383",
                "R-HSA-5602549", "R-HSA-5602606", "R-HSA-5607838", "R-HSA-5615556", "R-HSA-5615604", "R-HSA-5617096",
                "R-HSA-5617820", "R-HSA-5623513", "R-HSA-5623521", "R-HSA-5624256", "R-HSA-5625015", "R-HSA-5625029",
                "R-HSA-5625210", "R-HSA-5625574", "R-HSA-5625841", "R-HSA-5626270", "R-HSA-5627737", "R-HSA-5633241",
                "R-HSA-5638014", "R-HSA-5638137", "R-HSA-5651942", "R-HSA-5652099", "R-HSA-5653622", "R-HSA-5654544",
                "R-HSA-5654545", "R-HSA-5655702", "R-HSA-5655760", "R-HSA-5656248", "R-HSA-5660890", "R-HSA-5660910",
                "R-HSA-5661184", "R-HSA-5661198", "R-HSA-5672027", "R-HSA-5678517", "R-HSA-5678706", "R-HSA-5678749",
                "R-HSA-5678822", "R-HSA-5678863", "R-HSA-5678992", "R-HSA-5679031", "R-HSA-5679041", "R-HSA-5679101",
                "R-HSA-5679145", "R-HSA-5682285", "R-HSA-5682311", "R-HSA-5683113", "R-HSA-5683209", "R-HSA-5683355",
                "R-HSA-5683672", "R-HSA-5683714", "R-HSA-5688025", "R-HSA-5688397", "R-HSA-5690340", "R-HSA-5692462",
                "R-HSA-6785668", "R-HSA-6785722", "R-HSA-6787403", "R-HSA-6791221", "R-HSA-6791223", "R-HSA-6797568",
                "R-HSA-6802834", "R-HSA-6802910", "R-HSA-6802911", "R-HSA-6802912", "R-HSA-6802914", "R-HSA-6802925",
                "R-HSA-6802927", "R-HSA-6802930", "R-HSA-6802932", "R-HSA-6802933", "R-HSA-6802934", "R-HSA-6802935",
                "R-HSA-6802938", "R-HSA-6802942", "R-HSA-6814559", "R-HSA-8851710", "R-HSA-8852552", "R-HSA-8853309",
                "R-HSA-8853313", "R-HSA-8853317", "R-HSA-8853319", "R-HSA-8853320", "R-HSA-8853322", "R-HSA-8853325",
                "R-HSA-8854908", "R-HSA-8855062", "R-HSA-8863101", "R-HSA-8865275", "R-HSA-8865276", "R-HSA-8867876",
                "R-HSA-8869241", "R-HSA-8877620", "R-HSA-8948146", "R-HSA-8948832", "R-HSA-8949609", "R-HSA-8949687",
                "R-HSA-8949688", "R-HSA-8949703", "R-HSA-8951850", "R-HSA-9013533", "R-HSA-9015379", "R-HSA-9036025",
                "R-HSA-9036056",
                "R-MMU-8851711", "R-MMU-9005872",
                "R-NUL-9005752", "R-NUL-9606338"
        ));
        System.out.println(identifiers.size() + " reactions");
        final long start = System.nanoTime();
        for (String stId : identifiers) convert(stId);
        final long elapsed = System.nanoTime() - start;
        System.out.println();
        System.out.println(formatTime(elapsed));
        System.out.println(formatTime(elapsed / identifiers.size()));
        System.out.println("Total errors = " + total);
        // TODO: 21/10/18 add resource
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
        test.runTests(stId, DiagramTest.Level.ERROR);
        total += test.getLogs().getOrDefault(DiagramTest.Level.ERROR, Collections.emptyList()).size();
        // test.printResults(DiagramTest.Level.WARNING);
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
        nanoSeconds = nanoSeconds - seconds * 1_000_000_000L;
        final long millis = nanoSeconds / 1_000_000L;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

}
