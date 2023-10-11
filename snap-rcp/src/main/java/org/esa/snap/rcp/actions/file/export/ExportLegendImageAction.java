/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.actions.file.export;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.glayer.Layer;
import org.esa.snap.core.datamodel.ColorSchemeInfo;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.ImageLegend;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.layer.ColorBarLayer;
import org.esa.snap.core.layer.ColorBarLayerType;
import org.esa.snap.core.param.*;
import org.esa.snap.core.util.MetadataUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.colormanip.ColorSchemeUtils;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.List;

/**
 * @author Brockmann Consult
 * @author Daniel Knowles
 * @version $Revision$ $Date$
 */
//MAY2020 - Daniel Knowles - Major revision to color bar legend tools

@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.actions.file.export.ExportLegendImageAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportLegendImageAction_MenuText",
        popupText = "#CTL_ExportLegendImageAction_PopupText",
        lazy = false

)
@ActionReferences({
        @ActionReference(path = "Menu/File/Export/Other", position = 10),
        @ActionReference(path = "Context/ProductSceneView", position = 90)
})
@NbBundle.Messages({
        "CTL_ExportLegendImageAction_MenuText=" + ColorBarLayerType.COLOR_BAR_LEGEND_NAME,
        "CTL_ExportLegendImageAction_PopupText=Export " + ColorBarLayerType.COLOR_BAR_LEGEND_NAME,
        "CTL_ExportLegendImageAction_ShortDescription=Export the " + ColorBarLayerType.COLOR_BAR_LEGEND_NAME_LOWER_CASE + " of the current view as an image."
})

public class ExportLegendImageAction extends AbstractExportImageAction {

    private static final String HELP_ID = "exportLegendImageFile";


    private final static String[][] IMAGE_FORMAT_DESCRIPTIONS = {
            PNG_FORMAT_DESCRIPTION,
            BMP_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION
    };


    // Make different keys for export parameters so it doesn't affect color bar layer
    // Keys named differently from preferences to not overwrite preferences

    // Title and Units Text
    private static final String PROPERTY_TITLE_KEY2 = ColorBarLayerType.PROPERTY_TITLE_KEY + ".export";

    private static final String PROPERTY_UNITS_KEY2 = ColorBarLayerType.PROPERTY_UNITS_KEY + ".export";
    private static final String PROPERTY_UNITS_NULL_KEY2 = ColorBarLayerType.PROPERTY_UNITS_NULL_KEY + ".export";


    private static final String PROPERTY_CONVERT_CARET_KEY2 = ColorBarLayerType.PROPERTY_CONVERT_CARET_KEY + ".export";
    private static final String PROPERTY_UNITS_PARENTHESIS_KEY2 = ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_KEY + ".export";

    // Orientation
    private static final String PROPERTY_ORIENTATION_KEY2 = ColorBarLayerType.PROPERTY_ORIENTATION_KEY + ".export";
    private static final String PROPERTY_LOCATION_TITLE_VERTICAL_KEY2 = ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_KEY + ".export";
    private static final String PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY2 = ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY + ".export";

    // Tick Label Values
    private static final String PROPERTY_LABEL_VALUES_MODE_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_COUNT_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_ACTUAL_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY + ".export";
    private static final String PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY2 = ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_SCALING_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY + ".export";
    private static final String PROPERTY_WEIGHT_TOLERANCE_KEY2 = ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_KEY + ".export";

    // Image Scaling Section
    private static final String PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2 = ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY + ".export";
    private static final String PROPERTY_EXPORT_LEGEND_WIDTH_KEY2 = ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_KEY + ".export";
    private static final String PROPERTY_COLORBAR_LENGTH_KEY2 = ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_KEY + ".export";
    private static final String PROPERTY_COLORBAR_WIDTH_KEY2 = ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_KEY + ".export";

    // Title Section
    private static final String PROPERTY_TITLE_SHOW_KEY2 = ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY + ".export";
    private static final String PROPERTY_TITLE_FONT_SIZE_KEY2 = ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY + ".export";
    private static final String PROPERTY_TITLE_FONT_BOLD_KEY2 = ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY + ".export";
    private static final String PROPERTY_TITLE_FONT_ITALIC_KEY2 = ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY + ".export";
    private static final String PROPERTY_TITLE_FONT_NAME_KEY2 = ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY + ".export";
    private static final String PROPERTY_TITLE_COLOR_KEY2 = ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY + ".export";

    // Units Section
    private static final String PROPERTY_UNITS_SHOW_KEY2 = ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY + ".export";
    private static final String PROPERTY_UNITS_FONT_SIZE_KEY2 = ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_KEY + ".export";
    private static final String PROPERTY_UNITS_FONT_BOLD_KEY2 = ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_KEY + ".export";
    private static final String PROPERTY_UNITS_FONT_ITALIC_KEY2 = ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_KEY + ".export";
    private static final String PROPERTY_UNITS_FONT_NAME_KEY2 = ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_KEY + ".export";
    private static final String PROPERTY_UNITS_FONT_COLOR_KEY2 = ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_KEY + ".export";

    // Tick-Mark Labels Section
    private static final String PROPERTY_LABELS_SHOW_KEY2 = ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY + ".export";
    private static final String PROPERTY_LABELS_FONT_SIZE_KEY2 = ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY + ".export";
    private static final String PROPERTY_LABELS_FONT_BOLD_KEY2 = ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY + ".export";
    private static final String PROPERTY_LABELS_FONT_ITALIC_KEY2 = ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY + ".export";
    private static final String PROPERTY_LABELS_FONT_NAME_KEY2 = ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY + ".export";
    private static final String PROPERTY_LABELS_FONT_COLOR_KEY2 = ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY + ".export";

    // Tickmarks Section
    private static final String PROPERTY_TICKMARKS_SHOW_KEY2 = ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY + ".export";
    private static final String PROPERTY_TICKMARKS_LENGTH_KEY2 = ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY + ".export";
    private static final String PROPERTY_TICKMARKS_WIDTH_KEY2 = ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY + ".export";
    private static final String PROPERTY_TICKMARKS_COLOR_KEY2 = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY + ".export";

    // Backdrop Section
    private static final String PROPERTY_BACKDROP_SHOW_KEY2 = ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY + ".export";
    private static final String PROPERTY_BACKDROP_TRANSPARENCY_KEY2 = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY + ".export";
    private static final String PROPERTY_BACKDROP_COLOR_KEY2 = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY + ".export";

    // Palette Border Section
    private static final String PROPERTY_PALETTE_BORDER_SHOW_KEY2 = ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY + ".export";
    private static final String PROPERTY_PALETTE_BORDER_WIDTH_KEY2 = ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY + ".export";
    private static final String PROPERTY_PALETTE_BORDER_COLOR_KEY2 = ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY + ".export";

    // Legend Border Section
    private static final String PROPERTY_LEGEND_BORDER_SHOW_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY + ".export";
    private static final String PROPERTY_LEGEND_BORDER_WIDTH_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_KEY + ".export";
    private static final String PROPERTY_LEGEND_BORDER_COLOR_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY + ".export";

    private static final String PROPERTY_LEGEND_BORDER_GAP_FACTOR_TOP_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_KEY + ".export";
    private static final String PROPERTY_LEGEND_BORDER_GAP_FACTOR_BOTTOM_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_KEY + ".export";
    private static final String PROPERTY_LEGEND_BORDER_GAP_FACTOR_LEFTSIDE_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_KEY + ".export";
    private static final String PROPERTY_LEGEND_BORDER_GAP_FACTOR_RIGHTSIDE_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_KEY + ".export";
    private static final String PROPERTY_LEGEND_TITLE_GAP_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_KEY + ".export";
    private static final String PROPERTY_LEGEND_LABEL_GAP_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_KEY + ".export";

    // These are all the color keys which get bypasses dependent on the PROPERTY_EXPORT_USE_BW_COLOR_KEY2
    private static final String PROPERTY_EXPORT_USE_BW_COLOR_KEY2 = ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_KEY + ".export";


    private SnapFileFilter[] imageFileFilters;

    private ParamGroup legendParamGroup;
    private ImageLegend imageLegend;
    private boolean showEditorFirst;
    private boolean blackWhiteColor;
    private boolean populateLabelValuesTextfield;
    private int legendWidth;
    private boolean useLegendWidth;
    private boolean discrete;
    private static boolean legendInitialized;


    private ParamChangeListener paramChangeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private Lookup.Result<ProductSceneView> result;


    public ExportLegendImageAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportLegendImageAction(Lookup lookup) {

        super(Bundle.CTL_ExportLegendImageAction_MenuText(), HELP_ID);

        putValue("popupText", Bundle.CTL_ExportLegendImageAction_PopupText());
        imageFileFilters = new SnapFileFilter[IMAGE_FORMAT_DESCRIPTIONS.length];
        for (int i = 0; i < IMAGE_FORMAT_DESCRIPTIONS.length; i++) {
            imageFileFilters[i] = createFileFilter(IMAGE_FORMAT_DESCRIPTIONS[i]);
        }

        result = lookup.lookupResult(ProductSceneView.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);


        paramChangeListener = createParamChangeListener();
    }


    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new ExportLegendImageAction(lookup);
    }


    @Override
    public void actionPerformed(ActionEvent e) {

        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        PropertyMap configuration = view.getSceneImage().getConfiguration();

        // initialize these parameters from the preferences
        showEditorFirst = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_KEY,
                ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_DEFAULT);

        blackWhiteColor = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_KEY,
                ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_DEFAULT);

        populateLabelValuesTextfield = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY,
                ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_DEFAULT);

        useLegendWidth = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_DEFAULT);

        legendWidth = configuration.getPropertyInt(ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_DEFAULT);


        discrete = SnapApp.getDefault().getSelectedProductSceneView().getImageInfo().getColorPaletteDef().isDiscrete();

        legendInitialized = false;


        if (showEditorFirst == true) {
            initImageLegend(view);

            final ImageLegendDialog dialog = new ImageLegendDialog(legendParamGroup,
                    imageLegend,
                    true,
                    getHelpCtx().getHelpID());
            dialog.show();
        }

        exportImage(imageFileFilters);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        boolean enabled = view != null && !view.isRGB();
        setEnabled(enabled);
    }


    private void initImageLegend(ProductSceneView view) {

        imageLegend = null;

//        SystemUtils.LOG.severe("Test severe message Danny");
//        SystemUtils.LOG.info("Test info message Danny");
//        SystemUtils.LOG.warning("Test warning message Danny");


        // Look for the existence of the ColorBar Layer and get a copy of its imageLegend

        List<Layer> layers = SnapApp.getDefault().getSelectedProductSceneView().getRootLayer().getChildren();
        for (Layer layer : layers) {
            //   System.out.println("layerName=" + layer.getName());

            if (ColorBarLayerType.COLOR_BAR_LAYER_NAME.equals(layer.getName())) {
                //   System.out.println("Found ColorBar layer");

                ColorBarLayer colorBarLayer = (ColorBarLayer) layer;
                if (colorBarLayer != null) {
                    ImageLegend imageLegendFromLayer = colorBarLayer.getImageLegend();
                    if (imageLegendFromLayer != null) {
                        imageLegend = imageLegendFromLayer.getCopyOfImageLegend();
                    }
                }
                break;
            }
        }


        // If null then set imageLegend based on the preferences defaults
        if (imageLegend == null) {

            final RasterDataNode raster = view.getRaster();
            PropertyMap configuration = view.getSceneImage().getConfiguration();

            if (configuration != null) {
                imageLegend = new ImageLegend(raster.getImageInfo(), raster);
                initLegendWithPreferences(view, configuration);
            }
        }


        if (imageLegend != null) {

            if (discrete) {
                imageLegend.setDistributionType(ColorBarLayerType.DISTRIB_EXACT_STR);
            }

            // this will initialize the custom label values
            if (populateLabelValuesTextfield) {
                String distributionTypeOriginal = imageLegend.getDistributionType();


                if (ColorBarLayerType.DISTRIB_MANUAL_STR.equals(distributionTypeOriginal)) {
                    if (imageLegend.getCustomLabelValues() == null || imageLegend.getCustomLabelValues().length() == 0) {
                        imageLegend.setDistributionType(ColorBarLayerType.DISTRIB_EVEN_STR);
                    }
                }

                imageLegend.setLayerScaling(100.0);

                imageLegend.createImage(new Dimension(legendWidth, legendWidth), useLegendWidth, true, false);

                imageLegend.setDistributionType(distributionTypeOriginal);
            }



            legendParamGroup = createLegendParamGroup(imageLegend, paramChangeListener, blackWhiteColor, populateLabelValuesTextfield, useLegendWidth, legendWidth);


            updateEnablement();
        }
    }


    @Override
    protected void configureFileChooser(SnapFileChooser fileChooser, ProductSceneView view, String imageBaseName) {

        if (!showEditorFirst == true) {
            initImageLegend(view);
        }

        if (imageLegend != null) {
            fileChooser.setDialogTitle(SnapApp.getDefault().getInstanceName() + " - export " + ColorBarLayerType.COLOR_BAR_LEGEND_NAME); /*I18N*/

            fileChooser.setCurrentFilename(imageBaseName + "_legend");

            fileChooser.setAccessory(createImageLegendAccessory(
                    fileChooser,
                    legendParamGroup,
                    imageLegend, getHelpCtx().getHelpID()));
        }
    }


    private void initLegendWithPreferences(ProductSceneView view, PropertyMap configuration) {

        final RasterDataNode raster = view.getRaster();

        imageLegend.initLegendWithPreferences(configuration, raster);

        boolean autoApplySchemes = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_KEY,
                ColorBarLayerType.PROPERTY_SCHEME_AUTO_APPLY_DEFAULT);

        boolean schemeLabelsApply = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_KEY,
                ColorBarLayerType.PROPERTY_SCHEME_LABELS_APPLY_DEFAULT);

        String unitsNullValue = configuration.getPropertyString(ColorBarLayerType.PROPERTY_UNITS_NULL_KEY,
                ColorBarLayerType.PROPERTY_UNITS_NULL_DEFAULT);

        String description = raster.getDescription();
        String bandname = raster.getName();
        String units = raster.getUnit();
        if (units == null) {
            units = unitsNullValue;
        }
        float wavelength = raster.getProduct().getBand(raster.getName()).getSpectralWavelength();
        float angle = raster.getProduct().getBand(raster.getName()).getAngularValue();
        boolean allowWavelengthZero = true;

        if (autoApplySchemes || schemeLabelsApply) {
            String bandName = view.getBaseImageLayer().getName().trim();
//            String mission = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS);
//            if (mission == null || mission.length() == 0) {
//                mission = raster.getProduct().getProductType();
//            }
            ColorSchemeInfo schemeInfo = ColorSchemeInfo.getColorPaletteInfoByBandNameLookup(bandName, raster.getProduct());

//        if (!legendInitialized) {
            if (autoApplySchemes) {//auto-apply
                if (schemeInfo != null) {
//                    if (schemeInfo.getColorBarLabels() != null && schemeInfo.getColorBarLabels().trim().length() > 0) {
//                        imageLegend.setDistributionType(ColorBarLayerType.DISTRIB_MANUAL_STR);
//                        imageLegend.setCustomLabelValues(schemeInfo.getColorBarLabels());
//                    }

                    if (schemeInfo.getColorBarTitle() != null && schemeInfo.getColorBarTitle().trim().length() > 0) {
                        imageLegend.setTitle(schemeInfo.getColorBarTitle());
                    }


                    if (schemeInfo.getColorBarUnits() != null && schemeInfo.getColorBarUnits().trim().length() > 0) {
                        imageLegend.setUnits(schemeInfo.getColorBarUnits());

                    }

                    if (schemeInfo.getColorBarLengthStr() != null && schemeInfo.getColorBarLengthStr().trim().length() > 0) {
                        imageLegend.setColorBarLength(Integer.parseInt(schemeInfo.getColorBarLengthStr()));
                    }
                    if (schemeInfo.getColorBarLabelScalingStr() != null && schemeInfo.getColorBarLabelScalingStr().trim().length() > 0) {
                        imageLegend.setScalingFactor(Double.parseDouble(schemeInfo.getColorBarLabelScalingStr()));
                    }
                }
            }

            if (schemeLabelsApply) {//auto-apply
                if (schemeInfo != null) {
                    if (schemeInfo.getColorBarLabels() != null && schemeInfo.getColorBarLabels().trim().length() > 0) {
                        imageLegend.setDistributionType(ColorBarLayerType.DISTRIB_MANUAL_STR);
                        imageLegend.setCustomLabelValues(schemeInfo.getColorBarLabels());
                    }
                }
            }

//            String convertedTitle = ColorSchemeInfo.getColorBarTitle(imageLegend.getTitleText(), bandname, description, wavelength, angle, units, allowWavelengthZero);
            String convertedTitle = MetadataUtils.getReplacedStringAllVariables(imageLegend.getTitleText(), raster, "", MetadataUtils.INFO_PARAM_WAVE);
            imageLegend.setTitle(convertedTitle);


//            String convertedUnits = ColorSchemeInfo.getColorBarTitle(imageLegend.getUnitsText(), bandname, description, wavelength, angle, units, allowWavelengthZero);
            String convertedUnits = MetadataUtils.getReplacedStringAllVariables(imageLegend.getUnitsText(), raster, "", MetadataUtils.INFO_PARAM_WAVE);
            imageLegend.setUnits(convertedUnits);




//            legendInitialized = true;
        }




//        imageLegend.initLegendWithPreferences(configuration, raster);
    }


    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        transferParamsToImageLegend(legendParamGroup, imageLegend);


        imageLegend.setTransparencyEnabled(isTransparencySupportedByFormat(imageFormat));
        imageLegend.setLayerScaling(100.0);

        useLegendWidth = (Boolean) legendParamGroup.getParameter(PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2).getValue();
        legendWidth = (Integer) legendParamGroup.getParameter(PROPERTY_EXPORT_LEGEND_WIDTH_KEY2).getValue();
        return imageLegend.createImage(new Dimension(legendWidth, legendWidth), useLegendWidth, true, false);
    }

    @Override
    protected boolean isEntireImageSelected() {
        return true;
    }

    private static ParamGroup createLegendParamGroup(ImageLegend imageLegend, ParamChangeListener paramChangeListener, boolean blackWhiteColor, boolean populateLabelValuesTextfield, boolean useLegendWidth, int legendWidth) {

        ParamGroup paramGroup = new ParamGroup();
        Parameter param;


        // Colors Override

        param = new Parameter(PROPERTY_EXPORT_USE_BW_COLOR_KEY2, blackWhiteColor);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_LABEL);
        paramGroup.addParameter(param);


        // Title and Units Text

        param = new Parameter(PROPERTY_TITLE_KEY2, imageLegend.getTitleText());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_LABEL);
        param.getProperties().setNumCols(24);
        param.getProperties().setNullValueAllowed(true);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_UNITS_KEY2, imageLegend.getUnitsText());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_LABEL);
        param.getProperties().setNumCols(24);
        param.getProperties().setNullValueAllowed(true);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_UNITS_NULL_KEY2, imageLegend.getUnitsNull());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_NULL_LABEL);
        param.getProperties().setNumCols(24);
        param.getProperties().setNullValueAllowed(true);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_CONVERT_CARET_KEY2, imageLegend.isConvertCaret());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_CONVERT_CARET_LABEL);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_UNITS_PARENTHESIS_KEY2, imageLegend.isUnitsParenthesis());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_LABEL);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);


        // Orientation

        param = new Parameter(PROPERTY_ORIENTATION_KEY2, imageLegend.getOrientation());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_ORIENTATION_LABEL);
        param.getProperties().setValueSet(new String[]{ColorBarLayerType.PROPERTY_ORIENTATION_OPTION1, ColorBarLayerType.PROPERTY_ORIENTATION_OPTION2, ColorBarLayerType.PROPERTY_ORIENTATION_OPTION3});
        param.getProperties().setValueSetBound(true);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LOCATION_TITLE_VERTICAL_KEY2, imageLegend.getTitleVerticalAnchor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_LABEL);
        param.getProperties().setValueSet(new String[]{ColorBarLayerType.VERTICAL_TITLE_LEFT, ColorBarLayerType.VERTICAL_TITLE_RIGHT,
                ColorBarLayerType.VERTICAL_TITLE_TOP, ColorBarLayerType.VERTICAL_TITLE_BOTTOM});
        param.getProperties().setValueSetBound(true);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY2, imageLegend.isReversePalette());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_LABEL);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);


        // Tick Label Values

        param = new Parameter(PROPERTY_LABEL_VALUES_MODE_KEY2, imageLegend.getDistributionType());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_LABEL);
        param.getProperties().setValueSet(new String[]{ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_OPTION1,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_OPTION2,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_OPTION3
        });
        param.getProperties().setValueSetBound(true);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LABEL_VALUES_COUNT_KEY2, imageLegend.getTickMarkCount());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_LABEL);
        param.getProperties().setMinValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MIN);
        param.getProperties().setMaxValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_MAX);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);

        String labelValuesPreferencesDefault = "";
        if (populateLabelValuesTextfield) {
            labelValuesPreferencesDefault = imageLegend.getCustomLabelValues();
        }
        param = new Parameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2, imageLegend.getCustomLabelValues());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY2, populateLabelValuesTextfield);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_LABEL);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LABEL_VALUES_SCALING_KEY2, imageLegend.getScalingFactor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_LABEL);
        param.getProperties().setMinValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MIN);
        param.getProperties().setMaxValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MAX);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY2, imageLegend.getDecimalPlaces());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_LABEL);
        param.getProperties().setMinValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MIN);
        param.getProperties().setMaxValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_MAX);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY2, imageLegend.isDecimalPlacesForce());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_LABEL);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_WEIGHT_TOLERANCE_KEY2, imageLegend.getWeightTolerance());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_LABEL);
        paramGroup.addParameter(param);


        // Image Scaling Section

        param = new Parameter(PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2, useLegendWidth);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_LABEL);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_EXPORT_LEGEND_WIDTH_KEY2, legendWidth);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_COLORBAR_LENGTH_KEY2, imageLegend.getColorBarLength());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_LABEL);
        param.getProperties().setMinValue(ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_VALUE_MIN);
        param.getProperties().setMaxValue(ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_VALUE_MAX);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_COLORBAR_WIDTH_KEY2, imageLegend.getColorBarWidth());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_LABEL);
        param.getProperties().setMinValue(ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_MIN);
        param.getProperties().setMaxValue(ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_MAX);
        paramGroup.addParameter(param);


        // Title Section

        param = new Parameter(PROPERTY_TITLE_SHOW_KEY2, imageLegend.isShowTitle());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_SHOW_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_TITLE_FONT_SIZE_KEY2, imageLegend.getTitleFontSize());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_LABEL);
        param.getProperties().setMinValue(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_VALUE_MIN);
        param.getProperties().setMaxValue(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_VALUE_MAX);
        paramGroup.addParameter(param);


        boolean titleFontTypeBold = ColorBarLayer.isFontTypeBold(imageLegend.getTitleFontType());
        param = new Parameter(PROPERTY_TITLE_FONT_BOLD_KEY2, titleFontTypeBold);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_LABEL);
        paramGroup.addParameter(param);

        boolean titleFontTypeItalicBold = ColorBarLayer.isFontTypeItalic(imageLegend.getTitleFontType());
        param = new Parameter(PROPERTY_TITLE_FONT_ITALIC_KEY2, titleFontTypeItalicBold);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_TITLE_FONT_NAME_KEY2, imageLegend.getTitleFontName());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_LABEL);
        param.getProperties().setValueSet(new String[]{ColorBarLayerType.FONT_NAME_SANSERIF,
                ColorBarLayerType.FONT_NAME_SERIF,
                ColorBarLayerType.FONT_NAME_COURIER,
                ColorBarLayerType.FONT_NAME_MONOSPACED
        });
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_TITLE_COLOR_KEY2, imageLegend.getTitleColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_COLOR_LABEL);
        paramGroup.addParameter(param);


        // Units Section

        param = new Parameter(PROPERTY_UNITS_SHOW_KEY2, imageLegend.isShowUnits());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_SHOW_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_UNITS_FONT_SIZE_KEY2, imageLegend.getUnitsFontSize());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_LABEL);
        param.getProperties().setMinValue(ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_VALUE_MIN);
        param.getProperties().setMaxValue(ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_VALUE_MAX);
        paramGroup.addParameter(param);


        boolean unitsFontTypeBold = ColorBarLayer.isFontTypeBold(imageLegend.getUnitsFontType());
        param = new Parameter(PROPERTY_UNITS_FONT_BOLD_KEY2, unitsFontTypeBold);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_LABEL);
        paramGroup.addParameter(param);

        boolean unitsFontTypeItalicBold = ColorBarLayer.isFontTypeItalic(imageLegend.getUnitsFontType());
        param = new Parameter(PROPERTY_UNITS_FONT_ITALIC_KEY2, unitsFontTypeItalicBold);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_UNITS_FONT_NAME_KEY2, imageLegend.getUnitsFontName());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_LABEL);
        param.getProperties().setValueSet(new String[]{ColorBarLayerType.FONT_NAME_SANSERIF,
                ColorBarLayerType.FONT_NAME_SERIF,
                ColorBarLayerType.FONT_NAME_COURIER,
                ColorBarLayerType.FONT_NAME_MONOSPACED
        });
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_UNITS_FONT_COLOR_KEY2, imageLegend.getUnitsColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_LABEL);
        paramGroup.addParameter(param);


        // Tick-Mark Labels Section

        param = new Parameter(PROPERTY_LABELS_SHOW_KEY2, imageLegend.isLabelsShow());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABELS_SHOW_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_LABELS_FONT_SIZE_KEY2, imageLegend.getLabelsFontSize());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_LABEL);
        param.getProperties().setMinValue(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_MIN);
        param.getProperties().setMaxValue(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_VALUE_MAX);
        paramGroup.addParameter(param);


        boolean labelsFontTypeBold = ColorBarLayer.isFontTypeBold(imageLegend.getLabelsFontType());
        param = new Parameter(PROPERTY_LABELS_FONT_BOLD_KEY2, labelsFontTypeBold);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_LABEL);
        paramGroup.addParameter(param);

        boolean labelsFontTypeItalicBold = ColorBarLayer.isFontTypeItalic(imageLegend.getLabelsFontType());
        param = new Parameter(PROPERTY_LABELS_FONT_ITALIC_KEY2, labelsFontTypeItalicBold);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_LABELS_FONT_NAME_KEY2, imageLegend.getLabelsFontName());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_LABEL);
        param.getProperties().setValueSet(new String[]{ColorBarLayerType.FONT_NAME_SANSERIF,
                ColorBarLayerType.FONT_NAME_SERIF,
                ColorBarLayerType.FONT_NAME_COURIER,
                ColorBarLayerType.FONT_NAME_MONOSPACED
        });
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_LABELS_FONT_COLOR_KEY2, imageLegend.getLabelsColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_LABEL);
        paramGroup.addParameter(param);


        // Tickmarks Section

        param = new Parameter(PROPERTY_TICKMARKS_SHOW_KEY2, imageLegend.isTickmarkShow());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_TICKMARKS_LENGTH_KEY2, imageLegend.getTickmarkLength());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_TICKMARKS_WIDTH_KEY2, imageLegend.getTickmarkWidth());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_TICKMARKS_COLOR_KEY2, imageLegend.getTickmarkColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_LABEL);
        paramGroup.addParameter(param);


        // Backdrop Section

        param = new Parameter(PROPERTY_BACKDROP_SHOW_KEY2, imageLegend.isBackdropShow());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_BACKDROP_TRANSPARENCY_KEY2, imageLegend.getBackdropTransparency());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_LABEL);
        param.getProperties().setMinValue(0.0f);
        param.getProperties().setMaxValue(1.0f);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_BACKDROP_COLOR_KEY2, imageLegend.getBackdropColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_LABEL);
        paramGroup.addParameter(param);


        // Palette Border Section

        param = new Parameter(PROPERTY_PALETTE_BORDER_SHOW_KEY2, imageLegend.isBorderShow());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_PALETTE_BORDER_WIDTH_KEY2, imageLegend.getBorderWidth());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_PALETTE_BORDER_COLOR_KEY2, imageLegend.getBorderColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_LABEL);
        paramGroup.addParameter(param);


        // Legend Border Section

        param = new Parameter(PROPERTY_LEGEND_BORDER_SHOW_KEY2, imageLegend.isBackdropBorderShow());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LEGEND_BORDER_WIDTH_KEY2, imageLegend.getBackdropBorderWidth());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LEGEND_BORDER_COLOR_KEY2, imageLegend.getBackdropBorderColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_LABEL);
        paramGroup.addParameter(param);


        // Legend Border Section

        param = new Parameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_TOP_KEY2, imageLegend.getTopBorderGapFactor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_BOTTOM_KEY2, imageLegend.getTopBorderGapFactor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_LEFTSIDE_KEY2, imageLegend.getTopBorderGapFactor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_RIGHTSIDE_KEY2, imageLegend.getTopBorderGapFactor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LEGEND_TITLE_GAP_KEY2, imageLegend.getTitleGapFactor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LEGEND_LABEL_GAP_KEY2, imageLegend.getLabelGapFactor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_LABEL);
        paramGroup.addParameter(param);



        return paramGroup;
    }


    private ParamChangeListener createParamChangeListener() {
        return new ParamChangeListener() {

            public void parameterValueChanged(ParamChangeEvent event) {
                updateUIState(event.getParameter().getName());
            }
        };
    }


    private void updateUIState(String parameterName) {

        if (PROPERTY_LABEL_VALUES_MODE_KEY2.equals(parameterName) || PROPERTY_LABEL_VALUES_COUNT_KEY2.equals(parameterName)) {
            Object distributionType = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_MODE_KEY2).getValue();

            if (!ColorBarLayerType.DISTRIB_MANUAL_STR.equals(distributionType)) {
                if (imageLegend.getDistributionType() == null || !imageLegend.getDistributionType().equals(distributionType)) {
                    imageLegend.setDistributionType((String) distributionType);
                }

                if (ColorBarLayerType.DISTRIB_EVEN_STR.equals(distributionType)) {
                    Object tickMarkCount = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).getValue();
                    imageLegend.setTickMarkCount((Integer) tickMarkCount);
                }

                // update custom labels
                imageLegend.setLayerScaling(100.0);

                useLegendWidth = (Boolean) legendParamGroup.getParameter(PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2).getValue();
                legendWidth = (Integer) legendParamGroup.getParameter(PROPERTY_EXPORT_LEGEND_WIDTH_KEY2).getValue();
                imageLegend.createImage(new Dimension(legendWidth, legendWidth), useLegendWidth, true, false);
            }

            updateEnablement();
        }


        populateLabelValuesTextfield = (Boolean) legendParamGroup.getParameter(PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY2).getValue();
        if (populateLabelValuesTextfield) {
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setValue(imageLegend.getCustomLabelValues(), null);
        }


        if (PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2.equals(parameterName)) {
            updateEnablement();
        }


    }

    private void updateEnablement() {

        Object distributionType = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_MODE_KEY2).getValue();

        if (ColorBarLayerType.DISTRIB_MANUAL_STR.equals(distributionType)) {
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setUIEnabled(true);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_WEIGHT_TOLERANCE_KEY2).setUIEnabled(false);
        } else if (ColorBarLayerType.DISTRIB_EXACT_STR.equals(distributionType)) {
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY2).setUIEnabled(true);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY2).setUIEnabled(true);
            legendParamGroup.getParameter(PROPERTY_WEIGHT_TOLERANCE_KEY2).setUIEnabled(true);
        } else {
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).setUIEnabled(true);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY2).setUIEnabled(true);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY2).setUIEnabled(true);
            legendParamGroup.getParameter(PROPERTY_WEIGHT_TOLERANCE_KEY2).setUIEnabled(true);
        }


        legendParamGroup.getParameter(PROPERTY_TITLE_KEY2).setUIEnabled(true);

        boolean useLegendWidthTmp = (Boolean) legendParamGroup.getParameter(PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2).getValue();
        legendParamGroup.getParameter(PROPERTY_EXPORT_LEGEND_WIDTH_KEY2).setUIEnabled(useLegendWidthTmp);

    }





    private static JComponent createImageLegendAccessory(final JFileChooser fileChooser,
                                                         final ParamGroup legendParamGroup,
                                                         final ImageLegend imageLegend, String helpId) {
        final JButton button = new JButton("Properties...");
        button.setMnemonic('P');
        button.addActionListener(e -> {
            final SnapFileFilter fileFilter = (SnapFileFilter) fileChooser.getFileFilter();
            final ImageLegendDialog dialog = new ImageLegendDialog(
                    legendParamGroup,
                    imageLegend,
                    isTransparencySupportedByFormat(fileFilter.getFormatName()), helpId);
            dialog.show();
        });
        final JPanel accessory = new JPanel(new BorderLayout());
        accessory.setBorder(new EmptyBorder(3, 3, 3, 3));
        accessory.add(button, BorderLayout.NORTH);
        return accessory;
    }

    private static void transferParamsToImageLegend(ParamGroup legendParamGroup, ImageLegend imageLegend) {

        Object value;

        imageLegend.setLayerScaling(100.0);


        // Title and Units Text


        value = legendParamGroup.getParameter(PROPERTY_TITLE_KEY2).getValue();
        imageLegend.setTitle((String) value);

        value = legendParamGroup.getParameter(PROPERTY_UNITS_KEY2).getValue();
        imageLegend.setUnits((String) value);

        value = legendParamGroup.getParameter(PROPERTY_UNITS_NULL_KEY2).getValue();
        imageLegend.setUnitsNull((String) value);


        value = legendParamGroup.getParameter(PROPERTY_CONVERT_CARET_KEY2).getValue();
        imageLegend.setConvertCaret((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_UNITS_PARENTHESIS_KEY2).getValue();
        imageLegend.setUnitsParenthesis((Boolean) value);

        // Orientation

        value = legendParamGroup.getParameter(PROPERTY_ORIENTATION_KEY2).getValue();
        imageLegend.setOrientation((String) value);


        value = legendParamGroup.getParameter(PROPERTY_LOCATION_TITLE_VERTICAL_KEY2).getValue();
        imageLegend.setTitleVerticalAnchor((String) value);

        value = legendParamGroup.getParameter(PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY2).getValue();
        imageLegend.setReversePalette((Boolean) value);


        // Tick Label Values

        // Set this prior to set distributionType in order to update custom labels if needed
        value = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).getValue();
        imageLegend.setTickMarkCount((Integer) value);

        value = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_MODE_KEY2).getValue();
        imageLegend.setDistributionType((String) value);

        value = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).getValue();
        imageLegend.setCustomLabelValues((String) value);

        value = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_SCALING_KEY2).getValue();
        imageLegend.setScalingFactor((Double) value);

        value = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY2).getValue();
        imageLegend.setDecimalPlaces((Integer) value);

        value = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY2).getValue();
        imageLegend.setDecimalPlacesForce((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_WEIGHT_TOLERANCE_KEY2).getValue();
        imageLegend.setWeightTolerance((Double) value);


        // Image Scaling Section
        value = legendParamGroup.getParameter(PROPERTY_COLORBAR_LENGTH_KEY2).getValue();
        imageLegend.setColorBarLength((Integer) value);

        value = legendParamGroup.getParameter(PROPERTY_COLORBAR_WIDTH_KEY2).getValue();
        imageLegend.setColorBarWidth((Integer) value);


        // Title Section
        value = legendParamGroup.getParameter(PROPERTY_TITLE_SHOW_KEY2).getValue();
        imageLegend.setShowTitle((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_TITLE_FONT_SIZE_KEY2).getValue();
        imageLegend.setTitleFontSize((Integer) value);

        boolean titleFontBold = (Boolean) legendParamGroup.getParameter(PROPERTY_TITLE_FONT_BOLD_KEY2).getValue();
        boolean titleFontItalic = (Boolean) legendParamGroup.getParameter(PROPERTY_TITLE_FONT_ITALIC_KEY2).getValue();
        imageLegend.setTitleFontType(ColorBarLayer.getFontType(titleFontItalic, titleFontBold));


        value = legendParamGroup.getParameter(PROPERTY_TITLE_FONT_NAME_KEY2).getValue();
        imageLegend.setTitleFontName((String) value);

        value = legendParamGroup.getParameter(PROPERTY_TITLE_COLOR_KEY2).getValue();
        imageLegend.setTitleColor((Color) value);


        // Units Section
        value = legendParamGroup.getParameter(PROPERTY_UNITS_SHOW_KEY2).getValue();
        imageLegend.setShowUnits((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_UNITS_FONT_SIZE_KEY2).getValue();
        imageLegend.setUnitsFontSize((Integer) value);


        boolean unitsFontBold = (Boolean) legendParamGroup.getParameter(PROPERTY_UNITS_FONT_BOLD_KEY2).getValue();
        boolean unitsFontItalic = (Boolean) legendParamGroup.getParameter(PROPERTY_UNITS_FONT_ITALIC_KEY2).getValue();
        imageLegend.setUnitsFontType(ColorBarLayer.getFontType(unitsFontItalic, unitsFontBold));


        value = legendParamGroup.getParameter(PROPERTY_UNITS_FONT_NAME_KEY2).getValue();
        imageLegend.setUnitsFontName((String) value);

        value = legendParamGroup.getParameter(PROPERTY_UNITS_FONT_COLOR_KEY2).getValue();
        imageLegend.setUnitsColor((Color) value);


        // Tick-Mark Labels Section
        value = legendParamGroup.getParameter(PROPERTY_LABELS_SHOW_KEY2).getValue();
        imageLegend.setLabelsShow((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_LABELS_FONT_SIZE_KEY2).getValue();
        imageLegend.setLabelsFontSize((Integer) value);


        boolean labelsFontBold = (Boolean) legendParamGroup.getParameter(PROPERTY_LABELS_FONT_BOLD_KEY2).getValue();
        boolean labelsFontItalic = (Boolean) legendParamGroup.getParameter(PROPERTY_LABELS_FONT_ITALIC_KEY2).getValue();
        imageLegend.setLabelsFontType(ColorBarLayer.getFontType(labelsFontItalic, labelsFontBold));


        value = legendParamGroup.getParameter(PROPERTY_LABELS_FONT_NAME_KEY2).getValue();
        imageLegend.setLabelsFontName((String) value);

        value = legendParamGroup.getParameter(PROPERTY_LABELS_FONT_COLOR_KEY2).getValue();
        imageLegend.setLabelsColor((Color) value);


        // Tickmarks Section
        value = legendParamGroup.getParameter(PROPERTY_TICKMARKS_SHOW_KEY2).getValue();
        imageLegend.setTickmarkShow((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_TICKMARKS_LENGTH_KEY2).getValue();
        imageLegend.setTickmarkLength((Integer) value);

        value = legendParamGroup.getParameter(PROPERTY_TICKMARKS_WIDTH_KEY2).getValue();
        imageLegend.setTickmarkWidth((Integer) value);

        value = legendParamGroup.getParameter(PROPERTY_TICKMARKS_COLOR_KEY2).getValue();
        imageLegend.setTickmarkColor((Color) value);


        // Backdrop Section
        value = legendParamGroup.getParameter(PROPERTY_BACKDROP_SHOW_KEY2).getValue();
        imageLegend.setBackdropShow((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_BACKDROP_TRANSPARENCY_KEY2).getValue();
        imageLegend.setBackdropTransparency(((Number) value).floatValue());

        value = legendParamGroup.getParameter(PROPERTY_BACKDROP_COLOR_KEY2).getValue();
        imageLegend.setBackdropColor((Color) value);


        // Palette Border Section
        value = legendParamGroup.getParameter(PROPERTY_PALETTE_BORDER_SHOW_KEY2).getValue();
        imageLegend.setBorderShow((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_PALETTE_BORDER_WIDTH_KEY2).getValue();
        imageLegend.setBorderWidth((Integer) value);

        value = legendParamGroup.getParameter(PROPERTY_PALETTE_BORDER_COLOR_KEY2).getValue();
        imageLegend.setBorderColor((Color) value);


        // Legend Border Section
        value = legendParamGroup.getParameter(PROPERTY_LEGEND_BORDER_SHOW_KEY2).getValue();
        imageLegend.setBackdropBorderShow((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_LEGEND_BORDER_WIDTH_KEY2).getValue();
        imageLegend.setBackdropBorderWidth((Integer) value);

        value = legendParamGroup.getParameter(PROPERTY_LEGEND_BORDER_COLOR_KEY2).getValue();
        imageLegend.setBackdropBorderColor((Color) value);


        // Legend Border Gap Section
        value = legendParamGroup.getParameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_TOP_KEY2).getValue();
        imageLegend.setTopBorderGapFactor((Double) value);

        value = legendParamGroup.getParameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_BOTTOM_KEY2).getValue();
        imageLegend.setBottomBorderGapFactor((Double) value);

        value = legendParamGroup.getParameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_LEFTSIDE_KEY2).getValue();
        imageLegend.setLeftSideBorderGapFactor((Double) value);

        value = legendParamGroup.getParameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_RIGHTSIDE_KEY2).getValue();
        imageLegend.setRightSideBorderGapFactor((Double) value);

        value = legendParamGroup.getParameter(PROPERTY_LEGEND_TITLE_GAP_KEY2).getValue();
        imageLegend.setTitleGapFactor((Double) value);

        value = legendParamGroup.getParameter(PROPERTY_LEGEND_LABEL_GAP_KEY2).getValue();
        imageLegend.setLabelGapFactor((Double) value);


        // Override all the colors if requested
        Boolean blackWhiteColor = (Boolean) legendParamGroup.getParameter(PROPERTY_EXPORT_USE_BW_COLOR_KEY2).getValue();
        if (blackWhiteColor) {
            imageLegend.setTitleColor(Color.BLACK);
            imageLegend.setUnitsColor(Color.BLACK);
            imageLegend.setLabelsColor(Color.BLACK);
            imageLegend.setTickmarkColor(Color.BLACK);
            imageLegend.setBackdropColor(Color.WHITE);
            imageLegend.setBorderColor(Color.BLACK);
            imageLegend.setBackdropBorderColor(Color.WHITE);
        }


    }


    public static class ImageLegendDialog extends ModalDialog {

        private ImageInfo imageInfo;
        private RasterDataNode raster;

        private ImageLegend imageLegend;
        private boolean transparencyEnabled;

        private ParamGroup paramGroup;

        private Parameter bwColorOverrideParam;


        // Title and Units Text
        private Parameter titleTextParam;
        private Parameter unitsTextParam;
        private Parameter unitsNullParam;
        private Parameter convertCaretParam;
        private Parameter unitsParenthesisParam;

        // Orientation
        private Parameter orientationParam;
        private Parameter titleAnchorParam;
        private Parameter reversePaletteParam;

        // Tick Label Values
        private Parameter distributionTypeParam;
        private Parameter numberOfTicksParam;
        private Parameter labelValuesActualParam;
        private Parameter populateLabelValuesTextfieldParam;
        private Parameter labelValuesScalingParam;
        private Parameter labelValuesDecimalPlacesParam;
        private Parameter labelValuesForceDecimalPlacesParam;
        private Parameter weightToleranceParam;

        // Image Scaling Section
        private Parameter useLegendWidthParam;
        private Parameter legendWidthParam;
        private Parameter colorbarLengthParam;
        private Parameter colorbarWidthParam;


        // Title Section
        private Parameter titleShowParam;
        private Parameter titleFontSizeParam;
        private Parameter titleBoldParam;
        private Parameter titleItalicParam;
        private Parameter titleFontNameParam;
        private Parameter titleColorParam;


        // Units Section
        private Parameter unitsShowParam;
        private Parameter unitsColorParam;
        private Parameter unitsBoldParam;
        private Parameter unitsItalicParam;
        private Parameter unitsFontSizeParam;
        private Parameter unitsFontNameParam;

        // Tick-Mark Labels Section
        private Parameter labelsShowParam;
        private Parameter labelsFontSizeParam;
        private Parameter labelsBoldParam;
        private Parameter labelsItalicParam;
        private Parameter labelsFontNameParam;
        private Parameter labelsColorParam;

        // Tickmarks Section
        private Parameter tickmarksShowParam;
        private Parameter tickmarksLengthParam;
        private Parameter tickmarksWidthParam;
        private Parameter tickmarksColorParam;

        // Backdrop Section
        private Parameter backdropShowParam;
        private Parameter backgroundTransparencyParam;
        private Parameter backdropColorParam;

        // Palette Border Section
        private Parameter paletteBorderShowParam;
        private Parameter paletteBorderWidthParam;
        private Parameter paletteBorderColorParam;

        // Legend Border Section
        private Parameter legendBorderShowParam;
        private Parameter legendBorderWidthParam;
        private Parameter legendBorderColorParam;

        // Legend Border Gap Section
        private Parameter legendTitleGapFactorParam;
        private Parameter legendLabelGapFactorParam;
        private Parameter legendBorderGapFactorTopParam;
        private Parameter legendBorderGapFactorBottomParam;
        private Parameter  legendBorderGapFactorLeftsideParam;
        private Parameter legendBorderGapFactorRightsideParam;


        public ImageLegendDialog(ParamGroup paramGroup, ImageLegend imageLegend,
                                 boolean transparencyEnabled, String helpId) {
            super(SnapApp.getDefault().getMainFrame(), SnapApp.getDefault().getInstanceName() + " - " + ColorBarLayerType.COLOR_BAR_LEGEND_NAME, ID_OK_CANCEL_HELP, helpId);
//            System.out.println("helpId=" + helpId);
            this.imageInfo = imageLegend.getImageInfo();
            this.raster = imageLegend.getRaster();
            this.transparencyEnabled = transparencyEnabled;
            this.imageLegend = imageLegend;
            this.paramGroup = paramGroup;
            initParams();
            initUI();
            updateUIState();
            this.paramGroup.addParamChangeListener(event -> updateUIState());
        }

        private void updateUIState() {
//            boolean headerTextEnabled = (Boolean) titleShowParam.getValue();
//            titleTextParam.setUIEnabled(headerTextEnabled);
//
//            boolean unitsTextEnabled = (Boolean) unitsShowParam.getValue();
//            unitsTextParam.setUIEnabled(unitsTextEnabled);


            // Colors Override
            boolean bwColorOverride = (Boolean) bwColorOverrideParam.getValue();

            titleTextParam.setUIEnabled(true);
            unitsTextParam.setUIEnabled(true);


            // Title Section
            boolean titleShowParamEnabled = (Boolean) titleShowParam.getValue();
            titleFontSizeParam.setUIEnabled(titleShowParamEnabled);
            titleBoldParam.setUIEnabled(titleShowParamEnabled);
            titleItalicParam.setUIEnabled(titleShowParamEnabled);
            titleFontNameParam.setUIEnabled(titleShowParamEnabled);
            titleColorParam.setUIEnabled(titleShowParamEnabled && !bwColorOverride);

            // Units Section
            boolean unitsShowParamEnabled = (Boolean) unitsShowParam.getValue();
            unitsColorParam.setUIEnabled(unitsShowParamEnabled && !bwColorOverride);
            unitsBoldParam.setUIEnabled(unitsShowParamEnabled);
            unitsItalicParam.setUIEnabled(unitsShowParamEnabled);
            unitsFontSizeParam.setUIEnabled(unitsShowParamEnabled);
            unitsFontNameParam.setUIEnabled(unitsShowParamEnabled);


            // Tick-Mark Labels Section
            boolean labelsShowParamEnabled = (Boolean) labelsShowParam.getValue();
            labelsFontSizeParam.setUIEnabled(labelsShowParamEnabled);
            labelsBoldParam.setUIEnabled(labelsShowParamEnabled);
            labelsItalicParam.setUIEnabled(labelsShowParamEnabled);
            labelsFontNameParam.setUIEnabled(labelsShowParamEnabled);
            labelsColorParam.setUIEnabled(labelsShowParamEnabled && !bwColorOverride);


            // Tickmarks Section
            boolean tickmarksShowParamEnabled = (Boolean) tickmarksShowParam.getValue();
            tickmarksLengthParam.setUIEnabled(tickmarksShowParamEnabled);
            tickmarksWidthParam.setUIEnabled(tickmarksShowParamEnabled);
            tickmarksColorParam.setUIEnabled(tickmarksShowParamEnabled && !bwColorOverride);


            // Backdrop Section
            boolean backdropShowParamEnabled = (Boolean) backdropShowParam.getValue();
            backgroundTransparencyParam.setUIEnabled(backdropShowParamEnabled);
            backdropColorParam.setUIEnabled(backdropShowParamEnabled && !bwColorOverride);


            // Palette Border Section
            boolean paletteBorderShowParamEnabled = (Boolean) paletteBorderShowParam.getValue();
            paletteBorderWidthParam.setUIEnabled(paletteBorderShowParamEnabled);
            paletteBorderColorParam.setUIEnabled(paletteBorderShowParamEnabled && !bwColorOverride);


            // Legend Border Section
            boolean legendBorderShowParamEnabled = (Boolean) legendBorderShowParam.getValue();
            legendBorderWidthParam.setUIEnabled(legendBorderShowParamEnabled);
            legendBorderColorParam.setUIEnabled(legendBorderShowParamEnabled && !bwColorOverride);






            String orientation = (String) orientationParam.getValue();
            if (ColorBarLayerType.OPTION_VERTICAL.equals(orientation) || ColorBarLayerType.OPTION_BEST_FIT.equals(orientation)) {
                titleAnchorParam.setUIEnabled(true);
            } else {
                titleAnchorParam.setUIEnabled(false);
            }

        }

        public ParamGroup getParamGroup() {
            return paramGroup;
        }

        public void getImageLegend(ImageLegend imageLegend) {
            transferParamsToImageLegend(getParamGroup(), imageLegend);
        }

        public ImageInfo getImageInfo() {
            return imageInfo;
        }

        @Override
        protected void onOK() {
            super.onOK();
        }

        private void initUI() {

            final JButton previewButton = new JButton("Preview...");
            previewButton.setMnemonic('v');
            previewButton.addActionListener(e -> showPreview());

            final GridBagConstraints gbc = new GridBagConstraints();
            final JPanel p = GridBagUtils.createPanel();

            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets.top = 5;


            // Title and Units Text

            gbc.gridy = 0;
            gbc.gridwidth = 2;
            JLabel headerTitleSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_HEADER_TITLE_SECTION_LABEL);
            headerTitleSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_HEADER_TITLE_SECTION_TOOLTIP);
            p.add(headerTitleSectionLabel, gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(titleShowParam.getEditor().getEditorComponent(), gbc);
            titleShowParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_SHOW_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(titleTextParam.getEditor().getLabelComponent(), gbc);
            p.add(titleTextParam.getEditor().getEditorComponent(), gbc);
            titleTextParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_TOOLTIP);
            titleTextParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_TOOLTIP);


            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel headerUnitsSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_HEADER_UNITS_SECTION_LABEL);
            headerUnitsSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_HEADER_UNITS_SECTION_TOOLTIP);
            p.add(headerUnitsSectionLabel, gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(unitsShowParam.getEditor().getEditorComponent(), gbc);
            unitsShowParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_SHOW_TOOLTIP);


            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(unitsTextParam.getEditor().getLabelComponent(), gbc);
            p.add(unitsTextParam.getEditor().getEditorComponent(), gbc);
            unitsTextParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_TOOLTIP);
            unitsTextParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_TOOLTIP);


            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(unitsNullParam.getEditor().getLabelComponent(), gbc);
            p.add(unitsNullParam.getEditor().getEditorComponent(), gbc);
            unitsNullParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_NULL_TOOLTIP);
            unitsNullParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_NULL_TOOLTIP);


            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(convertCaretParam.getEditor().getEditorComponent(), gbc);
            convertCaretParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_CONVERT_CARET_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(unitsParenthesisParam.getEditor().getEditorComponent(), gbc);
            unitsParenthesisParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_PARENTHESIS_TOOLTIP);





            // Tick Label Values

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel tickLabelValuesSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_LABEL);
            tickLabelValuesSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_TOOLTIP);
            p.add(tickLabelValuesSectionLabel, gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(labelsShowParam.getEditor().getEditorComponent(), gbc);
            labelsShowParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABELS_SHOW_TOOLTIP);

            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(distributionTypeParam.getEditor().getLabelComponent(), gbc);
            p.add(distributionTypeParam.getEditor().getEditorComponent(), gbc);
            distributionTypeParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_TOOLTIP);
            distributionTypeParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_TOOLTIP);

            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(numberOfTicksParam.getEditor().getLabelComponent(), gbc);
            p.add(numberOfTicksParam.getEditor().getEditorComponent(), gbc);
            numberOfTicksParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_TOOLTIP);
            numberOfTicksParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_TOOLTIP);

            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(labelValuesActualParam.getEditor().getLabelComponent(), gbc);
            p.add(labelValuesActualParam.getEditor().getEditorComponent(), gbc);
            labelValuesActualParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_TOOLTIP);
            labelValuesActualParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(populateLabelValuesTextfieldParam.getEditor().getEditorComponent(), gbc);
            populateLabelValuesTextfieldParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_POPULATE_VALUES_TEXTFIELD_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(labelValuesScalingParam.getEditor().getLabelComponent(), gbc);
            p.add(labelValuesScalingParam.getEditor().getEditorComponent(), gbc);
            labelValuesScalingParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_TOOLTIP);
            labelValuesScalingParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(labelValuesDecimalPlacesParam.getEditor().getLabelComponent(), gbc);
            p.add(labelValuesDecimalPlacesParam.getEditor().getEditorComponent(), gbc);
            labelValuesDecimalPlacesParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TOOLTIP);
            labelValuesDecimalPlacesParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(labelValuesForceDecimalPlacesParam.getEditor().getEditorComponent(), gbc);
            labelValuesForceDecimalPlacesParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_TOOLTIP);


            // Orientation

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel orientationSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_LABEL);
            orientationSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_TOOLTIP);
            p.add(orientationSectionLabel, gbc);

            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(orientationParam.getEditor().getLabelComponent(), gbc);
            p.add(orientationParam.getEditor().getEditorComponent(), gbc);
            orientationParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_ORIENTATION_TOOLTIP);
            orientationParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_ORIENTATION_TOOLTIP);

            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(titleAnchorParam.getEditor().getLabelComponent(), gbc);
            p.add(titleAnchorParam.getEditor().getEditorComponent(), gbc);
            titleAnchorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_TOOLTIP);
            titleAnchorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_TOOLTIP);


            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(reversePaletteParam.getEditor().getEditorComponent(), gbc);
            reversePaletteParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_TOOLTIP);




            // Image Scaling Section

//            gbc.gridy++;
//            gbc.gridwidth = 2;
//            JLabel imageScalingSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_LABEL);
//            imageScalingSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_IMAGE_SCALING_SECTION_TOOLTIP);
//            p.add(imageScalingSectionLabel, gbc);



            // Sizing Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel sizingSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_LEGEND_SIZING_SECTION_LABEL);
            sizingSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_SIZING_SECTION_TOOLTIP);
            p.add(sizingSectionLabel, gbc);



            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(useLegendWidthParam.getEditor().getEditorComponent(), gbc);
            useLegendWidthParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendWidthParam.getEditor().getLabelComponent(), gbc);
            p.add(legendWidthParam.getEditor().getEditorComponent(), gbc);
            legendWidthParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_TOOLTIP);
            legendWidthParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_TOOLTIP);






            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(colorbarLengthParam.getEditor().getLabelComponent(), gbc);
            p.add(colorbarLengthParam.getEditor().getEditorComponent(), gbc);
            colorbarLengthParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_TOOLTIP);
            colorbarLengthParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_COLORBAR_LENGTH_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(colorbarWidthParam.getEditor().getLabelComponent(), gbc);
            p.add(colorbarWidthParam.getEditor().getEditorComponent(), gbc);
            colorbarWidthParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_TOOLTIP);
            colorbarWidthParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_COLORBAR_WIDTH_TOOLTIP);





            // Title Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel titleSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_TITLE_SECTION_LABEL);
            titleSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_TITLE_SECTION_TOOLTIP);
            p.add(titleSectionLabel, gbc);


            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(titleFontSizeParam.getEditor().getLabelComponent(), gbc);
            p.add(titleFontSizeParam.getEditor().getEditorComponent(), gbc);
            titleFontSizeParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_TOOLTIP);
            titleFontSizeParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_TOOLTIP);


            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(titleColorParam.getEditor().getLabelComponent(), gbc);
            p.add(titleColorParam.getEditor().getEditorComponent(), gbc);
            titleColorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_COLOR_TOOLTIP);
            titleColorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_COLOR_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(titleFontNameParam.getEditor().getLabelComponent(), gbc);
            p.add(titleFontNameParam.getEditor().getEditorComponent(), gbc);
            titleFontNameParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_TOOLTIP);
            titleFontNameParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(titleItalicParam.getEditor().getEditorComponent(), gbc);
            titleItalicParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(titleBoldParam.getEditor().getEditorComponent(), gbc);
            titleBoldParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_TOOLTIP);



            // Units Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel unitsSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_UNITS_SECTION_LABEL);
            unitsSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_UNITS_SECTION_TOOLTIP);
            p.add(unitsSectionLabel, gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(unitsFontSizeParam.getEditor().getLabelComponent(), gbc);
            p.add(unitsFontSizeParam.getEditor().getEditorComponent(), gbc);
            unitsFontSizeParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_TOOLTIP);
            unitsFontSizeParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(unitsColorParam.getEditor().getLabelComponent(), gbc);
            p.add(unitsColorParam.getEditor().getEditorComponent(), gbc);
            unitsColorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_TOOLTIP);
            unitsColorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(unitsFontNameParam.getEditor().getLabelComponent(), gbc);
            p.add(unitsFontNameParam.getEditor().getEditorComponent(), gbc);
            unitsFontNameParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_TOOLTIP);
            unitsFontNameParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(unitsItalicParam.getEditor().getEditorComponent(), gbc);
            unitsItalicParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(unitsBoldParam.getEditor().getEditorComponent(), gbc);
            unitsBoldParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_TOOLTIP);




            // Tick-Mark Labels Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel tickmarkLabelsSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_LABELS_SECTION_LABEL);
            tickmarkLabelsSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_LABELS_SECTION_TOOLTIP);
            p.add(tickmarkLabelsSectionLabel, gbc);


            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(labelsFontSizeParam.getEditor().getLabelComponent(), gbc);
            p.add(labelsFontSizeParam.getEditor().getEditorComponent(), gbc);
            labelsFontSizeParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_TOOLTIP);
            labelsFontSizeParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(labelsColorParam.getEditor().getLabelComponent(), gbc);
            p.add(labelsColorParam.getEditor().getEditorComponent(), gbc);
            labelsColorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_TOOLTIP);
            labelsColorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(labelsFontNameParam.getEditor().getLabelComponent(), gbc);
            p.add(labelsFontNameParam.getEditor().getEditorComponent(), gbc);
            labelsFontNameParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_TOOLTIP);
            labelsFontNameParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(labelsItalicParam.getEditor().getEditorComponent(), gbc);
            labelsItalicParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(labelsBoldParam.getEditor().getEditorComponent(), gbc);
            labelsBoldParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_TOOLTIP);





//            gbc.gridy++;
//            gbc.gridwidth = 1;
//            p.add(weightToleranceParam.getEditor().getLabelComponent(), gbc);
//            p.add(weightToleranceParam.getEditor().getEditorComponent(), gbc);
//            weightToleranceParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_TOOLTIP);
//            weightToleranceParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_WEIGHT_TOLERANCE_TOOLTIP);
//


            // Tickmarks Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel tickmarksSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_LABEL);
            tickmarksSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_TICKMARKS_SECTION_TOOLTIP);
            p.add(tickmarksSectionLabel, gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(tickmarksShowParam.getEditor().getEditorComponent(), gbc);
            tickmarksShowParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(tickmarksLengthParam.getEditor().getLabelComponent(), gbc);
            p.add(tickmarksLengthParam.getEditor().getEditorComponent(), gbc);
            tickmarksLengthParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_TOOLTIP);
            tickmarksLengthParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(tickmarksWidthParam.getEditor().getLabelComponent(), gbc);
            p.add(tickmarksWidthParam.getEditor().getEditorComponent(), gbc);
            tickmarksWidthParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_TOOLTIP);
            tickmarksWidthParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_TOOLTIP);


            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(tickmarksColorParam.getEditor().getLabelComponent(), gbc);
            p.add(tickmarksColorParam.getEditor().getEditorComponent(), gbc);
            tickmarksColorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP);
            tickmarksColorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_TOOLTIP);



            // Palette Border Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel paletteBorderSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_LABEL);
            paletteBorderSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SECTION_TOOLTIP);
            p.add(paletteBorderSectionLabel, gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(paletteBorderShowParam.getEditor().getEditorComponent(), gbc);
            paletteBorderShowParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(paletteBorderWidthParam.getEditor().getLabelComponent(), gbc);
            p.add(paletteBorderWidthParam.getEditor().getEditorComponent(), gbc);
            paletteBorderWidthParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_TOOLTIP);
            paletteBorderWidthParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(paletteBorderColorParam.getEditor().getLabelComponent(), gbc);
            p.add(paletteBorderColorParam.getEditor().getEditorComponent(), gbc);
            paletteBorderColorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_TOOLTIP);
            paletteBorderColorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_TOOLTIP);



            // Legend Border Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel legendBorderSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_LABEL);
            legendBorderSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SECTION_TOOLTIP);
            p.add(legendBorderSectionLabel, gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(legendBorderShowParam.getEditor().getEditorComponent(), gbc);
            legendBorderShowParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendBorderWidthParam.getEditor().getLabelComponent(), gbc);
            p.add(legendBorderWidthParam.getEditor().getEditorComponent(), gbc);
            legendBorderWidthParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_TOOLTIP);
            legendBorderWidthParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendBorderColorParam.getEditor().getLabelComponent(), gbc);
            p.add(legendBorderColorParam.getEditor().getEditorComponent(), gbc);
            legendBorderColorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_TOOLTIP);
            legendBorderColorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_TOOLTIP);


            // Backdrop Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel backdropSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_BACKDROP_SECTION_LABEL);
            backdropSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_BACKDROP_SECTION_TOOLTIP);
            p.add(backdropSectionLabel, gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(backdropShowParam.getEditor().getEditorComponent(), gbc);
            backdropShowParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_TOOLTIP);

            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(backgroundTransparencyParam.getEditor().getLabelComponent(), gbc);
            p.add(backgroundTransparencyParam.getEditor().getEditorComponent(), gbc);
            backgroundTransparencyParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_TOOLTIP);
            backgroundTransparencyParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_TOOLTIP);


            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(backdropColorParam.getEditor().getLabelComponent(), gbc);
            p.add(backdropColorParam.getEditor().getEditorComponent(), gbc);
            backdropColorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_TOOLTIP);
            backdropColorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_TOOLTIP);





            // Legend Border Gap Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel legendBorderGapSectionLabel = sectionBreak(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_SECTION_LABEL);
            legendBorderGapSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_SECTION_TOOLTIP);
            p.add(legendBorderGapSectionLabel, gbc);


            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendBorderGapFactorTopParam.getEditor().getLabelComponent(), gbc);
            p.add(legendBorderGapFactorTopParam.getEditor().getEditorComponent(), gbc);
            legendBorderGapFactorTopParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_TOOLTIP);
            legendBorderGapFactorTopParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_TOP_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendBorderGapFactorBottomParam.getEditor().getLabelComponent(), gbc);
            p.add(legendBorderGapFactorBottomParam.getEditor().getEditorComponent(), gbc);
            legendBorderGapFactorBottomParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_TOOLTIP);
            legendBorderGapFactorBottomParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_BOTTOM_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendBorderGapFactorLeftsideParam.getEditor().getLabelComponent(), gbc);
            p.add(legendBorderGapFactorLeftsideParam.getEditor().getEditorComponent(), gbc);
            legendBorderGapFactorLeftsideParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_TOOLTIP);
            legendBorderGapFactorLeftsideParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_LEFTSIDE_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendBorderGapFactorRightsideParam.getEditor().getLabelComponent(), gbc);
            p.add(legendBorderGapFactorRightsideParam.getEditor().getEditorComponent(), gbc);
            legendBorderGapFactorRightsideParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_TOOLTIP);
            legendBorderGapFactorRightsideParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_BORDER_GAP_RIGHTSIDE_TOOLTIP);


            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendTitleGapFactorParam.getEditor().getLabelComponent(), gbc);
            p.add(legendTitleGapFactorParam.getEditor().getEditorComponent(), gbc);
            legendTitleGapFactorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_TOOLTIP);
            legendTitleGapFactorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_TITLE_GAP_TOOLTIP);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendLabelGapFactorParam.getEditor().getLabelComponent(), gbc);
            p.add(legendLabelGapFactorParam.getEditor().getEditorComponent(), gbc);
            legendLabelGapFactorParam.getEditor().getLabelComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_TOOLTIP);
            legendLabelGapFactorParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_LEGEND_LABEL_GAP_TOOLTIP);





            // Colors Override Section

            gbc.gridy++;
            gbc.gridwidth = 2;
            JLabel colorsOverrideSectionLabel = sectionBreak("Colors Override");
            colorsOverrideSectionLabel.setToolTipText(ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_TOOLTIP);
            p.add(colorsOverrideSectionLabel, gbc);


            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(bwColorOverrideParam.getEditor().getEditorComponent(), gbc);
            bwColorOverrideParam.getEditor().getEditorComponent().setToolTipText(ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_TOOLTIP);


            p.setBorder(new EmptyBorder(7, 7, 7, 7));

            p.setMinimumSize(p.getPreferredSize());

            JScrollPane jScrollPane = new JScrollPane(p);


            jScrollPane.setMinimumSize(jScrollPane.getPreferredSize());


            final GridBagConstraints gbcOuter = new GridBagConstraints();
            final JPanel outer = GridBagUtils.createPanel();

            gbcOuter.anchor = GridBagConstraints.WEST;
            gbcOuter.fill = GridBagConstraints.BOTH;
            gbcOuter.gridx = 0;
            gbcOuter.gridy = 0;
            gbcOuter.weighty = 1;

            outer.add(jScrollPane, gbcOuter);

            gbcOuter.gridx = 0;
            gbcOuter.gridy = 1;
            gbcOuter.weighty = 0;
            gbcOuter.insets.top = 15;
            gbcOuter.fill = GridBagConstraints.NONE;
            gbcOuter.anchor = GridBagConstraints.CENTER;
            outer.add(previewButton, gbcOuter);


            setContent(outer);
        }


        private void initParams() {

            // Title and Units Text
            titleTextParam = paramGroup.getParameter(PROPERTY_TITLE_KEY2);

            unitsTextParam = paramGroup.getParameter(PROPERTY_UNITS_KEY2);
            unitsNullParam = paramGroup.getParameter(PROPERTY_UNITS_NULL_KEY2);


            convertCaretParam = paramGroup.getParameter(PROPERTY_CONVERT_CARET_KEY2);
            unitsParenthesisParam = paramGroup.getParameter(PROPERTY_UNITS_PARENTHESIS_KEY2);


            // Orientation
            orientationParam = paramGroup.getParameter(PROPERTY_ORIENTATION_KEY2);
            titleAnchorParam = paramGroup.getParameter(PROPERTY_LOCATION_TITLE_VERTICAL_KEY2);
            reversePaletteParam = paramGroup.getParameter(PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY2);


            // Tick Label Values
            distributionTypeParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_MODE_KEY2);
            numberOfTicksParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2);
            labelValuesActualParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2);
            populateLabelValuesTextfieldParam = paramGroup.getParameter(PROPERTY_POPULATE_VALUES_TEXTFIELD_KEY2);
            labelValuesScalingParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_SCALING_KEY2);
            labelValuesDecimalPlacesParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY2);
            labelValuesForceDecimalPlacesParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY2);
            weightToleranceParam = paramGroup.getParameter(PROPERTY_WEIGHT_TOLERANCE_KEY2);


            // Image Scaling Section
            useLegendWidthParam = paramGroup.getParameter(PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2);
            legendWidthParam = paramGroup.getParameter(PROPERTY_EXPORT_LEGEND_WIDTH_KEY2);
            colorbarLengthParam = paramGroup.getParameter(PROPERTY_COLORBAR_LENGTH_KEY2);
            colorbarWidthParam = paramGroup.getParameter(PROPERTY_COLORBAR_WIDTH_KEY2);


            // Title Section
            titleShowParam = paramGroup.getParameter(PROPERTY_TITLE_SHOW_KEY2);
            titleFontSizeParam = paramGroup.getParameter(PROPERTY_TITLE_FONT_SIZE_KEY2);
            titleBoldParam = paramGroup.getParameter(PROPERTY_TITLE_FONT_BOLD_KEY2);
            titleItalicParam = paramGroup.getParameter(PROPERTY_TITLE_FONT_ITALIC_KEY2);
            titleFontNameParam = paramGroup.getParameter(PROPERTY_TITLE_FONT_NAME_KEY2);
            titleColorParam = paramGroup.getParameter(PROPERTY_TITLE_COLOR_KEY2);


            // Units Section
            unitsShowParam = paramGroup.getParameter(PROPERTY_UNITS_SHOW_KEY2);
            unitsFontSizeParam = paramGroup.getParameter(PROPERTY_UNITS_FONT_SIZE_KEY2);
            unitsBoldParam = paramGroup.getParameter(PROPERTY_UNITS_FONT_BOLD_KEY2);
            unitsItalicParam = paramGroup.getParameter(PROPERTY_UNITS_FONT_ITALIC_KEY2);
            unitsFontNameParam = paramGroup.getParameter(PROPERTY_UNITS_FONT_NAME_KEY2);
            unitsColorParam = paramGroup.getParameter(PROPERTY_UNITS_FONT_COLOR_KEY2);


            // Tick-Mark Labels Section
            labelsShowParam = paramGroup.getParameter(PROPERTY_LABELS_SHOW_KEY2);
            labelsFontSizeParam = paramGroup.getParameter(PROPERTY_LABELS_FONT_SIZE_KEY2);
            labelsBoldParam = paramGroup.getParameter(PROPERTY_LABELS_FONT_BOLD_KEY2);
            labelsItalicParam = paramGroup.getParameter(PROPERTY_LABELS_FONT_ITALIC_KEY2);
            labelsFontNameParam = paramGroup.getParameter(PROPERTY_LABELS_FONT_NAME_KEY2);
            labelsColorParam = paramGroup.getParameter(PROPERTY_LABELS_FONT_COLOR_KEY2);


            // Tickmarks Section
            tickmarksShowParam = paramGroup.getParameter(PROPERTY_TICKMARKS_SHOW_KEY2);
            tickmarksLengthParam = paramGroup.getParameter(PROPERTY_TICKMARKS_LENGTH_KEY2);
            tickmarksWidthParam = paramGroup.getParameter(PROPERTY_TICKMARKS_WIDTH_KEY2);
            tickmarksColorParam = paramGroup.getParameter(PROPERTY_TICKMARKS_COLOR_KEY2);


            // Backdrop Section
            backdropShowParam = paramGroup.getParameter(PROPERTY_BACKDROP_SHOW_KEY2);
            backgroundTransparencyParam = paramGroup.getParameter(PROPERTY_BACKDROP_TRANSPARENCY_KEY2);
            backdropColorParam = paramGroup.getParameter(PROPERTY_BACKDROP_COLOR_KEY2);


            // Palette Border Section
            paletteBorderShowParam = paramGroup.getParameter(PROPERTY_PALETTE_BORDER_SHOW_KEY2);
            paletteBorderWidthParam = paramGroup.getParameter(PROPERTY_PALETTE_BORDER_WIDTH_KEY2);
            paletteBorderColorParam = paramGroup.getParameter(PROPERTY_PALETTE_BORDER_COLOR_KEY2);


            // Legend Border Section
            legendBorderShowParam = paramGroup.getParameter(PROPERTY_LEGEND_BORDER_SHOW_KEY2);
            legendBorderWidthParam = paramGroup.getParameter(PROPERTY_LEGEND_BORDER_WIDTH_KEY2);
            legendBorderColorParam = paramGroup.getParameter(PROPERTY_LEGEND_BORDER_COLOR_KEY2);

            // Legend Border Gap Section
            legendTitleGapFactorParam = paramGroup.getParameter(PROPERTY_LEGEND_TITLE_GAP_KEY2);
            legendLabelGapFactorParam = paramGroup.getParameter(PROPERTY_LEGEND_LABEL_GAP_KEY2);
            legendBorderGapFactorTopParam = paramGroup.getParameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_TOP_KEY2);
            legendBorderGapFactorBottomParam = paramGroup.getParameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_BOTTOM_KEY2);
            legendBorderGapFactorLeftsideParam = paramGroup.getParameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_LEFTSIDE_KEY2);
            legendBorderGapFactorRightsideParam = paramGroup.getParameter(PROPERTY_LEGEND_BORDER_GAP_FACTOR_RIGHTSIDE_KEY2);

            // Colors Override Section
            bwColorOverrideParam = paramGroup.getParameter(PROPERTY_EXPORT_USE_BW_COLOR_KEY2);

        }

        private void showPreview() {

            getImageLegend(imageLegend);
            double scalingOriginal = imageLegend.getLayerScaling();

            imageLegend.setLayerScaling(70.0);  // preview will be 70% of screen width
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            final BufferedImage image = imageLegend.createImage(screenSize, true, true, false);
            imageLegend.setLayerScaling(scalingOriginal);

            final JLabel imageDisplay = new JLabel(new ImageIcon(image));
            imageDisplay.setOpaque(true);
            imageDisplay.addMouseListener(new MouseAdapter() {
                // Both events (releases & pressed) must be checked, otherwise it won't work on all
                // platforms

                /**
                 * Invoked when a mouse button has been released on a component.
                 */
                @Override
                public void mouseReleased(MouseEvent e) {
                    // On Windows
                    showPopup(e, image, imageDisplay);
                }

                /**
                 * Invoked when a mouse button has been pressed on a component.
                 */
                @Override
                public void mousePressed(MouseEvent e) {
                    // On Linux
                    // todo - clipboard does not work on linux.
                    // todo - better not to show popup until it works correctly
//                    showPopup(e, image, imageDisplay);
                }
            });
            final ModalDialog dialog = new ModalDialog(getParent(),
                    SnapApp.getDefault().getInstanceName() + " - " + ColorBarLayerType.COLOR_BAR_LEGEND_NAME,
                    imageDisplay,
                    ID_OK, null);
            dialog.getJDialog().setResizable(false);
            dialog.show();
        }


        private static JLabel sectionBreak(String title) {
            return new JLabel(ColorBarLayerType.DASHES + " " + title + " " + ColorBarLayerType.DASHES);
        }

        private static void showPopup(final MouseEvent e, final BufferedImage image, final JComponent imageDisplay) {
            if (e.isPopupTrigger()) {
                final JPopupMenu popupMenu = new JPopupMenu();
                final JMenuItem menuItem = new JMenuItem("Copy image to clipboard");
                menuItem.addActionListener(e1 -> SystemUtils.copyToClipboard(image));
                popupMenu.add(menuItem);
                popupMenu.show(imageDisplay, e.getX(), e.getY());
            }
        }
    }

}
