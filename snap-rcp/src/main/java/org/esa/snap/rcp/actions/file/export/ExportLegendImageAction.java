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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.List;

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
        "CTL_ExportLegendImageAction_MenuText=Colour Legend as Image",
        "CTL_ExportLegendImageAction_PopupText=Export Colour Bar Legend",
        "CTL_ExportLegendImageAction_ShortDescription=Export the colour legend of the current view as an image."
})

public class ExportLegendImageAction extends AbstractExportImageAction {
    private final static String[][] IMAGE_FORMAT_DESCRIPTIONS = {
            BMP_FORMAT_DESCRIPTION,
            PNG_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION,
    };

    public static final String PARAM_ORIENTATION_KEY = ColorBarLayerType.PROPERTY_ORIENTATION_KEY + ".export";
    private static final String DISTRIBUTION_TYPE_PARAM_STR = "legend.label.distribution.type";

    private static final String NUM_TICKS_PARAM_STR = "legend.numberOfTicks";
    private static final int NUM_TICKS_PARAM_DEFAULT = 5;


    public static final String SHOW_TITLE_PARAM_STR = ColorBarLayerType.PROPERTY_TITLE_SHOW_KEY + ".export";
    private static final String TITLE_PARAM_STR = "legend.headerText";
    private static final String TITLE_UNITS_PARAM_STR = "legend.header.units.text";
    private static final String MANUAL_POINTS_PARAM_STR = "legend.fullCustomAddThesePoints";
    private static final String DECIMAL_PLACES_PARAM_STR = "legend.decimalPlaces";
    private static final String DECIMAL_PLACES_FORCE_PARAM_STR = "legend.decimalPlacesForce";
    public static final String FOREGROUND_COLOR_PARAM_STR = "legend.foregroundColor";
    public static final String PARAM_BACKDROP_COLOR_KEY = ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY + ".export";
    public static final String TRANSPARENCY_PARAM_STR = "legend.transparent";

    public static final String SCALING_FACTOR_PARAM_STR = "legend.scalingFactor";
    private static final String TITLE_FONT_SIZE_PARAM_STR = "legend.titleFontSize";
    private static final String TITLE_UNITS_FONT_SIZE_PARAM_STR = "legend.titleUnitsFontSize";
    private static final String LABELS_FONT_SIZE_PARAM_STR = "legend.labelsFontSize";

    private static final String COLOR_BAR_LENGTH_PARAM_STR = "legend.colorBarLength";
    private static final String COLOR_BAR_THICKNESS_PARAM_STR = "legend.colorBarThickness";
    public static final String LAYER_SCALING_PARAM_STR = "legend.layerScalingThickness";
    private static final String LAYER_OFFSET_PARAM_STR = "legend.layerOffset";
    private static final String LAYER_SHIFT_PARAM_STR = "legend.layerShift";
    private static final String CENTER_ON_LAYER_PARAM_STR = "legend.centerOnLayer";

    private static final String HELP_ID = "exportLegendImageFile";
    private static final String HORIZONTAL_STR = "Horizontal";
    private static final String VERTICAL_STR = "Vertical";

    private SnapFileFilter[] imageFileFilters;

    private ParamGroup legendParamGroup;
    private ImageLegend imageLegend;

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


    }


    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new ExportLegendImageAction(lookup);
    }


    @Override
    public void actionPerformed(ActionEvent e) {

//        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
//        final RasterDataNode raster = view.getRaster();
//        imageLegend = new ImageLegend(raster.getImageInfo(), raster);
//
//        legendParamGroup = createLegendParamGroup();
//        legendParamGroup.setParameterValues(SnapApp.getDefault().getPreferencesPropertyMap(), null);
//
//        final ImageLegendDialog dialog = new ImageLegendDialog(legendParamGroup,
//                imageLegend,
//                true,
//                getHelpCtx().getHelpID());
//        dialog.show();


        // todo Danny
        exportImage(imageFileFilters);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        boolean enabled = view != null && !view.isRGB();
        setEnabled(enabled);
    }


    @Override
    protected void configureFileChooser(SnapFileChooser fileChooser, ProductSceneView view, String imageBaseName) {

        imageLegend = null;

//        SystemUtils.LOG.severe("Test severe message Danny");
//        SystemUtils.LOG.info("Test info message Danny");
//        SystemUtils.LOG.warning("Test warning message Danny");


        // Look for the existence of the ColorBar Layer and get a copy of its imageLegend

        List<Layer> layers = SnapApp.getDefault().getSelectedProductSceneView().getRootLayer().getChildren();
        for (Layer layer : layers) {
//            System.out.println("layerName=" + layer.getName());

            if (ColorBarLayerType.COLOR_BAR_LAYER_NAME.equals(layer.getName())) {
//                System.out.println("Found ColorBar layer");

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
//                imageLegend.updateWithProperties(configuration, raster);
            }
        }



        if (imageLegend != null) {
            // it's not a layer so no scaling
            imageLegend.setLayerScaling(1.0);


            legendParamGroup = createLegendParamGroup(imageLegend);
//        legendParamGroup.setParameterValues(SnapApp.getDefault().getPreferencesPropertyMap(), null);

//        modifyHeaderText(legendParamGroup, view.getRaster());
            fileChooser.setDialogTitle(SnapApp.getDefault().getInstanceName() + " - export Colour Legend Image"); /*I18N*/


            fileChooser.setCurrentFilename(imageBaseName + "_legend");

            //


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

        imageLegend.setNumberOfTicks(configuration.getPropertyInt(ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_COUNT_DEFAULT));

        imageLegend.setDistributionType(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_KEY,
                ColorBarLayerType.PROPERTY_LABEL_VALUES_MODE_DEFAULT));

        imageLegend.setFullCustomAddThesePoints(configuration.getPropertyString(ColorBarLayerType.PROPERTY_LABEL_VALUES_ACTUAL_KEY,
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

        imageLegend.setHeaderText(titleText);


        imageLegend.setTitleFontSize(
                configuration.getPropertyInt(ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_FONT_SIZE_DEFAULT));

        imageLegend.setTitleColor(
                configuration.getPropertyColor(ColorBarLayerType.PROPERTY_TITLE_COLOR_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_COLOR_DEFAULT));

        imageLegend.setTitleParameterFontName(
                configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_FONT_NAME_DEFAULT));


        boolean titleParameterBold = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_BOLD_DEFAULT);

        boolean titleParameterItalic = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_TITLE_FONT_ITALIC_DEFAULT);

        int titleFontType = ColorBarLayer.getFontType(titleParameterItalic, titleParameterBold);

        imageLegend.setTitleParameterFontType(titleFontType);












        // Units parameters

        imageLegend.setShowTitleUnits(
                configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_UNITS_SHOW_DEFAULT));


        String titleUnitsTextDefault = configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_DEFAULT);


        String titleUnitsText = (ColorBarLayerType.NULL_SPECIAL.equals(titleUnitsTextDefault)) ? "(" + raster.getUnit() + ")" : titleUnitsTextDefault;

        imageLegend.setHeaderUnitsText(titleUnitsText);


        imageLegend.setTitleUnitsFontSize(
                configuration.getPropertyInt(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_SIZE_DEFAULT));

        imageLegend.setTitleUnitsColor(
                configuration.getPropertyColor(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_COLOR_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_COLOR_DEFAULT));

        imageLegend.setTitleUnitsFontName(
                configuration.getPropertyString(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_NAME_KEY,
                        ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_NAME_DEFAULT));


        boolean titleUnitsBold = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_BOLD_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_BOLD_DEFAULT);

        boolean titleUnitsItalic = configuration.getPropertyBool(ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_ITALIC_KEY,
                ColorBarLayerType.PROPERTY_TITLE_UNITS_FONT_ITALIC_DEFAULT);

        int titleUnitsFontType = ColorBarLayer.getFontType(titleUnitsItalic, titleUnitsBold);

        imageLegend.setTitleUnitsFontType(titleUnitsFontType);








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

        imageLegend.setBackgroundColor(configuration.getPropertyColor(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_COLOR_DEFAULT));

        double backdropTrans = configuration.getPropertyDouble(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_KEY,
                ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_DEFAULT);

        imageLegend.setBackgroundTransparency(((Number) backdropTrans).floatValue());





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

        //            imageLegend.setBackgroundTransparencyEnabled(true);



    }






    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        transferParamsToImageLegend(legendParamGroup, imageLegend);
//        imageLegend.setTitleParameterColor(Color.white);
        imageLegend.setBackgroundTransparencyEnabled(isTransparencySupportedByFormat(imageFormat));
        return imageLegend.createImage();
    }

    @Override
    protected boolean isEntireImageSelected() {
        return true;
    }

    private static ParamGroup createLegendParamGroup(ImageLegend imageLegend) {
        ParamGroup paramGroup = new ParamGroup();


        Parameter param = new Parameter(SHOW_TITLE_PARAM_STR, imageLegend.isShowTitle());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_SHOW_LABEL);
        paramGroup.addParameter(param);


        param = new Parameter("legend.headerText", imageLegend.getTitleText());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_TEXT_LABEL);
        param.getProperties().setNumCols(24);
        param.getProperties().setNullValueAllowed(true);
        paramGroup.addParameter(param);


        param = new Parameter("legend.foregroundColor", imageLegend.getTitleColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_COLOR_LABEL);
        paramGroup.addParameter(param);




        param = new Parameter("legend.unitsText", imageLegend.getTitleUnitsText());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_UNITS_TEXT_LABEL);
        param.getProperties().setNumCols(24);
        param.getProperties().setNullValueAllowed(true);
        paramGroup.addParameter(param);




//        param = new Parameter("legend.orientation", HORIZONTAL_STR);
//        param.getProperties().setLabel("Orientation");
//        param.getProperties().setValueSet(new String[]{HORIZONTAL_STR, VERTICAL_STR});
//        param.getProperties().setValueSetBound(true);
//        paramGroup.addParameter(param);


//        String orientation = ColorBarLayerType.PROPERTY_ORIENTATION_DEFAULT;
//        if (imageLegend != null) {
//            System.out.println("obtaining orientation from imageLegend");
//            int orientationInt = imageLegend.getOrientation();
//            orientation = (orientationInt == ImageLegend.VERTICAL) ? ColorBarLayerType.OPTION_VERTICAL : ColorBarLayerType.OPTION_HORIZONTAL;
//        }
////        else if (configuration != null) {
////            System.out.println("obtaining orientation from configuration");
////            orientation = configuration.getPropertyString(ColorBarLayerType.PROPERTY_FORMATTING_ORIENTATION_KEY,
////                    ColorBarLayerType.PROPERTY_FORMATTING_ORIENTATION_DEFAULT);
////        }
//
//        System.out.println("orientation1=" + orientation);

        param = new Parameter(PARAM_ORIENTATION_KEY, imageLegend.getOrientation());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_ORIENTATION_LABEL);
        param.getProperties().setValueSet(new String[]{ColorBarLayerType.PROPERTY_ORIENTATION_OPTION1,
                ColorBarLayerType.PROPERTY_ORIENTATION_OPTION2});
        param.getProperties().setValueSetBound(true);
        paramGroup.addParameter(param);



        param = new Parameter(NUM_TICKS_PARAM_STR, imageLegend.getNumberOfTicks());
        param.getProperties().setLabel("Tick Mark Count");
        param.getProperties().setMinValue(0);
        param.getProperties().setMaxValue(40);
        paramGroup.addParameter(param);


//        int titleFontSize = ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_DEFAULT;
//        if (imageLegend != null) {
//            titleFontSize = imageLegend.getTitleFontSize();
//        } else if (configuration != null) {
//            titleFontSize = configuration.getPropertyInt(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_KEY,
//                    ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_DEFAULT);
//        }

//        param = new Parameter("legend.fontSize",   imageLegend.getTitleFontSize());
//        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_TITLE_PARAMETER_FONT_SIZE_LABEL);
//        param.getProperties().setMinValue(4);
//        param.getProperties().setMaxValue(100);
//        paramGroup.addParameter(param);
//





        param = new Parameter(PARAM_BACKDROP_COLOR_KEY, imageLegend.getBackgroundColor());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_BACKDROP_COLOR_LABEL);
        paramGroup.addParameter(param);

        param = new Parameter("legend.backgroundTransparency", imageLegend.getBackgroundTransparency());
        param.getProperties().setLabel(ColorBarLayerType.PROPERTY_BACKDROP_TRANSPARENCY_LABEL);
        param.getProperties().setMinValue(0.0f);
        param.getProperties().setMaxValue(1.0f);
        paramGroup.addParameter(param);

        param = new Parameter("legend.antialiasing", imageLegend.isAntialiasing());
        param.getProperties().setLabel("Perform anti-aliasing");
        paramGroup.addParameter(param);

        return paramGroup;
    }




    private static void modifyHeaderText(ParamGroup legendParamGroup, RasterDataNode raster) {
        String name = raster.getName();
        String unit = raster.getUnit() != null ? raster.getUnit() : "-";
        unit = unit.replace('*', ' ');
        String headerText = name + " [" + unit + "]";
        legendParamGroup.getParameter("legend.headerText").setValue(headerText, null);
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

        value = legendParamGroup.getParameter(SHOW_TITLE_PARAM_STR).getValue();
        imageLegend.setShowTitle((Boolean) value);

        value = legendParamGroup.getParameter("legend.headerText").getValue();
        imageLegend.setHeaderText((String) value);

        value = legendParamGroup.getParameter("legend.unitsText").getValue();
        imageLegend.setHeaderUnitsText((String) value);


        value = legendParamGroup.getParameter(PARAM_ORIENTATION_KEY).getValue();

        System.out.println("orientation (value)=" + value);
        if (ColorBarLayerType.OPTION_VERTICAL.equals(value)) {
            imageLegend.setOrientation(ImageLegend.VERTICAL);
            System.out.println("setting to vertical");
        } else {
            imageLegend.setOrientation(ImageLegend.HORIZONTAL);
            System.out.println("setting to horizontal");
        }

//
//        value = legendParamGroup.getParameter("legend.fontSize").getValue();
//        imageLegend.setTitleFontSize((Integer) value);
//


//        imageLegend.setFont(imageLegend.getFont().deriveFont(((Number) value).floatValue()));

        value = legendParamGroup.getParameter(PARAM_BACKDROP_COLOR_KEY).getValue();
        imageLegend.setBackgroundColor((Color) value);

        value = legendParamGroup.getParameter("legend.foregroundColor").getValue();
        imageLegend.setTitleColor((Color) value);

        value = legendParamGroup.getParameter("legend.backgroundTransparency").getValue();
        imageLegend.setBackgroundTransparency(((Number) value).floatValue());

        value = legendParamGroup.getParameter("legend.antialiasing").getValue();
        imageLegend.setAntialiasing((Boolean) value);


        value = legendParamGroup.getParameter(NUM_TICKS_PARAM_STR).getValue();
        imageLegend.setNumberOfTicks((Integer) value);


        // todo Danny just to get it working
//        imageLegend.setColorBarLength((Integer) 1200);
//        imageLegend.setColorBarThickness((Integer) 60);
//        imageLegend.setTitleFontSize((Integer) 12);
//        imageLegend.setTitleUnitsFontSize((Integer) 12);
//        imageLegend.setLabelsFontSize((Integer) 12);
//        imageLegend.setShowTitle((Boolean) true);
//        imageLegend.setDistributionType((String) ImageLegend.DISTRIB_EVEN_STR);
//        imageLegend.setNumberOfTicks((Integer) 5);
//        imageLegend.setScalingFactor((Double) 1.0);


    }


    public static class ImageLegendDialog extends ModalDialog {

        private ImageInfo imageInfo;
        private RasterDataNode raster;

        private ImageLegend imageLegend;
        private boolean transparencyEnabled;

        private ParamGroup paramGroup;


        private Parameter numberOfTicksParam;


        private Parameter usingHeaderParam;
        private Parameter headerTextParam;
        private Parameter unitsTextParam;
        private Parameter orientationParam;
        private Parameter fontSizeParam;
        private Parameter backgroundColorParam;
        private Parameter foregroundColorParam;
        private Parameter antialiasingParam;
        private Parameter backgroundTransparencyParam;


        public ImageLegendDialog(ParamGroup paramGroup, ImageLegend imageLegend,
                                 boolean transparencyEnabled, String helpId) {
            super(SnapApp.getDefault().getMainFrame(), SnapApp.getDefault().getInstanceName() + " - Colour Legend Properties", ID_OK_CANCEL_HELP, helpId);
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
            getParamGroup().getParameterValues(SnapApp.getDefault().getPreferencesPropertyMap());
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
            gbc.insets.top = 0;


            gbc.gridy = 0;
            gbc.gridwidth = 2;

            JLabel info = new JLabel("<html>More color bar format options available using the layer editor<br> and preferences.  See help page link below for details.<hr></html");
            p.add(info, gbc);


            gbc.gridy++;
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
            gbc.insets.top = 10;
            p.add(orientationParam.getEditor().getLabelComponent(), gbc);
            p.add(orientationParam.getEditor().getEditorComponent(), gbc);
//
//            gbc.gridy++;
//            gbc.insets.top = 3;
//            p.add(fontSizeParam.getEditor().getLabelComponent(), gbc);
//            p.add(fontSizeParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 10;
            p.add(foregroundColorParam.getEditor().getLabelComponent(), gbc);
            p.add(foregroundColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 3;
            p.add(backgroundColorParam.getEditor().getLabelComponent(), gbc);
            p.add(backgroundColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 3;
            p.add(backgroundTransparencyParam.getEditor().getLabelComponent(), gbc);
            p.add(backgroundTransparencyParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 10;
            p.add(numberOfTicksParam.getEditor().getLabelComponent(), gbc);
            p.add(numberOfTicksParam.getEditor().getEditorComponent(), gbc);


            gbc.gridy++;


            gbc.insets.top = 10;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            p.add(antialiasingParam.getEditor().getEditorComponent(), gbc);

            gbc.insets.top = 10;
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.NORTHEAST;
            p.add(previewButton, gbc);

            p.setBorder(new EmptyBorder(7, 7, 7, 7));

            setContent(p);
        }

        private void initParams() {
            usingHeaderParam = paramGroup.getParameter(SHOW_TITLE_PARAM_STR);
            numberOfTicksParam = paramGroup.getParameter(NUM_TICKS_PARAM_STR);
            headerTextParam = paramGroup.getParameter("legend.headerText");
            unitsTextParam = paramGroup.getParameter("legend.unitsText");
            System.out.println("Initializing orientation");
            orientationParam = paramGroup.getParameter(PARAM_ORIENTATION_KEY);
//            fontSizeParam = paramGroup.getParameter("legend.fontSize");
            foregroundColorParam = paramGroup.getParameter("legend.foregroundColor");
            backgroundColorParam = paramGroup.getParameter(PARAM_BACKDROP_COLOR_KEY);
            backgroundTransparencyParam = paramGroup.getParameter("legend.backgroundTransparency");
            antialiasingParam = paramGroup.getParameter("legend.antialiasing");
        }

        private void showPreview() {
//            final ImageLegend imageLegend = new ImageLegend(getImageInfo(), raster);
            getImageLegend(imageLegend);
            final BufferedImage image = imageLegend.createImage();
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
                    SnapApp.getDefault().getInstanceName() + " - Colour Legend Preview",
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
