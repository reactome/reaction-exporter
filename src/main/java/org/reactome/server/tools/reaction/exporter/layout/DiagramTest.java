package org.reactome.server.tools.reaction.exporter.layout;

import org.reactome.server.tools.diagram.data.layout.*;
import org.reactome.server.tools.reaction.exporter.layout.common.CoordinateUtils;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.stream.Collectors;

public class DiagramTest {

    private final Map<Level, List<String>> logs = new EnumMap<>(Level.class);
    private final HashMap<Long, Compartment> compartmentIndex;
    private final Diagram diagram;
    private Level minLevel = Level.PASSED;
    private String name;
    private boolean first;

    public DiagramTest(Diagram diagram) {
        this.diagram = diagram;
        compartmentIndex = new HashMap<>();
        for (final Compartment compartment : diagram.getCompartments())
            compartmentIndex.put(compartment.getId(), compartment);
    }

    public void runTests(String name, Level minLevel) {
        this.name = name;
        this.minLevel = minLevel;
        this.first = true;
        notInCompartment();
        crossingSegments();
        manyChildren();
    }

    private void manyChildren() {
        for (final Compartment compartment : diagram.getCompartments()) {
            final long count = compartment.getComponentIds().stream().filter(compartmentIndex::containsKey).count();
            if (count > 3) log(Level.INFO, String.format("%s compartment in reaction %s (%s) has more than 3 children", compartment.getDisplayName(), diagram.getEdges().get(0).getId(), diagram.getEdges().get(0).getDisplayName()));
        }
    }

    private void notInCompartment() {
        for (final Compartment compartment : diagram.getCompartments()) {
            final Rectangle2D.Double box = toRectangle(compartment.getProp());
            for (final Node node : diagram.getNodes()) {
                testBounds(compartment, box, node);
            }
            for (final Edge edge : diagram.getEdges()) {
                testBounds(compartment, box, edge);
            }
        }
    }

    private void testBounds(Compartment compartment, Rectangle2D compartmentBox, DiagramObject object) {
        final boolean expected = isInside(compartment, object);
        final Rectangle2D.Double bounds = getBounds(object);
        final boolean intersects = compartmentBox.intersects(bounds);
        if (intersects == expected) {
            log(Level.PASSED, String.format("[%s %d %s] intersects with [compartment %d %s]",
                    object.getRenderableClass(), object.getId(), object.getDisplayName(),
                    compartment.getId(), compartment.getDisplayName()));
        } else if (intersects) {
            log(Level.ERROR, String.format("[%s %d %s] should not intersect with [compartment %d %s]",
                    object.getRenderableClass(), object.getId(), object.getDisplayName(), compartment.getId(),
                    compartment.getDisplayName()));
        } else { // !intersects && expected
            log(Level.ERROR, String.format("[%s %d %s] should intersect with [compartment %d %s]",
                    object.getRenderableClass(), object.getId(), object.getDisplayName(), compartment.getId(),
                    compartment.getDisplayName()));
        }
    }

    private void testSegments(Compartment compartment, Rectangle2D.Double box, Edge edge) {
        final boolean expected = isInside(compartment, edge);
        int i = 0;
        for (final Segment segment : edge.getSegments()) {
            final Line2D.Double line = toLine(segment);
            final boolean intersects = box.intersectsLine(line);
            if (intersects == expected) {
                log(Level.PASSED, String.format("[segment %d.%d %s] intersects with [compartment %d %s]",
                        edge.getId(), i, edge.getDisplayName(),
                        compartment.getId(), compartment.getDisplayName()));
            } else if (intersects) {
                log(Level.WARNING, String.format("[segment %d.%d %s] should not intersect with [compartment %d %s]",
                        edge.getId(), i, edge.getDisplayName(),
                        compartment.getId(), compartment.getDisplayName()));
            } else { // !intersects && expected
                log(Level.WARNING, String.format("[segment %d.%d %s] should intersect with [compartment %d %s]",
                        edge.getId(), i, edge.getDisplayName(),
                        compartment.getId(), compartment.getDisplayName()));
            }
            i++;
        }
    }

    private boolean isInside(Compartment compartment, DiagramObject node) {
        for (final Long id : compartment.getComponentIds()) {
            if (id.equals(node.getId())) return true;
            if (compartmentIndex.containsKey(id)) {
                if (isInside(compartmentIndex.get(id), node)) return true;
            }
        }
        return false;
    }

    private Rectangle2D.Double toRectangle(NodeProperties properties) {
        return new Rectangle2D.Double(properties.getX(), properties.getY(), properties.getWidth(), properties.getHeight());
    }

    private Rectangle2D.Double getBounds(DiagramObject object) {
        final double height = object.getMaxY() - object.getMinY();
        final double width = object.getMaxX() - object.getMinX();
        return new Rectangle2D.Double(object.getMinX(), object.getMinY(), width, height);
    }

    private void crossingSegments() {
        final List<Line2D> lines = diagram.getNodes().stream()
                .flatMap(node -> node.getConnectors().stream())
                .flatMap(connector -> connector.getSegments().stream())
                .map(this::toLine)
                .collect(Collectors.toList());
        int intersections = 0;
        for (int i = 0; i < lines.size(); i++) {
            for (int j = i + 1; j < lines.size(); j++) {
                final Line2D a = lines.get(i);
                final Line2D b = lines.get(j);
                if (CoordinateUtils.intersects(a, b)) intersections++;
            }
        }
        if (intersections > 0) {
            log(Level.ERROR, String.format("contains %d segment intersections", intersections));
        }
        for (final Node node : diagram.getNodes()) {
            final Rectangle2D.Double box = getBounds(node);
            for (final Line2D line : lines) {
                if (CoordinateUtils.intersects(line, box)) {
                    log(Level.ERROR, String.format("[%s %d %s] intersects with a segment",
                            node.getRenderableClass(), node.getId(), node.getDisplayName()));
                }
            }
        }
    }

    private Line2D.Double toLine(Segment segment) {
        return new Line2D.Double(segment.getFrom().getX(), segment.getFrom().getY(), segment.getTo().getX(), segment.getTo().getY());
    }

    private void log(Level level, String message) {
        if (level.ordinal() >= minLevel.ordinal()) {
            if (first) {
                System.out.println(name);
                first = false;
            }
            System.out.printf("[%s] %s%n", level, message);
        }
        logs.computeIfAbsent(level, l -> new ArrayList<>()).add(message);
    }

    public void printResults() {
        printResults(Level.PASSED);
    }

    public void printResults(Level minLevel) {
        final int totalTests = logs.values().stream().mapToInt(List::size).sum();
        final long toPrint = logs.keySet().stream()
                .filter(level -> level.ordinal() >= minLevel.ordinal())
                .map(logs::get)
                .mapToInt(List::size)
                .count();
        if (toPrint > 0) {
            System.out.printf(" - %d tests run%n", totalTests);
        }
        logs.forEach((level, messages) -> {
            if (level.ordinal() >= minLevel.ordinal())
                System.out.printf(" - [%s] %d%n", level, messages.size());
        });
    }

    public Map<Level, List<String>> getLogs() {
        return logs;
    }


    public enum Level {INFO, PASSED, WARNING, ERROR}
}
