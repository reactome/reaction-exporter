package org.reactome.server.tools.reaction.exporter.renderer;

import java.awt.*;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;

/**
 * Choose among different stroke widths.
 *
 * @author Lorente-Arencibia, Pascual (pasculorente@gmail.com)
 */
public enum StrokeStyle {
    /**
     * width: 1
     */
    SEGMENT(1),
    BORDER (2);

    private final Stroke stroke;

    StrokeStyle(float width) {
        stroke = new BasicStroke(width, CAP_BUTT, JOIN_MITER);
    }


    public final Stroke getStroke() {
        return stroke;
    }
}
