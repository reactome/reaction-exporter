package org.reactome.server.tools.reaction.exporter.renderer.utils;

import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.Segment;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Convenient place to find no so common shapes.
 *
 * @author Lorente-Arencibia, Pascual (pasculorente@gmail.com)
 */
public class ShapeFactory {

    private static final double COMPLEX_RECT_ARC_WIDTH = 6;
    private static final double ROUND_RECT_ARC_WIDTH = 8;
    private static final double RNA_LOOP_WIDTH = 16;
    private static final double GENE_SYMBOL_Y_OFFSET = 50;
    private static final double GENE_SYMBOL_PAD = 4;
    private static final double ARROW_LENGTH = 8;
    private static final double ENCAPSULATED_TANH = Math.tanh(Math.PI / 9);

    public static final double ARROW_ANGLE = Math.PI / 6;
    public static final int EDGE_TYPE_WIDGET_WIDTH = 12;
    public static final int CIRCLE_WIDGET_CORRECTION = 1;
    public static final int EDGE_MODULATION_WIDGET_WIDTH = 8;
    /**
     * Creates a rectangle with edged corners (an octagon)
     *
     * @param x      top left x coordinate
     * @param y      top left y coordinate
     * @param width  width
     * @param height height
     * @return an edged rectangle
     */
    public static Shape getCornedRectangle(double x, double y, double width, double height) {
        final double corner = COMPLEX_RECT_ARC_WIDTH;
        final int[] xs = new int[]{
                (int) (x + corner),
                (int) (x + width - corner),
                (int) (x + width),
                (int) (x + width),
                (int) (x + width - corner),
                (int) (x + corner),
                (int) x,
                (int) x,
                (int) (x + corner)
        };
        final int[] ys = new int[]{
                (int) y,
                (int) y,
                (int) (y + corner),
                (int) (y + height - corner),
                (int) (y + height),
                (int) (y + height),
                (int) (y + height - corner),
                (int) (y + corner),
                (int) y
        };
        return new Polygon(xs, ys, xs.length);
    }

    /**
     * Creates the shape of the gene fill, a bottom rounded rectangle.
     *
     * @return the gene fill shape
     */
    public static Shape getGeneFillShape(Position prop) {
        final GeneralPath path = new GeneralPath();
        final double y1 = prop.getY() + 0.5 * GENE_SYMBOL_Y_OFFSET;
        final double bottom = (double) prop.getY() + prop.getHeight();
        final double arcWidth = ROUND_RECT_ARC_WIDTH;
        final double right = (double) prop.getX() + prop.getWidth();
        path.moveTo(prop.getX(), y1);
        path.lineTo(right, y1);
        path.lineTo(right, bottom - arcWidth);
        path.quadTo(right, bottom, right - arcWidth, bottom);
        path.lineTo(prop.getX() + arcWidth, bottom);
        path.quadTo(prop.getX(), bottom, prop.getX(), bottom - arcWidth);
        path.closePath();
        return path;
    }

    /**
     * Returns a path with three perpendicular lines.
     */
    public static Shape getGeneLine(Position prop) {
        final double y1 = prop.getY() + 0.5 * GENE_SYMBOL_Y_OFFSET;
        final double maxX = prop.getX() + prop.getWidth();
        final Path2D path = new GeneralPath();
        // Horizontal line
        path.moveTo(prop.getX(), y1);
        path.lineTo(maxX, y1);
        // Vertical line
        final double x1 = maxX - GENE_SYMBOL_PAD;
        path.moveTo(x1, y1);
        path.lineTo(x1, prop.getY());
        // another very short horizontal line
        path.lineTo(maxX, prop.getY());
        return path;
    }

    /**
     * Creates the arrow shape of a gene.
     *
     * @return the gene arrow
     */
    public static Shape getGeneArrow(Position prop) {
        final double maxX = prop.getX() + prop.getWidth();
        final double arrowX = maxX + ARROW_LENGTH;
        final double ay = prop.getY() + 0.5 * ARROW_LENGTH;
        final double by = prop.getY() - 0.5 * ARROW_LENGTH;
        final Path2D triangle = new GeneralPath();
        triangle.moveTo(arrowX, prop.getY());
        triangle.lineTo(maxX, ay);
        triangle.lineTo(maxX, by);
        triangle.closePath();
        return triangle;
    }

    public static Shape getRoundedRectangle(Position properties) {
        return getRoundedRectangle(properties.getX(), properties.getY(),
                properties.getWidth(), properties.getHeight());
    }

    public static Shape getRoundedRectangle(double x, double y, double width, double height) {
        return new RoundRectangle2D.Double(
                x,
                y,
                width,
                height,
                ROUND_RECT_ARC_WIDTH,
                ROUND_RECT_ARC_WIDTH);
    }

    public static Shape getRoundedRectangle(Position prop, double padding) {
        return getRoundedRectangle(prop.getX(), prop.getY(),
                prop.getWidth(), prop.getHeight(), padding);
    }

    private static Shape getRoundedRectangle(double x, double y, double width, double height, double padding) {
        return new RoundRectangle2D.Double(
                x + padding,
                y + padding,
                width - 2 * padding,
                height - 2 * padding,
                ROUND_RECT_ARC_WIDTH,
                ROUND_RECT_ARC_WIDTH);
    }

//	/**
//	 * Returns a list of java.awt.shapes that make up the reactome Shape.
//	 * Although most of the shapes are unique, the double circle returns two
//	 * circles.
//	 *
//	 * @param shape reactome shape
//	 *
//	 * @return a list of java shapes
//	 */
//	public static Shape getShape(org.reactome.server.tools.diagram.data.layout.Shape shape) {
//		switch (shape.getType()) {
//			case "ARROW":
//				return (arrow(shape));
//			case "BOX":
//				return (box(shape));
//			case "CIRCLE":
//			case "DOUBLE_CIRCLE":
//				return (circle(shape));
//			case "STOP":
//				return (stop(shape));
//			default:
//				throw new RuntimeException("Do not know shape " + shape.getType());
//		}
//	}
//
//	private static Shape arrow(org.reactome.server.tools.diagram.data.layout.Shape shape) {
//		final int[] xs = new int[]{
//				shape.getA().getX().intValue(),
//				shape.getB().getX().intValue(),
//				shape.getC().getX().intValue()
//		};
//		final int[] ys = new int[]{
//				shape.getA().getY().intValue(),
//				shape.getB().getY().intValue(),
//				shape.getC().getY().intValue()
//		};
//		return new Polygon(xs, ys, xs.length);
//	}
//
//	private static Shape box(org.reactome.server.tools.diagram.data.layout.Shape shape) {
//		return new Rectangle2D.Double(
//				shape.getA().getX(),
//				shape.getA().getY(),
//				shape.getB().getX() - shape.getA().getX(),
//				shape.getB().getY() - shape.getA().getY());
//	}
//
//	private static Shape circle(org.reactome.server.tools.diagram.data.layout.Shape shape) {
//		final double x = shape.getC().getX() - shape.getR();
//		final double y = shape.getC().getY() - shape.getR();
//		return new Ellipse2D.Double(
//				x,
//				y,
//				2 * shape.getR(),
//				2 * shape.getR());
//	}
//
//	public static Shape innerCircle(org.reactome.server.tools.diagram.data.layout.Shape shape) {
//		final double x = shape.getC().getX() - shape.getR1();
//		final double y = shape.getC().getY() - shape.getR1();
//		return new Ellipse2D.Double(
//				x,
//				y,
//				2 * shape.getR1(),
//				2 * shape.getR1()
//		);
//	}
//
//	private static Shape stop(org.reactome.server.tools.diagram.data.layout.Shape shape) {
//		return new Line2D.Double(
//				shape.getA().getX(),
//				shape.getA().getY(),
//				shape.getB().getX(),
//				shape.getB().getY()
//		);
//	}
//
//	public static Shape line(Coordinate from, Coordinate to) {
//		return new Line2D.Double(from.getX(), from.getY(), to.getX(), to.getY());
//	}

    public static List<Shape> cross(Position properties) {
        return Arrays.asList(
                new Line2D.Double(
                        properties.getX(),
                        properties.getY(),
                        properties.getX() + properties.getWidth(),
                        properties.getY() + properties.getHeight()),
                new Line2D.Double(
                        properties.getX(),
                        properties.getY() + properties.getHeight(),
                        properties.getX() + properties.getWidth(),
                        properties.getY())
        );
    }

    public static Shape rectangle(Position prop, double padding) {
        return new Rectangle2D.Double(prop.getX() + padding,
                prop.getY() + padding,
                prop.getWidth() - 2 * padding,
                prop.getHeight() - 2 * padding);

    }

    public static Shape rectangle(Position prop) {
        return new Rectangle2D.Double(prop.getX(), prop.getY(), prop.getWidth(), prop.getHeight());
    }

    public static Shape getRnaShape(Position prop) {
        final double xoffset = RNA_LOOP_WIDTH;
        final double yoffset = 0.5 * RNA_LOOP_WIDTH;
        double x = prop.getX();
        double maxX = prop.getX() + prop.getWidth();
        double x1 = x + xoffset;
        double x2 = maxX - xoffset;

        double y = prop.getY();
        double centerY = prop.getY() + 0.5 * prop.getHeight();
        double maxY = prop.getY() + prop.getHeight();
        double y1 = prop.getY() + yoffset;
        double y2 = maxY - yoffset;

        final Path2D path = new GeneralPath();
        path.moveTo(x1, y1);
        path.lineTo(x2, y1);
        path.quadTo(maxX, y, maxX, centerY);
        path.quadTo(maxX, maxY, x2, y2);
        path.lineTo(x1, y2);
        path.quadTo(x, maxY, x, centerY);
        path.quadTo(x, y, x1, y1);
        path.closePath();
        return path;
    }

    public static Shape hexagon(Position prop) {
        return hexagon(prop, 0);
    }

    public static Shape hexagon(Position prop, double padding) {
        final double x = prop.getX() + padding;
        final double y = prop.getY() + padding;
        final double maxX = x + prop.getWidth() - 2 * padding;
        final double maxY = y + prop.getHeight() - 2 * padding;
        final double height = maxY - y;

        final double corner = height * 0.5 * ENCAPSULATED_TANH;
        final double x1 = x + corner;
        final double x2 = maxX - corner;
        final double centerY = y + 0.5 * (maxY - y);

        final Path2D path2D = new GeneralPath();
        path2D.moveTo(x, centerY);
        path2D.lineTo(x1, y);
        path2D.lineTo(x2, y);
        path2D.lineTo(maxX, centerY);
        path2D.lineTo(x2, maxY);
        path2D.lineTo(x1, maxY);
        path2D.closePath();
        return path2D;
    }

    public static Shape getCornedRectangle(Position position) {
        return getCornedRectangle(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }

    public static Shape getOval(Position position) {
        return new Ellipse2D.Double(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }

    public static Shape getOval(Position position, int padding) {
        return new Ellipse2D.Double(position.getX() + padding, position.getY() + padding, position.getWidth() - 2 * padding, position.getHeight() - 2 * padding);
    }

    public static Shape getLine(Segment segment) {
        return new Line2D.Double(segment.getFrom().getX(), segment.getFrom().getY(), segment.getTo().getX(), segment.getTo().getY());
    }


    public static List<Coordinate> createArrow(
            double arrowPositionX,
            double arrowPositionY,
            double controlX,
            double controlY){
        // IMPORTANT!!! Segments start from the node and point to the backbone
        // Point used to calculate the angle of the arrow
        // Point used to place the  arrow

        double arrowLength = ARROW_LENGTH;
        double arrowAngle = ARROW_ANGLE;

        List<Coordinate> rtn = new LinkedList<>();

        // Get the angle of the line segment
        double alpha = Math.atan((arrowPositionY - controlY) / (arrowPositionX - controlX) );
        if (controlX > arrowPositionX)
            alpha += Math.PI;
        double angle = arrowAngle - alpha;
        float x1 = (float)(arrowPositionX - arrowLength * Math.cos(angle));
        float y1 = (float)(arrowPositionY + arrowLength * Math.sin(angle));
        rtn.add( new Coordinate(Math.round(x1), Math.round(y1)));
        // The tip of the arrow is the end of the segment
        rtn.add( new Coordinate(Math.round((float)arrowPositionX), Math.round((float)arrowPositionY)));

        angle = arrowAngle + alpha;
        float x2 = (float)(arrowPositionX - arrowLength * Math.cos(angle));
        float y2 = (float)(arrowPositionY - arrowLength * Math.sin(angle));
        rtn.add( new Coordinate(Math.round( x2), Math.round( y2)));
        return rtn;
    }

    public static List<Coordinate> createStop(
            double anchorX,
            double anchorY,
            double controlX,
            double controlY){

        List<Coordinate> rtn = new LinkedList<>();

        double deltaY = anchorY - controlY;
        double deltaX = controlX - anchorX;

        double angle = deltaY == 0 ? Math.PI / 2 : Math.atan(-deltaX/deltaY);
        double x1 = anchorX - (Math.cos(angle) * EDGE_MODULATION_WIDGET_WIDTH / 2.0d);
        double x2 = anchorX + (Math.cos(angle) * EDGE_MODULATION_WIDGET_WIDTH / 2.0d);
        double y1 = anchorY + (Math.sin(angle) * EDGE_MODULATION_WIDGET_WIDTH / 2.0d);
        double y2 = anchorY - (Math.sin(angle) * EDGE_MODULATION_WIDGET_WIDTH / 2.0d);

        rtn.add( new Coordinate(Math.round((float)x1), Math.round( (float)y1)));
        rtn.add( new Coordinate(Math.round((float)x2), Math.round( (float)y2)));
        rtn.add( new Coordinate(Math.round((float)anchorX), Math.round( (float)anchorY)));

        return rtn;
    }
    public static org.reactome.server.tools.reaction.exporter.layout.model.Shape createStoichiometryBox(Coordinate boxCentre, String symbol){
        double width = -1;
        if(symbol!=null) { width = measureText(symbol).getWidth(); }
        if(width<EDGE_TYPE_WIDGET_WIDTH){ width = EDGE_TYPE_WIDGET_WIDTH; }
        Coordinate topLeft = new Coordinate(
                Math.round( (float) (boxCentre.getX() - width / 2.0)),
                Math.round( (float) (boxCentre.getY() - width / 2.0))
        );
        Coordinate bottomRight = new Coordinate(
                Math.round( (float) (topLeft.getX() + width)),
                Math.round( (float) (topLeft.getY() + width))
        );
        return new org.reactome.server.tools.reaction.exporter.layout.model.Shape(topLeft, bottomRight, Boolean.TRUE, org.reactome.server.tools.reaction.exporter.layout.model.Shape.Type.BOX);
    }

    private static Rectangle2D measureText(String text){
        //Create a dummy BufferedImage to get the Graphincs object
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        //Set the used font
        g2.setFont(new Font("Arial", Font.PLAIN, 8));
        Font font = g2.getFont();
        // get context from the graphics
        FontRenderContext context = g2.getFontRenderContext();
        return font.getStringBounds(text, context);
    }


    public static Shape from(org.reactome.server.tools.reaction.exporter.layout.model.Shape shape) {
        switch (shape.getType()) {
            case ARROW:
                return (arrow(shape));
            case BOX:
                return (box(shape));
            case CIRCLE:
            case DOUBLE_CIRCLE:
                return (circle(shape));
            case STOP:
                return (stop(shape));
            default:
                throw new RuntimeException("Do not know shape " + shape.getType());
        }
    }

    private static Shape arrow(org.reactome.server.tools.reaction.exporter.layout.model.Shape shape) {
        final int[] xs = new int[]{
                shape.getA().getX().intValue(),
                shape.getB().getX().intValue(),
                shape.getC().getX().intValue()
        };
        final int[] ys = new int[]{
                shape.getA().getY().intValue(),
                shape.getB().getY().intValue(),
                shape.getC().getY().intValue()
        };
        return new Polygon(xs, ys, xs.length);
    }

    private static Shape box(org.reactome.server.tools.reaction.exporter.layout.model.Shape shape) {
        return new Rectangle2D.Double(
                shape.getA().getX(),
                shape.getA().getY(),
                shape.getB().getX() - shape.getA().getX(),
                shape.getB().getY() - shape.getA().getY());
    }

    private static Shape circle(org.reactome.server.tools.reaction.exporter.layout.model.Shape shape) {
        final double x = shape.getC().getX() - shape.getR();
        final double y = shape.getC().getY() - shape.getR();
        return new Ellipse2D.Double(
                x,
                y,
                2 * shape.getR(),
                2 * shape.getR());
    }

    public static Shape innerCircle(org.reactome.server.tools.reaction.exporter.layout.model.Shape shape) {
        final double x = shape.getC().getX() - shape.getR1();
        final double y = shape.getC().getY() - shape.getR1();
        return new Ellipse2D.Double(
                x,
                y,
                2 * shape.getR1(),
                2 * shape.getR1()
        );
    }

    private static Shape stop(org.reactome.server.tools.reaction.exporter.layout.model.Shape shape) {
        return new Line2D.Double(
                shape.getA().getX(),
                shape.getA().getY(),
                shape.getB().getX(),
                shape.getB().getY()
        );
    }

}
