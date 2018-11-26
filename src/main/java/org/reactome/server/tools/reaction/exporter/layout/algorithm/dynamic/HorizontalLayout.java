package org.reactome.server.tools.reaction.exporter.layout.algorithm.dynamic;

import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;

import java.util.List;

public class HorizontalLayout extends GlyphsLayout {

    public HorizontalLayout(List<? extends Glyph> glyphs) {
        super(glyphs);
    }

    @Override
     Position layout() {
        if (getGlyphs().isEmpty()) return new Position(0d, 0d, 2 * getHorizontalPadding(), 2 * getVerticalPadding());
        final double height = getGlyphs().stream().map(Transformer::getBounds).mapToDouble(Position::getHeight).max().orElse(0);
        final double cy = 0.5 * height + getVerticalPadding();
        double x = getHorizontalPadding();
        for (final Glyph glyph : getGlyphs()) {
            final double width = Transformer.getBounds(glyph).getWidth();
            final double cx = x + 0.5 * width;
            Transformer.center(glyph, new CoordinateImpl(cx, cy));
            x += getSeparation() + width;
        }
        return new Position(0d, 0d, x - getSeparation() + getHorizontalPadding(), height + 2 * getVerticalPadding());
    }


}