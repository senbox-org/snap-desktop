package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.ui.product.ProductSceneView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

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

    public static void setImageInfoToDefaultColor(PropertyMap configuration, ImageInfo defaultImageInfo, ProductSceneView productSceneView) {

        boolean imageInfoSet = false;

        ColorSchemeManager colorPaletteSchemes = ColorSchemeManager.getDefault();

        if (ColorSchemeDefaults.isPropertySchemeAutoApply(configuration)) {
            ColorSchemeInfo colorSchemeInfo = getColorPaletteInfoByBandNameLookup(productSceneView);

            if (colorSchemeInfo != null) {
                imageInfoSet = ColorSchemeUtils.setImageInfoToColorScheme(colorSchemeInfo, productSceneView);

                if (imageInfoSet) {
                    colorPaletteSchemes.setSelected(colorSchemeInfo);
                }
            }
        }

        if (!imageInfoSet) {
            colorPaletteSchemes.reset();
            setImageInfoToGeneralColor(configuration, defaultImageInfo, productSceneView);
        }

    }


    public static boolean isRangeFromDataNonScheme(PropertyMap configuration) {
        String generalRange = ColorSchemeDefaults.getPropertyGeneralRange(configuration);

        if (generalRange != null && generalRange.equals(ColorSchemeDefaults.OPTION_RANGE_FROM_DATA)) {
            return true;
        } else {
            return false;
        }
    }


    public static boolean isGeneralLogScaled(ColorPaletteDef colorPaletteDef, ProductSceneView productSceneView) {

        boolean logScaled = false;

        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();
        String logScaledOption = ColorSchemeDefaults.getPropertyLogScaledOption(configuration);

        if (logScaledOption != null) {
            switch (logScaledOption) {
                case ColorSchemeDefaults.OPTION_LOG_TRUE:
                    logScaled = true;
                    break;
                case ColorSchemeDefaults.OPTION_LOG_FALSE:
                    logScaled = false;
                    break;
                case ColorSchemeDefaults.OPTION_LOG_FROM_CPD:
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

        String fileCategory = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_GENERAL_CPD_KEY, ColorSchemeDefaults.PROPERTY_GENERAL_CPD_DEFAULT);
        if (fileCategory != null) {
            switch (fileCategory) {
                case ColorSchemeDefaults.OPTION_COLOR_GRAY_SCALE:
                    filename = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_KEY, ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_DEFAULT);
                    break;
                case ColorSchemeDefaults.OPTION_COLOR_STANDARD:
                    filename = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_CPD_STANDARD_KEY, ColorSchemeDefaults.PROPERTY_CPD_STANDARD_DEFAULT);
                    break;
                case ColorSchemeDefaults.OPTION_COLOR_UNIVERSAL:
                    filename = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_KEY, ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_DEFAULT);
                    break;
                case ColorSchemeDefaults.OPTION_COLOR_ANOMALIES:
                    filename = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_KEY, ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_DEFAULT);
                    break;
                default:
                    filename = ColorSchemeDefaults.CPD_DEFAULT;
            }
        }

        File auxDir = ColorSchemeUtils.getDirNameColorPalettes().toFile();

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

        ColorPaletteDef colorPaletteDef = null;

        if (defaultCpdFile.exists()) {
            try {
                colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(defaultCpdFile);

            } catch (IOException e) {
            }
        }

        if (colorPaletteDef != null) {
            Stx stx = productSceneView.getRaster().getStx();

            double min;
            double max;

            if (isRangeFromDataNonScheme(configuration)) {
                min = stx.getMinimum();
                max = stx.getMaximum();
            } else {
                min = colorPaletteDef.getMinDisplaySample();
                max = colorPaletteDef.getMaxDisplaySample();
            }


            boolean logScaledSource = colorPaletteDef.isLogScaled();
            boolean logScaledTarget = isGeneralLogScaled(colorPaletteDef, productSceneView);

            productSceneView.getImageInfo().setColorPaletteDef(colorPaletteDef,
                    min,
                    max,
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


    public static ColorSchemeInfo getColorPaletteInfoByBandNameLookup(ProductSceneView productSceneView) {

        ColorSchemeManager colorSchemeManager = ColorSchemeManager.getDefault();
        if (colorSchemeManager != null) {

            String bandName = productSceneView.getBaseImageLayer().getName().trim();
            bandName = bandName.substring(bandName.indexOf(" ")).trim();

            ArrayList<ColorSchemeLookupInfo> colorSchemeLookupInfos = colorSchemeManager.getColorSchemeLookupInfos();
            for (ColorSchemeLookupInfo colorSchemeLookupInfo : colorSchemeLookupInfos) {
                if (colorSchemeLookupInfo.isMatch(bandName)) {
                    return colorSchemeManager.getColorSchemeInfoBySchemeId(colorSchemeLookupInfo.getScheme_id());
                }
            }
        }

        return null;
    }


    public static boolean getLogScaledFromScheme(PropertyMap configuration, ColorSchemeInfo colorSchemeInfo, ColorPaletteDef colorPaletteDef) {
        boolean logScaled = false;

        String schemeLogScaling = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_SCHEME_LOG_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_LOG_DEFAULT);
        if (schemeLogScaling != null) {
            switch (schemeLogScaling) {
                case ColorSchemeDefaults.OPTION_LOG_TRUE:
                    logScaled = true;
                    break;
                case ColorSchemeDefaults.OPTION_LOG_FALSE:
                    logScaled = false;
                    break;
                case ColorSchemeDefaults.OPTION_LOG_FROM_CPD:
                    if (colorPaletteDef != null) {
                        logScaled = colorPaletteDef.isLogScaled();
                    }
                    break;
                case ColorSchemeDefaults.OPTION_LOG_FROM_SCHEME:
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
                ColorSchemeDefaults.PROPERTY_SCHEME_CPD_KEY,
                ColorSchemeDefaults.PROPERTY_SCHEME_CPD_DEFAULT);

        switch (schemeCpdOption) {
            case ColorSchemeDefaults.OPTION_COLOR_STANDARD_SCHEME:
                cpdFileName = colorSchemeInfo.getCpdFilename(false);
                break;
            case ColorSchemeDefaults.OPTION_COLOR_UNIVERSAL_SCHEME:
                cpdFileName = colorSchemeInfo.getCpdFilename(true);
                break;
            case ColorSchemeDefaults.OPTION_COLOR_GRAY_SCALE:
                cpdFileName = configuration.getPropertyString(
                        ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_KEY,
                        ColorSchemeDefaults.PROPERTY_CPD_GRAY_SCALE_DEFAULT);
                break;
            case ColorSchemeDefaults.OPTION_COLOR_STANDARD:
                cpdFileName = configuration.getPropertyString(
                        ColorSchemeDefaults.PROPERTY_CPD_STANDARD_KEY,
                        ColorSchemeDefaults.PROPERTY_CPD_STANDARD_DEFAULT);
                break;
            case ColorSchemeDefaults.OPTION_COLOR_UNIVERSAL:
                cpdFileName = configuration.getPropertyString(
                        ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_KEY,
                        ColorSchemeDefaults.PROPERTY_CPD_UNIVERSAL_DEFAULT);
                break;
            case ColorSchemeDefaults.OPTION_COLOR_ANOMALIES:
                cpdFileName = configuration.getPropertyString(
                        ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_KEY,
                        ColorSchemeDefaults.PROPERTY_CPD_ANOMALIES_DEFAULT);
                break;
            default:
                break;
        }

        return cpdFileName;
    }


    public static boolean setImageInfoToColorScheme(ColorSchemeInfo colorSchemeInfo, ProductSceneView productSceneView) {

        if (colorSchemeInfo == null || productSceneView == null) {
            return false;
        }

        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();

        String cpdFileName = getCdpFileNameFromSchemeSelection(configuration, colorSchemeInfo);


        ColorPaletteDef colorPaletteDef = null;

        File auxDir = ColorSchemeUtils.getDirNameColorPalettes().toFile();

        if (cpdFileName != null) {
            File cpdFile = new File(auxDir, cpdFileName);
            try {
                colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(cpdFile);

            } catch (IOException e) {
            }
        }


        // Determine range: min and max

        double min;
        double max;

        Stx stx = productSceneView.getRaster().getStx();

        String schemeRange = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_RANGE_DEFAULT);
        switch (schemeRange) {
            case ColorSchemeDefaults.OPTION_RANGE_FROM_SCHEME:
                min = colorSchemeInfo.getMinValue();
                if (min == ColorSchemeDefaults.DOUBLE_NULL) {
                    min = stx.getMinimum();
                }
                max = colorSchemeInfo.getMaxValue();
                if (max == ColorSchemeDefaults.DOUBLE_NULL) {
                    max = stx.getMaximum();
                }
                break;
            case ColorSchemeDefaults.OPTION_RANGE_FROM_DATA:
                min = stx.getMinimum();
                max = stx.getMaximum();
                break;
            case ColorSchemeDefaults.OPTION_RANGE_FROM_CPD:
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


    public static Path getDirNameColorPalettes() {
        return SystemUtils.getAuxDataPath().resolve(ColorSchemeDefaults.DIR_NAME_COLOR_PALETTES);
    }

    public static Path getDirNameColorSchemes() {
        return SystemUtils.getAuxDataPath().resolve(ColorSchemeDefaults.DIR_NAME_COLOR_SCHEMES);
    }


}
