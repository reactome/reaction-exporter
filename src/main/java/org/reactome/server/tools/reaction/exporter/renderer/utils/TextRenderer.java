package org.reactome.server.tools.reaction.exporter.renderer.utils;

import org.reactome.server.tools.reaction.exporter.layout.common.Coordinate;
import org.reactome.server.tools.reaction.exporter.layout.common.Position;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility to calculate the size of the text based of a Font and a width/height ratio.
 */
public class TextRenderer {

    private static final List<Character> WORD_SPLIT_CHARS = Arrays.asList(':', '.', '-', ',', ')', '/', '+');

    private TextRenderer() {
    }

    public static void draw(String text, Position position, Graphics2D graphics, double padding) {
        final Position padded = new Position();
        padded.setX(position.getX() + padding);
        padded.setY(position.getY() + padding);
        padded.setWidth(position.getWidth() - 2 * padding);
        padded.setHeight(position.getHeight() - 2 * padding);
        draw(text, padded, graphics);
    }

    public static void draw(String text, Position position, Graphics2D graphics) {
        // Fit the text
        Font font = graphics.getFont();
        List<String> lines;
        while ((lines = fit(text, graphics, font, position.getWidth(), position.getHeight())) == null)
            font = font.deriveFont(font.getSize() - 1f);

        // Impossible to fit even with font size 1. May happen with thumbnails.
        // Don't draw anything
        if (lines.isEmpty()) return;

        final Font old = graphics.getFont();
        graphics.setFont(font);

        final int lineHeight = graphics.getFontMetrics().getHeight();
        final int textHeight = lines.size() * lineHeight;
        final double centerX = position.getX() + position.getWidth() * 0.5;
        double yOffset = position.getY() + (position.getHeight() - textHeight) * 0.5;
        // Centering at ascent gives a more natural view (centers at -)
        // https://goo.gl/x1EExY [difference between ascent/descent/height]
        yOffset += graphics.getFontMetrics().getAscent();
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final int lineWidth = computeWidth(line, graphics, font);
            final float left = (float) (centerX - 0.5 * lineWidth);
            final float base = (float) (yOffset + i * lineHeight);
            graphics.drawString(line, left, base);
        }
        graphics.setFont(old);
    }

    /**
     * @return a list of lines if text can be fit inside maxWidth and maxHeight.
     * If text does not fit, returns null. If font size is less than 1, then it
     * returns an empty list to indicate that text is impossible to be shown,
     * probably because image size is too small.
     */
    private static List<String> fit(String text, Graphics2D graphics, Font font, double maxWidth, double maxHeight) {
        if (font.getSize() < 1) return Collections.emptyList();
        // Test if text fits in 1 line
        if (computeWidth(text, graphics, font) < maxWidth
                && computeHeight(1, graphics, font) < maxHeight)
            return Collections.singletonList(text);

        final List<String> lines = new LinkedList<>();
        final String[] words = text.trim().split(" ");
        String line = "";
        String temp;
        for (String word : words) {
            temp = line.isEmpty() ? word : line + " " + word;
            if (computeWidth(temp, graphics, font) < maxWidth)
                line = temp;
            else {
                // Split word in smaller parts and add as much parts as possible
                // to the line
                final List<String> parts = split(word);
                boolean firstPart = true;
                for (String part : parts) {
                    // If the part can't fit a line, the text won't fit
                    if (computeWidth(part, graphics, font) > maxWidth)
                        return null;
                    if (line.isEmpty()) temp = part;
                    else if (firstPart) temp = line + " " + part;
                    else temp = line + part;
                    if (computeWidth(temp, graphics, font) < maxWidth)
                        line = temp;
                    else {
                        // Start a new line with part
                        lines.add(line);
                        line = part;
                        if (computeHeight(lines.size(), graphics, font) > maxHeight)
                            return null;
                    }
                    firstPart = false;
                }
            }
        }
        if (!line.isEmpty()) lines.add(line);
        if (computeHeight(lines.size(), graphics, font) > maxHeight)
            return null;
        else return lines;
    }

    /**
     * Will split a word by any character in WORD_SPLIT_CHARS
     * <pre>:.-,)/+</pre>
     * The split characters are inserted as the last character of the fragment
     * <p>
     * For example <pre>splitWord("p-T402-PAK2(213-524)")</pre> will result in
     * <pre>{"p-", "T402-", "PAK2(", "213-", "524)"}</pre>
     */
    private static List<String> split(String word) {
        final List<String> strings = new LinkedList<>();
        int start = 0;
        for (int i = 0; i < word.length(); i++) {
            if (WORD_SPLIT_CHARS.contains(word.charAt(i))) {
                strings.add(word.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < word.length()) strings.add(word.substring(start));
        return strings;
    }

    private static int computeHeight(int lines, Graphics2D graphics, Font font) {
        return lines * graphics.getFontMetrics(font).getHeight();
    }

    private static int computeWidth(String text, Graphics2D graphics, Font font) {
        return graphics.getFontMetrics(font).charsWidth(text.toCharArray(), 0, text.length());
    }

    /**
     * Draws text in coordinate, where coordinate is the center of the text
     *
     * @param text       text to draw
     * @param coordinate center of the text
     */
    public static void draw(String text, Coordinate coordinate, Graphics2D graphics) {
        final int textHeight = graphics.getFontMetrics().getHeight();
        final int textWidth = computeWidth(text, graphics, graphics.getFont());
        final int x = coordinate.getX() - textWidth / 2;
        int baseline = coordinate.getY() + textHeight / 2;
        baseline -= graphics.getFontMetrics().getAscent() / 2;
        graphics.drawString(text, x, baseline);
    }
}
