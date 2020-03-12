package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.ui.product.ProductSceneView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.esa.snap.core.datamodel.ColorManipulationDefaults.*;

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

        if (configuration != null && configuration.getPropertyBool(PROPERTY_SCHEME_AUTO_APPLY_KEY, PROPERTY_SCHEME_AUTO_APPLY_DEFAULT)) {
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


}
