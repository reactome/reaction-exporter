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
		final int width = (int) layout.getPosition().getWidth() + 1;
		final int height = (int) layout.getPosition().getHeight() + 1;
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
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

		return image;
	}
}
