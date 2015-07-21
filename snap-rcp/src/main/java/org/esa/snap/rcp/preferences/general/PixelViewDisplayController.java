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
import org.esa.snap.rcp.pixelinfo.PixelInfoView;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import static com.bc.ceres.swing.TableLayout.*;
import static org.esa.snap.rcp.pixelinfo.PixelInfoView.PROPERTY_DEFAULT_SHOW_GEO_POS_DECIMALS;
import static org.esa.snap.rcp.pixelinfo.PixelInfoView.PROPERTY_DEFAULT_SHOW_PIXEL_POS_DECIMALS;
import static org.esa.snap.rcp.pixelinfo.PixelInfoView.PROPERTY_DEFAULT_SHOW_PIXEL_POS_OFFSET_1;
import static org.esa.snap.rcp.pixelinfo.PixelInfoView.PROPERTY_KEY_SHOW_PIXEL_POS_OFFSET_ONE;
import static org.esa.snap.rcp.pixelinfo.PixelInfoView.PROPERTY_KEY_SHOW_GEO_POS_DECIMALS;
import static org.esa.snap.rcp.pixelinfo.PixelInfoView.PROPERTY_KEY_SHOW_PIXEL_POS_DECIMALS;

/**
 * The preferences panel handling geo-location details. Sub-level panel to the "Miscellaneous"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#TXT_PixelViewDisplayController_DisplayName",
        keywords = "#TXT_PixelViewDisplayController_Keyword",
        keywordsCategory = "Pixel Display",
        id = "PixelViewDisplayController",
        position = 2)
@NbBundle.Messages({
        "TXT_PixelViewDisplayController_DisplayName=Geo-Location",
        "TXT_PixelViewDisplayController_Keyword=geo, location, geo-location, compatibility, differ"
})
public final class PixelViewDisplayController extends DefaultConfigController {

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
        Property showGeoPosAsDecimals = context.getPropertySet().getProperty(PROPERTY_KEY_SHOW_GEO_POS_DECIMALS);
        Property showPixelPosAsDecimals = context.getPropertySet().getProperty(PROPERTY_KEY_SHOW_PIXEL_POS_DECIMALS);
        Property showPixelPosOffset1 = context.getPropertySet().getProperty(PROPERTY_KEY_SHOW_PIXEL_POS_OFFSET_ONE);

        JComponent[] showGeoPosAsDecimalsComponents = registry.findPropertyEditor(showGeoPosAsDecimals.getDescriptor()).createComponents(showGeoPosAsDecimals.getDescriptor(), context);
        JComponent[] showPixelPosAsDecimalsComponents = registry.findPropertyEditor(showPixelPosAsDecimals.getDescriptor()).createComponents(showPixelPosAsDecimals.getDescriptor(), context);
        JComponent[] showPixelPosOffset1Components = registry.findPropertyEditor(showPixelPosAsDecimals.getDescriptor()).createComponents(showPixelPosOffset1.getDescriptor(), context);

        TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(Anchor.NORTHWEST);
        tableLayout.setTablePadding(4, 10);
        tableLayout.setTableFill(Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowWeightY(4, 1.0);

        final JPanel pageUI = new JPanel(tableLayout);

        tableLayout.setRowPadding(0, new Insets(10, 80, 10, 4));
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

        @Preference(label = "Show geographical coordinates in decimal degrees", key = PROPERTY_KEY_SHOW_GEO_POS_DECIMALS)
        boolean showGeoPosAsDecimals = PROPERTY_DEFAULT_SHOW_GEO_POS_DECIMALS;

        @Preference(label = "Show pixel coordinates with fractional part", key = PROPERTY_KEY_SHOW_PIXEL_POS_DECIMALS)
        boolean showPixelPosAsDecimals = PROPERTY_DEFAULT_SHOW_PIXEL_POS_DECIMALS;

        @Preference(label = "Show pixel coordinates starting at (1,1)", key = PROPERTY_KEY_SHOW_PIXEL_POS_OFFSET_ONE,
                description = "Show pixel coordinates so that the upper left image corner is (1,1), instead of (0,0).")
        boolean showPixelPosWithOffset1 = PROPERTY_DEFAULT_SHOW_PIXEL_POS_OFFSET_1;
    }

}
