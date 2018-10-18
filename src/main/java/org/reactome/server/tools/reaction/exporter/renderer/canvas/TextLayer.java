package org.reactome.server.tools.reaction.exporter.renderer.canvas;

import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.renderer.utils.TextRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Lorente-Arencibia, Pascual (pasculorente@gmail.com)
 */
public class TextLayer extends CommonLayer {

	private List<RenderableText> objects = new LinkedList<>();

	public void add(String text, Position limits, Color color, double padding) {
		objects.add(new RenderableText(text, limits, padding, color));
		addShape(new Rectangle2D.Double(limits.getX(), limits.getY(), limits.getWidth(), limits.getHeight()));
	}

	public void add(String text, Coordinate coordinate, Color color) {
		objects.add(new RenderableText(text, coordinate, color));
	}

	@Override
	public void render(Graphics2D graphics) {
		objects.forEach(text -> {
			graphics.setPaint(text.color);
			if (text.position == null) {
				TextRenderer.draw(text.text, text.coordinate, graphics);
			} else {
				TextRenderer.draw(text.text, text.position, graphics, text.padding);
			}
		});
	}

	@Override
	public void clear() {
		super.clear();
		objects.clear();
	}

	private class RenderableText {

		private final String text;
		private final Coordinate coordinate;
		private final Position position;
		private double padding;
		private final Color color;

		RenderableText(String text, Position position, double padding, Color color) {
			this.text = text;
			this.position = position;
			this.padding = padding;
			this.color = color;
			this.coordinate = null;
		}

		RenderableText(String text, Coordinate coordinate, Color color) {
			this.text = text;
			this.coordinate = coordinate;
			this.color = color;
			this.position = null;
		}
	}

}
