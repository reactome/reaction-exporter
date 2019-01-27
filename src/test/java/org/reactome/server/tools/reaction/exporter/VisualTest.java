package org.reactome.server.tools.reaction.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.batik.transcoder.TranscoderException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
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
import java.io.*;
import java.util.*;

/**
 * Not intended for automatic testing, but for visual testing. This class will create and maintain a folder with the
 * generated images, as well as profiling files.
 */
public class VisualTest extends BaseTest {

    private static final File TEST_IMAGES = new File("test-images");

    @Autowired
    private DatabaseObjectService databaseObjectService;

    @Autowired
    private AdvancedDatabaseObjectService ads;

    private RasterExporter rasterExporter = new RasterExporter();
    private int total;

    @BeforeClass
    public static void setUpClass() {
        logger.info("Running " + VisualTest.class.getName());
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

    @Ignore
    @Test
    public void testOne() {
        try {
            // 210430
            // 917744
            convert("R-HSA-4655355", new BufferedWriter(new FileWriter(new File(TEST_IMAGES, "data.tsv"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testHomoSapiens() {
        Collection<String> identifiers = new ArrayList<>();
        try {
            // MATCH (rle:ReactionLikeEvent)-[:species]->(:Species{displayName:"Homo sapiens"}) RETURN count(rle) AS reactions 12047
            identifiers = ads.getCustomQueryResults(String.class, "MATCH (rle:ReactionLikeEvent)-[:species]->(:Species{displayName:\"Homo sapiens\"}) RETURN rle.stId");
        } catch (CustomQueryException e) {
            e.printStackTrace();
        }
        test(identifiers);
    }

    @Ignore
    @Test
    public void testFailedReactions() {
        test(new LinkedHashSet<>(Arrays.asList(
                "R-HSA-1225956", "R-HSA-2206299", "R-HSA-2220967", "R-HSA-2220978", "R-HSA-2262743", "R-HSA-2263444",
                "R-HSA-2263490", "R-HSA-2263492", "R-HSA-2263495", "R-HSA-2263496", "R-HSA-2265534", "R-HSA-2282889",
                "R-HSA-2317387", "R-HSA-2318373", "R-HSA-2318585", "R-HSA-2453818", "R-HSA-2466706", "R-HSA-2466710",
                "R-HSA-2466802", "R-HSA-2466822", "R-HSA-2466828", "R-HSA-2466832", "R-HSA-2466834", "R-HSA-2466861",
                "R-HSA-2471641", "R-HSA-2471660", "R-HSA-2471670", "R-HSA-2660815", "R-HSA-2660816", "R-HSA-2660819",
                "R-HSA-2660822", "R-HSA-3229118", "R-HSA-3274540", "R-HSA-3282876", "R-HSA-3296462", "R-HSA-3296477",
                "R-HSA-3299657", "R-HSA-3304394", "R-HSA-3311014", "R-HSA-3315437", "R-HSA-3315455", "R-HSA-3315483",
                "R-HSA-3318563", "R-HSA-3318571", "R-HSA-3318576", "R-HSA-3318590", "R-HSA-3321918", "R-HSA-3322125",
                "R-HSA-3322135", "R-HSA-3322140", "R-HSA-3322971", "R-HSA-3323184", "R-HSA-3325540", "R-HSA-3325546",
                "R-HSA-3560785", "R-HSA-3560789", "R-HSA-3560794", "R-HSA-3560802", "R-HSA-3560804", "R-HSA-3595175",
                "R-HSA-3595176", "R-HSA-3595178", "R-HSA-3636919", "R-HSA-3642203", "R-HSA-3645780", "R-HSA-3656230",
                "R-HSA-3656254", "R-HSA-3656257", "R-HSA-3656258", "R-HSA-3656259", "R-HSA-3656261", "R-HSA-3656267",
                "R-HSA-3656269", "R-HSA-3656382", "R-HSA-3656523", "R-HSA-3662344", "R-HSA-3781832", "R-HSA-3781926",
                "R-HSA-3791349", "R-HSA-3797226", "R-HSA-3814838", "R-HSA-3828061", "R-HSA-3858506", "R-HSA-3878762",
                "R-HSA-4085027", "R-HSA-4088322", "R-HSA-4088338", "R-HSA-4225086", "R-HSA-4341669", "R-HSA-4420365",
                "R-HSA-4549334", "R-HSA-4549368", "R-HSA-4549382", "R-HSA-4551297", "R-HSA-4570573", "R-HSA-4686998",
                "R-HSA-4717406", "R-HSA-4719354", "R-HSA-4719375", "R-HSA-4720473", "R-HSA-4720478", "R-HSA-4720497",
                "R-HSA-4724291", "R-HSA-4724330", "R-HSA-4755545", "R-HSA-4755572", "R-HSA-4755600", "R-HSA-4791278",
                "R-HSA-4793947", "R-HSA-4793949", "R-HSA-4793955", "R-HSA-4793956", "R-HSA-4827388", "R-HSA-4839634",
                "R-HSA-4839635", "R-HSA-4839638", "R-HSA-4839734", "R-HSA-4839746", "R-HSA-5096532", "R-HSA-5096537",
                "R-HSA-5096538", "R-HSA-5228811", "R-HSA-5228840", "R-HSA-5246696", "R-HSA-5251559", "R-HSA-5251562",
                "R-HSA-5262921", "R-HSA-5334052", "R-HSA-5339711", "R-HSA-5339713", "R-HSA-5340587", "R-HSA-5358460",
                "R-HSA-5483229", "R-HSA-5545484", "R-HSA-5577244", "R-HSA-5577259", "R-HSA-5578663", "R-HSA-5579081",
                "R-HSA-5579084", "R-HSA-5580269", "R-HSA-5580292", "R-HSA-5600598", "R-HSA-5601843", "R-HSA-5601849",
                "R-HSA-5601976", "R-HSA-5602004", "R-HSA-5602050", "R-HSA-5602063", "R-HSA-5602147", "R-HSA-5602170",
                "R-HSA-5602186", "R-HSA-5602242", "R-HSA-5602272", "R-HSA-5602316", "R-HSA-5602353", "R-HSA-5602383",
                "R-HSA-5602472", "R-HSA-5602549", "R-HSA-5602603", "R-HSA-5602606", "R-HSA-5602624", "R-HSA-5602672",
                "R-HSA-5602712", "R-HSA-5602885", "R-HSA-5602892", "R-HSA-5602901", "R-HSA-5602966", "R-HSA-5602984",
                "R-HSA-5603087", "R-HSA-5603108", "R-HSA-5603208", "R-HSA-5603251", "R-HSA-5603275", "R-HSA-5603297",
                "R-HSA-5603379", "R-HSA-5604954", "R-HSA-5604975", "R-HSA-5605147", "R-HSA-5607838", "R-HSA-5609939",
                "R-HSA-5610026", "R-HSA-5610036", "R-HSA-5610038", "R-HSA-5615556", "R-HSA-5615604", "R-HSA-5617096",
                "R-HSA-5621402", "R-HSA-5621425", "R-HSA-5621888", "R-HSA-5621918", "R-HSA-5623051", "R-HSA-5623558",
                "R-HSA-5623588", "R-HSA-5623705", "R-HSA-5623806", "R-HSA-5624211", "R-HSA-5624239", "R-HSA-5624256",
                "R-HSA-5625015", "R-HSA-5625029", "R-HSA-5625123", "R-HSA-5625210", "R-HSA-5625574", "R-HSA-5625674",
                "R-HSA-5625841", "R-HSA-5626270", "R-HSA-5626356", "R-HSA-5627737", "R-HSA-5627870", "R-HSA-5627891",
                "R-HSA-5628807", "R-HSA-5632804", "R-HSA-5632871", "R-HSA-5632958", "R-HSA-5632970", "R-HSA-5633241",
                "R-HSA-5637794", "R-HSA-5638209", "R-HSA-5638222", "R-HSA-5649483", "R-HSA-5649742", "R-HSA-5651685",
                "R-HSA-5651697", "R-HSA-5651942", "R-HSA-5651971", "R-HSA-5652099", "R-HSA-5653596", "R-HSA-5653622",
                "R-HSA-5653850", "R-HSA-5654125", "R-HSA-5655702", "R-HSA-5655733", "R-HSA-5655760", "R-HSA-5656219",
                "R-HSA-5656248", "R-HSA-5656356", "R-HSA-5656438", "R-HSA-5656459", "R-HSA-5658001", "R-HSA-5658163",
                "R-HSA-5658195", "R-HSA-5658483", "R-HSA-5659674", "R-HSA-5659734", "R-HSA-5659755", "R-HSA-5659764",
                "R-HSA-5659879", "R-HSA-5659899", "R-HSA-5659922", "R-HSA-5659926", "R-HSA-5659989", "R-HSA-5659998",
                "R-HSA-5660013", "R-HSA-5660015", "R-HSA-5660694", "R-HSA-5660706", "R-HSA-5660840", "R-HSA-5660890",
                "R-HSA-5660910", "R-HSA-5661039", "R-HSA-5661086", "R-HSA-5661184", "R-HSA-5661188", "R-HSA-5661195",
                "R-HSA-5661198", "R-HSA-5661474", "R-HSA-5662851", "R-HSA-5678418", "R-HSA-5678517", "R-HSA-5678749",
                "R-HSA-5678822", "R-HSA-5679031", "R-HSA-5679101", "R-HSA-5679145", "R-HSA-5682111", "R-HSA-5682311",
                "R-HSA-5683113", "R-HSA-5683325", "R-HSA-5683355", "R-HSA-5683672", "R-HSA-5684043", "R-HSA-5687585",
                "R-HSA-5687875", "R-HSA-5688025", "R-HSA-5688377", "R-HSA-5688397", "R-HSA-5688884", "R-HSA-5688899",
                "R-HSA-5690340", "R-HSA-6785244", "R-HSA-6785245", "R-HSA-6785524", "R-HSA-6785565", "R-HSA-6785668",
                "R-HSA-6802834", "R-HSA-6802837", "R-HSA-9005561", "R-HSA-9005585", "R-HSA-9022462", "R-HSA-9022465",
                "R-HSA-9023461", "R-HSA-9035514", "R-HSA-9035517", "R-HSA-9035949", "R-HSA-9035950", "R-HSA-9035954",
                "R-HSA-9035956", "R-HSA-9035960", "R-HSA-9035966", "R-HSA-9035976", "R-HSA-9035978", "R-HSA-9035982",
                "R-HSA-9035983", "R-HSA-9035987", "R-HSA-9035988", "R-HSA-9035990", "R-HSA-9036008", "R-HSA-9036011",
                "R-HSA-9036012", "R-HSA-9036020", "R-HSA-9036021", "R-HSA-9036025", "R-HSA-9036037", "R-HSA-9036041",
                "R-HSA-9036046", "R-HSA-9036050", "R-HSA-9036052", "R-HSA-9036056", "R-HSA-9036061", "R-HSA-9036065",
                "R-HSA-9036068", "R-HSA-9036070", "R-HSA-9036077", "R-HSA-9036102", "R-HSA-9036104", "R-HSA-9036283",
                "R-HSA-9036285", "R-HSA-9036289", "R-HSA-9036290", "R-HSA-9036729", "R-HSA-9605313", "R-HSA-9608288",
                "R-NUL-9005752", "R-NUL-9606338")));
    }

    @Ignore
    @Test
    public void testGoF() {
        test(new LinkedHashSet<>(Arrays.asList(
                "R-HSA-6803233", "R-HSA-6802922", "R-HSA-6802926", "R-HSA-6802925", "R-HSA-6802924", "R-HSA-6802908",
                "R-HSA-6803240", "R-HSA-6802930", "R-HSA-6802912", "R-HSA-6802911", "R-HSA-6802910", "R-HSA-6803227",
                "R-HSA-6802938", "R-HSA-6802919", "R-HSA-6802914", "R-HSA-6802916", "R-HSA-6802915", "R-HSA-6803230",
                "R-HSA-6802921", "R-HSA-8936676", "R-HSA-6802918", "R-HSA-8936731", "R-HSA-6802937", "R-HSA-6803234",
                "R-HSA-6802943", "R-HSA-6802942", "R-HSA-6802941", "R-HSA-6802935", "R-HSA-6802933", "R-HSA-6802934",
                "R-HSA-6802927", "R-HSA-6802932", "R-HSA-3215391", "R-HSA-1839100", "R-HSA-1839098", "R-HSA-1839094",
                "R-HSA-1839065", "R-HSA-1839031", "R-HSA-5655269", "R-HSA-5655266", "R-HSA-5655278", "R-HSA-1839102",
                "R-HSA-1839114", "R-HSA-1839110", "R-HSA-1839095", "R-HSA-1839067", "R-HSA-1839039", "R-HSA-1839091",
                "R-HSA-1839080", "R-HSA-1839078", "R-HSA-1839112", "R-HSA-1888198", "R-HSA-1839107", "R-HSA-5654544",
                "R-HSA-5654545", "R-HSA-2023462", "R-HSA-1982065", "R-HSA-1982066", "R-HSA-2023455", "R-HSA-2023451",
                "R-HSA-2023460", "R-HSA-2023456", "R-HSA-8853325", "R-HSA-8853322", "R-HSA-5655326", "R-HSA-5655263",
                "R-HSA-5655240", "R-HSA-5655290", "R-HSA-8853317", "R-HSA-8853316", "R-HSA-8853315", "R-HSA-8853310",
                "R-HSA-8853309", "R-HSA-8853314", "R-HSA-8853308", "R-HSA-8853307", "R-HSA-8853323", "R-HSA-5655270",
                "R-HSA-5655285", "R-HSA-2077420", "R-HSA-2038387", "R-HSA-2038386", "R-HSA-2033485", "R-HSA-2033476",
                "R-HSA-2012074", "R-HSA-2012073", "R-HSA-2012084", "R-HSA-2012082", "R-HSA-5655315", "R-HSA-5655262",
                "R-HSA-5655295", "R-HSA-5655247", "R-HSA-5655243", "R-HSA-5655244", "R-HSA-5655277", "R-HSA-5655289",
                "R-HSA-5655268", "R-HSA-5655301", "R-HSA-5655233", "R-HSA-5655343", "R-HSA-2029983", "R-HSA-2033486",
                "R-HSA-2033472", "R-HSA-2033479", "R-HSA-2033474", "R-HSA-2033490", "R-HSA-2029984", "R-HSA-2033488",
                "R-HSA-2077424", "R-HSA-8853319", "R-HSA-8853313", "R-HSA-2029992", "R-HSA-2029988", "R-HSA-2029989",
                "R-HSA-2067713", "R-HSA-8851710", "R-MMU-8851711", "R-HSA-8853320", "R-HSA-5654748", "R-HSA-5655245",
                "R-HSA-5655339", "R-HSA-5655323", "R-HSA-5655320", "R-HSA-5655241", "R-HSA-5655351", "R-HSA-5655284",
                "R-HSA-5655348", "R-HSA-5655252", "R-HSA-2046363", "R-HSA-2038946", "R-HSA-2012086", "R-HSA-2012087",
                "R-HSA-2038944", "R-HSA-5655347", "R-HSA-5655235", "R-HSA-5655248", "R-HSA-5655313", "R-HSA-5655341",
                "R-HSA-5655336", "R-HSA-2666278", "R-HSA-2769000", "R-HSA-2769007", "R-HSA-2220964", "R-HSA-2220982",
                "R-HSA-2769015", "R-HSA-2220988", "R-HSA-2220971", "R-HSA-2220957", "R-HSA-2220966", "R-HSA-4396393",
                "R-HSA-2768993", "R-HSA-4396392", "R-HSA-2768999", "R-HSA-2220944", "R-HSA-4396402", "R-HSA-2220979",
                "R-HSA-4396401", "R-HSA-2220981", "R-HSA-2220985", "R-HSA-2769008", "R-HSA-2900748", "R-HSA-2900743",
                "R-HSA-2900756", "R-HSA-2691211", "R-HSA-2691226", "R-HSA-2691214", "R-HSA-2691219", "R-HSA-1218833",
                "R-HSA-1225947", "R-HSA-1169421", "R-HSA-1220614", "R-HSA-1220613", "R-HSA-1220612", "R-HSA-1248655",
                "R-HSA-1225950", "R-HSA-1226014", "R-HSA-1226012", "R-HSA-1226016", "R-HSA-1225951", "R-HSA-1225949",
                "R-HSA-1247842", "R-HSA-1247844", "R-HSA-1247841", "R-HSA-1225952", "R-HSA-1225960", "R-HSA-1225957",
                "R-HSA-1225961", "R-HSA-5637796", "R-HSA-5638137", "R-HSA-5637766", "R-HSA-5637764", "R-HSA-5637792",
                "R-HSA-5637770", "R-HSA-5637801", "R-HSA-2243938", "R-HSA-2219536", "R-HSA-2399941", "R-HSA-2243942",
                "R-HSA-2243937", "R-HSA-2399969", "R-HSA-2400001", "R-HSA-2399981", "R-HSA-2399982", "R-HSA-2399977",
                "R-HSA-2399988", "R-HSA-2399997", "R-HSA-2399985", "R-HSA-2399996", "R-HSA-2399966", "R-HSA-2399999",
                "R-HSA-2399992", "R-HSA-2394007", "R-HSA-5387392", "R-HSA-5387389", "R-HSA-5483238", "R-HSA-5387386",
                "R-HSA-5362450", "R-HSA-3645786", "R-HSA-3645778", "R-HSA-3656517", "R-HSA-3656484", "R-HSA-3713560",
                "R-HSA-3702184", "R-HSA-3702153", "R-HSA-3702186", "R-HSA-5683209")));
    }

    @Ignore
    @Test
    public void testMostDifficult() {
        Collection<String> identifiers = new LinkedHashSet<>(Arrays.asList(
                "R-HSA-68947", "R-HSA-69144", "R-HSA-70634", "R-HSA-71670", "R-HSA-72107", "R-HSA-74885",
                "R-HSA-74986", "R-HSA-75097", "R-HSA-75849",
                "R-HSA-111883", "R-HSA-111930", "R-HSA-112381", "R-HSA-139952", "R-HSA-140664", "R-HSA-158419",
                "R-HSA-159796", "R-HSA-162683", "R-HSA-162721", "R-HSA-162730", "R-HSA-162857", "R-HSA-163214",
                "R-HSA-163215", "R-HSA-163217", "R-HSA-163595", "R-HSA-163798", "R-HSA-164651", "R-HSA-164834",
                "R-HSA-168909", "R-HSA-168921", "R-HSA-170026", "R-HSA-170835", "R-HSA-181567", "R-HSA-186785",
                "R-HSA-190256", "R-HSA-192065", "R-HSA-192830", "R-HSA-193064", "R-HSA-193362", "R-HSA-194079",
                "R-HSA-194083", "R-HSA-194121", "R-HSA-194130", "R-HSA-194153", "R-HSA-194187", "R-HSA-198440",
                "R-HSA-198513", "R-HSA-200424", "R-HSA-203613", "R-HSA-203946", "R-HSA-203977", "R-HSA-210404",
                "R-HSA-210426", "R-HSA-210430", "R-HSA-211734", "R-HSA-211951", "R-HSA-265166", "R-HSA-265682",
                "R-HSA-265783", "R-HSA-350769", "R-HSA-352174", "R-HSA-352182", "R-HSA-352347", "R-HSA-352354",
                "R-HSA-352364", "R-HSA-352371", "R-HSA-352379", "R-HSA-352385", "R-HSA-372448", "R-HSA-372449",
                "R-HSA-372480", "R-HSA-372529", "R-HSA-372843", "R-HSA-374899", "R-HSA-374909", "R-HSA-374922",
                "R-HSA-375342", "R-HSA-376149", "R-HSA-376851", "R-HSA-378513", "R-HSA-379415", "R-HSA-379426",
                "R-HSA-379432", "R-HSA-380869", "R-HSA-380901", "R-HSA-382553", "R-HSA-382560", "R-HSA-383190",
                "R-HSA-392513", "R-HSA-416320", "R-HSA-420586", "R-HSA-420883", "R-HSA-422088", "R-HSA-425482",
                "R-HSA-425483", "R-HSA-425577", "R-HSA-425661", "R-HSA-425678", "R-HSA-425822", "R-HSA-425994",
                "R-HSA-426015", "R-HSA-427666", "R-HSA-427910", "R-HSA-428015", "R-HSA-428052", "R-HSA-428127",
                "R-HSA-428625", "R-HSA-432162", "R-HSA-434650", "R-HSA-442725", "R-HSA-444393", "R-HSA-444419",
                "R-HSA-446187", "R-HSA-446191", "R-HSA-446195", "R-HSA-446200", "R-HSA-446207", "R-HSA-446208",
                "R-HSA-446214", "R-HSA-446218", "R-HSA-446277", "R-HSA-446278", "R-HSA-447074", "R-HSA-449718",
                "R-HSA-450971", "R-HSA-452392", "R-HSA-452838", "R-HSA-452894", "R-HSA-480301", "R-HSA-480520",
                "R-HSA-517731", "R-HSA-549493", "R-HSA-549533", "R-HSA-560491", "R-HSA-561041", "R-HSA-561059",
                "R-HSA-561253", "R-HSA-597628", "R-HSA-727807", "R-HSA-735702", "R-HSA-741450", "R-HSA-744230",
                "R-HSA-744231", "R-HSA-888589", "R-HSA-917744", "R-HSA-917979", "R-HSA-936802", "R-HSA-936883",
                "R-HSA-936895", "R-HSA-936897", "R-HSA-937311", "R-HSA-975608", "R-HSA-983144",
                "R-HSA-1015702", "R-HSA-1169421", "R-HSA-1183058", "R-HSA-1218824", "R-HSA-1218833", "R-HSA-1220614",
                "R-HSA-1222738", "R-HSA-1225952", "R-HSA-1225956", "R-HSA-1225960", "R-HSA-1236949", "R-HSA-1237038",
                "R-HSA-1247665", "R-HSA-1247844", "R-HSA-1247999", "R-HSA-1248655", "R-HSA-1362408", "R-HSA-1368065",
                "R-HSA-1368119", "R-HSA-1368140", "R-HSA-1369028", "R-HSA-1369052", "R-HSA-1369065", "R-HSA-1454916",
                "R-HSA-1454928", "R-HSA-1458875", "R-HSA-1467457", "R-HSA-1592234", "R-HSA-1592238", "R-HSA-1592252",
                "R-HSA-1614546", "R-HSA-1678920", "R-HSA-1834939", "R-HSA-1839031", "R-HSA-1839039", "R-HSA-1839065",
                "R-HSA-1839067", "R-HSA-1839078", "R-HSA-1839091", "R-HSA-1839095", "R-HSA-1839098", "R-HSA-1839102",
                "R-HSA-1839107", "R-HSA-1839110", "R-HSA-1839112", "R-HSA-1839114", "R-HSA-1888198", "R-HSA-1912374",
                "R-HSA-1980118", "R-HSA-1982065", "R-HSA-1982066", "R-HSA-2012073", "R-HSA-2012074", "R-HSA-2012082",
                "R-HSA-2012084", "R-HSA-2012087", "R-HSA-2023451", "R-HSA-2023455", "R-HSA-2023456", "R-HSA-2023460",
                "R-HSA-2023462", "R-HSA-2029475", "R-HSA-2029983", "R-HSA-2029984", "R-HSA-2029988", "R-HSA-2029989",
                "R-HSA-2029992", "R-HSA-2033472", "R-HSA-2033474", "R-HSA-2033476", "R-HSA-2033479", "R-HSA-2033485",
                "R-HSA-2033486", "R-HSA-2033488", "R-HSA-2033490", "R-HSA-2038386", "R-HSA-2038387", "R-HSA-2038944",
                "R-HSA-2046363", "R-HSA-2067713", "R-HSA-2077420", "R-HSA-2077424", "R-HSA-2106586", "R-HSA-2130731",
                "R-HSA-2134519", "R-HSA-2160932", "R-HSA-2161506", "R-HSA-2161538", "R-HSA-2220966", "R-HSA-2220971",
                "R-HSA-2220979", "R-HSA-2220981", "R-HSA-2220985", "R-HSA-2243942", "R-HSA-2263490", "R-HSA-2263492",
                "R-HSA-2265534", "R-HSA-2316352", "R-HSA-2396007", "R-HSA-2404137", "R-HSA-2429643", "R-HSA-2453855",
                "R-HSA-2466710", "R-HSA-2485180", "R-HSA-2514891", "R-HSA-2581474", "R-HSA-2584246", "R-HSA-2666278",
                "R-HSA-2682349", "R-HSA-2855020", "R-HSA-2872444", "R-HSA-2889070", "R-HSA-2993780", "R-HSA-3000122",
                "R-HSA-3095901", "R-HSA-3149432", "R-HSA-3215391", "R-HSA-3229118", "R-HSA-3257122", "R-HSA-3274540",
                "R-HSA-3325546", /*"R-HSA-3645786", "R-HSA-3656517", "R-HSA-3656523", "R-HSA-3702153", "R-HSA-3702184",
                "R-HSA-3713560", */ "R-HSA-3790130", "R-HSA-3790137", "R-HSA-3791349", "R-HSA-3814838", "R-HSA-3828061",
                "R-HSA-3858506", "R-HSA-4419979", "R-HSA-4419986", "R-HSA-4549334", "R-HSA-4549368", "R-HSA-4549382",
                "R-HSA-4551297", "R-HSA-4655355", "R-HSA-4717406", "R-HSA-4719354", "R-HSA-4719375", "R-HSA-4755572",
                "R-HSA-4755600", "R-HSA-5205661", "R-HSA-5205663", "R-HSA-5205681", "R-HSA-5213462", "R-HSA-5228811",
                /*"R-HSA-5250579",*/ "R-HSA-5251989", "R-HSA-5357845", "R-HSA-5358460", "R-HSA-5362450", "R-HSA-5362459",
                "R-HSA-5387386", "R-HSA-5387389", "R-HSA-5387392", "R-HSA-5483229", "R-HSA-5483238", "R-HSA-5578663",
                "R-HSA-5602383", "R-HSA-5602549", "R-HSA-5602606", "R-HSA-5602885", "R-HSA-5607838", "R-HSA-5610727",
                "R-HSA-5615556", "R-HSA-5615604", "R-HSA-5617096", "R-HSA-5617820", "R-HSA-5623513", "R-HSA-5623521",
                "R-HSA-5624256", "R-HSA-5625015", "R-HSA-5625029", "R-HSA-5625210", "R-HSA-5625574", "R-HSA-5625841",
                "R-HSA-5626270", "R-HSA-5627737", "R-HSA-5632668", "R-HSA-5633241", "R-HSA-5637795", "R-HSA-5637796",
                "R-HSA-5638014", "R-HSA-5638137", "R-HSA-5651942", "R-HSA-5652099", "R-HSA-5653622", "R-HSA-5654544",
                "R-HSA-5654545", "R-HSA-5655243", "R-HSA-5655268", "R-HSA-5655270", "R-HSA-5655278", "R-HSA-5655284",
                "R-HSA-5655301", "R-HSA-5655341", "R-HSA-5655702", "R-HSA-5655760", "R-HSA-5656248", "R-HSA-5660890",
                "R-HSA-5660910", "R-HSA-5661184", "R-HSA-5661198", "R-HSA-5662662", "R-HSA-5668481", "R-HSA-5672027",
                "R-HSA-5678517", "R-HSA-5678706", "R-HSA-5678749", "R-HSA-5678822", "R-HSA-5678863", "R-HSA-5678992",
                "R-HSA-5679031", "R-HSA-5679041", "R-HSA-5679101", "R-HSA-5679145", "R-HSA-5682285", "R-HSA-5682311",
                "R-HSA-5683113", "R-HSA-5683209", "R-HSA-5683355", "R-HSA-5683672", "R-HSA-5683714", "R-HSA-5688025",
                "R-HSA-5688397", "R-HSA-5690340", "R-HSA-5692462", "R-HSA-5692480", "R-HSA-6785668", "R-HSA-6785722",
                "R-HSA-6787403", "R-HSA-6791221", "R-HSA-6791223", "R-HSA-6797568", "R-HSA-6802834", "R-HSA-6802910",
                "R-HSA-6802911", "R-HSA-6802912", "R-HSA-6802914", "R-HSA-6802918", "R-HSA-6802919", "R-HSA-6802921",
                "R-HSA-6802922", "R-HSA-6802925", "R-HSA-6802926", "R-HSA-6802927", "R-HSA-6802930", "R-HSA-6802932",
                "R-HSA-6802933", "R-HSA-6802934", "R-HSA-6802935", "R-HSA-6802938", "R-HSA-6802942", "R-HSA-6802943",
                "R-HSA-6803545", "R-HSA-6814559", "R-HSA-8849353", "R-HSA-8851710", "R-HSA-8852552", "R-HSA-8853309",
                "R-HSA-8853313", "R-HSA-8853315", "R-HSA-8853317", "R-HSA-8853319", "R-HSA-8853320", "R-HSA-8853322",
                "R-HSA-8853325", "R-HSA-8854908", "R-HSA-8855062", "R-HSA-8856808", "R-HSA-8863101", "R-HSA-8865275",
                "R-HSA-8865276", "R-HSA-8866856", "R-HSA-8867876", "R-HSA-8869241", "R-HSA-8877620", "R-HSA-8948146",
                "R-HSA-8948832", "R-HSA-8949609", "R-HSA-8949687", "R-HSA-8949688", "R-HSA-8949703", "R-HSA-8951850",
                "R-HSA-8982640", "R-HSA-9006323", "R-HSA-9013069", "R-HSA-9013533", "R-HSA-9015379", "R-HSA-9036025",
                "R-HSA-9036056", "R-HSA-9603534",
                "R-MMU-8851711", "R-MMU-9005872",
                "R-NUL-9005752", "R-NUL-9606338"
        ));
        test(identifiers);
        // TODO: 21/10/18 add resource
    }

    @Ignore
    public void testOddities(){
        Collection<String> identifiers = new LinkedHashSet<>(Arrays.asList(
                "R-HSA-4839638", "R-NUL-9606338"
        ));
        test(identifiers);
    }

    private void test(Collection<String> identifiers) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(TEST_IMAGES, "data.tsv")))) {
            System.out.println(identifiers.size() + " reactions");
            writer.write("Layout\tTest\tImage");
            writer.newLine();
            long elapsed = 0;
            for (String stId : identifiers) elapsed += convert(stId, writer);
            System.out.println();
            System.out.println(formatTime(elapsed));
            System.out.println(formatTime(elapsed / identifiers.size()));
            System.out.println("Total errors = " + total);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testPerformance() {
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
    }

    @Ignore
    @Test
    public void bruteForce() {
        String stId = "R-HSA-434650";
        try {
            ReactionLikeEvent rle = databaseObjectService.findById(stId);
            final String pStId = rle.getEventOf().isEmpty() ? stId : rle.getEventOf().get(0).getStId();

            final LayoutFactory layoutFactory = new LayoutFactory(ads);
            final Layout layout = layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.BRUTE_FORCE);
            final Diagram diagram = ReactionDiagramFactory.get(layout);

            final Graph graph = new ReactionGraphFactory(ads).getGraph(rle, layout);

            runTest(diagram, stId);
            saveImage(stId, pStId, diagram, graph, "svg");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(stId);
        }

    }



    private long convert(String stId, BufferedWriter writer) {
        return convert(stId, writer, true, true, false, false, false);
    }

    private long convert(String stId, BufferedWriter writer, boolean test, boolean svg, boolean json, boolean pptx, boolean sbgn) {
        final long start = System.nanoTime();
        try {
            ReactionLikeEvent rle = databaseObjectService.findById(stId);
            final String pStId = rle.getEventOf().isEmpty() ? stId : rle.getEventOf().get(0).getStId();

            final LayoutFactory layoutFactory = new LayoutFactory(ads);
            final Layout layout = layoutFactory.getReactionLikeEventLayout(rle, LayoutFactory.Style.BOX);
            final Diagram diagram = ReactionDiagramFactory.get(layout);

            final Graph graph = new ReactionGraphFactory(ads).getGraph(rle, layout);
            final long a = System.nanoTime();

            if (test) runTest(diagram, stId);
            final long b = System.nanoTime();
            if (json) printJsons(diagram, graph, layout);
            if (svg) saveImage(stId, pStId, diagram, graph, "svg");
            if (sbgn) saveSbgn(stId, diagram);
            if (pptx) savePptx(diagram);
            final long c = System.nanoTime();
            final long l = a - start;
            final long t = b - a;
            final long image = c - b;
            writer.write(String.format("%d\t%d\t%d\t", l, t, image));
            writer.newLine();
            return c - start;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(stId);
        }
        return System.nanoTime() - start;
    }

    private void saveImage(String stId, String pStId, Diagram diagram, Graph graph, String format) {
        try {
            final File file = new File(TEST_IMAGES, String.format("%s.%s", stId, format));
            OutputStream os = new FileOutputStream(file);
            RasterArgs args = new RasterArgs(pStId, format
            ).setQuality(8).setMargin(1);
            rasterExporter.export(diagram, graph, args, null, os);
        } catch (IOException | AnalysisException | TranscoderException e) {
            e.printStackTrace();
        }
    }

    private void runTest(Diagram diagram, String stId) {
        final DiagramTest test = new DiagramTest(diagram);
        test.runTests(stId);
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
