package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.product.ProductSceneView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.esa.snap.core.datamodel.ColorPaletteSchemes.*;

public class ColorSchemeUtils {

    /**
     * Top level method which will set the desired color palette, range and log scaling within the imageInfo
     * of the given productSceneView.
     *
     * This is called by either the reset button within the ColorManipulation GUI or when a new View
     * Window is opened for a band.
     *
     * @param auxDir
     * @param defaultImageInfo
     * @param productSceneView
     */

    public static void setToDefaultColor(File auxDir, ImageInfo defaultImageInfo, ProductSceneView productSceneView) {

        ColorSchemeManager colorPaletteSchemes = ColorSchemeManager.getDefault();
        ColorSchemeManager.getDefault().init(auxDir);

        if (isApplyScheme(productSceneView)) {
            ColorPaletteInfo matchingColorPaletteInfo = setToDefaultColorScheme(auxDir, defaultImageInfo, productSceneView);

            if (matchingColorPaletteInfo != null) {
                colorPaletteSchemes.setSelected(matchingColorPaletteInfo);
            } else {
                colorPaletteSchemes.reset();
                setToDefaultColorNoScheme(auxDir, defaultImageInfo, productSceneView);
            }

        } else {
            colorPaletteSchemes.reset();

            setToDefaultColorNoScheme(auxDir, defaultImageInfo, productSceneView);
            productSceneView.setColorPaletteInfo(null);
        }

    }



    public static ColorPaletteInfo setToDefaultColorScheme(File auxDir, ImageInfo defaultImageInfo, ProductSceneView productSceneView) {

        ColorPaletteInfo matchingColorPaletteInfo = getColorPaletteInfoByBandNameLookup(auxDir, productSceneView);

        if (matchingColorPaletteInfo != null) {
            boolean imageInfoSet = ColorSchemeUtils.setImageInfoToColorScheme(auxDir, matchingColorPaletteInfo, productSceneView);

            if (imageInfoSet) {
                return matchingColorPaletteInfo;
            }
        }

        return null;
    }



    /**
     * Determines from preferences whether or not to use a color scheme based on a band name look up.
     *
     * @param productSceneView
     * @return
     */
    public static boolean isApplyScheme(ProductSceneView productSceneView) {
        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();
        return configuration.getPropertyBool(ColorSchemeManager.PROPERTY_AUTO_APPLY_SCHEMES_KEY, true);
    }



    public static boolean isGeneralRangeFromData(ProductSceneView productSceneView) {
        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();

        String generalRange = configuration.getPropertyString(ColorSchemeManager.PROPERTY_GENERAL_RANGE_KEY, ColorSchemeManager.PROPERTY_GENERAL_RANGE_DEFAULT);

        if (generalRange != null && generalRange.equals(ColorSchemeManager.RANGE_FROM_DATA)) {
            return true;
        } else {
            return false;
        }
    }


    public static boolean isGeneralLogScaled(ColorPaletteDef colorPaletteDef, ProductSceneView productSceneView) {

        boolean logScaled = false;
        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();

        String generalLogScaled = configuration.getPropertyString(ColorSchemeManager.PROPERTY_GENERAL_LOG_KEY, ColorSchemeManager.PROPERTY_GENERAL_LOG_DEFAULT);

        if (generalLogScaled != null) {
            switch (generalLogScaled) {
                case LOG_TRUE:
                    logScaled = true;
                    break;
                case LOG_FALSE:
                    logScaled = false;
                    break;
                case LOG_FROM_CPD:
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


    public static File getDefaultCpd(File auxDir, ProductSceneView productSceneView) {
        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();
        String fileName = configuration.getPropertyString(ColorSchemeManager.PROPERTY_GENERAL_CPD_KEY, null);

        if (fileName != null) {
            switch (fileName) {
                case GRAY_SCALE:
                    fileName = configuration.getPropertyString(ColorSchemeManager.PROPERTY_GRAY_SCALE_CPD_KEY, null);
                    break;
                case STANDARD_COLOR:
                    fileName = configuration.getPropertyString(ColorSchemeManager.PROPERTY_STANDARD_COLOR_CPD_KEY, null);
                    break;
                case UNIVERSAL_COLOR:
                    fileName = configuration.getPropertyString(ColorSchemeManager.PROPERTY_COLOR_BLIND_CPD_KEY, null);
                    break;
                case OTHER_COLOR:
                    fileName = configuration.getPropertyString(ColorSchemeManager.PROPERTY_OTHER_CPD_KEY, null);
                    break;
                default:
                    fileName = ColorSchemeManager.DEFAULT_CPD_FILENAME;
            }
        }

        if (fileName != null) {
            File defaultCpd = new File(auxDir, fileName);

            if (defaultCpd.exists()) {
                return defaultCpd;
            }
        }

        return null;
    }




    public static void setToDefaultColorNoScheme(File auxDir, ImageInfo defaultImageInfo, ProductSceneView productSceneView) {

        boolean imageInfoSet = false;

        File defaultCpdFile = getDefaultCpd(auxDir, productSceneView);


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

            if (isGeneralRangeFromData(productSceneView)) {
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





    public static ColorPaletteInfo getColorPaletteInfoByBandNameLookup(File auxDir, ProductSceneView productSceneView) {

//        PropertyMap configuration = sceneImage.getConfiguration();

        ColorPaletteInfo matchingColorPaletteInfo = null;

        ColorSchemeManager colorPaletteSchemes = ColorSchemeManager.getDefault();
        ColorSchemeManager.getDefault().init(auxDir);

        if (colorPaletteSchemes != null) {

            String bandName = productSceneView.getBaseImageLayer().getName().trim();
            bandName = bandName.substring(bandName.indexOf(" ")).trim();

            ArrayList<ColorPaletteInfo> defaultSchemes = colorPaletteSchemes.getColorSchemeLutInfos();

            final String WILDCARD = new String("*");

            for (ColorPaletteInfo colorPaletteInfo : defaultSchemes) {
                String cpdName = colorPaletteInfo.getName().trim();

                if (matchingColorPaletteInfo == null || (matchingColorPaletteInfo != null && colorPaletteInfo.isOverRide())) {
                    if (bandName.equals(cpdName)) {
                        matchingColorPaletteInfo = colorPaletteInfo;
                    } else if (cpdName.contains(WILDCARD)) {
                        if (!cpdName.startsWith(WILDCARD) && cpdName.endsWith(WILDCARD)) {
                            String basename = new String(cpdName.substring(0, cpdName.length() - 1));
                            if (bandName.startsWith(basename)) {
                                matchingColorPaletteInfo = colorPaletteInfo;
                            }
                        } else if (cpdName.startsWith(WILDCARD) && !cpdName.endsWith(WILDCARD)) {
                            String basename = new String(cpdName.substring(1, cpdName.length()));
                            if (bandName.endsWith(basename)) {
                                matchingColorPaletteInfo = colorPaletteInfo;
                            }
                        } else if (cpdName.startsWith(WILDCARD) && cpdName.endsWith(WILDCARD)) {
                            String basename = new String(cpdName.substring(1, cpdName.length() - 1));
                            if (bandName.contains(basename)) {
                                matchingColorPaletteInfo = colorPaletteInfo;
                            }
                        } else {
                            String basename = new String(cpdName);
                            String basenameSplit[] = basename.split("\\" + WILDCARD);
                            if (basenameSplit.length == 2 && basenameSplit[0].length() > 0 && basenameSplit[1].length() > 0) {
                                if (bandName.startsWith(basenameSplit[0]) && bandName.endsWith(basenameSplit[1])) {
                                    matchingColorPaletteInfo = colorPaletteInfo;
                                }
                            }
                        }
                    }
                }

            }
        }


        return matchingColorPaletteInfo;
    }


    public static boolean setImageInfoToColorScheme(File auxDir, ColorPaletteInfo colorPaletteInfo, ProductSceneView productSceneView) {

        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();

        if (colorPaletteInfo == null) {
            return false;
        }

        String cpdFileName = null;

        String schemeCpd = configuration.getPropertyString(ColorSchemeManager.PROPERTY_SCHEME_CPD_KEY, ColorSchemeManager.PROPERTY_SCHEME_CPD_DEFAULT);
        switch (schemeCpd) {
            case ColorSchemeManager.STANDARD_SCHEME:
                cpdFileName = colorPaletteInfo.getCpdFilename(false);
                break;
            case ColorSchemeManager.UNIVERSAL_SCHEME:
                cpdFileName = colorPaletteInfo.getCpdFilename(true);
                break;
            case ColorSchemeManager.GRAY_SCALE:
                cpdFileName = configuration.getPropertyString(ColorSchemeManager.PROPERTY_GRAY_SCALE_CPD_KEY, null);
                break;
            case ColorSchemeManager.STANDARD_COLOR:
                cpdFileName = configuration.getPropertyString(ColorSchemeManager.PROPERTY_STANDARD_COLOR_CPD_KEY, null);
                break;
            case ColorSchemeManager.UNIVERSAL_COLOR:
                cpdFileName = configuration.getPropertyString(ColorSchemeManager.PROPERTY_COLOR_BLIND_CPD_KEY, null);
                break;
            case ColorSchemeManager.OTHER_COLOR:
                cpdFileName = configuration.getPropertyString(ColorSchemeManager.PROPERTY_OTHER_CPD_KEY, null);
                break;
            default:
                break;
        }

        ColorPaletteDef colorPaletteDef = null;

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

        String schemeRange = configuration.getPropertyString(ColorSchemeManager.PROPERTY_SCHEME_RANGE_KEY, ColorSchemeManager.PROPERTY_SCHEME_RANGE_DEFAULT);
        switch (schemeRange) {
            case ColorSchemeManager.RANGE_FROM_SCHEME:
                min = colorPaletteInfo.getMinValue();
                if (min == ColorSchemeManager.DOUBLE_NULL) {
                    min = stx.getMinimum();
                }
                max = colorPaletteInfo.getMaxValue();
                if (max == ColorSchemeManager.DOUBLE_NULL) {
                    max = stx.getMaximum();
                }
                break;
            case ColorSchemeManager.RANGE_FROM_DATA:
                min = stx.getMinimum();
                max = stx.getMaximum();
                break;
            case ColorSchemeManager.RANGE_FROM_CPD:
                min = colorPaletteDef.getMinDisplaySample();
                max = colorPaletteDef.getMaxDisplaySample();
                break;
            default:
                min = stx.getMinimum();
                max = stx.getMaximum();
                break;
        }


        boolean logScaled = false;

        String schemeLogScaling = configuration.getPropertyString(ColorSchemeManager.PROPERTY_SCHEME_LOG_KEY, ColorSchemeManager.PROPERTY_SCHEME_LOG_DEFAULT);
        if (schemeLogScaling != null) {
            switch (schemeLogScaling) {
                case ColorSchemeManager.LOG_TRUE:
                    logScaled = true;
                    break;
                case ColorSchemeManager.LOG_FALSE:
                    logScaled = false;
                    break;
                case ColorSchemeManager.LOG_FROM_CPD:
                    if (colorPaletteDef != null) {
                        logScaled = colorPaletteDef.isLogScaled();
                    }
                    break;
                case ColorSchemeManager.LOG_FROM_SCHEME:
                    logScaled = colorPaletteInfo.isLogScaled();
                    break;
                default:
                    logScaled = false;
            }
        }


        if (colorPaletteDef != null) {
            productSceneView.getImageInfo().setColorPaletteDef(colorPaletteDef,
                    min,
                    max,
                    true, //colorPaletteDef.isAutoDistribute(),
                    colorPaletteDef.isLogScaled(),
                    logScaled);
            productSceneView.getImageInfo().setLogScaled(logScaled);

            productSceneView.setColorPaletteInfo(colorPaletteInfo);
            return true;

        }

        return false;
    }

}
