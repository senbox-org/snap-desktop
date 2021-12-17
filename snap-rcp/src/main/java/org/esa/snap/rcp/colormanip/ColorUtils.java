package org.esa.snap.rcp.colormanip;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;

/**
 * Utility class containing methods to the Color Manipulation Tool.
 *
 * @author Jean Coravu
 * @author Daniel Knowles (NASA)
 */
// OCT 2019 - Knowles
//          - Added methods to perform numerical checks which return a boolean with the additional option to display
//            the error message in the GUI status bar.





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

    public static boolean isNumber(String string) {
        try {
            double d = Double.parseDouble(string);
        } catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }

    public static boolean isNumber(String string, String componentName, boolean showMessage) {
        try {
            double d = Double.parseDouble(string);
        } catch (NumberFormatException nfe) {
            if (showMessage) {
                ColorUtils.showErrorDialog("INPUT ERROR!!: \" + componentName + \"=\" + string + \" is not a number");
//                SnapApp.getDefault().setStatusBarMessage("INPUT ERROR!!: " + componentName + "=" + string + " is not a number");
            }
            return false;
        }

        return true;
    }


    public static boolean checkRangeCompatibility(String minStr, String maxStr) {

        if (!isNumber(minStr, "Min Textfield", true)) {
            return false;
        }

        if (!isNumber(maxStr, "Min Textfield", true)) {
            return false;
        }

        double min = Double.parseDouble(minStr);
        double max = Double.parseDouble(maxStr);
        if (!checkRangeCompatibility(min, max)) {
            return false;
        }

        return true;
    }



    public static boolean checkRangeCompatibility(double min, double max) {

        if (min >= max) {
            ColorUtils.showErrorDialog("INPUT ERROR!!: Max must be greater than Min");
            return false;
        }

        return true;
    }


    public static void showErrorDialog(final String message) {
        if (message != null && message.trim().length() > 0) {
            if (SnapApp.getDefault() != null) {
                Dialogs.showError(message);
            } else {
                Dialogs.showError("Error", message);
            }
        }
    }


    public static boolean checkRangeCompatibility(double min, double max, boolean isLogScaled) {

        if (!checkRangeCompatibility(min, max)) {
            return false;
        }

        if (!checkLogCompatibility(min, "Min Textfield", isLogScaled)) {
            return false;
        }

        return true;
    }


    public static boolean checkSliderRangeCompatibility(double value, double min, double max) {
        if (value <= min || value >= max) {
            ColorUtils.showErrorDialog("INPUT ERROR!!: Slider outside range of adjacent sliders");

            return false;
        }
        return true;
    }



    public static boolean checkLogCompatibility(double value, String componentName, boolean isLogScaled) {

        if ((isLogScaled) && value <= 0) {
            ColorUtils.showErrorDialog("INPUT ERROR!!: \" + componentName + \" must be greater than zero in log scaling mode");
            return false;
        }
        return true;
    }

    public static boolean checkTableRangeCompatibility(double value, double min, double max) {
        if (value <= min || value >= max) {
            ColorUtils.showErrorDialog("INPUT ERROR!!: Value outside range of adjacent Table Values");
            return false;
        }
        return true;
    }
}
