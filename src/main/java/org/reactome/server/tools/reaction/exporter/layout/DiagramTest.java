package org.reactome.server.tools.reaction.exporter.layout;

import org.reactome.server.tools.diagram.data.layout.*;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

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

    private void runTests() {
        notInCompartment();
    }

    private void notInCompartment() {
        for (final Compartment compartment : diagram.getCompartments()) {
            final Rectangle2D.Double box = toRectangle(compartment.getProp());
            for (final Node node : diagram.getNodes()) {
                testBounds(compartment, box, node);
            }
            for (final Edge edge : diagram.getEdges()) {
                testBounds(compartment, box, edge);
                testSegments(compartment, box, edge);
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
            final Line2D.Double line = new Line2D.Double(segment.getFrom().getX(), segment.getFrom().getY(), segment.getTo().getX(), segment.getTo().getY());
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

    private void log(Level level, String message) {
        if (level.ordinal() > minLevel.ordinal()) {
            if (first) {
                System.out.println(name);
                first = false;
            }
            System.out.printf("[%s] %s%n", level, message);
        }
        logs.computeIfAbsent(level, l -> new ArrayList<>()).add(message);
    }

    public void printResults() {
        printResults(Level.PASSED, diagram.getStableId());
    }

    public void printResults(Level minLevel, String name) {
        this.minLevel = minLevel;
        this.name = name;
        this.first = true;
        runTests();
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

    public enum Level {PASSED, WARNING, ERROR}
}
