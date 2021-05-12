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

import com.bc.ceres.glayer.Layer;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.ImageLegend;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.layer.ColorBarLayer;
import org.esa.snap.core.layer.ColorBarLayerType;
import org.esa.snap.core.param.ParamChangeEvent;
import org.esa.snap.core.param.ParamChangeListener;
import org.esa.snap.core.param.ParamGroup;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
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

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
//MAY2020 - Daniel Knowles - Major revision to color bar and color bar layer tools

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
    private final static String[][] IMAGE_FORMAT_DESCRIPTIONS = {
            BMP_FORMAT_DESCRIPTION,
            PNG_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION
    };


    // Make different keys for export parameters so it doesn't affect color bar layer
    private static final String PROPERTY_ORIENTATION_KEY2 = ColorBarLayerType.PROPERTY_ORIENTATION_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_COUNT_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY + ".export";
    private static final String PROPERTY_TITLE_SHOW_KEY2 = ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY + ".export";
    private static final String PROPERTY_TITLE_TEXT_KEY2 = ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY + ".export";
    private static final String PROPERTY_UNITS_TEXT_KEY2 = ColorBarLayerType.PROPERTY_UNITS_TEXT_KEY + ".export";
    private static final String PROPERTY_BACKDROP_TRANSPARENCY_KEY2 = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_ACTUAL_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_MODE_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY + ".export";
    private static final String PROPERTY_EXPORT_USE_BW_COLOR_KEY2 = ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_KEY + ".export";


    // these parameters are not used in GUI but are used to store initial values used if deselecting the black/white override
    private static final String PROPERTY_TITLE_COLOR_KEY2 = ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY + ".export";
    private static final String PROPERTY_UNITS_FONT_COLOR_KEY2 = ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_KEY + ".export";
    private static final String PROPERTY_LABELS_FONT_COLOR_KEY2 = ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY + ".export";
    private static final String PROPERTY_TICKMARKS_COLOR_KEY2 = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY + ".export";
    private static final String PROPERTY_PALETTE_BORDER_COLOR_KEY2 = ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY + ".export";
    private static final String PROPERTY_BACKDROP_COLOR_KEY2 = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY + ".export";
    private static final String PROPERTY_LEGEND_BORDER_COLOR_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY + ".export";



    private static final String HELP_ID = "exportLegendImageFile";


    private SnapFileFilter[] imageFileFilters;

    private ParamGroup legendParamGroup;
    private ImageLegend imageLegend;
    private boolean showEditorFirst;
    private boolean blackWhiteColor;


    private ParamChangeListener paramChangeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private Lookup.Result<ProductSceneView> result;


    public ExportLegendImageAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportLegendImageAction(Lookup lookup) {

        super(Bundle.CTL_ExportLegendImageAction_MenuText(), HELP_ID);

        System.out.println("Entering export image legend");

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
        showEditorFirst = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_KEY,
                ColorBarLayerType.PROPERTY_EXPORT_EDITOR_SHOW_DEFAULT);

        blackWhiteColor = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_KEY,
                ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_DEFAULT);

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





    private void  initImageLegend(ProductSceneView view) {

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
                System.out.println("Making new imageLegend");

                imageLegend = new ImageLegend(raster.getImageInfo(), raster);
                initLegendWithPreferences(view);
            }
        }



        if (imageLegend != null) {

            // it's not a layer so no scaling
            imageLegend.setLayerScaling(1.0);


            // this will initialize the custom label values
            String distributionTypeOriginal = imageLegend.getDistributionType();
            if (ColorBarLayerType.DISTRIB_MANUAL_STR.equals(distributionTypeOriginal)) {
                if (imageLegend.getCustomLabelValues() == null || imageLegend.getCustomLabelValues().length() == 0) {
                    imageLegend.setDistributionType(ColorBarLayerType.DISTRIB_EVEN_STR);
                }
            }


            imageLegend.createImage();
            imageLegend.setDistributionType(distributionTypeOriginal);

            legendParamGroup = createLegendParamGroup(imageLegend, paramChangeListener, blackWhiteColor);


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


    private void initLegendWithPreferences(ProductSceneView view) {

        final RasterDataNode raster = view.getRaster();
        PropertyMap configuration = view.getSceneImage().getConfiguration();

        // Orientation Parameters

        String orientationString = configuration.getPropertyString(ColorBarLayerType.PROPERTY_ORIENTATION_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_DEFAULT);

        if (ColorBarLayerType.OPTION_VERTICAL.equals(orientationString)) {
            imageLegend.setOrientation(ImageLegend.VERTICAL);
        } else {
            imageLegend.setOrientation(ImageLegend.HORIZONTAL);
        }

        imageLegend.setReversePalette(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY,
                ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_DEFAULT));


        // Label Distribution and Values

        imageLegend.setTickMarkCount(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT));

        imageLegend.setDistributionType(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_DEFAULT));

        imageLegend.setCustomLabelValues(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_DEFAULT));

        imageLegend.setScalingFactor(configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_DEFAULT));

        imageLegend.setDecimalPlaces(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_DECIMAL_PLACES_DEFAULT));

        imageLegend.setDecimalPlacesForce(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_FORCE_DECIMAL_PLACES_DEFAULT));


        // Sizing and Location
        imageLegend.setColorBarLength(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LEGEND_LENGTH_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_LENGTH_DEFAULT));

        imageLegend.setColorBarThickness(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LEGEND_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_WIDTH_DEFAULT));

        imageLegend.setTitleVerticalAnchor(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_KEY,
                ColorBarLayerType.PROPERTY_LOCATION_TITLE_VERTICAL_DEFAULT));


        // Title parameters

        imageLegend.setShowTitle(
                configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_SHOW_DEFAULT));


        String titleTextDefault = configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_TEXT_DEFAULT);

        String titleText = (ColorBarLayerType.NULL_SPECIAL.equals(titleTextDefault)) ? raster.getName() : titleTextDefault;

        imageLegend.setTitleText(titleText);


        imageLegend.setTitleFontSize(
                configuration.getPropertyInt(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_DEFAULT));

        imageLegend.setTitleColor(
                configuration.getPropertyColor(ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_COLOR_DEFAULT));

        imageLegend.setTitleFontName(
                configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_DEFAULT));


        boolean titleParameterBold = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_DEFAULT);

        boolean titleParameterItalic = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_DEFAULT);

        int titleFontType = ColorBarLayer.getFontType(titleParameterItalic, titleParameterBold);

        imageLegend.setTitleFontType(titleFontType);


        // Units parameters

        imageLegend.setShowUnits(
                configuration.getPropertyBool(ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY,
                        ColorBarLayerType.PROPERTY_UNITS_SHOW_DEFAULT));


        String unitsTextDefault = configuration.getPropertyString(ColorBarLayerType.PROPERTY_UNITS_TEXT_KEY,
                ColorBarLayerType.PROPERTY_UNITS_TEXT_DEFAULT);



        String unitsText = "";
        if (ColorBarLayerType.NULL_SPECIAL.equals(unitsTextDefault)) {
            String unit = raster.getUnit();
            if (unit != null && unit.length() > 0) {
                unitsText = "(" + raster.getUnit() + ")";
            }
        } else {
            unitsText = unitsTextDefault;
        }


        imageLegend.setUnitsText(unitsText);


        imageLegend.setUnitsFontSize(
                configuration.getPropertyInt(ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_KEY,
                        ColorBarLayerType.PROPERTY_UNITS_FONT_SIZE_DEFAULT));

        imageLegend.setUnitsColor(
                configuration.getPropertyColor(ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_KEY,
                        ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_DEFAULT));

        imageLegend.setUnitsFontName(
                configuration.getPropertyString(ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_KEY,
                        ColorBarLayerType.PROPERTY_UNITS_FONT_NAME_DEFAULT));


        boolean unitsBold = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_UNITS_FONT_BOLD_DEFAULT);

        boolean unitsItalic = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_UNITS_FONT_ITALIC_DEFAULT);

        int unitsFontType = ColorBarLayer.getFontType(unitsItalic, unitsBold);

        imageLegend.setUnitsFontType(unitsFontType);




        // Labels Parameters

        imageLegend.setLabelsShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LABELS_SHOW_KEY,
                ColorBarLayerType.PROPERTY_LABELS_SHOW_DEFAULT));

        imageLegend.setLabelsFontName(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_NAME_DEFAULT));

        boolean labelsFontBold = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_BOLD_DEFAULT);

        boolean labelsFontItalic = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_ITALIC_DEFAULT);

        imageLegend.setLabelsFontType(ColorBarLayer.getFontType(labelsFontItalic, labelsFontBold));

        imageLegend.setLabelsFontSize(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_SIZE_DEFAULT));

        imageLegend.setLabelsColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY,
                ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_DEFAULT));



        // Tick Marks Section

        imageLegend.setTickmarkShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT));

        imageLegend.setTickmarkLength(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT));

        imageLegend.setTickmarkWidth(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_WIDTH_DEFAULT));

        imageLegend.setTickmarkColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY,
                ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT));



        // Backdrop Section

        imageLegend.setBackdropShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_BACKDROP_SHOW_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_SHOW_DEFAULT));

        imageLegend.setBackdropColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_COLOR_DEFAULT));

        double backdropTrans = configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT);

        imageLegend.setBackdropTransparency(((Number) backdropTrans).floatValue());


        // Palette Border Section

        imageLegend.setBorderShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_SHOW_DEFAULT));

        imageLegend.setBorderWidth(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_WIDTH_DEFAULT));

        imageLegend.setBorderColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY,
                ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_DEFAULT));


        // Legend Border Section

        imageLegend.setBackdropBorderShow(configuration.getPropertyBool(ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_SHOW_DEFAULT));

        imageLegend.setBackdropBorderWidth(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_WIDTH_DEFAULT));

        imageLegend.setBackdropBorderColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY,
                ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_DEFAULT));


        imageLegend.setLayerScaling(1.0);
        imageLegend.setAntialiasing((Boolean) true);

        //  imageLegend.setBackgroundTransparencyEnabled(true);
    }


    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        transferParamsToImageLegend(legendParamGroup, imageLegend);
        imageLegend.setTransparencyEnabled(isTransparencySupportedByFormat(imageFormat));
        return imageLegend.createImage();
    }

    @Override
    protected boolean isEntireImageSelected() {
        return true;
    }

    private static ParamGroup createLegendParamGroup(ImageLegend imageLegend, ParamChangeListener paramChangeListener, boolean blackWhiteColor) {

        ParamGroup paramGroup = new ParamGroup();
        Parameter param;

        param = new Parameter(PROPERTY_TITLE_SHOW_KEY2, imageLegend.isShowTitle());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_SHOW_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_EXPORT_USE_BW_COLOR_KEY2, blackWhiteColor);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_TITLE_TEXT_KEY2, imageLegend.getTitleText());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_TEXT_LABEL);
        param.getProperties().setNumCols(24);
        param.getProperties().setNullValueAllowed(true);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_UNITS_TEXT_KEY2, imageLegend.getUnitsText());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_TEXT_LABEL);
        param.getProperties().setNumCols(24);
        param.getProperties().setNullValueAllowed(true);
        paramGroup.addParameter(param);


        int orientationInt = imageLegend.getOrientation();
        String orientationString = (orientationInt == ImageLegend.VERTICAL) ? ColorBarLayerType.OPTION_VERTICAL : ColorBarLayerType.OPTION_HORIZONTAL;

        param = new Parameter(PROPERTY_ORIENTATION_KEY2, orientationString);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_ORIENTATION_LABEL);
        param.getProperties().setValueSet(new String[]{ColorBarLayerType.PROPERTY_ORIENTATION_OPTION1,
                ColorBarLayerType.PROPERTY_ORIENTATION_OPTION2});
        param.getProperties().setValueSetBound(true);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2, imageLegend.getCustomLabelValues());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_LABEL);
        paramGroup.addParameter(param);

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
        param.getProperties().setMinValue(0);
        param.getProperties().setMaxValue(40);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);



        param = new Parameter(PROPERTY_BACKDROP_TRANSPARENCY_KEY2, imageLegend.getBackdropTransparency());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_LABEL);
        param.getProperties().setMinValue(0.0f);
        param.getProperties().setMaxValue(1.0f);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_TITLE_COLOR_KEY2, imageLegend.getTitleColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_COLOR_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_UNITS_FONT_COLOR_KEY2, imageLegend.getUnitsColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LABELS_FONT_COLOR_KEY2, imageLegend.getLabelsColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_TICKMARKS_COLOR_KEY2, imageLegend.getTickmarkColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_PALETTE_BORDER_COLOR_KEY2, imageLegend.getBorderColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_BACKDROP_COLOR_KEY2, imageLegend.getBackdropColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LEGEND_BORDER_COLOR_KEY2, imageLegend.getBackdropBorderColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_LABEL);
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

        System.out.println("parameterName=" + parameterName);
        if (PROPERTY_LABEL_VALUES_MODE_KEY2.equals(parameterName) || PROPERTY_LABEL_VALUES_COUNT_KEY2.equals(parameterName)) {
            System.out.println("test1");
            Object distributionType = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_MODE_KEY2).getValue();

            if (!ColorBarLayerType.DISTRIB_MANUAL_STR.equals(distributionType)) {
                if (imageLegend.getDistributionType() == null || !imageLegend.getDistributionType().equals(distributionType)) {
                    imageLegend.setDistributionType((String) distributionType);
                    System.out.println("setDistributionType=" + (String) distributionType);
                }


                if (ColorBarLayerType.DISTRIB_EVEN_STR.equals(distributionType)) {
                    Object tickMarkCount = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).getValue();
                    imageLegend.setTickMarkCount((Integer) tickMarkCount);
                }
                // update custom labels
                imageLegend.createImage();
                System.out.println("custom=" + imageLegend.getCustomLabelValues());
                legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setValue(imageLegend.getCustomLabelValues(), null);
            }


            updateEnablement();
        }

    }

    private void updateEnablement() {

        Object distributionType = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_MODE_KEY2).getValue();

        if (ColorBarLayerType.DISTRIB_MANUAL_STR.equals(distributionType)) {
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setUIEnabled(true);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).setUIEnabled(false);
        } else if (ColorBarLayerType.DISTRIB_EXACT_STR.equals(distributionType)) {
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).setUIEnabled(false);
        } else {
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).setUIEnabled(true);
        }
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


        value = legendParamGroup.getParameter(PROPERTY_TITLE_SHOW_KEY2).getValue();
        imageLegend.setShowTitle((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_TITLE_TEXT_KEY2).getValue();
        imageLegend.setTitleText((String) value);

        value = legendParamGroup.getParameter(PROPERTY_UNITS_TEXT_KEY2).getValue();
        imageLegend.setUnitsText((String) value);


        value = legendParamGroup.getParameter(PROPERTY_ORIENTATION_KEY2).getValue();

        System.out.println("orientation (value)=" + value);
        if (ColorBarLayerType.OPTION_VERTICAL.equals(value)) {
            imageLegend.setOrientation(ImageLegend.VERTICAL);
            System.out.println("setting to vertical");
        } else {
            imageLegend.setOrientation(ImageLegend.HORIZONTAL);
            System.out.println("setting to horizontal");
        }


        Boolean blackWhiteColor =  (Boolean)legendParamGroup.getParameter(PROPERTY_EXPORT_USE_BW_COLOR_KEY2).getValue();
        if (blackWhiteColor) {
            imageLegend.setTitleColor(Color.BLACK);
            imageLegend.setUnitsColor(Color.BLACK);
            imageLegend.setLabelsColor(Color.BLACK);
            imageLegend.setTickmarkColor(Color.BLACK);
            imageLegend.setBorderColor(Color.BLACK);
            imageLegend.setBackdropColor(Color.WHITE);
            imageLegend.setBackdropBorderColor(Color.WHITE);
        } else {

            value = legendParamGroup.getParameter(PROPERTY_TITLE_COLOR_KEY2).getValue();
            imageLegend.setTitleColor((Color) value);

            value = legendParamGroup.getParameter(PROPERTY_UNITS_FONT_COLOR_KEY2).getValue();
            imageLegend.setUnitsColor((Color) value);

            value = legendParamGroup.getParameter(PROPERTY_LABELS_FONT_COLOR_KEY2).getValue();
            imageLegend.setLabelsColor((Color) value);

            value = legendParamGroup.getParameter(PROPERTY_TICKMARKS_COLOR_KEY2).getValue();
            imageLegend.setTickmarkColor((Color) value);

            value = legendParamGroup.getParameter(PROPERTY_PALETTE_BORDER_COLOR_KEY2).getValue();
            imageLegend.setBorderColor((Color) value);

            value = legendParamGroup.getParameter(PROPERTY_BACKDROP_COLOR_KEY2).getValue();
            imageLegend.setBackdropColor((Color) value);

            value = legendParamGroup.getParameter(PROPERTY_LEGEND_BORDER_COLOR_KEY2).getValue();
            imageLegend.setBackdropBorderColor((Color) value);

        }


        value = legendParamGroup.getParameter(PROPERTY_BACKDROP_TRANSPARENCY_KEY2).getValue();
        imageLegend.setBackdropTransparency(((Number) value).floatValue());


        // Set this prior to set distributionType in order to update custom labels if needed
        value = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).getValue();
        imageLegend.setTickMarkCount((Integer) value);


        value = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_MODE_KEY2).getValue();
        imageLegend.setDistributionType((String) value);


        value = legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).getValue();
        imageLegend.setCustomLabelValues((String) value);
    }


    public static class ImageLegendDialog extends ModalDialog {

        private ImageInfo imageInfo;
        private RasterDataNode raster;

        private ImageLegend imageLegend;
        private boolean transparencyEnabled;

        private ParamGroup paramGroup;

        private Parameter numberOfTicksParam;
        private Parameter usingHeaderParam;
        private Parameter bwColorOverrideParam;
        private Parameter headerTextParam;
        private Parameter unitsTextParam;
        private Parameter orientationParam;
        private Parameter backgroundTransparencyParam;
        private Parameter labelValuesActualParam;
        private Parameter distributionTypeParam;


        public ImageLegendDialog(ParamGroup paramGroup, ImageLegend imageLegend,
                                 boolean transparencyEnabled, String helpId) {
            super(SnapApp.getDefault().getMainFrame(), SnapApp.getDefault().getInstanceName() + " - " + ColorBarLayerType.COLOR_BAR_LEGEND_NAME, ID_OK_CANCEL_HELP, helpId);
            System.out.println("helpId=" + helpId);
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
            boolean headerTextEnabled = (Boolean) usingHeaderParam.getValue();
            headerTextParam.setUIEnabled(headerTextEnabled);
            backgroundTransparencyParam.setUIEnabled(transparencyEnabled);
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

            gbc.gridy = 0;
            gbc.gridwidth = 2;
            p.add(usingHeaderParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(headerTextParam.getEditor().getLabelComponent(), gbc);
            p.add(headerTextParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(unitsTextParam.getEditor().getLabelComponent(), gbc);
            p.add(unitsTextParam.getEditor().getEditorComponent(), gbc);
            gbc.gridy++;
            gbc.insets.top = 5;
            p.add(orientationParam.getEditor().getLabelComponent(), gbc);
            p.add(orientationParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 5;
            p.add(distributionTypeParam.getEditor().getLabelComponent(), gbc);
            p.add(distributionTypeParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 5;
            p.add(numberOfTicksParam.getEditor().getLabelComponent(), gbc);
            p.add(numberOfTicksParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 5;
            p.add(labelValuesActualParam.getEditor().getLabelComponent(), gbc);
            p.add(labelValuesActualParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 5;
            p.add(backgroundTransparencyParam.getEditor().getLabelComponent(), gbc);
            p.add(backgroundTransparencyParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(bwColorOverrideParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 15;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            p.add(previewButton, gbc);

            gbc.gridy++;
            gbc.anchor = GridBagConstraints.CENTER;
            JLabel info = new JLabel("<html><hr>More color bar format options available using the layer editor<br> and preferences.  See help page link below for details.<hr></html");
            p.add(info, gbc);
            p.setBorder(new EmptyBorder(7, 7, 7, 7));

            setContent(p);
        }

        private void initParams() {
            usingHeaderParam = paramGroup.getParameter(PROPERTY_TITLE_SHOW_KEY2);
            bwColorOverrideParam = paramGroup.getParameter(PROPERTY_EXPORT_USE_BW_COLOR_KEY2);
            numberOfTicksParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2);
            headerTextParam = paramGroup.getParameter(PROPERTY_TITLE_TEXT_KEY2);
            unitsTextParam = paramGroup.getParameter(PROPERTY_UNITS_TEXT_KEY2);
            orientationParam = paramGroup.getParameter(PROPERTY_ORIENTATION_KEY2);
            backgroundTransparencyParam = paramGroup.getParameter(PROPERTY_BACKDROP_TRANSPARENCY_KEY2);
            labelValuesActualParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2);
            distributionTypeParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_MODE_KEY2);
        }

        private void showPreview() {

            getImageLegend(imageLegend);
            double scalingOriginal = imageLegend.getLayerScaling();

            imageLegend.setLayerScaling(70.0);  // preview will be 70% of screen width
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            final BufferedImage image = imageLegend.createImage(screenSize, true);
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
