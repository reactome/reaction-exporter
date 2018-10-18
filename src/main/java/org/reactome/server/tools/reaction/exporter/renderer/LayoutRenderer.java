package org.reactome.server.tools.reaction.exporter.renderer;

import org.reactome.server.tools.reaction.exporter.layout.common.RenderableClass;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.glyph.Renderer;
import org.reactome.server.tools.reaction.exporter.renderer.glyph.RendererFactory;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;
import org.reactome.server.tools.reaction.exporter.renderer.profile.ProfileFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LayoutRenderer {

    private static final Font DEFAULT_FONT = new Font("arial", Font.BOLD, 8);

    public BufferedImage render(RenderArgs args, Layout layout) {
        final DiagramProfile profile = ProfileFactory.get(args.getProfile());
        ImageCanvas canvas = toCanvas(layout, profile);

        final double factor = Math.sqrt(scale(args.getQuality()));
        final int width = (int) Math.ceil(factor * layout.getPosition().getWidth());
        final int height = (int) Math.ceil(factor * layout.getPosition().getHeight());
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        graphics.scale(factor, factor);
        graphics.setFont(DEFAULT_FONT);

        canvas.render(graphics);
        return image;
    }

    private ImageCanvas toCanvas(Layout layout, DiagramProfile profile) {
        ImageCanvas canvas = new ImageCanvas();
        final Renderer<CompartmentGlyph> renderer = RendererFactory.getRenderer(RenderableClass.COMPARTMENT);
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            renderer.draw(compartment, canvas, profile);
        }
        for (EntityGlyph entity : layout.getEntities()) {
            RendererFactory.getRenderer(entity.getRenderableClass()).draw(entity, canvas, profile);
        }
        RendererFactory.getRenderer(layout.getReaction().getRenderableClass()).draw(layout.getReaction(), canvas, profile);
        return canvas;
    }

    private double scale(int quality) {
        if (quality < 5) {
            return interpolate(quality, 1, 5, 0.1, 1);
        } else return interpolate(quality, 5, 10, 1, 3);
    }

    private double interpolate(double x, double min, double max, double dest_min, double dest_max) {
        return (x - min) / (max - min) * (dest_max - dest_min) + dest_min;
    }
}
