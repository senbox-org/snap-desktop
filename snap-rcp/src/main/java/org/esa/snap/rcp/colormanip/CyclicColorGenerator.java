package org.esa.snap.rcp.colormanip;

import java.awt.Color;

/**
 * Utility class for generating distinct colors in a cyclic manner.
 * This class provides a set of predefined distinct colors and manages
 * the sequential assignment of these colors.
 */
public class CyclicColorGenerator {
    
    private static final Color[] DISTINCT_COLORS = new Color[]{
        new Color(0, 0, 255),     // Blue
        new Color(255, 0, 0),     // Red
        new Color(0, 255, 0),     // Green
        new Color(255, 255, 0),   // Yellow
        new Color(255, 0, 255),   // Magenta
        new Color(255, 128, 0),   // Orange
        new Color(0, 255, 255),   // Cyan
        new Color(128, 0, 255),   // Purple
    };

    private final Color[] colors;
    private int colorIndex = 0;

    /**
     * Constructs a new instance of {@code DistinctColorGenerator} using a predefined
     * array of distinct colors. The generator will cycle through these colors in
     * a round-robin manner when retrieving the next color.
     */
    public CyclicColorGenerator() {
        this(DISTINCT_COLORS);
    }

    /**
     * Constructs a new {@code DistinctColorGenerator} instance using the provided array of colors.
     * The generator will cycle through the colors in the array in a round-robin manner when
     * retrieving the next color.
     *
     * @param colors the array of colors to be used for generating distinct colors
     * @throws IllegalArgumentException if the provided {@code colors} array is null or empty
     */
    public CyclicColorGenerator(Color[] colors) {
        if (colors == null || colors.length == 0) {
            throw new IllegalArgumentException("Colors array cannot be null or empty");
        }
        this.colors = colors;
    }

    /**
     * Gets the next distinct color in the sequence.
     * Colors are cycled through in a round-robin fashion.
     * 
     * @return the next distinct color
     */
    public Color getNextDistinctColor() {
        Color color = colors[colorIndex];
        colorIndex = (colorIndex + 1) % colors.length;
        return color;
    }
    
    /**
     * Resets the color index to start from the beginning of the color sequence.
     */
    public void resetColorIndex() {
        colorIndex = 0;
    }
    
    /**
     * Gets the total number of distinct colors available.
     * 
     * @return the number of distinct colors
     */
    public int getColorCount() {
        return colors.length;
    }

    /**
     * Gets a specific color by its index.
     * 
     * @param index the index of the color (0-based)
     * @return the color at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public Color getColorByIndex(int index) {
        if (index < 0 || index >= colors.length) {
            throw new IndexOutOfBoundsException("Color index must be between 0 and " + (colors.length - 1));
        }
        return colors[index];
    }
}
