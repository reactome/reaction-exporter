package org.reactome.server.tools.reaction.exporter.layout.algorithm;

import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.AttachmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.CompartmentGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Like divs in html. It can only contain glyphs or children, but never both.
 */
final class BorderLayout extends Position {

    private final List<Glyph> glyphs = new ArrayList<>();
    private final Map<Place, BorderLayout> layoutMap = new HashMap<>();

    /**
     * This is the compartment surrounding the border layout, in case it is not null
     */
    private CompartmentGlyph compartment;

    public BorderLayout() {
    }

    public void add(Glyph glyph) {
        glyphs.add(glyph);
    }

    /**
     * Adds <em>glyph</em> to the borderLayout in <em>position</em>. If there is no borderLayout in position, creates a
     * new one.
     */
    public void add(Place place, Glyph glyph) {
        layoutMap.computeIfAbsent(place, p -> new BorderLayout()).add(glyph);
    }

    public void set(Place place, BorderLayout borderLayout) {
        layoutMap.put(place, borderLayout);
    }

    public List<Glyph> getGlyphs() {
        return glyphs;
    }

    public CompartmentGlyph getCompartment() {
        return compartment;
    }

    public void setCompartment(CompartmentGlyph compartment) {
        this.compartment = compartment;
    }

    public BorderLayout get(Place place) {
        return layoutMap.get(place);
    }

    @Override
    public void move(double dx, double dy) {
        super.move(dx, dy);
        for (Glyph glyph : glyphs) {
            final double x1 = glyph.getPosition().getX();
            final double y1 = glyph.getPosition().getY();
            glyph.getPosition().move(dx, dy);
            if (glyph instanceof EntityGlyph)
                moveAttachments((EntityGlyph) glyph, glyph.getPosition().getX() - x1, glyph.getPosition().getY() - y1);
        }
        for (BorderLayout layout : layoutMap.values()) {
            if (!layout.isEmpty()) layout.move(dx, dy);
        }
        if (compartment != null) {
            compartment.getPosition().move(dx, dy);
            if (compartment.getLabelPosition() != null)
                compartment.setLabelPosition(compartment.getLabelPosition().add(new CoordinateImpl(dx, dy)));
        }
    }

    private void moveAttachments(EntityGlyph glyph, double dx, double dy) {
        if (glyph.getAttachments().isEmpty()) return;
        for (AttachmentGlyph attachment : glyph.getAttachments()) {
            attachment.getPosition().move(dx, dy);
        }
    }

    @Override
    public void setCenter(double x, double y) {
        move(x - getCenterX(), y - getCenterY());
    }

    public boolean isEmpty() {
        return glyphs.isEmpty() && layoutMap.isEmpty();
    }

    @Override
    public String toString() {
        if (glyphs.isEmpty()) {
            final StringJoiner joiner = new StringJoiner(",", "{", "}");
            layoutMap.forEach((place, borderLayout) -> {
                joiner.add(String.format("%s=%s", place, borderLayout));
            });
            return joiner.toString();
        } else return String.format("%d glyphs", glyphs.size());
    }

    public void print() {
        print(0);
    }

    private void print(int level) {
        final String spaces = spaces(level);
        if (compartment != null) System.out.println(spaces + compartment.getName());
        if (glyphs.isEmpty()) {
            layoutMap.forEach((place, borderLayout) -> {
                System.out.printf("%s%s {%n", spaces, place);
                borderLayout.print(level + 1);
                System.out.println(spaces + "}");
            });
        } else {
            final String glyphTest = glyphs.stream().map(Glyph::getName).collect(Collectors.joining(", "));
            System.out.println(String.format("%s %s", spaces, glyphTest));
        }
    }

    private String spaces(int level) {
        final StringBuilder space = new StringBuilder();
        for (int i = 0; i < level; i++) space.append("    ");
        return space.toString();
    }


    enum Place {
        NORTH, SOUTH, CENTER, EAST, WEST, SOUTH_WEST, NORTH_WEST, SOUTH_EAST, NORTH_EAST
    }
}
