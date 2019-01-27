package org.reactome.server.tools.reaction.exporter.layout.text;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility to calculate the size of the text based of a Font and a width/height ratio.
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class TextUtils {

	private static final List<Character> WORD_SPLIT_CHARS = Arrays.asList(':', '.', '-', ',', ')', '/', '+');
	private static final Font DEFAULT_FONT = new Font("arial", Font.BOLD, 8);
	private static final Graphics2D GRAPHICS = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();

	public static final double RATIO = 7. / 3;  // (4 / 3) ^ 3
	private static final double MIN_HEIGHT = 15;


	private TextUtils() {
	}

	/**
	 * Computes the dimension of the text using 'Arial (8px)' as font and 7/3 as ratio
	 *
	 * @param text text to compute its width and height
	 * @return width and height of the text once split to meet ration
	 */
	public static Dimension2D textDimension(String text) {
		return textDimension(text, DEFAULT_FONT, RATIO);
	}

	/**
	 * Computes the dimension of the text using 'Arial (8px)' as font and 7/3 as ratio
	 * @param text text to compute its width and height
	 * @param font font to use for the computation
	 * @return width and height of the text once split to meet ration
	 */
	public static Dimension2D textDimension(String text, Font font, double ratio) {
		// width = max of
		// (i) constant minimum,
		// (ii) length of largest text fragment after splitting text by separators, or
		// (iii) a nice width
		final int height = GRAPHICS.getFontMetrics(font).getHeight();
		double h = 2 * height;
		double w = h * ratio;
		while (fit(text, font, w, h) == null) {
			h += height;
			w = h * ratio;
		}
		// if (h < MIN_HEIGHT) h = MIN_HEIGHT;
		// Add 1 pixel top, 1 pixel bottom for rounding problems
		w = h * ratio;
		return new Dimension((int) Math.ceil(w), (int) Math.ceil(h));
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

	/**
	 * @return a list of lines if text can be fit inside maxWidth and maxHeight.
	 * If text does not fit, returns null. If font size is less than 1, then it
	 * returns an empty list to indicate that text is impossible to be shown,
	 * probably because image size is too small.
	 */
	private static List<String> fit(String text, Font font, double maxWidth, double maxHeight) {
		if (font.getSize() < 1) return Collections.emptyList();
		// Test if text fits in 1 line
		if (computeWidth(text, font) < maxWidth && computeHeight(1, font) < maxHeight)
			return Collections.singletonList(text);

		final List<String> lines = new LinkedList<>();
		final String[] words = text.trim().split(" ");
		String line = "";
		String temp;
		for (String word : words) {
			temp = line.isEmpty() ? word : line + " " + word;
			if (computeWidth(temp, font) < maxWidth)
				line = temp;
			else {
				// Split word in smaller parts and add as much parts as possible
				// to the line
				final List<String> parts = split(word);
				boolean firstPart = true;
				for (String part : parts) {
					// If the part can't fit a line, the text won't fit
					if (computeWidth(part, font) > maxWidth)
						return null;
					if (line.isEmpty()) temp = part;
					else if (firstPart) temp = line + " " + part;
					else temp = line + part;
					if (computeWidth(temp, font) < maxWidth)
						line = temp;
					else {
						// Start a new line with part
						lines.add(line);
						line = part;
						if (computeHeight(lines.size(), font) > maxHeight)
							return null;
					}
					firstPart = false;
				}
			}
		}
		if (!line.isEmpty()) lines.add(line);
		if (computeHeight(lines.size(), font) > maxHeight)
			return null;
		else return lines;
	}

	private static int computeHeight(int lines, Font font) {
		return lines * GRAPHICS.getFontMetrics(font).getHeight();
	}

	private static int computeWidth(String text, Font font) {
		return GRAPHICS.getFontMetrics(font).stringWidth(text);
	}

}
