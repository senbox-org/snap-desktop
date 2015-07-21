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
import org.esa.snap.framework.ui.GridBagUtils;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import static com.bc.ceres.swing.TableLayout.*;

/**
 * The preferences panel handling geo-location details. Sub-level panel to the "Miscellaneous"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_GeoLocation",
        keywords = "#Options_Keywords_GeoLocation",
        keywordsCategory = "Geo-Location",
        id = "GeoLocation",
        position = 2)
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_GeoLocation=Geo-Location",
        "Options_Keywords_GeoLocation=geo, location, geo-location, compatibility, differ"
})
public final class GeoLocationController extends DefaultConfigController {

    /**
     * Preference key for coordinate system starting at (1,1)
     */
    public static final String PROPERTY_KEY_PIXEL_OFFSET_IS_ONE = "pixel.coordinates.starting.at.one";

    /**
     * Preferences key for showing floating-point image coordinates
     */
    public static final String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = "pixel.offset.display.show.decimals";

    /**
     * Preferences key for display style of geo-locations
     */
    public static final String PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL = "geolocation.display.decimal";

    /**
     * Default value for pixel offset's for display pixel positions
     */
    public static final boolean PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = false;
    /**
     * Default value for display style of geo-locations.
     */
    public static final boolean PROPERTY_DEFAULT_DISPLAY_GEOLOCATION_AS_DECIMAL = false;

    /**
     * Default value for steeing the starting point for pixel coordinate
     */
    public static final boolean PROPERTY_DEFAULT_PIXEL_OFFSET_IS_ONE = false;

    protected PropertySet createPropertySet() {
        return createPropertySet(new GeoLocationBean());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("geo-location");
    }

    @Override
    protected JPanel createPanel(BindingContext context) {

        final PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property paramShowDecimals = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS);
        Property paramPixelCoordinateStartingAtOne = context.getPropertySet().getProperty(PROPERTY_KEY_PIXEL_OFFSET_IS_ONE);
        Property paramGeolocationAsDecimal = context.getPropertySet().getProperty(PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL);

        JComponent[] showDecimalComponents = registry.findPropertyEditor(paramShowDecimals.getDescriptor()).createComponents(paramShowDecimals.getDescriptor(), context);
        JComponent[] pixelCoordinateStart = registry.findPropertyEditor(paramShowDecimals.getDescriptor()).createComponents(paramPixelCoordinateStartingAtOne.getDescriptor(), context);
        JComponent[] geolocationAsDecimalComponents = registry.findPropertyEditor(paramGeolocationAsDecimal.getDescriptor()).createComponents(paramGeolocationAsDecimal.getDescriptor(), context);

        TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(Anchor.NORTHWEST);
        tableLayout.setTablePadding(4, 10);
        tableLayout.setTableFill(Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowWeightY(4, 1.0);

        final JPanel pageUI = new JPanel(tableLayout);

        tableLayout.setRowPadding(0, new Insets(10, 80, 10, 4));
        pageUI.add(pixelCoordinateStart[0]);
        pageUI.add(showDecimalComponents[0]);
        pageUI.add(geolocationAsDecimalComponents[0]);
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

        @Preference(label = "Show floating-point image coordinates", key = PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS)
        boolean paramShowDecimals = PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS;

        @Preference(label = "Display pixel coordinate starting at (1,1)", key = PROPERTY_KEY_PIXEL_OFFSET_IS_ONE,
                description = "Pixel coordinate with the upper left image pixel of (1,1) coordinate, instead of (0,0).")
        boolean pixelCoordinateStart = PROPERTY_DEFAULT_PIXEL_OFFSET_IS_ONE;

        @Preference(label = "Show geo-location coordinates in decimal degrees", key = PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL)
        boolean paramGeolocationAsDecimal = PROPERTY_DEFAULT_DISPLAY_GEOLOCATION_AS_DECIMAL;
    }

}
