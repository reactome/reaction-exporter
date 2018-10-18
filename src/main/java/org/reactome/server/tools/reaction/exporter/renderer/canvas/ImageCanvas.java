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

    private FillLayer attachmentFill = new FillLayer();
    private DrawLayer attachmentBorder = new DrawLayer();
    private TextLayer attachmentText = new TextLayer();

    private DrawLayer segments = new DrawLayer();

    private final Collection<Layer> layers = Arrays.asList(
            compartmentFill,
            compartmentBorder,
            compartmentText,

            segments,

            nodeFill,
            nodeBorder,
            nodeText,

            attachmentFill,
            attachmentBorder,
            attachmentText
    );

    public void render(Graphics2D graphics) {
        DrugHelper.setGraphics2D(graphics);
        layers.forEach(layer -> layer.render(graphics));
    }

    public DrawLayer getNodeBorder() {
        return nodeBorder;
    }

    public FillLayer getNodeFill() {
        return nodeFill;
    }

    public TextLayer getNodeText() {
        return nodeText;
    }

    public FillLayer getCompartmentFill() {
        return compartmentFill;
    }

    public DrawLayer getCompartmentBorder() {
        return compartmentBorder;
    }

    public TextLayer getCompartmentText() {
        return compartmentText;
    }

    public FillLayer getAttachmentFill() {
        return attachmentFill;
    }

    public DrawLayer getAttachmentBorder() {
        return attachmentBorder;
    }

    public TextLayer getAttachmentText() {
        return attachmentText;
    }

    public DrawLayer getSegments() {
        return segments;
    }

}
