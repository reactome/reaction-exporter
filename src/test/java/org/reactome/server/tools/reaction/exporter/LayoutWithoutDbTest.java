package org.reactome.server.tools.reaction.exporter;

import org.apache.batik.transcoder.TranscoderException;
import org.junit.jupiter.api.Test;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.diagram.exporter.common.analysis.AnalysisException;
import org.reactome.server.tools.diagram.exporter.raster.RasterExporter;
import org.reactome.server.tools.diagram.exporter.raster.api.RasterArgs;
import org.reactome.server.tools.reaction.exporter.diagram.ReactionDiagramFactory;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.box.BoxAlgorithm;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.layout.model.Role;
import org.reactome.server.tools.reaction.exporter.ontology.GoTreeFactory.Source;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LayoutWithoutDbTest extends BaseTest {

    Logger logger = Logger.getLogger(LayoutWithoutDbTest.class.getName());

    private RasterExporter rasterExporter = new RasterExporter();

    long idCounter = 1;

    @Test
    public void testCreateLayoutWithoutNeo4jDatabase() throws Throwable {
        try {
            // create test reaction
            Compartment childCompartment = createCompartment("0005829", "child-compartment");
            Compartment parentCompartment = createCompartment("0005886", "parent-compartment");

            ReactionLikeEvent rle = new Reaction();

            SimpleEntity input1 = createEntity("in", parentCompartment);

            SimpleEntity input2 = createEntity("in2", childCompartment);

            rle.setInput(Arrays.asList(input1, input2));

            SimpleEntity modifier = createEntity("cat", childCompartment);

            CatalystActivity catalystActivity = new CatalystActivity();
            catalystActivity.setPhysicalEntity(modifier);
            rle.setCatalystActivity(Arrays.asList(catalystActivity));
            rle.setCompartment(Arrays.asList(childCompartment));

            SimpleEntity output = createEntity("out", parentCompartment);
            rle.setOutput(Arrays.asList(output));

            // prepare layout object without filling layout information
            Layout layout = new Layout(Source.GO);
            List<EntityGlyph> glyphs = new ArrayList<>();

            glyphs.add(createEntityGlyph(input1, "input"));
            glyphs.add(createEntityGlyph(input2, "input"));
            glyphs.add(createEntityGlyph(modifier, "catalyst"));

            glyphs.add(createEntityGlyph(output, "output"));

            layout.setParticipants(glyphs);
            layout.setReactionLikeEvent(rle);

            String stId = "artifitial";

            // compute layout
            new BoxAlgorithm(layout).compute();

            // check if we can save layout without problems
            Diagram diagram = ReactionDiagramFactory.get(layout);

            saveImage(stId, stId, diagram, "png");
            assertTrue(new File(stId + ".png").exists());
            // Desktop.getDesktop().open(new File(stId + ".png"));
            new File(stId + ".png").delete();
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private EntityGlyph createEntityGlyph(SimpleEntity entity, String type) {
        EntityGlyph inputGlyph1 = new EntityGlyph();
        inputGlyph1.setPhysicalEntity(entity);
        inputGlyph1.setDashed(false);

        inputGlyph1.getRoles().add(createRole(type));
        return inputGlyph1;
    }

    private Role createRole(String type) {
        Role inputRole = new Role();
        inputRole.setType(type);
        inputRole.setStoichiometry(1);
        return inputRole;
    }

    private SimpleEntity createEntity(String name, Compartment compartment) {
        long id = idCounter++;
        SimpleEntity input2 = new SimpleEntity();
        input2.setDbId(id);
//        input2.setId(id);
        input2.setStId(id + "");
        input2.setCompartment(Arrays.asList(compartment));
        input2.setName(Arrays.asList(name));
        input2.setInDisease(false);
        return input2;
    }

    private Compartment createCompartment(String accession, String name) {
        long id = idCounter++;
        Compartment compartment = new Compartment();
        compartment.setAccession(accession);
        compartment.setName(name);
        compartment.setDatabaseName("GO");
//        compartment.setId(id);
        compartment.setStId(id + "");
        compartment.setDisplayName(name);
        return compartment;
    }

    private void saveImage(String stId, String pStId, Diagram diagram, String format) {
        try {
            final File file = new File(String.format("%s.%s", stId, format));
            OutputStream os = new FileOutputStream(file);
            RasterArgs args = new RasterArgs(pStId, format).setQuality(8).setMargin(1);
            rasterExporter.export(diagram, null, args, null, os);
        } catch (IOException | AnalysisException | TranscoderException e) {
            e.printStackTrace();
        }
    }

}