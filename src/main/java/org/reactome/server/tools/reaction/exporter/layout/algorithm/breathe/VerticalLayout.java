package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.diagram.data.layout.impl.CoordinateImpl;
import org.reactome.server.tools.reaction.exporter.layout.algorithm.common.Transformer;
import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;
import org.reactome.server.tools.reaction.exporter.layout.model.EntityGlyph;
import org.reactome.server.tools.reaction.exporter.layout.model.Glyph;

import java.util.Arrays;
import java.util.List;

public class VerticalLayout extends GlyphsLayout {

    public VerticalLayout(List<? extends Glyph> glyphs) {
        super(glyphs);
    }

    @Override
    public String toString() {
        return String.format("vertical (%d)", getGlyphs().size());
    }

    @Override
    Position layout() {
        if (getGlyphs().isEmpty()) return new Position(0d, 0d, getLeftPadding() + getRightPadding(), getTopPadding() + getBottomPadding());
        for (final Glyph glyph : getGlyphs()) Transformer.setSize(glyph);
        if (getGlyphs().size() > 6) return layoutInTwoColumns();
        // special case: the list of inputs has 2 catalysts: common in diseases
        final long cats = getGlyphs().stream().filter(o -> hasRole((EntityGlyph) o, EntityRole.CATALYST)).count();
        if (cats > 1) return layoutInTwoColumns();
        final double width = getGlyphs().stream().map(Transformer::getBounds).mapToDouble(Position::getWidth).max().orElse(0);
        final double cx = getLeftPadding() + 0.5 * width;
        double y = getTopPadding();
        for (final Glyph glyph : getGlyphs()) {
            final double height = Transformer.getBounds(glyph).getHeight();
            final double cy = y + 0.5 * height;
            Transformer.center(glyph, new CoordinateImpl(cx, cy));
            y += getSeparation() + height;
        }
        return new Position(0d, 0d, width + getLeftPadding() + getRightPadding(), y - getSeparation() + getBottomPadding());
    }

    private boolean hasRole(EntityGlyph entity, EntityRole role) {
        return entity.getRoles().stream().anyMatch(r -> r.getType() == role);
    }

    private Position layoutInTwoColumns() {
        // the width of each column
        final int columns = 2;
        double[] widths = new double[columns];
        Arrays.fill(widths, 0);
        for (int i = 0; i < getGlyphs().size(); i++) {
            final double width = Transformer.getBounds(getGlyphs().get(i)).getWidth();
            final int j = i % columns;
            if (width > widths[j]) widths[j] = width;
        }
        // the x for each column (the center)
        final double[] xs = new double[columns];
        Arrays.fill(xs, 0);
        xs[0] = 0.5 * widths[0];
        for (int i = 1; i < columns; i++) {
            xs[i] = xs[i - 1] + 0.5 * widths[i - 1] + 0.5 * widths[i] + 30;
        }
        final double height = getGlyphs().stream().map(Transformer::getBounds).mapToDouble(Position::getHeight).max().orElse(0);
        final double step = 0.5 * (height + Math.max(getSeparation(), 16));  // space for stoichiometry
        final Position limits = new Position();
        double y = 0;
        for (int i = 0; i < getGlyphs().size(); i++) {
            final Glyph glyph = getGlyphs().get(i);
            y += step;
            final double x = xs[i % columns];
            Transformer.center(glyph, new CoordinateImpl(x, y));
            limits.union(Transformer.getBounds(glyph));
        }
        return limits;
    }


}
