package org.reactome.server.tools.reaction.exporter.layout.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.renderer.utils.ShapeFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Connector belongs to Nodes, so that is the reason why edge is needed. A connector points to
 * the edge backbone (either the first, last or middle point) so we keep the edge instead
 * 1) Memory usage is lower
 * 2) If a node is not meant to be drawn (note this is different of not being in the viewport)
 * we have a way to detect it and easily avoid drawing it
 * <p>
 * Connector.Type is kept to distinguish whether it ends with arrow, or other kind of shapes
 * <p>
 * Connectors are further divided into segments, so the math to detect point in segment is
 * moved one layer down. Segments start from the node to the backbone
 * <p>
 * Connector also includes the shape (arrow, circle, etc.). The points of the shapes are
 * calculated server-side to avoid the cost of processing at the client
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Connector {

    private List<Segment> segments = new ArrayList<>();

    private Shape shape;
    private Shape stoichiometry;

    public List<Segment> getSegments() {
        return segments;
    }

    public Shape getShape() {
        return shape;
    }

    public Shape getStoichiometry() {
        return stoichiometry;
    }

    public void setPointer(EntityRole type) {
        if (segments.size() > 0) {
            Segment segment;
            List<Coordinate> points;
            switch (type) {
                case INPUT:
                    return;
                case OUTPUT:
                    // Use the first segment of the Connector - closer to the node
                    // IMPORTANT!!! Segments start from the node and point to the backbone
                    segment = segments.get(0);
                    points = ShapeFactory.createArrow(
                            segment.getFrom().getX(),
                            segment.getFrom().getY(),
                            segment.getTo().getX(),
                            segment.getTo().getY());
                    // Shape is a filled arrow
                    this.shape = new Shape(points.get(0), points.get(1), points.get(2), Boolean.FALSE, Shape.Type.ARROW);
                    break;
                case CATALYST:
                    // Use the last segment of the Connector - closer to the edge (reaction)
                    segment = segments.get(segments.size() - 1);
                    // Adjust the position of the segment to have a distance from the reaction position
                    Integer radius = Math.round((float) (ShapeFactory.EDGE_MODULATION_WIDGET_WIDTH / 2.0d));
                    Coordinate centre = calculateEndpoint(segment, getDistanceForEndpoint(type));
                    segments.remove(segment);
                    segments.add(new Segment(segment.getFrom(), calculateEndpoint(segment, getDistanceForEndpoint(type, radius))));
                    // Shape is an empty circle
                    this.shape = new Shape(centre, radius, Boolean.TRUE, Shape.Type.CIRCLE);
                    break;
                case NEGATIVE_REGULATOR:
                    // Use the last segment of the Connector - closer to the edge (reaction)
                    segment = segments.get(segments.size() - 1);
                    // Adjust the position of the segment to have a distance from the reaction position
                    segments.remove(segment);
                    segment = new Segment(segment.getFrom(), calculateEndpoint(segment, getDistanceForEndpoint(type)));
                    segments.add(segment);
                    points = ShapeFactory.createStop(
                            segment.getTo().getX(),
                            segment.getTo().getY(),
                            segment.getFrom().getX(),
                            segment.getFrom().getY());
                    // Shape is a stop sign
                    this.shape = new Shape(points.get(0), points.get(1), points.get(2), Boolean.FALSE, Shape.Type.STOP);
                    break;
                case POSITIVE_REGULATOR:
                    // Use the last segment of the Connector - closer to the edge (reaction)
                    segment = segments.get(segments.size() - 1);
                    segments.remove(segment);
                    segment = new Segment(segment.getFrom(), calculateEndpoint(segment, getDistanceForEndpoint(type)));
                    segments.add(segment);
                    points = ShapeFactory.createArrow(
                            segment.getTo().getX(),
                            segment.getTo().getY(),
                            segment.getFrom().getX(),
                            segment.getFrom().getY());
                    // Shape is an empty arrow
                    this.shape = new Shape(points.get(0), points.get(1), points.get(2), Boolean.TRUE, Shape.Type.ARROW);
                    break;
            }
        }
    }

    /**
     * Calculates the end point on the segment
     */
    private Coordinate calculateEndpoint(Segment segment, double dist) {
        // Point used to calculate the angle of the segment
        double controlX = segment.getFrom().getX();
        double controlY = segment.getFrom().getY();
        // Point to replace
        double oldX = segment.getTo().getX();
        double oldY = segment.getTo().getY();

        // Remember: the y axis is contrary to the ordinary coordinate system
        double tan = (controlY - oldY) / (controlX - oldX);
        double theta = Math.atan(tan);
        if (controlX - oldX < 0)
            theta += Math.PI;
        double x = oldX + dist * Math.cos(theta);
        double y = oldY + dist * Math.sin(theta);

        return new Coordinate(Math.round((float) x), Math.round((float) y));
    }

    private double getDistanceForEndpoint(EntityRole connectRole) {
        return getDistanceForEndpoint(connectRole, 0);
    }

    private double getDistanceForEndpoint(EntityRole connectRole, Integer radius) {
        if (connectRole == EntityRole.CATALYST) {
            return (ShapeFactory.EDGE_TYPE_WIDGET_WIDTH + ShapeFactory.EDGE_MODULATION_WIDGET_WIDTH) * 0.6 + radius;
        } else {
            return ShapeFactory.EDGE_TYPE_WIDGET_WIDTH * 0.75;
        }
    }

    /**
     * Calculates the position of the stoichiometry box.
     * However, there are cases where the node is attached directly on the
     * backbone without any connector segments. In this case, we place the
     * stoichiometry box on the backbone segments of the edge.
     */
    public void setStoichiometry(int stoichiometry) {
        if (stoichiometry > 1) {
            final Segment segment = this.segments.get(0);

            this.stoichiometry = ShapeFactory.createStoichiometryBox(
                    setStoichiometryPosition(segment.getFrom(), segment.getTo()),
                    String.valueOf(stoichiometry)
            );
            this.stoichiometry.setS(String.valueOf(stoichiometry));
        }
    }

    /**
     * Calculate the position of Stoichiometry box on the segments.
     */
    private Coordinate setStoichiometryPosition(Coordinate from, Coordinate to) {
        return new Coordinate(0.5 * (from.getX() + to.getX()), 0.5 * (from.getY() + to.getY()));
    }


}
