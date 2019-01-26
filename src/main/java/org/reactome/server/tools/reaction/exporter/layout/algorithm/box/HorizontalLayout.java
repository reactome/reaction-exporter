package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;

import java.util.List;

/**
 * Horizontal layout of glyphs. Entities will be placed in a horizontal line, centered vertically.
 */
public class HorizontalLayout extends GlyphsLayout {

    public HorizontalLayout(List<? extends Glyph> glyphs) {
        super(glyphs);
    }

    @Override
    Position layout() {
        if (getGlyphs().isEmpty()) return new Position(0d, 0d, getLeftPadding() + getRightPadding(), getTopPadding() + getBottomPadding());
        for (final Glyph glyph : getGlyphs()) Transformer.setSize(glyph);
        final double height = getGlyphs().stream().map(Transformer::getBounds).mapToDouble(Position::getHeight).max().orElse(0);
        final double cy = 0.5 * height + getTopPadding();
        double x = getLeftPadding();
        for (final Glyph glyph : getGlyphs()) {
            final double width = Transformer.getBounds(glyph).getWidth();
            final double cx = x + 0.5 * width;
            Transformer.center(glyph, new CoordinateImpl(cx, cy));
            x += getSeparation() + width;
        }
        return new Position(0d, 0d, x - getSeparation() + getRightPadding(), height + getBottomPadding() + getTopPadding());
    }

    @Override
    public String toString() {
        return String.format("horizontal (%d)", getGlyphs().size());
    }

    @Override
    public Character getInitial() {
        return 'h';
    }
}
