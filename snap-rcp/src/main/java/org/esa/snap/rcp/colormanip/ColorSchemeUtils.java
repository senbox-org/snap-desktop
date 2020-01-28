package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.product.ProductSceneView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class ColorSchemeUtils {

    /**
     * Top level method which will set the desired color palette, range and log scaling within the imageInfo
     * of the given productSceneView.
     *
     * This is called by either the reset button within the ColorManipulation GUI or when a new View
     * Window is opened for a band.
     *
     * @param defaultImageInfo
     * @param productSceneView
     */

    public static void setToDefaultColor(PropertyMap configuration, ImageInfo defaultImageInfo, ProductSceneView productSceneView) {

        ColorSchemeManager colorPaletteSchemes = ColorSchemeManager.getDefault();

        if (ColorSchemeDefaults.isPreferencesAutoApplyScheme(configuration)) {
            ColorPaletteInfo matchingColorPaletteInfo = setToDefaultColorScheme(productSceneView);

            if (matchingColorPaletteInfo != null) {
                colorPaletteSchemes.setSelected(matchingColorPaletteInfo);
            } else {
                colorPaletteSchemes.reset();
                setToDefaultColorNoScheme(configuration, defaultImageInfo, productSceneView);
            }

        } else {
            colorPaletteSchemes.reset();

            setToDefaultColorNoScheme(configuration, defaultImageInfo, productSceneView);
            productSceneView.setColorPaletteInfo(null);
        }

    }



    public static ColorPaletteInfo setToDefaultColorScheme(ProductSceneView productSceneView) {

        ColorPaletteInfo matchingColorPaletteInfo = getColorPaletteInfoByBandNameLookup(productSceneView);

        if (matchingColorPaletteInfo != null) {
            boolean imageInfoSet = ColorSchemeUtils.setImageInfoToColorScheme(matchingColorPaletteInfo, productSceneView);

            if (imageInfoSet) {
                return matchingColorPaletteInfo;
            }
        }

        return null;
    }






    public static boolean isRangeFromDataNonScheme(PropertyMap configuration) {
        String generalRange = ColorSchemeDefaults.getPreferencesRangeNonScheme(configuration);

        if (generalRange != null && generalRange.equals(ColorSchemeDefaults.RANGE_FROM_DATA)) {
            return true;
        } else {
            return false;
        }
    }


    public static boolean isLogScaledNonScheme(ColorPaletteDef colorPaletteDef, ProductSceneView productSceneView) {

        boolean logScaled = false;

        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();
        String generalLogScaled = ColorSchemeDefaults.getPreferencesLogScaledNonScheme(configuration);

        if (generalLogScaled != null) {
            switch (generalLogScaled) {
                case ColorSchemeDefaults.LOG_TRUE:
                    logScaled = true;
                    break;
                case ColorSchemeDefaults.LOG_FALSE:
                    logScaled = false;
                    break;
                case ColorSchemeDefaults.LOG_FROM_CPD:
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
        String fileName = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_GENERAL_CPD_KEY, null);

        if (fileName != null) {
            switch (fileName) {
                case ColorSchemeDefaults.GRAY_SCALE:
                    fileName = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_GRAY_SCALE_CPD_KEY, null);
                    break;
                case ColorSchemeDefaults.STANDARD_COLOR:
                    fileName = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_STANDARD_COLOR_CPD_KEY, null);
                    break;
                case ColorSchemeDefaults.UNIVERSAL_COLOR:
                    fileName = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_COLOR_BLIND_CPD_KEY, null);
                    break;
                case ColorSchemeDefaults.OTHER_COLOR:
                    fileName = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_OTHER_CPD_KEY, null);
                    break;
                default:
                    fileName = ColorSchemeDefaults.DEFAULT_CPD_FILENAME;
            }
        }

        File colorPaletteDir = ColorSchemeDefaults.getColorPalettesDir().toFile();
        if (fileName != null) {
            File defaultCpd = new File(colorPaletteDir, fileName);

            if (defaultCpd.exists()) {
                return defaultCpd;
            }
        }

        return null;
    }




    public static void setToDefaultColorNoScheme(PropertyMap configuration, ImageInfo defaultImageInfo, ProductSceneView productSceneView) {

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
            boolean logScaledTarget = isLogScaledNonScheme(colorPaletteDef, productSceneView);

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





    public static ColorPaletteInfo getColorPaletteInfoByBandNameLookup(ProductSceneView productSceneView) {

        ColorPaletteInfo matchingColorPaletteInfo = null;

        ColorSchemeManager colorPaletteSchemes = ColorSchemeManager.getDefault();

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


    public static boolean setImageInfoToColorScheme(ColorPaletteInfo colorPaletteInfo, ProductSceneView productSceneView) {

        PropertyMap configuration = productSceneView.getSceneImage().getConfiguration();

        if (colorPaletteInfo == null) {
            return false;
        }

        String cpdFileName = null;

        String schemeCpd = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_SCHEME_CPD_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_CPD_DEFAULT);
        switch (schemeCpd) {
            case ColorSchemeDefaults.STANDARD_SCHEME:
                cpdFileName = colorPaletteInfo.getCpdFilename(false);
                break;
            case ColorSchemeDefaults.UNIVERSAL_SCHEME:
                cpdFileName = colorPaletteInfo.getCpdFilename(true);
                break;
            case ColorSchemeDefaults.GRAY_SCALE:
                cpdFileName = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_GRAY_SCALE_CPD_KEY, null);
                break;
            case ColorSchemeDefaults.STANDARD_COLOR:
                cpdFileName = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_STANDARD_COLOR_CPD_KEY, null);
                break;
            case ColorSchemeDefaults.UNIVERSAL_COLOR:
                cpdFileName = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_COLOR_BLIND_CPD_KEY, null);
                break;
            case ColorSchemeDefaults.OTHER_COLOR:
                cpdFileName = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_OTHER_CPD_KEY, null);
                break;
            default:
                break;
        }

        ColorPaletteDef colorPaletteDef = null;

        File colorPalettesDir = ColorSchemeDefaults.getColorPalettesDir().toFile();
        if (cpdFileName != null) {

            File cpdFile = new File(colorPalettesDir, cpdFileName);
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
            case ColorSchemeDefaults.RANGE_FROM_SCHEME:
                min = colorPaletteInfo.getMinValue();
                if (min == ColorSchemeDefaults.DOUBLE_NULL) {
                    min = stx.getMinimum();
                }
                max = colorPaletteInfo.getMaxValue();
                if (max == ColorSchemeDefaults.DOUBLE_NULL) {
                    max = stx.getMaximum();
                }
                break;
            case ColorSchemeDefaults.RANGE_FROM_DATA:
                min = stx.getMinimum();
                max = stx.getMaximum();
                break;
            case ColorSchemeDefaults.RANGE_FROM_CPD:
                min = colorPaletteDef.getMinDisplaySample();
                max = colorPaletteDef.getMaxDisplaySample();
                break;
            default:
                min = stx.getMinimum();
                max = stx.getMaximum();
                break;
        }


        boolean logScaled = false;

        String schemeLogScaling = configuration.getPropertyString(ColorSchemeDefaults.PROPERTY_SCHEME_LOG_KEY, ColorSchemeDefaults.PROPERTY_SCHEME_LOG_DEFAULT);
        if (schemeLogScaling != null) {
            switch (schemeLogScaling) {
                case ColorSchemeDefaults.LOG_TRUE:
                    logScaled = true;
                    break;
                case ColorSchemeDefaults.LOG_FALSE:
                    logScaled = false;
                    break;
                case ColorSchemeDefaults.LOG_FROM_CPD:
                    if (colorPaletteDef != null) {
                        logScaled = colorPaletteDef.isLogScaled();
                    }
                    break;
                case ColorSchemeDefaults.LOG_FROM_SCHEME:
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
