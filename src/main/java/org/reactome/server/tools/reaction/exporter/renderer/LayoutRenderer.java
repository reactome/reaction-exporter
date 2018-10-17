package org.reactome.server.tools.reaction.exporter.renderer;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Layout;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class LayoutRenderer {

    public BufferedImage render(RenderArgs args, Layout layout) {
        final double factor = Math.sqrt(scale(args.getQuality()));
        final int width = (int) Math.ceil(factor * layout.getPosition().getWidth());
        final int height = (int) Math.ceil(factor * layout.getPosition().getHeight());
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        graphics.scale(factor, factor);
        for (CompartmentGlyph compartment : layout.getCompartments()) {
            graphics.setPaint(new Color(122, 122, 122, 122));
            final Position position = compartment.getPosition();
            graphics.fill(new Rectangle2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight()));
        }

        for (EntityGlyph entity : layout.getEntities()) {
            final Position position = entity.getPosition();
            final Rectangle2D.Double rect = new Rectangle2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight());
            graphics.setPaint(Color.RED);
            graphics.draw(rect);
            graphics.setPaint(Color.GREEN);
            graphics.fill(rect);
        }
        System.out.println(layout.getEntities().size() + " entities");

        return image;
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
