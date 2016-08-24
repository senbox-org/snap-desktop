package org.esa.snap.rcp.imagebrightness;

/**
 * Utility class containing methods to process the colors.
 *
 * @author Jean Coravu
 */
public class ColorUtils {

    /**
     * Private constructor to avoid creating new objects.
     */
    private  ColorUtils() {
    }

    /**
     * Returns the RGB color as int.
     *
     * @param red the red component
     * @param green the green component
     * @param blue the blue component
     * @return the RGB color as int
     */
    public static int rgba(int red, int green, int blue) {
        int rgba = 255;
        rgba = (rgba << 8) + red;
        rgba = (rgba << 8) + green;
        rgba = (rgba << 8) + blue;
        return rgba;
    }

    /**
     * Returns the alpha component.
     *
     * @param color the RGB color
     * @return the alpha component
     */
    public static int alpha(int color) {
        return color >> 24 & 0x0FF;
    }

    /**
     * Returns the red component.
     *
     * @param color the RGB color
     * @return the red component
     */
    public static int red(int color) {
        return color >> 16 & 0x0FF;
    }

    /**
     * Returns the green component.
     *
     * @param color the RGB color
     * @return the green component
     */
    public static int green(int color) {
        return color >> 8 & 0x0FF;
    }

    /**
     * Returns the blue component.
     *
     * @param color the RGB color
     * @return the blue component
     */
    public static int blue(int color) {
        return color & 0x0FF;
    }
}
