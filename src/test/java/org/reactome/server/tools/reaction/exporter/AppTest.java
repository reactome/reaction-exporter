package org.reactome.server.tools.reaction.exporter;

import org.apache.batik.transcoder.TranscoderException;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.tools.diagram.data.graph.Graph;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.diagram.exporter.common.analysis.AnalysisException;
import org.reactome.server.tools.diagram.exporter.raster.RasterExporter;
import org.reactome.server.tools.diagram.exporter.raster.api.RasterArgs;
import org.reactome.server.tools.reaction.exporter.diagram.ReactionDiagramFactory;
import org.reactome.server.tools.reaction.exporter.graph.ReactionGraphFactory;
import org.reactome.server.tools.reaction.exporter.layout.LayoutFactory;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Unit testing.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppTest extends BaseTest {

    @Autowired
    private DatabaseObjectService databaseObjectService;

    @Autowired
    private AdvancedDatabaseObjectService ads;

    private RasterExporter rasterExporter = new RasterExporter();

    @BeforeClass
    public static void setUpClass() {
        logger.info(" --- !!! Running " + AppTest.class.getName() + "!!! --- \n");
    }

    @Test
    public void findByDbIdTest() {

        ReactionLikeEvent rle = databaseObjectService.findById("R-HSA-5205661");

        RasterArgs args = new RasterArgs("png");

        final LayoutFactory layoutFactory = new LayoutFactory(ads);
        final Layout layout = layoutFactory.getReactionLikeEventLayout(rle);
        final Diagram diagram = ReactionDiagramFactory.get(layout);

        final ReactionGraphFactory graphFactory = new ReactionGraphFactory(ads);
        final Graph graph = graphFactory.getGraph(rle, layout);

        try {
            File file = new File("output.png");
            OutputStream os = new FileOutputStream(file);
            rasterExporter.export(diagram, graph, args, null, os);
        } catch (IOException | AnalysisException | TranscoderException e) {
            e.printStackTrace();
        }

    }

}
