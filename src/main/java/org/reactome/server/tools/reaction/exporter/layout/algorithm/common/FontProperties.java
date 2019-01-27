package org.reactome.server.tools.reaction.exporter.layout.algorithm.common;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Helper class for measuring text.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class FontProperties {

    private FontProperties(){}

    private static final FontMetrics FONT_METRICS;

    static {
        try {
            final Font font = Font.createFont(Font.TRUETYPE_FONT, FontProperties.class.getResourceAsStream("/fonts/arialbd.ttf")).deriveFont(8f);
            FONT_METRICS = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    .createGraphics()
                    .getFontMetrics(font);
        } catch (FontFormatException | IOException e) {
            // resources shouldn't throw exceptions
            throw new IllegalArgumentException("/fonts/arialbd.ttf not found", e);
        }
    }

    public static double getTextWidth(String text) {
        return FONT_METRICS.stringWidth(text);
    }


    public static double getTextHeight() {
        return FONT_METRICS.getHeight();
    }
}
