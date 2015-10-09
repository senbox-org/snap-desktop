/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.preferences.general;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.rcp.pixelinfo.PixelInfoView;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.rcp.preferences.PreferenceUtils;
import org.esa.snap.ui.GridBagUtils;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import static com.bc.ceres.swing.TableLayout.*;
import static org.esa.snap.rcp.pixelinfo.PixelInfoView.*;

/**
 * The preferences panel handling geo-location details. Sub-level panel to the "Miscellaneous"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#TXT_GeoLocationController_DisplayName",
        keywords = "#TXT_GeoLocationController_Keyword",
        keywordsCategory = "Pixel Display, Geolocation",
        id = "GeolocationController",
        position = 2)
@NbBundle.Messages({
        "TXT_GeoLocationController_DisplayName=Geo-Location",
        "TXT_GeoLocationController_Keyword=geo, location, geo-location, compatibility, differ"
})
public final class GeoLocationController extends DefaultConfigController {

    private static final String PREFERENCE_KEY_ADJUST_PIN_GEO_POS = Placemark.PREFERENCE_KEY_ADJUST_PIN_GEO_POS;
    private static final String PREFERENCE_KEY_PIXEL_GEO_CODING_FRACTION_ACCURACY = "snap.pixelGeoCoding.fractionAccuracy";


    protected PropertySet createPropertySet() {
        return createPropertySet(new GeoLocationBean());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(PixelInfoView.HELP_ID);
    }

    @Override
    protected JPanel createPanel(BindingContext context) {

        final PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property snapToExactGeolocationProperty = context.getPropertySet().getProperty(PREFERENCE_KEY_ADJUST_PIN_GEO_POS);
        Property pixelGeocodingFractionAccuracyProperty = context.getPropertySet().getProperty(PREFERENCE_KEY_PIXEL_GEO_CODING_FRACTION_ACCURACY);
        Property showGeoPosAsDecimals = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_GEO_POS_DECIMALS);
        Property showPixelPosAsDecimals = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_PIXEL_POS_DECIMALS);
        Property showPixelPosOffset1 = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_PIXEL_POS_OFFSET_ONE);

        JComponent[] snapToExactGeolocationComponents = registry.findPropertyEditor(snapToExactGeolocationProperty.getDescriptor()).createComponents(snapToExactGeolocationProperty.getDescriptor(), context);
        JComponent[] pixelGeocodingfractionAccuracyComponents = registry.findPropertyEditor(pixelGeocodingFractionAccuracyProperty.getDescriptor()).createComponents(pixelGeocodingFractionAccuracyProperty.getDescriptor(), context);
        JComponent[] showGeoPosAsDecimalsComponents = registry.findPropertyEditor(showGeoPosAsDecimals.getDescriptor()).createComponents(showGeoPosAsDecimals.getDescriptor(), context);
        JComponent[] showPixelPosAsDecimalsComponents = registry.findPropertyEditor(showPixelPosAsDecimals.getDescriptor()).createComponents(showPixelPosAsDecimals.getDescriptor(), context);
        JComponent[] showPixelPosOffset1Components = registry.findPropertyEditor(showPixelPosAsDecimals.getDescriptor()).createComponents(showPixelPosOffset1.getDescriptor(), context);

        TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setRowWeightY(8, 1.0);

        final JPanel pageUI = new JPanel(tableLayout);

        pageUI.add(PreferenceUtils.createTitleLabel("General Settings"));
        pageUI.add(snapToExactGeolocationComponents[0]);
        pageUI.add(pixelGeocodingfractionAccuracyComponents[0]);
        tableLayout.createHorizontalSpacer();

        pageUI.add(PreferenceUtils.createTitleLabel("Display Settings"));
        pageUI.add(showGeoPosAsDecimalsComponents[0]);
        pageUI.add(showPixelPosAsDecimalsComponents[0]);
        pageUI.add(showPixelPosOffset1Components[0]);
        pageUI.add(tableLayout.createVerticalSpacer());
        return createPageUIContentPane(pageUI);
    }

    private static JPanel createPageUIContentPane(JPanel pane) {
        JPanel contentPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=NORTHWEST");
        gbc.insets.top = 15;
        gbc.weightx = 1;
        gbc.weighty = 0;
        contentPane.add(pane, gbc);
        GridBagUtils.addVerticalFiller(contentPane, gbc);
        return contentPane;
    }

    static class GeoLocationBean {

        @Preference(label = "Use sub-pixel fraction accuracy for pixel-based geo-coding",
                key = PREFERENCE_KEY_PIXEL_GEO_CODING_FRACTION_ACCURACY, config = "snap")
        boolean getPixelPosWithFractionAccuracy = false;

        @Preference(label = "Snap pins to exact geo-location after import, transfer to another product, or geo-coding change",
                key = PREFERENCE_KEY_ADJUST_PIN_GEO_POS, config = "snap")
        boolean snapToExactGeoLocation = true;

        @Preference(label = "Show geographical coordinates in decimal degrees", key = PREFERENCE_KEY_SHOW_GEO_POS_DECIMALS)
        boolean showGeoPosAsDecimals = PREFERENCE_DEFAULT_SHOW_GEO_POS_DECIMALS;

        @Preference(label = "Show pixel coordinates with fractional part", key = PREFERENCE_KEY_SHOW_PIXEL_POS_DECIMALS)
        boolean showPixelPosAsDecimals = PREFERENCE_DEFAULT_SHOW_PIXEL_POS_DECIMALS;

        @Preference(label = "Show pixel coordinates starting at (1,1)", key = PREFERENCE_KEY_SHOW_PIXEL_POS_OFFSET_ONE,
                description = "Show pixel coordinates so that the upper left image corner is (1,1), instead of (0,0).")
        boolean showPixelPosWithOffset1 = PREFERENCE_DEFAULT_SHOW_PIXEL_POS_OFFSET_1;
    }

}
