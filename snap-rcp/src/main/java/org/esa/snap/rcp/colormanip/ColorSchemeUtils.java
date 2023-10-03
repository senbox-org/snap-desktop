package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.Histogram;
import org.esa.snap.core.util.math.Range;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.product.ProductSceneView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.esa.snap.core.datamodel.ColorManipulationDefaults.*;
import static org.esa.snap.core.util.SystemUtils.getApplicationContextId;

/**
 * Panel handling general layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author Daniel Knowles (NASA)
 */

public class ColorSchemeUtils {

    /**
     * Top level method which will set the desired color palette, range and log scaling within the imageInfo
     * of the given productSceneView.
     * <p>
     * This is called by either the reset button within the ColorManipulation GUI or when a new View
     * Window is opened for a band.
     *
     * @param defaultImageInfo
     * @param productSceneView
     */

    public static void setImageInfoToDefaultColor(PropertyMap configuration, ImageInfo defaultImageInfo, ProductSceneView productSceneView, boolean resetToDefaults) {


        boolean imageInfoSet = false;

        ColorSchemeManager colorPaletteSchemes = ColorSchemeManager.getDefault();

        if (configuration != null && configuration.getPropertyBool(PROPERTY_SCHEME_AUTO_APPLY_KEY, PROPERTY_SCHEME_AUTO_APPLY_DEFAULT)) {
            String bandName = productSceneView.getRaster().getName().trim();
//            String mission = ProductUtils.getMetaData(productSceneView.getProduct(), ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS);
//            if (mission == null || mission.length() == 0) {
//                mission = productSceneView.getProduct().getProductType();
//            }
            ColorSchemeInfo colorSchemeInfo = ColorSchemeInfo.getColorPaletteInfoByBandNameLookup(bandName, productSceneView.getProduct());

            if (colorSchemeInfo != null) {
                imageInfoSet = ColorSchemeUtils.setImageInfoToColorScheme(colorSchemeInfo, productSceneView);

                if (imageInfoSet) {
                    colorPaletteSchemes.setSelected(colorSchemeInfo);
                }
            }
        }

        boolean customDefaultScheme = configuration.getPropertyBool(PROPERTY_GENERAL_CUSTOM_KEY, PROPERTY_GENERAL_CUSTOM_DEFAULT);

        if (!imageInfoSet) {
            if (customDefaultScheme) {
                colorPaletteSchemes.reset();
                setImageInfoToGeneralColor(configuration, defaultImageInfo, productSceneView);
            } else {
                if (resetToDefaults) {
                    productSceneView.setImageInfo(defaultImageInfo);
                }
            }

        }

    }


    public static boolean isRangeFromDataNonScheme(PropertyMap configuration) {
        if (configuration == null) {
            return false;
        }

        String generalRange = configuration.getPropertyString(PROPERTY_GENERAL_RANGE_KEY, PROPERTY_GENERAL_RANGE_DEFAULT);
        if (generalRange != null && generalRange.equals(ColorManipulationDefaults.OPTION_RANGE_FROM_DATA)) {
            return true;
        } else {
            return false;
        }
    }


    public static boolean isGeneralLogScaled(ColorPaletteDef colorPaletteDef, ProductSceneView productSceneView) {

        boolean logScaled = false;

        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();
        if (configuration == null) {
            return false;
        }
        String logScaledOption = configuration.getPropertyString(PROPERTY_GENERAL_LOG_KEY, PROPERTY_GENERAL_LOG_DEFAULT);

        if (logScaledOption != null) {
            switch (logScaledOption) {
                case ColorManipulationDefaults.OPTION_LOG_TRUE:
                    logScaled = true;
                    break;
                case ColorManipulationDefaults.OPTION_LOG_FALSE:
                    logScaled = false;
                    break;
                case ColorManipulationDefaults.OPTION_LOG_FROM_PALETTE:
                    if (colorPaletteDef != null) {
                        logScaled = colorPaletteDef.isLogScaled();
                    }
                    break;
                default:
                    logScaled = false;
            }

        }

        return logScaled;
    }


    public static File getDefaultCpd(PropertyMap configuration) {

        String filename = null;

        String fileCategory = configuration.getPropertyString(ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_KEY, ColorManipulationDefaults.PROPERTY_GENERAL_PALETTE_DEFAULT);
        if (fileCategory != null) {
            switch (fileCategory) {
                case ColorManipulationDefaults.OPTION_COLOR_GRAY_SCALE:
                    filename = configuration.getPropertyString(ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_KEY, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_DEFAULT);
                    break;
                case ColorManipulationDefaults.OPTION_COLOR_STANDARD:
                    filename = configuration.getPropertyString(ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_KEY, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_DEFAULT);
                    break;
                case ColorManipulationDefaults.OPTION_COLOR_UNIVERSAL:
                    filename = configuration.getPropertyString(ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_KEY, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_DEFAULT);
                    break;
                case ColorManipulationDefaults.OPTION_COLOR_ANOMALIES:
                    filename = configuration.getPropertyString(ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_KEY, ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_DEFAULT);
                    break;
                default:
                    filename = ColorManipulationDefaults.PALETTE_DEFAULT;
            }
        }

        File auxDir = ColorSchemeUtils.getColorPalettesAuxDataDir().toFile();

        if (filename != null) {
            File defaultCpd = new File(auxDir, filename);

            if (defaultCpd.exists()) {
                return defaultCpd;
            }
        }

        return null;
    }


    public static void setImageInfoToGeneralColor(PropertyMap configuration, ImageInfo defaultImageInfo, ProductSceneView productSceneView) {

        boolean imageInfoSet = false;

        File defaultCpdFile = getDefaultCpd(configuration);

        ColorSchemeInfo colorSchemeInfo = ColorSchemeManager.getDefault().getNoneColorSchemeInfo();
        productSceneView.getImageInfo().setColorSchemeInfo(colorSchemeInfo);


        ColorPaletteDef colorPaletteDef = null;

        if (defaultCpdFile.exists()) {
            try {
                colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(defaultCpdFile);

            } catch (IOException e) {
            }
        }

        if (colorPaletteDef != null) {

            double minTarget;
            double maxTarget;

            if (isRangeFromDataNonScheme(configuration)) {
                Stx stx = productSceneView.getRaster().getStx();


                if (stx != null) {
                    final Histogram histogram = new Histogram(stx.getHistogramBins(), stx.getMinimum(), stx.getMaximum());

                    double percentile = configuration.getPropertyDouble(PROPERTY_RANGE_PERCENTILE_KEY, PROPERTY_RANGE_PERCENTILE_DEFAULT);

                    Range autoStretchRange = histogram.findRangeForPercent(percentile);

                    if (autoStretchRange != null) {
                        minTarget = autoStretchRange.getMin();
                        maxTarget = autoStretchRange.getMax();
                    } else {
                        minTarget = stx.getMinimum();
                        maxTarget = stx.getMaximum();
                    }
                } else {
                    minTarget = colorPaletteDef.getMinDisplaySample();
                    maxTarget = colorPaletteDef.getMaxDisplaySample();
                }
            } else {
                minTarget = colorPaletteDef.getMinDisplaySample();
                maxTarget = colorPaletteDef.getMaxDisplaySample();
            }


            boolean logScaledSource = colorPaletteDef.isLogScaled();

            boolean logScaledTarget = isGeneralLogScaled(colorPaletteDef, productSceneView);

            // todo Possibly show alert GUI in future - this line just prevents log scaling for min < 0
            if (minTarget <= 0) {
                logScaledTarget = false;
            }


            productSceneView.getImageInfo().setColorPaletteDef(colorPaletteDef,
                    minTarget,
                    maxTarget,
                    true, //colorPaletteDef.isAutoDistribute(),
                    logScaledSource,
                    logScaledTarget);
            productSceneView.getImageInfo().setLogScaled(logScaledTarget);

            imageInfoSet = true;
        }


        if (!imageInfoSet) {
            productSceneView.setImageInfo(defaultImageInfo);
        }

        return;

    }


//    public static ColorSchemeInfo getColorPaletteInfoByBandNameLookup(ProductSceneView productSceneView) {
//
//        ColorSchemeManager colorSchemeManager = ColorSchemeManager.getDefault();
//        if (colorSchemeManager != null) {
//
//            String bandName = productSceneView.getBaseImageLayer().getName().trim();
//            bandName = bandName.substring(bandName.indexOf(" ")).trim();
//
//            ArrayList<ColorSchemeLookupInfo> colorSchemeLookupInfos = colorSchemeManager.getColorSchemeLookupInfos();
//            for (ColorSchemeLookupInfo colorSchemeLookupInfo : colorSchemeLookupInfos) {
//                if (colorSchemeLookupInfo.isMatch(bandName)) {
//                    return colorSchemeManager.getColorSchemeInfoBySchemeId(colorSchemeLookupInfo.getScheme_id());
//                }
//            }
//        }
//
//        return null;
//    }


    public static boolean getLogScaledFromScheme(PropertyMap configuration, ColorSchemeInfo colorSchemeInfo, ColorPaletteDef colorPaletteDef) {
        boolean logScaled = false;

        String schemeLogScaling = configuration.getPropertyString(ColorManipulationDefaults.PROPERTY_SCHEME_LOG_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_LOG_DEFAULT);
        if (schemeLogScaling != null) {
            switch (schemeLogScaling) {
                case ColorManipulationDefaults.OPTION_LOG_TRUE:
                    logScaled = true;
                    break;
                case ColorManipulationDefaults.OPTION_LOG_FALSE:
                    logScaled = false;
                    break;
                case ColorManipulationDefaults.OPTION_LOG_FROM_PALETTE:
                    if (colorPaletteDef != null) {
                        logScaled = colorPaletteDef.isLogScaled();
                    }
                    break;
                case ColorManipulationDefaults.OPTION_LOG_FROM_SCHEME:
                    logScaled = colorSchemeInfo.isLogScaled();
                    break;
                default:
                    logScaled = false;
            }
        }

        return logScaled;
    }


    public static String getCdpFileNameFromSchemeSelection(PropertyMap configuration, ColorSchemeInfo colorSchemeInfo) {
        String cpdFileName = null;

        String schemeCpdOption = configuration.getPropertyString(
                ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_KEY,
                ColorManipulationDefaults.PROPERTY_SCHEME_PALETTE_DEFAULT);

        switch (schemeCpdOption) {
            case ColorManipulationDefaults.OPTION_COLOR_STANDARD_SCHEME:
                cpdFileName = colorSchemeInfo.getCpdFilename(false);
                break;
            case ColorManipulationDefaults.OPTION_COLOR_UNIVERSAL_SCHEME:
                cpdFileName = colorSchemeInfo.getCpdFilename(true);
                break;
            case ColorManipulationDefaults.OPTION_COLOR_GRAY_SCALE:
                cpdFileName = configuration.getPropertyString(
                        ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_KEY,
                        ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_DEFAULT);
                break;
            case ColorManipulationDefaults.OPTION_COLOR_STANDARD:
                cpdFileName = configuration.getPropertyString(
                        ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_KEY,
                        ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_DEFAULT);
                break;
            case ColorManipulationDefaults.OPTION_COLOR_UNIVERSAL:
                cpdFileName = configuration.getPropertyString(
                        ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_KEY,
                        ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_DEFAULT);
                break;
            case ColorManipulationDefaults.OPTION_COLOR_ANOMALIES:
                cpdFileName = configuration.getPropertyString(
                        ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_KEY,
                        ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_DEFAULT);
                break;
            default:
                break;
        }

        return cpdFileName;
    }


    public static boolean setImageInfoToColorScheme(ColorSchemeInfo colorSchemeInfo, ProductSceneView productSceneView) {

        ColorManipulationDefaults.debug("Inside setImageInfoToColorScheme");

        if (colorSchemeInfo == null || productSceneView == null) {
            return false;
        }

        productSceneView.getImageInfo().setColorSchemeInfo(colorSchemeInfo);

        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();

        String cpdFileName = getCdpFileNameFromSchemeSelection(configuration, colorSchemeInfo);

        ColorManipulationDefaults.debug("Inside setImageInfoToColorScheme: cpdFileName=" + cpdFileName);


        ColorPaletteDef colorPaletteDef = null;

        File auxDir = ColorSchemeUtils.getColorPalettesAuxDataDir().toFile();

        if (cpdFileName != null) {
            File cpdFile = new File(auxDir, cpdFileName);
            try {
                if (cpdFileName.endsWith("cpt")) {
                    colorPaletteDef = ColorPaletteDef.loadCpt(cpdFile);
                } else {
                    colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(cpdFile);
                }
            } catch (IOException e) {
                ColorManipulationDefaults.debug("Inside setImageInfoToColorScheme: cpd read exception");
                return false;
            }
        }


        // Determine range: min and max

        double min;
        double max;

        Stx stx = productSceneView.getRaster().getStx();

        String schemeRange = configuration.getPropertyString(ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_KEY, ColorManipulationDefaults.PROPERTY_SCHEME_RANGE_DEFAULT);
        switch (schemeRange) {
            case ColorManipulationDefaults.OPTION_RANGE_FROM_SCHEME:
                min = colorSchemeInfo.getMinValue();
                if (min == ColorManipulationDefaults.DOUBLE_NULL) {
                    min = stx.getMinimum();
                }
                max = colorSchemeInfo.getMaxValue();
                if (max == ColorManipulationDefaults.DOUBLE_NULL) {
                    max = stx.getMaximum();
                }
                break;
            case ColorManipulationDefaults.OPTION_RANGE_FROM_DATA:
                min = stx.getMinimum();
                max = stx.getMaximum();
                break;
            case ColorManipulationDefaults.OPTION_RANGE_FROM_PALETTE:
                min = colorPaletteDef.getMinDisplaySample();
                max = colorPaletteDef.getMaxDisplaySample();
                break;
            default:
                min = stx.getMinimum();
                max = stx.getMaximum();
                break;
        }


        boolean logScaled = getLogScaledFromScheme(configuration, colorSchemeInfo, colorPaletteDef);


        if (colorPaletteDef != null) {
            productSceneView.getImageInfo().setColorPaletteDef(colorPaletteDef,
                    min,
                    max,
                    true, //colorPaletteDef.isAutoDistribute(),
                    colorPaletteDef.isLogScaled(),
                    logScaled);
            productSceneView.getImageInfo().setLogScaled(logScaled);

            return true;

        }

        return false;
    }


    public static Path getColorPalettesAuxDataDir() {
        return SystemUtils.getAuxDataPath().resolve(ColorManipulationDefaults.DIR_NAME_COLOR_PALETTES);
    }

    public static Path getColorSchemesAuxDataDir() {
        return SystemUtils.getAuxDataPath().resolve(ColorManipulationDefaults.DIR_NAME_COLOR_SCHEMES);
    }

    public static Path getRgbProfilesAuxDataDir() {
        return SystemUtils.getAuxDataPath().resolve(ColorManipulationDefaults.DIR_NAME_RGB_PROFILES);
    }


    public final static String FILE_DOES_NOT_EXIST = "File does not exist";
    public final static String INVALID_BOOLEAN = "Invalid boolean value";
    public final static String INVALID_NUMBER = "Invalid number";
    public final static String INVALID_TEXT_ENTRY = "Invalid text entry";

    public static void initColorManipulationDefaults() {

        String preferenceKey;
        String preferenceValue;

        String errorMsg = "";


        // Palettes (Default)

        File colorPalettesAuxDir = getColorPalettesAuxDataDir().toFile();

        // Test to see if palettes have been installed based on existence on the core default palette
        boolean palettesInstalled = false;
        if (colorPalettesAuxDir != null && colorPalettesAuxDir.exists()) {
            File defaultPalette = new File(colorPalettesAuxDir, PALETTE_DEFAULT);
            if (defaultPalette.exists()) {
                palettesInstalled = true;
            }
        }

        preferenceKey = getPreferenceContextKey(PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (palettesInstalled) {
                File file = new File(colorPalettesAuxDir, preferenceValue);
                if (file.exists()) {
                    ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_DEFAULT = preferenceValue;
                } else {
                    errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, FILE_DOES_NOT_EXIST);
                }
            } else {
                ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_DEFAULT = preferenceValue;
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_PALETTE_DEFAULT_GRAY_SCALE_DEFAULT);


        preferenceKey = getPreferenceContextKey(PROPERTY_PALETTE_DEFAULT_STANDARD_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (palettesInstalled) {
                File file = new File(colorPalettesAuxDir, preferenceValue);
                if (file.exists()) {
                    ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_DEFAULT = preferenceValue;
                } else {
                    errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, FILE_DOES_NOT_EXIST);
                }
            } else {
                ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_STANDARD_DEFAULT = preferenceValue;
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_PALETTE_DEFAULT_STANDARD_DEFAULT);


        preferenceKey = getPreferenceContextKey(PROPERTY_PALETTE_DEFAULT_UNIVERSAL_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (palettesInstalled) {
                File file = new File(colorPalettesAuxDir, preferenceValue);
                if (file.exists()) {
                    ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_DEFAULT = preferenceValue;
                } else {
                    errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, FILE_DOES_NOT_EXIST);
                }
            } else {
                ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_UNIVERSAL_DEFAULT = preferenceValue;
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_PALETTE_DEFAULT_UNIVERSAL_DEFAULT);


        preferenceKey = getPreferenceContextKey(PROPERTY_PALETTE_DEFAULT_ANOMALIES_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (palettesInstalled) {
                File file = new File(colorPalettesAuxDir, preferenceValue);
                if (file.exists()) {
                    ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_DEFAULT = preferenceValue;
                } else {
                    errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, FILE_DOES_NOT_EXIST);
                }
            } else {
                ColorManipulationDefaults.PROPERTY_PALETTE_DEFAULT_ANOMALIES_DEFAULT = preferenceValue;
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_PALETTE_DEFAULT_ANOMALIES_DEFAULT);


        // Scheme (Default)

        preferenceKey = getPreferenceContextKey(PROPERTY_GENERAL_CUSTOM_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_GENERAL_CUSTOM_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_GENERAL_CUSTOM_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_GENERAL_PALETTE_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (PROPERTY_GENERAL_PALETTE_OPTION1.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_GENERAL_PALETTE_DEFAULT = PROPERTY_GENERAL_PALETTE_OPTION1;
            } else if (PROPERTY_GENERAL_PALETTE_OPTION2.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_GENERAL_PALETTE_DEFAULT = PROPERTY_GENERAL_PALETTE_OPTION2;
            } else if (PROPERTY_GENERAL_PALETTE_OPTION3.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_GENERAL_PALETTE_DEFAULT = PROPERTY_GENERAL_PALETTE_OPTION3;
            } else if (PROPERTY_GENERAL_PALETTE_OPTION4.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_GENERAL_PALETTE_DEFAULT = PROPERTY_GENERAL_PALETTE_OPTION4;
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_TEXT_ENTRY);
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_GENERAL_PALETTE_DEFAULT);


        preferenceKey = getPreferenceContextKey(PROPERTY_GENERAL_RANGE_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (PROPERTY_GENERAL_RANGE_OPTION1.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_GENERAL_RANGE_DEFAULT = PROPERTY_GENERAL_RANGE_OPTION1;
            } else if (PROPERTY_GENERAL_RANGE_OPTION2.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_GENERAL_RANGE_DEFAULT = PROPERTY_GENERAL_RANGE_OPTION2;
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_TEXT_ENTRY);
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_GENERAL_RANGE_DEFAULT);


        preferenceKey = getPreferenceContextKey(PROPERTY_GENERAL_LOG_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (PROPERTY_GENERAL_LOG_OPTION1.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_GENERAL_LOG_DEFAULT = PROPERTY_GENERAL_LOG_OPTION1;
            } else if (PROPERTY_GENERAL_LOG_OPTION2.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_GENERAL_LOG_DEFAULT = PROPERTY_GENERAL_LOG_OPTION2;
            } else if (PROPERTY_GENERAL_LOG_OPTION3.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_GENERAL_LOG_DEFAULT = PROPERTY_GENERAL_LOG_OPTION3;
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_TEXT_ENTRY);
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_GENERAL_LOG_DEFAULT);


        // Scheme (Band Lookup)

        preferenceKey = getPreferenceContextKey(PROPERTY_SCHEME_AUTO_APPLY_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_SCHEME_AUTO_APPLY_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_SCHEME_PALETTE_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (PROPERTY_SCHEME_PALETTE_OPTION1.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_PALETTE_DEFAULT = PROPERTY_SCHEME_PALETTE_OPTION1;
            } else if (PROPERTY_SCHEME_PALETTE_OPTION2.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_PALETTE_DEFAULT = PROPERTY_SCHEME_PALETTE_OPTION2;
            } else if (PROPERTY_SCHEME_PALETTE_OPTION3.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_PALETTE_DEFAULT = PROPERTY_SCHEME_PALETTE_OPTION3;
            } else if (PROPERTY_SCHEME_PALETTE_OPTION4.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_PALETTE_DEFAULT = PROPERTY_SCHEME_PALETTE_OPTION4;
            } else if (PROPERTY_SCHEME_PALETTE_OPTION5.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_PALETTE_DEFAULT = PROPERTY_SCHEME_PALETTE_OPTION5;
            } else if (PROPERTY_SCHEME_PALETTE_OPTION6.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_PALETTE_DEFAULT = PROPERTY_SCHEME_PALETTE_OPTION6;
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_TEXT_ENTRY);
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_SCHEME_PALETTE_DEFAULT);


        preferenceKey = getPreferenceContextKey(PROPERTY_SCHEME_RANGE_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (PROPERTY_SCHEME_RANGE_OPTION1.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_RANGE_DEFAULT = PROPERTY_SCHEME_RANGE_OPTION1;
            } else if (PROPERTY_SCHEME_RANGE_OPTION2.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_RANGE_DEFAULT = PROPERTY_SCHEME_RANGE_OPTION2;
            } else if (PROPERTY_SCHEME_RANGE_OPTION3.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_RANGE_DEFAULT = PROPERTY_SCHEME_RANGE_OPTION3;
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_TEXT_ENTRY);
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_SCHEME_RANGE_DEFAULT);


        preferenceKey = getPreferenceContextKey(PROPERTY_SCHEME_LOG_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (PROPERTY_SCHEME_LOG_OPTION1.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_LOG_DEFAULT = PROPERTY_SCHEME_LOG_OPTION1;
            } else if (PROPERTY_SCHEME_LOG_OPTION2.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_LOG_DEFAULT = PROPERTY_SCHEME_LOG_OPTION2;
            } else if (PROPERTY_SCHEME_LOG_OPTION3.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_LOG_DEFAULT = PROPERTY_SCHEME_LOG_OPTION3;
            } else if (PROPERTY_SCHEME_LOG_OPTION4.equalsIgnoreCase(preferenceValue)) {
                PROPERTY_SCHEME_LOG_DEFAULT = PROPERTY_SCHEME_LOG_OPTION4;
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_TEXT_ENTRY);
            }
        }
        showEffectiveDefault(preferenceKey, PROPERTY_SCHEME_LOG_DEFAULT);


        // Percentile Range

        preferenceKey = getPreferenceContextKey(PROPERTY_RANGE_PERCENTILE_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            try {
                double value = Double.parseDouble(preferenceValue);
                if (value > 0 && value <= 100.0) {
                    ColorManipulationDefaults.PROPERTY_RANGE_PERCENTILE_DEFAULT = value;
                } else {
                    String msg = "Percentile must be between 0 and 100";
                    errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, msg);
                }
            } catch (NumberFormatException ex) {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_NUMBER);
            }
        }
        showEffectiveDefault(preferenceKey, Double.toString(PROPERTY_RANGE_PERCENTILE_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_1_SIGMA_BUTTON_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_1_SIGMA_BUTTON_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_1_SIGMA_BUTTON_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_2_SIGMA_BUTTON_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_2_SIGMA_BUTTON_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_2_SIGMA_BUTTON_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_3_SIGMA_BUTTON_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_3_SIGMA_BUTTON_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_3_SIGMA_BUTTON_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_95_PERCENT_BUTTON_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_95_PERCENT_BUTTON_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_95_PERCENT_BUTTON_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_100_PERCENT_BUTTON_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_100_PERCENT_BUTTON_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_100_PERCENT_BUTTON_DEFAULT));


        // Scheme Selector Options

        preferenceKey = getPreferenceContextKey(PROPERTY_SCHEME_VERBOSE_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_SCHEME_VERBOSE_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_SCHEME_VERBOSE_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_SCHEME_SORT_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_SCHEME_SORT_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_SCHEME_SORT_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_SCHEME_CATEGORIZE_DISPLAY_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_SCHEME_CATEGORIZE_DISPLAY_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_SCHEME_CATEGORIZE_DISPLAY_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_SCHEME_SHOW_DISABLED_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_SCHEME_SHOW_DISABLED_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_SCHEME_SHOW_DISABLED_DEFAULT));


        // Sliders editor Options

        preferenceKey = getPreferenceContextKey(PROPERTY_SLIDERS_ZOOM_IN_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_SLIDERS_ZOOM_IN_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_SLIDERS_ZOOM_IN_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_SLIDERS_SHOW_INFORMATION_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_SLIDERS_SHOW_INFORMATION_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_SLIDERS_SHOW_INFORMATION_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_ZOOM_VERTICAL_BUTTONS_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_ZOOM_VERTICAL_BUTTONS_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_ZOOM_VERTICAL_BUTTONS_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_INFORMATION_BUTTON_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            if (isValidBooleanString(preferenceValue)) {
                ColorManipulationDefaults.PROPERTY_INFORMATION_BUTTON_DEFAULT = Boolean.parseBoolean(preferenceValue);
            } else {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_BOOLEAN);
            }
        }
        showEffectiveDefault(preferenceKey, Boolean.toString(PROPERTY_INFORMATION_BUTTON_DEFAULT));


        // RGB Options

        preferenceKey = getPreferenceContextKey(PROPERTY_RGB_OPTIONS_MIN_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            try {
                double value = Double.parseDouble(preferenceValue);
                ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_DEFAULT = value;
            } catch (NumberFormatException ex) {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_NUMBER);
            }
        }
        showEffectiveDefault(preferenceKey, Double.toString(PROPERTY_RGB_OPTIONS_MIN_DEFAULT));


        preferenceKey = getPreferenceContextKey(PROPERTY_RGB_OPTIONS_MAX_KEY);
        preferenceValue = Config.instance().preferences().get(preferenceKey, null);
        if (preferenceValue != null && preferenceValue.length() > 0) {
            try {
                double value = Double.parseDouble(preferenceValue);

                if (value > ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_DEFAULT) {
                    ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MAX_DEFAULT = value;
                } else {
                    String msg = PROPERTY_RGB_OPTIONS_MAX_KEY + " cannot be less than " + PROPERTY_RGB_OPTIONS_MIN_KEY;
                    errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, msg);
                }
            } catch (NumberFormatException ex) {
                errorMsg += createPropertyErrorMessage(preferenceKey, preferenceValue, INVALID_NUMBER);
            }

        }
        showEffectiveDefault(preferenceKey, Double.toString(PROPERTY_RGB_OPTIONS_MAX_DEFAULT));


        if (errorMsg != null && errorMsg.length() > 0) {
            notifyPropertyError(errorMsg);
        }
    }


    private static String getPreferenceContextKey(String key) {
        return getApplicationContextId() + "." + key;
    }


    private static void showEffectiveDefault(String key, String value) {

        // todo the following block isn't operational but could be used in some form if needed in some kind of information page.
        // this is just used during software development to show what parameters are available to be put in the snap.properties file.
        boolean showParameterInfo = false;
        if (showParameterInfo) {
            Logger logger = Logger.getLogger(ColorSchemeUtils.class.getName());
            logger.log(Level.INFO, key + "=" + value);
            System.out.println("# " + key + "=" + value);
        }
        //end todo
    }


    private static String createPropertyErrorMessage(String propertyKey, String propertyValue, String message1) {
            return  "WARNING!!: " + message1 + "<br>" + propertyKey + "=" + propertyValue +"<br><br>";
    }


    private static void notifyPropertyError(String msg) {
        ColorUtils.showErrorDialog("<html>The following invalid values were found in the properties file:<br><br> " + msg);
    }


    private static boolean isValidBooleanString(String booleanString) {
        if (booleanString != null) {
            if (booleanString.trim().equalsIgnoreCase("true") || booleanString.trim().equalsIgnoreCase("false")) {
                return true;
            }
        }

        return false;
    }
}
