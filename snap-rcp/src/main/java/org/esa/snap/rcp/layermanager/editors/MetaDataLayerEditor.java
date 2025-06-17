
package org.esa.snap.rcp.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.layer.MetaDataLayerType;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.layer.AbstractLayerConfigurationEditor;

import java.awt.*;
import java.util.ArrayList;


/**
 * Editor for Meta Data layer.
 *
 * @author Daniel Knowles
 */


public class MetaDataLayerEditor extends AbstractLayerConfigurationEditor {


    BindingContext context;
    PropertyMap configuration;

    @Override
    protected void addEditablePropertyDescriptors() {

        configuration = SnapApp.getDefault().getSelectedProductSceneView().getSceneImage().getConfiguration();
        context = getBindingContext();

        boolean headerShow = configuration.getPropertyBool(MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_DEFAULT);
        boolean footerShow = configuration.getPropertyBool(MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_DEFAULT);
        boolean footer2Show = configuration.getPropertyBool(MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_DEFAULT);


        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        ArrayList<String> fontNames = new ArrayList<String>();
        for (Font font: fonts) {
            font.getName();
//            System.out.println("Font=" + font.getName());
            if (font.getName() != null && font.getName().length() > 0) {
                fontNames.add(font.getName());
            }
        }
        String[] fontNameArray = new String[fontNames.size()];
        fontNameArray =  fontNames.toArray(fontNameArray);

        final Rendering rendering = new BufferedImageRendering(16, 16);
        final Graphics2D g2d = rendering.getGraphics();
        Font defaultFont = g2d.getFont();





        // Header Section

        addSectionBreak(MetaDataLayerType.PROPERTY_HEADER_SECTION_KEY,
                MetaDataLayerType.PROPERTY_HEADER_SECTION_LABEL,
                MetaDataLayerType.PROPERTY_HEADER_SECTION_TOOLTIP);


        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY,
                MetaDataLayerType.PROPERTY_HEADER_SHOW_DEFAULT,
                MetaDataLayerType.PROPERTY_HEADER_SHOW_LABEL,
                MetaDataLayerType.PROPERTY_HEADER_SHOW_TOOLTIP,
                true);



        PropertyDescriptor pd = new PropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_LOCATION_KEY, MetaDataLayerType.PROPERTY_HEADER_LOCATION_TYPE);
        pd.setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_LOCATION_DEFAULT);
        pd.setValueSet(new ValueSet(MetaDataLayerType.getHeaderLocationArray()));
        pd.setDisplayName(MetaDataLayerType.PROPERTY_HEADER_LOCATION_LABEL);
        pd.setDescription(MetaDataLayerType.PROPERTY_HEADER_LOCATION_TOOLTIP);
        pd.setEnabled(headerShow);
        pd.setDefaultConverter();
        addPropertyDescriptor(pd);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_LOCATION_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);


        PropertyDescriptor headerGapFactorPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_GAP_KEY, Double.class);
        headerGapFactorPD.setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_GAP_DEFAULT);
        headerGapFactorPD.setDisplayName(MetaDataLayerType.PROPERTY_HEADER_GAP_LABEL);
        headerGapFactorPD.setDescription(MetaDataLayerType.PROPERTY_HEADER_GAP_TOOLTIP);
        headerGapFactorPD.setEnabled(headerShow);
        headerGapFactorPD.setValueRange(new ValueRange(MetaDataLayerType.PROPERTY_HEADER_GAP_MIN, MetaDataLayerType.PROPERTY_HEADER_GAP_MAX));
        headerGapFactorPD.setDefaultConverter();
        addPropertyDescriptor(headerGapFactorPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_GAP_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);



        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_KEY,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_DEFAULT,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_LABEL,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_TOOLTIP,
                headerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_KEY,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_DEFAULT,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_LABEL,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_TOOLTIP,
                headerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD2_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_KEY,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_DEFAULT,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_LABEL,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_TOOLTIP,
                headerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD3_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_KEY,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_DEFAULT,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_LABEL,
                MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_TOOLTIP,
                headerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_TEXTFIELD4_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);


        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_CONVERT_CARET_KEY,
                MetaDataLayerType.PROPERTY_HEADER_CONVERT_CARET_DEFAULT,
                MetaDataLayerType.PROPERTY_HEADER_CONVERT_CARET_LABEL,
                MetaDataLayerType.PROPERTY_HEADER_CONVERT_CARET_TOOLTIP,
                true
        );









        // Annotation Contents Section

        addSectionBreak(MetaDataLayerType.PROPERTY_MARGIN_SECTION_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_SECTION_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_SECTION_TOOLTIP);


        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_SHOW_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_SHOW_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_SHOW_TOOLTIP,
                true);


        // Header Location Subsection

//        addSectionBreak(MetaDataLayerType.PROPERTY_HEADER_LOCATION_SECTION_KEY,
//                MetaDataLayerType.PROPERTY_HEADER_LOCATION_SECTION_LABEL,
//                MetaDataLayerType.PROPERTY_HEADER_LOCATION_SECTION_TOOLTIP,
//                headerShow);
//        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_LOCATION_SECTION_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);



        PropertyDescriptor footerLocationPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_LOCATION_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_LOCATION_TYPE);
        footerLocationPD.setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_LOCATION_DEFAULT);
        footerLocationPD.setValueSet(new ValueSet(MetaDataLayerType.getMarginLocationArray()));
        footerLocationPD.setDisplayName(MetaDataLayerType.PROPERTY_MARGIN_LOCATION_LABEL);
        footerLocationPD.setDescription(MetaDataLayerType.PROPERTY_MARGIN_LOCATION_TOOLTIP);
        footerLocationPD.setEnabled(footerShow);
        footerLocationPD.setDefaultConverter();
        addPropertyDescriptor(footerLocationPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_LOCATION_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);




        PropertyDescriptor locationGapFactorPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_GAP_KEY, Double.class);
        locationGapFactorPD.setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_GAP_DEFAULT);
        locationGapFactorPD.setDisplayName(MetaDataLayerType.PROPERTY_MARGIN_GAP_LABEL);
        locationGapFactorPD.setDescription(MetaDataLayerType.PROPERTY_MARGIN_GAP_TOOLTIP);
        locationGapFactorPD.setEnabled(footerShow);
        locationGapFactorPD.setValueRange(new ValueRange(MetaDataLayerType.PROPERTY_MARGIN_GAP_MIN, MetaDataLayerType.PROPERTY_MARGIN_GAP_MAX));
        locationGapFactorPD.setDefaultConverter();
        addPropertyDescriptor(locationGapFactorPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_GAP_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_TOOLTIP,
                footerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);

        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_TOOLTIP,
                footerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_TEXTFIELD2_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);





// Only showing in preferences as it was too much
//        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_PROPERTY_HEADING_KEY,
//                MetaDataLayerType.PROPERTY_MARGIN_PROPERTY_HEADING_DEFAULT,
//                MetaDataLayerType.PROPERTY_MARGIN_PROPERTY_HEADING_LABEL,
//                MetaDataLayerType.PROPERTY_MARGIN_PROPERTY_HEADING_TOOLTIP,
//                footerShow);
//        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_PROPERTY_HEADING_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);


        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_TOOLTIP,
                footerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_INFO_KEYS_SHOW_ALL_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_TOOLTIP,
                footerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_METADATA2_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA2_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA2_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA2_TOOLTIP,
                footerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_METADATA2_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);




// Only showing in preferences as it was too much
//
//        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_GLOBAL_HEADING_KEY,
//                MetaDataLayerType.PROPERTY_MARGIN_GLOBAL_HEADING_DEFAULT,
//                MetaDataLayerType.PROPERTY_MARGIN_GLOBAL_HEADING_LABEL,
//                MetaDataLayerType.PROPERTY_MARGIN_GLOBAL_HEADING_TOOLTIP,
//                footerShow);
//        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_GLOBAL_HEADING_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);

        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_TOOLTIP,
                footerShow
        );
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_METADATA_SHOW_ALL_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);

        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_TOOLTIP,
                footerShow
        );
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_METADATA_PROCESS_CONTROL_SHOW_ALL_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_METADATA3_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA3_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA3_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA3_TOOLTIP,
                footerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_METADATA3_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);

        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_METADATA4_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA4_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA4_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA4_TOOLTIP,
                footerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_METADATA4_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);



// Only showing in preferences as it was too much
//        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_BAND_HEADING_KEY,
//                MetaDataLayerType.PROPERTY_MARGIN_BAND_HEADING_DEFAULT,
//                MetaDataLayerType.PROPERTY_MARGIN_BAND_HEADING_LABEL,
//                MetaDataLayerType.PROPERTY_MARGIN_BAND_HEADING_TOOLTIP,
//                footerShow);
//        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_BAND_HEADING_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);


        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_TOOLTIP,
                footerShow
        );
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_BAND_METADATA_SHOW_ALL_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_METADATA5_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA5_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA5_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_METADATA5_TOOLTIP,
                footerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_METADATA5_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);


// Only showing in preferences as it was too much
//        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_KEY,
//                MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_DEFAULT,
//                MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_LABEL,
//                MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_TOOLTIP,
//                footerShow);
//        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_METADATA_DELIMITER_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);
//




        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_CONVERT_CARET_KEY,
                MetaDataLayerType.PROPERTY_MARGIN_CONVERT_CARET_DEFAULT,
                MetaDataLayerType.PROPERTY_MARGIN_CONVERT_CARET_LABEL,
                MetaDataLayerType.PROPERTY_MARGIN_CONVERT_CARET_TOOLTIP,
                true
        );



//
//
//        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEYS_SHOW_KEY,
//                MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEYS_SHOW_DEFAULT,
//                MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEYS_SHOW_LABEL,
//                MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEYS_SHOW_TOOLTIP,
//                footerShow);
//        context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_METADATA_KEYS_SHOW_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);





        // Footer2 Contents Section

        addSectionBreak(MetaDataLayerType.PROPERTY_FOOTER2_SECTION_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_SECTION_LABEL,
                MetaDataLayerType.PROPERTY_FOOTER2_SECTION_TOOLTIP);


        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_SHOW_DEFAULT,
                MetaDataLayerType.PROPERTY_FOOTER2_SHOW_LABEL,
                MetaDataLayerType.PROPERTY_FOOTER2_SHOW_TOOLTIP,
                true);

        PropertyDescriptor footer2LocationPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_TYPE);
        footer2LocationPD.setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_DEFAULT);
        footer2LocationPD.setValueSet(new ValueSet(MetaDataLayerType.getFooter2LocationArray()));
        footer2LocationPD.setDisplayName(MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_LABEL);
        footer2LocationPD.setDescription(MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_TOOLTIP);
        footer2LocationPD.setEnabled(footer2Show);
        footer2LocationPD.setDefaultConverter();
        addPropertyDescriptor(footer2LocationPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_LOCATION_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);


        PropertyDescriptor footer2GapFactorPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_GAP_KEY, Double.class);
        footer2GapFactorPD.setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_GAP_DEFAULT);
        footer2GapFactorPD.setDisplayName(MetaDataLayerType.PROPERTY_FOOTER2_GAP_LABEL);
        footer2GapFactorPD.setDescription(MetaDataLayerType.PROPERTY_FOOTER2_GAP_TOOLTIP);
        footer2GapFactorPD.setEnabled(footer2Show);
        footer2GapFactorPD.setValueRange(new ValueRange(MetaDataLayerType.PROPERTY_FOOTER2_GAP_MIN, MetaDataLayerType.PROPERTY_FOOTER2_GAP_MAX));
        footer2GapFactorPD.setDefaultConverter();
        addPropertyDescriptor(footer2GapFactorPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_GAP_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);



        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_DEFAULT,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_LABEL,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_TOOLTIP,
                footer2Show);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);

        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_DEFAULT,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_LABEL,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_TOOLTIP,
                footer2Show);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD2_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_DEFAULT,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_LABEL,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_TOOLTIP,
                footer2Show);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD3_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);


        addStringPropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_DEFAULT,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_LABEL,
                MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_TOOLTIP,
                footer2Show);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_TEXTFIELD4_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);


        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER_CONVERT_CARET_KEY,
                MetaDataLayerType.PROPERTY_FOOTER_CONVERT_CARET_DEFAULT,
                MetaDataLayerType.PROPERTY_FOOTER_CONVERT_CARET_LABEL,
                MetaDataLayerType.PROPERTY_FOOTER_CONVERT_CARET_TOOLTIP,
                true
        );

        addBooleanPropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_DEFAULT,
                MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_LABEL,
                MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_TOOLTIP,
                footer2Show);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_MY_INFO_SHOW_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);





        // Header Format Section



        addSectionBreak(MetaDataLayerType.PROPERTY_HEADER_FORMAT_SECTION_KEY,
                MetaDataLayerType.PROPERTY_HEADER_FORMAT_SECTION_LABEL,
                MetaDataLayerType.PROPERTY_HEADER_FORMAT_SECTION_TOOLTIP,
                headerShow);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_FORMAT_SECTION_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);



        PropertyDescriptor labelSizePD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_KEY, Integer.class);
        labelSizePD.setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_DEFAULT);
        labelSizePD.setDisplayName(MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_LABEL);
        labelSizePD.setDescription(MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_TOOLTIP);
        labelSizePD.setEnabled(headerShow);
        labelSizePD.setValueRange(new ValueRange(MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_VALUE_MIN, MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_VALUE_MAX));
        labelSizePD.setDefaultConverter();
        addPropertyDescriptor(labelSizePD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_FONT_SIZE_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);


        PropertyDescriptor labelColorPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_KEY, Color.class);
        labelColorPD.setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_DEFAULT);
        labelColorPD.setDisplayName(MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_LABEL);
        labelColorPD.setDescription(MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_TOOLTIP);
        labelColorPD.setEnabled(headerShow);
        labelColorPD.setDefaultConverter();
        addPropertyDescriptor(labelColorPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_FONT_COLOR_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);


        PropertyDescriptor labelsFontPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_KEY, String.class);

//        labelsFontPD.setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_DEFAULT);
        boolean fontExists = false;
        for (String font : fontNameArray) {
            if (MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_DEFAULT.equals(font)) {
                fontExists = true;
                break;
            }
        }
        if (fontExists) {
            labelsFontPD.setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_DEFAULT);
        } else {
            labelsFontPD.setDefaultValue(defaultFont.toString());
        }

        labelsFontPD.setDisplayName(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_LABEL);
        labelsFontPD.setDescription(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_TOOLTIP);
        labelsFontPD.setEnabled(headerShow);
//        labelsFontPD.setValueSet(new ValueSet(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_VALUE_SET));
        labelsFontPD.setValueSet(new ValueSet(fontNameArray));
        labelsFontPD.setDefaultConverter();
        addPropertyDescriptor(labelsFontPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_FONT_STYLE_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);


        PropertyDescriptor labelsItalicsPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_KEY, Boolean.class);
        labelsItalicsPD.setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_DEFAULT);
        labelsItalicsPD.setDisplayName(MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_LABEL);
        labelsItalicsPD.setDescription(MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_TOOLTIP);
        labelsItalicsPD.setEnabled(headerShow);
        labelsItalicsPD.setDefaultConverter();
        addPropertyDescriptor(labelsItalicsPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_FONT_ITALIC_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);


        PropertyDescriptor labelsBoldPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_KEY, Boolean.class);
        labelsBoldPD.setDefaultValue(MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_DEFAULT);
        labelsBoldPD.setDisplayName(MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_LABEL);
        labelsBoldPD.setDescription(MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_TOOLTIP);
        labelsBoldPD.setEnabled(headerShow);
        labelsBoldPD.setDefaultConverter();
        addPropertyDescriptor(labelsBoldPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_HEADER_FONT_BOLD_KEY, MetaDataLayerType.PROPERTY_HEADER_SHOW_KEY);





        // Margin Format Section

            addSectionBreak(MetaDataLayerType.PROPERTY_MARGIN_FORMATTING_SECTION_KEY,
                    MetaDataLayerType.PROPERTY_MARGIN_FORMATTING_SECTION_LABEL,
                    MetaDataLayerType.PROPERTY_MARGIN_FORMATTING_SECTION_TOOLTIP);


            PropertyDescriptor footerFontSizePD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_KEY, Integer.class);
            footerFontSizePD.setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_DEFAULT);
            footerFontSizePD.setDisplayName(MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_LABEL);
            footerFontSizePD.setDescription(MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_TOOLTIP);
            footerFontSizePD.setEnabled(footerShow);
            footerFontSizePD.setValueRange(new ValueRange(MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_MIN, MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_MAX));
            footerFontSizePD.setDefaultConverter();
            addPropertyDescriptor(footerFontSizePD);
            context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_FONT_SIZE_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);

            PropertyDescriptor marginFontColorPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_KEY, Color.class);
            marginFontColorPD.setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_DEFAULT);
            marginFontColorPD.setDisplayName(MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_LABEL);
            marginFontColorPD.setDescription(MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_TOOLTIP);
            marginFontColorPD.setEnabled(footerShow);
            marginFontColorPD.setDefaultConverter();
            addPropertyDescriptor(marginFontColorPD);
            context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_FONT_COLOR_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);

            PropertyDescriptor marginFontStylePD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_KEY, String.class);

            boolean marginFontExists = false;
            for (String font : fontNameArray) {
                if (MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_DEFAULT.equals(font)) {
                    marginFontExists = true;
                    break;
                }
            }
            if (marginFontExists) {
                marginFontStylePD.setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_DEFAULT);
            } else {
                marginFontStylePD.setDefaultValue(defaultFont.toString());
            }

//        marginFontStylePD.setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_DEFAULT);
            marginFontStylePD.setDisplayName(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_LABEL);
            marginFontStylePD.setDescription(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_TOOLTIP);
            marginFontStylePD.setEnabled(footerShow);
            marginFontStylePD.setValueSet(new ValueSet(fontNameArray));
//        footerFontStylePD.setValueSet(new ValueSet(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_VALUE_SET));
            marginFontStylePD.setDefaultConverter();
            addPropertyDescriptor(marginFontStylePD);
            context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_FONT_STYLE_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);

            PropertyDescriptor footerFontItalicPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_KEY, Boolean.class);
            footerFontItalicPD.setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_DEFAULT);
            footerFontItalicPD.setDisplayName(MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_LABEL);
            footerFontItalicPD.setDescription(MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_TOOLTIP);
            footerFontItalicPD.setEnabled(footerShow);
            footerFontItalicPD.setDefaultConverter();
            addPropertyDescriptor(footerFontItalicPD);
            context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_FONT_ITALIC_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);


            PropertyDescriptor footerFontBoldPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_KEY, Boolean.class);
            footerFontBoldPD.setDefaultValue(MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_DEFAULT);
            footerFontBoldPD.setDisplayName(MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_LABEL);
            footerFontBoldPD.setDescription(MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_TOOLTIP);
            footerFontBoldPD.setEnabled(footerShow);
            footerFontBoldPD.setDefaultConverter();
            addPropertyDescriptor(footerFontBoldPD);
            context.bindEnabledState(MetaDataLayerType.PROPERTY_MARGIN_FONT_BOLD_KEY, MetaDataLayerType.PROPERTY_MARGIN_SHOW_KEY);







        // My Info Section
//
//        addSectionBreak(MetaDataLayerType.PROPERTY_MY_INFO_SECTION_KEY,
//                MetaDataLayerType.PROPERTY_MY_INFO_SECTION_LABEL,
//                MetaDataLayerType.PROPERTY_MY_INFO_SECTION_TOOLTIP);
//
//        PropertyDescriptor myInfo1PD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_KEY,
//                MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_TYPE);
//        myInfo1PD.setDefaultValue(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_DEFAULT);
//        myInfo1PD.setDisplayName(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_LABEL);
//        myInfo1PD.setDescription(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD1_TOOLTIP);
//        myInfo1PD.setEnabled(true);
//        myInfo1PD.setDefaultConverter();
//        addPropertyDescriptor(myInfo1PD);
//
//        PropertyDescriptor myInfo2PD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_KEY,
//                MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_TYPE);
//        myInfo2PD.setDefaultValue(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_DEFAULT);
//        myInfo2PD.setDisplayName(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_LABEL);
//        myInfo2PD.setDescription(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD2_TOOLTIP);
//        myInfo2PD.setEnabled(true);
//        myInfo2PD.setDefaultConverter();
//        addPropertyDescriptor(myInfo2PD);
//
//        PropertyDescriptor myInfo3PD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_KEY,
//                MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_TYPE);
//        myInfo3PD.setDefaultValue(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_DEFAULT);
//        myInfo3PD.setDisplayName(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_LABEL);
//        myInfo3PD.setDescription(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD3_TOOLTIP);
//        myInfo3PD.setEnabled(true);
//        myInfo3PD.setDefaultConverter();
//        addPropertyDescriptor(myInfo3PD);
//
//        PropertyDescriptor myInfo4PD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_KEY,
//                MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_TYPE);
//        myInfo4PD.setDefaultValue(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_DEFAULT);
//        myInfo4PD.setDisplayName(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_LABEL);
//        myInfo4PD.setDescription(MetaDataLayerType.PROPERTY_MY_INFO_TEXTFIELD4_TOOLTIP);
//        myInfo4PD.setEnabled(true);
//        myInfo4PD.setDefaultConverter();
//        addPropertyDescriptor(myInfo4PD);
//





        // Footer2 Format Section

        addSectionBreak(MetaDataLayerType.PROPERTY_FOOTER2_FORMATTING_SECTION_KEY,
                MetaDataLayerType.PROPERTY_FOOTER2_FORMATTING_SECTION_LABEL,
                MetaDataLayerType.PROPERTY_FOOTER2_FORMATTING_SECTION_TOOLTIP);


        PropertyDescriptor footer2FontSizePD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_KEY, Integer.class);
        footer2FontSizePD.setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_DEFAULT);
        footer2FontSizePD.setDisplayName(MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_LABEL);
        footer2FontSizePD.setDescription(MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_TOOLTIP);
        footer2FontSizePD.setEnabled(footer2Show);
        footer2FontSizePD.setValueRange(new ValueRange(MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_MIN, MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_MAX));
        footer2FontSizePD.setDefaultConverter();
        addPropertyDescriptor(footer2FontSizePD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_FONT_SIZE_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);

        PropertyDescriptor footer2FontColorPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_KEY, Color.class);
        footer2FontColorPD.setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_DEFAULT);
        footer2FontColorPD.setDisplayName(MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_LABEL);
        footer2FontColorPD.setDescription(MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_TOOLTIP);
        footer2FontColorPD.setEnabled(footer2Show);
        footer2FontColorPD.setDefaultConverter();
        addPropertyDescriptor(footer2FontColorPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_FONT_COLOR_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);

        PropertyDescriptor footer2FontStylePD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_KEY, String.class);

        boolean footer2FontExists = false;
        for (String font : fontNameArray) {
            if (MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_DEFAULT.equals(font)) {
                footer2FontExists = true;
                break;
            }
        }
        if (footer2FontExists) {
            footer2FontStylePD.setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_DEFAULT);
        } else {
            footer2FontStylePD.setDefaultValue(defaultFont.toString());
        }

//        footer2FontStylePD.setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_DEFAULT);
        footer2FontStylePD.setDisplayName(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_LABEL);
        footer2FontStylePD.setDescription(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_TOOLTIP);
        footer2FontStylePD.setEnabled(footer2Show);
        footer2FontStylePD.setValueSet(new ValueSet(fontNameArray));
//        footer2FontStylePD.setValueSet(new ValueSet(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_VALUE_SET));
        footer2FontStylePD.setDefaultConverter();
        addPropertyDescriptor(footer2FontStylePD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_FONT_STYLE_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);

        PropertyDescriptor footer2FontItalicPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_KEY, Boolean.class);
        footer2FontItalicPD.setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_DEFAULT);
        footer2FontItalicPD.setDisplayName(MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_LABEL);
        footer2FontItalicPD.setDescription(MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_TOOLTIP);
        footer2FontItalicPD.setEnabled(footer2Show);
        footer2FontItalicPD.setDefaultConverter();
        addPropertyDescriptor(footer2FontItalicPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_FONT_ITALIC_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);


        PropertyDescriptor footer2FontBoldPD = new PropertyDescriptor(MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_KEY, Boolean.class);
        footer2FontBoldPD.setDefaultValue(MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_DEFAULT);
        footer2FontBoldPD.setDisplayName(MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_LABEL);
        footer2FontBoldPD.setDescription(MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_TOOLTIP);
        footer2FontBoldPD.setEnabled(footer2Show);
        footer2FontBoldPD.setDefaultConverter();
        addPropertyDescriptor(footer2FontBoldPD);
        context.bindEnabledState(MetaDataLayerType.PROPERTY_FOOTER2_FONT_BOLD_KEY, MetaDataLayerType.PROPERTY_FOOTER2_SHOW_KEY);





    }


    private void addSectionBreak(String name, String label, String toolTip) {
        PropertyDescriptor descriptor = new PropertyDescriptor(name, Boolean.class);
        descriptor.setDisplayName(label);
        descriptor.setDescription(toolTip);
        addPropertyDescriptor(descriptor);
    }

    private void addSectionBreak(String name, String label, String toolTip, boolean enabled) {
        PropertyDescriptor descriptor = new PropertyDescriptor(name, Boolean.class);
        descriptor.setDisplayName(label);
        descriptor.setDescription(toolTip);
        descriptor.setEnabled(enabled);
        addPropertyDescriptor(descriptor);
    }

    private void addBooleanPropertyDescriptor(String name, boolean defaultValue, String label, String toolTip, boolean enabled) {
        PropertyDescriptor descriptor = new PropertyDescriptor(name, Boolean.class);
        descriptor.setDefaultValue(defaultValue);
        descriptor.setDisplayName(label);
        descriptor.setDescription(toolTip);
        descriptor.setEnabled(enabled);
        descriptor.setDefaultConverter();
        addPropertyDescriptor(descriptor);
    }

    private void addStringPropertyDescriptor(String name, String defaultValue, String label, String toolTip, boolean enabled) {
        PropertyDescriptor descriptor = new PropertyDescriptor(name, String.class);
        descriptor.setDefaultValue(defaultValue);
        descriptor.setDisplayName(label);
        descriptor.setDescription(toolTip);
        descriptor.setEnabled(enabled);
        descriptor.setDefaultConverter();
        addPropertyDescriptor(descriptor);
    }


}
