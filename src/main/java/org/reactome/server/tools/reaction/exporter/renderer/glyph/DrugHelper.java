package org.reactome.server.tools.reaction.exporter.renderer.glyph;

import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;
import org.reactome.server.tools.reaction.exporter.renderer.canvas.ImageCanvas;
import org.reactome.server.tools.reaction.exporter.renderer.profile.DiagramProfile;

import java.awt.*;

/**
 * Adds a method for drug classes to render the Rx text.
 * <p><b>Note:</b> Since all Renderable* classes already extend another class
 * (at least {@link DefaultEntityRenderer}), we cannot create a RenderableDrug class with the
 * {@link Renderer#draw(Glyph, ImageCanvas, DiagramProfile)} overridden, that's why we
 * implemented this helper class.
 */
public class DrugHelper {

	private static final String RX = "Rx";

	private DrugHelper() {
	}

	// We cannot know the size of the text by just using the font, since it's graphics2D implementation dependent.
	private static double WIDTH = 15;
	private static double HEIGHT = 10;

	/**
	 * Updates the graphics2D information. This method should be call once per diagram.
	 *
	 * @param graphics2D current graphics2D
	 */
	public static void setGraphics2D(Graphics2D graphics2D) {
		final FontMetrics metrics = graphics2D.getFontMetrics();
		HEIGHT = metrics.getHeight() + 2;
		WIDTH = metrics.charsWidth(RX.toCharArray(), 0, RX.length()) + 2;
	}

	/**
	 * Adds a Rx text in the bottom right corner of the node.
	 *  @param node          the node where to place the text
	 * @
	 * @param color
	 * @param xOff          distance between the text and the right margin of the node
	 * @param yOff          distance between the text and the bottom margin of the node
	 */
	static void addDrugText(EntityGlyph node, ImageCanvas canvas, Color color, double xOff, double yOff) {
		final Position position = new Position();
		position.setX(node.getPosition().getMaxX() - WIDTH - xOff);
		position.setY(position.getMaxY() - HEIGHT - yOff);
		canvas.getNodeTextLayer().add(RX, position, color, 0);
	}

}
