package org.reactome.server.tools.reaction.exporter.renderer.canvas;

import org.reactome.server.tools.reaction.exporter.renderer.glyph.entity.DrugHelper;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;

public class ImageCanvas {

    private FillLayer compartmentFill = new FillLayer();
    private DrawLayer compartmentBorder = new DrawLayer();
    private TextLayer compartmentText = new TextLayer();

    private FillLayer nodeFill = new FillLayer();
    private DrawLayer nodeBorder = new DrawLayer();
    private TextLayer nodeText = new TextLayer();

    private final Collection<Layer> layers = Arrays.asList(
            compartmentFill,
            compartmentBorder,
            compartmentText,

            nodeFill,
            nodeBorder,
            nodeText
    );

    public void render(Graphics2D graphics) {
        DrugHelper.setGraphics2D(graphics);
        layers.forEach(layer -> layer.render(graphics));
    }

    public DrawLayer getNodeBorderLayer() {
        return nodeBorder;
    }

    public FillLayer getNodeFillLayer() {
        return nodeFill;
    }

    public TextLayer getNodeTextLayer() {
        return nodeText;
    }

    public FillLayer getCompartmentFillLayer() {
        return compartmentFill;
    }

    public DrawLayer getCompartmentBorderLayer() {
        return compartmentBorder;
    }

    public TextLayer getCompartmentTextLayer() {
        return compartmentText;
    }
}
