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

    private static final String HELP_ID = "exportLegendImageFile";


    private final static String[][] IMAGE_FORMAT_DESCRIPTIONS = {
            BMP_FORMAT_DESCRIPTION,
            PNG_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION
    };



    // Make different keys for export parameters so it doesn't affect color bar layer
    // Keys named differently from preferences to not overwrite preferences

    // Title and Units Text
    private static final String PROPERTY_TITLE_TEXT_KEY2 = ColorBarLayerType.PROPERTY_TITLE_TEXT_KEY + ".export";
    private static final String PROPERTY_UNITS_TEXT_KEY2 = ColorBarLayerType.PROPERTY_UNITS_TEXT_KEY + ".export";

    // Orientation
    private static final String PROPERTY_ORIENTATION_KEY2 = ColorBarLayerType.PROPERTY_ORIENTATION_KEY + ".export";
    private static final String PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY2 = ColorBarLayerType.PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY + ".export";

    // Tick Label Values
    private static final String PROPERTY_LABEL_VALUES_MODE_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_COUNT_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_ACTUAL_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY + ".export";
    private static final String PROPERTY_LABEL_VALUES_SCALING_KEY2 = ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_KEY + ".export";




    private static final String PROPERTY_TITLE_SHOW_KEY2 = ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY + ".export";
    private static final String PROPERTY_UNITS_SHOW_KEY2 = ColorBarLayerType.PROPERTY_UNITS_SHOW_KEY + ".export";

    private static final String PROPERTY_BACKDROP_TRANSPARENCY_KEY2 = ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY + ".export";








    private static final String PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2 = ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY + ".export";
    private static final String PROPERTY_EXPORT_LEGEND_WIDTH_KEY2 = ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_KEY + ".export";

    // These are all the color keys which get bypasses dependent on the PROPERTY_EXPORT_USE_BW_COLOR_KEY2
    private static final String PROPERTY_EXPORT_USE_BW_COLOR_KEY2 = ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_KEY + ".export";
    private static final String PROPERTY_TITLE_COLOR_KEY2 = ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY + ".export";
    private static final String PROPERTY_UNITS_FONT_COLOR_KEY2 = ColorBarLayerType.PROPERTY_UNITS_FONT_COLOR_KEY + ".export";
    private static final String PROPERTY_LABELS_FONT_COLOR_KEY2 = ColorBarLayerType.PROPERTY_LABELS_FONT_COLOR_KEY + ".export";
    private static final String PROPERTY_TICKMARKS_COLOR_KEY2 = ColorBarLayerType.PROPERTY_TICKMARKS_COLOR_KEY + ".export";
    private static final String PROPERTY_PALETTE_BORDER_COLOR_KEY2 = ColorBarLayerType.PROPERTY_PALETTE_BORDER_COLOR_KEY + ".export";
    private static final String PROPERTY_BACKDROP_COLOR_KEY2 = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY + ".export";
    private static final String PROPERTY_LEGEND_BORDER_COLOR_KEY2 = ColorBarLayerType.PROPERTY_LEGEND_BORDER_COLOR_KEY + ".export";





    private SnapFileFilter[] imageFileFilters;

    private ParamGroup legendParamGroup;
    private ImageLegend imageLegend;
    private boolean showEditorFirst;
    private boolean blackWhiteColor;
    private int legendWidth;
    private boolean useLegendWidth;
    private boolean discrete;


    private ParamChangeListener paramChangeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private Lookup.Result<ProductSceneView> result;


    public ExportLegendImageAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportLegendImageAction(Lookup lookup) {

        super(Bundle.CTL_ExportLegendImageAction_MenuText(), HELP_ID);

//        System.out.println("Entering export image legend");

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

        useLegendWidth = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_DEFAULT);

        legendWidth = configuration.getPropertyInt(ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_KEY,
                ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_DEFAULT);


        discrete = SnapApp.getDefault().getSelectedProductSceneView().getImageInfo().getColorPaletteDef().isDiscrete();


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
//                System.out.println("Making new imageLegend");

                imageLegend = new ImageLegend(raster.getImageInfo(), raster);
                initLegendWithPreferences(view);
            }
        }


        if (imageLegend != null) {

            if (discrete) {
                imageLegend.setDistributionType(ColorBarLayerType.DISTRIB_EXACT_STR);
            }

            // this will initialize the custom label values
            String distributionTypeOriginal = imageLegend.getDistributionType();


            if (ColorBarLayerType.DISTRIB_MANUAL_STR.equals(distributionTypeOriginal)) {
                if (imageLegend.getCustomLabelValues() == null || imageLegend.getCustomLabelValues().length() == 0) {
                    imageLegend.setDistributionType(ColorBarLayerType.DISTRIB_EVEN_STR);
                }
            }

            imageLegend.setLayerScaling(100.0);

            imageLegend.createImage(new Dimension(legendWidth, legendWidth), useLegendWidth);
            imageLegend.setDistributionType(distributionTypeOriginal);

            legendParamGroup = createLegendParamGroup(imageLegend, paramChangeListener, blackWhiteColor, useLegendWidth, legendWidth);


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

        imageLegend.initLegendWithPreferences(configuration, raster);
    }


    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        transferParamsToImageLegend(legendParamGroup, imageLegend);


        imageLegend.setTransparencyEnabled(isTransparencySupportedByFormat(imageFormat));
        imageLegend.setLayerScaling(100.0);

        useLegendWidth = (Boolean) legendParamGroup.getParameter(PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2).getValue();
        legendWidth = (Integer) legendParamGroup.getParameter(PROPERTY_EXPORT_LEGEND_WIDTH_KEY2).getValue();
        return imageLegend.createImage(new Dimension(legendWidth, legendWidth), useLegendWidth);
    }

    @Override
    protected boolean isEntireImageSelected() {
        return true;
    }

    private static ParamGroup createLegendParamGroup(ImageLegend imageLegend, ParamChangeListener paramChangeListener, boolean blackWhiteColor, boolean useLegendWidth, int legendWidth) {

        ParamGroup paramGroup = new ParamGroup();
        Parameter param;

        param = new Parameter(PROPERTY_TITLE_SHOW_KEY2, imageLegend.isShowTitle());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_SHOW_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_UNITS_SHOW_KEY2, imageLegend.isShowUnits());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_UNITS_SHOW_LABEL);
        paramGroup.addParameter(param);



        param = new Parameter(PROPERTY_EXPORT_USE_BW_COLOR_KEY2, blackWhiteColor);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_EXPORT_USE_BW_COLOR_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter(PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2, useLegendWidth);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_EXPORT_USE_LEGEND_WIDTH_LABEL);
        param.addParamChangeListener(paramChangeListener);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_EXPORT_LEGEND_WIDTH_KEY2, legendWidth);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_EXPORT_LEGEND_WIDTH_LABEL);
        paramGroup.addParameter(param);





        // Title and Units Text

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



        // Orientation

        int orientationInt = imageLegend.getOrientation();
        String orientationString = (orientationInt == ImageLegend.VERTICAL) ? ColorBarLayerType.OPTION_VERTICAL : ColorBarLayerType.OPTION_HORIZONTAL;

        param = new Parameter(PROPERTY_ORIENTATION_KEY2, orientationString);
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_ORIENTATION_LABEL);
        param.getProperties().setValueSet(new String[]{ColorBarLayerType.PROPERTY_ORIENTATION_OPTION1, ColorBarLayerType.PROPERTY_ORIENTATION_OPTION2});
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

        param = new Parameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2, imageLegend.getCustomLabelValues());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter(PROPERTY_LABEL_VALUES_SCALING_KEY2, imageLegend.getScalingFactor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_LABEL);
        param.getProperties().setMinValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MIN);
        param.getProperties().setMaxValue(ColorBarLayerType.PROPERTY_LABEL_VALUES_SCALING_MAX);
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
                imageLegend.createImage(new Dimension(legendWidth, legendWidth), useLegendWidth);
                legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setValue(imageLegend.getCustomLabelValues(), null);
            }

            updateEnablement();
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
        } else if (ColorBarLayerType.DISTRIB_EXACT_STR.equals(distributionType)) {
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).setUIEnabled(false);
        } else {
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2).setUIEnabled(false);
            legendParamGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2).setUIEnabled(true);
        }

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

        value = legendParamGroup.getParameter(PROPERTY_TITLE_TEXT_KEY2).getValue();
        imageLegend.setTitleText((String) value);

        value = legendParamGroup.getParameter(PROPERTY_UNITS_TEXT_KEY2).getValue();
        imageLegend.setUnitsText((String) value);



        // Orientation

        value = legendParamGroup.getParameter(PROPERTY_ORIENTATION_KEY2).getValue();
        if (ColorBarLayerType.OPTION_VERTICAL.equals(value)) {
            imageLegend.setOrientation(ImageLegend.VERTICAL);
        } else {
            imageLegend.setOrientation(ImageLegend.HORIZONTAL);
        }

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






        value = legendParamGroup.getParameter(PROPERTY_TITLE_SHOW_KEY2).getValue();
        imageLegend.setShowTitle((Boolean) value);

        value = legendParamGroup.getParameter(PROPERTY_UNITS_SHOW_KEY2).getValue();
        imageLegend.setShowUnits((Boolean) value);


        Boolean blackWhiteColor = (Boolean) legendParamGroup.getParameter(PROPERTY_EXPORT_USE_BW_COLOR_KEY2).getValue();
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



    }


    public static class ImageLegendDialog extends ModalDialog {

        private ImageInfo imageInfo;
        private RasterDataNode raster;

        private ImageLegend imageLegend;
        private boolean transparencyEnabled;

        private ParamGroup paramGroup;

        private Parameter numberOfTicksParam;
        private Parameter usingHeaderParam;
        private Parameter usingUnitsParam;
        private Parameter bwColorOverrideParam;
        private Parameter useLegendWidthParam;
        private Parameter legendWidthParam;
        private Parameter headerTextParam;
        private Parameter unitsTextParam;

        private Parameter orientationParam;
        private Parameter reversePaletteParam;



        private Parameter backgroundTransparencyParam;
        private Parameter labelValuesActualParam;
        private Parameter labelValuesScalingParam;
        private Parameter distributionTypeParam;


        private Parameter titleColorParam;
        private Parameter unitsColorParam;
        private Parameter labelsColorParam;
        private Parameter tickmarksColorParam;
        private Parameter paletteBorderColorParam;
        private Parameter backdropColorParam;
        private Parameter legendBorderColorParam;






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
            boolean headerTextEnabled = (Boolean) usingHeaderParam.getValue();
            headerTextParam.setUIEnabled(headerTextEnabled);

            boolean unitsTextEnabled = (Boolean) usingUnitsParam.getValue();
            unitsTextParam.setUIEnabled(unitsTextEnabled);

            backgroundTransparencyParam.setUIEnabled(transparencyEnabled);


            boolean bwColorOverride = (Boolean) bwColorOverrideParam.getValue();
            titleColorParam.setUIEnabled(!bwColorOverride);
            unitsColorParam.setUIEnabled(!bwColorOverride);
            labelsColorParam.setUIEnabled(!bwColorOverride);
            tickmarksColorParam.setUIEnabled(!bwColorOverride);
            paletteBorderColorParam.setUIEnabled(!bwColorOverride);
            backdropColorParam.setUIEnabled(!bwColorOverride);
            legendBorderColorParam.setUIEnabled(!bwColorOverride);

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
            gbc.gridwidth = 2;
            p.add(usingUnitsParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(unitsTextParam.getEditor().getLabelComponent(), gbc);
            p.add(unitsTextParam.getEditor().getEditorComponent(), gbc);


            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(sectionBreak(ColorBarLayerType.PROPERTY_ORIENTATION_SECTION_LABEL), gbc);


            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(orientationParam.getEditor().getLabelComponent(), gbc);
            p.add(orientationParam.getEditor().getEditorComponent(), gbc);


            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(reversePaletteParam.getEditor().getEditorComponent(), gbc);



            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(sectionBreak(ColorBarLayerType.PROPERTY_LABEL_VALUES_SECTION_LABEL), gbc);

            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(distributionTypeParam.getEditor().getLabelComponent(), gbc);
            p.add(distributionTypeParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(numberOfTicksParam.getEditor().getLabelComponent(), gbc);
            p.add(numberOfTicksParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(labelValuesActualParam.getEditor().getLabelComponent(), gbc);
            p.add(labelValuesActualParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(labelValuesScalingParam.getEditor().getLabelComponent(), gbc);
            p.add(labelValuesScalingParam.getEditor().getEditorComponent(), gbc);





            gbc.gridy++;
            gbc.insets.top = 5;
            gbc.gridwidth = 1;
            p.add(backgroundTransparencyParam.getEditor().getLabelComponent(), gbc);
            p.add(backgroundTransparencyParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(useLegendWidthParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendWidthParam.getEditor().getLabelComponent(), gbc);
            p.add(legendWidthParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 2;
            p.add(bwColorOverrideParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(titleColorParam.getEditor().getLabelComponent(), gbc);
            p.add(titleColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(unitsColorParam.getEditor().getLabelComponent(), gbc);
            p.add(unitsColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(labelsColorParam.getEditor().getLabelComponent(), gbc);
            p.add(labelsColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(tickmarksColorParam.getEditor().getLabelComponent(), gbc);
            p.add(tickmarksColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(paletteBorderColorParam.getEditor().getLabelComponent(), gbc);
            p.add(paletteBorderColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(backdropColorParam.getEditor().getLabelComponent(), gbc);
            p.add(backdropColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(legendBorderColorParam.getEditor().getLabelComponent(), gbc);
            p.add(legendBorderColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 15;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            p.add(previewButton, gbc);

            gbc.gridy++;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.gridwidth = 2;
            JLabel info = new JLabel("<html><hr>More color bar format options available using the layer editor<br> and preferences.  See help page link below for details.<hr></html");
            p.add(info, gbc);
            p.setBorder(new EmptyBorder(7, 7, 7, 7));


            p.setMinimumSize(p.getPreferredSize());

            JScrollPane jScrollPane = new JScrollPane(p);

            setContent(jScrollPane);
        }






        private void initParams() {

            // Title and Units Text
            headerTextParam = paramGroup.getParameter(PROPERTY_TITLE_TEXT_KEY2);
            unitsTextParam = paramGroup.getParameter(PROPERTY_UNITS_TEXT_KEY2);

            // Orientation
            orientationParam = paramGroup.getParameter(PROPERTY_ORIENTATION_KEY2);
            reversePaletteParam = paramGroup.getParameter(PROPERTY_ORIENTATION_REVERSE_PALETTE_KEY2);

            // Tick Label Values
            distributionTypeParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_MODE_KEY2);
            numberOfTicksParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_COUNT_KEY2);
            labelValuesActualParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_ACTUAL_KEY2);
            labelValuesScalingParam = paramGroup.getParameter(PROPERTY_LABEL_VALUES_SCALING_KEY2);




            titleColorParam = paramGroup.getParameter(PROPERTY_TITLE_COLOR_KEY2);
            unitsColorParam = paramGroup.getParameter(PROPERTY_UNITS_FONT_COLOR_KEY2);
            labelsColorParam = paramGroup.getParameter(PROPERTY_LABELS_FONT_COLOR_KEY2);
            tickmarksColorParam = paramGroup.getParameter(PROPERTY_TICKMARKS_COLOR_KEY2);
            paletteBorderColorParam = paramGroup.getParameter(PROPERTY_PALETTE_BORDER_COLOR_KEY2);
            backdropColorParam = paramGroup.getParameter(PROPERTY_BACKDROP_COLOR_KEY2);
            legendBorderColorParam = paramGroup.getParameter(PROPERTY_LEGEND_BORDER_COLOR_KEY2);




            usingHeaderParam = paramGroup.getParameter(PROPERTY_TITLE_SHOW_KEY2);
            usingUnitsParam = paramGroup.getParameter(PROPERTY_UNITS_SHOW_KEY2);
            bwColorOverrideParam = paramGroup.getParameter(PROPERTY_EXPORT_USE_BW_COLOR_KEY2);
            useLegendWidthParam = paramGroup.getParameter(PROPERTY_EXPORT_USE_LEGEND_WIDTH_KEY2);
            legendWidthParam = paramGroup.getParameter(PROPERTY_EXPORT_LEGEND_WIDTH_KEY2);

            backgroundTransparencyParam = paramGroup.getParameter(PROPERTY_BACKDROP_TRANSPARENCY_KEY2);

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


        private static  JLabel sectionBreak(String title) {
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
