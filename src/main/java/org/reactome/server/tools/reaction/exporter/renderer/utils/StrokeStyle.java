package org.reactome.server.tools.reaction.exporter.renderer.utils;

import java.awt.*;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;

/**
 * Choose among different stroke widths.
 *
 * @author Lorente-Arencibia, Pascual (pasculorente@gmail.com)
 */
public enum StrokeStyle {
    SEGMENT(1),
    BORDER (2);

    private final Stroke normal;
    private final Stroke dashed;

    StrokeStyle(float width) {
        normal = new BasicStroke(width, CAP_BUTT, JOIN_MITER);
        dashed = new BasicStroke(width, CAP_BUTT, JOIN_MITER, 5f, new float[]{5f, 5f}, 5f);
    }

    public final Stroke getNormal() {
        return normal;
    }

    public Stroke getDashed() {
        return dashed;
    }
}
