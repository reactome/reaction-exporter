package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Helper class for measuring text
 */
class FontProperties {

    private FontProperties(){}

    private static final FontMetrics FONT_METRICS;

    static {
        try {
            final Font font = Font.createFont(Font.TRUETYPE_FONT, BreatheAlgorithm.class.getResourceAsStream("/fonts/arialbd.ttf"));
            FONT_METRICS = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    .createGraphics()
                    .getFontMetrics(font.deriveFont(8f));
        } catch (FontFormatException | IOException e) {
            // resources shouldn't throw exceptions
            throw new IllegalArgumentException("/fonts/arialbd.ttf not found", e);
        }
    }

    static double getTextWidth(String text) {
        return FONT_METRICS.stringWidth(text);
    }


    static double getTextHeight() {
        return FONT_METRICS.getHeight();
    }
}
