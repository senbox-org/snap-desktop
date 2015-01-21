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

package org.esa.snap.gui.preferences.general;

import com.bc.ceres.binding.Property;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.beam.framework.ui.PixelInfoView;
import org.esa.beam.framework.ui.SuppressibleOptionPane;
import org.esa.snap.gui.preferences.ConfigProperty;
import org.esa.snap.gui.preferences.DefaultConfigController;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Insets;

import static org.esa.snap.gui.preferences.PreferenceUtils.*;

/**
 * Preferences tab for handling the UI behavior preferences. Sub-level panel to the "Miscellaneous"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "Appearance",
        displayName = "#AdvancedOption_DisplayName_UiBehavior",
        keywords = "#AdvancedOption_Keywords_UiBehavior",
        keywordsCategory = "Advanced, UiBehavior",
        id = "UiBehavior")
@org.openide.util.NbBundle.Messages({
        "AdvancedOption_DisplayName_UiBehavior=UI Behavior",
        "AdvancedOption_Keywords_UiBehavior=UI, behavior"
})
public final class UiBehaviorPanelController extends DefaultConfigController {

    /**
     * Preferences key for automatically showing navigation
     */
    public static final String PROPERTY_KEY_AUTO_SHOW_NAVIGATION = "visat.autoshownavigation.enabled";
    /**
     * Preferences key for automatically showing new bands
     */
    public static final String PROPERTY_KEY_AUTO_SHOW_NEW_BANDS = "visat.autoshowbands.enabled";
    /**
     * Preferences key for on-line version check
     */
    public static final String PROPERTY_KEY_VERSION_CHECK_ENABLED =
            "visat.versionCheck" + SuppressibleOptionPane.KEY_PREFIX_ENABLED;
    /**
     * Preferences key for unsuppressing tips and messages
     */
    public static final String PROPERTY_KEY_UNSUPPRESS = "unsuppress";

    protected Object createBean() {
        return new UiBehaviorBean();
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(0, 1.0);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property autoShowNavigation = context.getPropertySet().getProperty(PROPERTY_KEY_AUTO_SHOW_NAVIGATION);
        Property showNewBands = context.getPropertySet().getProperty(PROPERTY_KEY_AUTO_SHOW_NEW_BANDS);
        Property showOnlyDisplayed = context.getPropertySet().getProperty(PixelInfoView.PROPERTY_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES);
        Property checkVersion = context.getPropertySet().getProperty(PROPERTY_KEY_VERSION_CHECK_ENABLED);
        Property unsuppress = context.getPropertySet().getProperty(PROPERTY_KEY_UNSUPPRESS);

        JComponent[] autoShowNavigationComponents = registry.findPropertyEditor(autoShowNavigation.getDescriptor()).createComponents(autoShowNavigation.getDescriptor(), context);
        JComponent[] showNewBandsComponents = registry.findPropertyEditor(showNewBands.getDescriptor()).createComponents(showNewBands.getDescriptor(), context);
        JComponent[] showOnlyDisplayedComponents = registry.findPropertyEditor(showOnlyDisplayed.getDescriptor()).createComponents(showOnlyDisplayed.getDescriptor(), context);
        JComponent[] checkVersionComponents = registry.findPropertyEditor(checkVersion.getDescriptor()).createComponents(checkVersion.getDescriptor(), context);
        JComponent[] unsuppressComponents = registry.findPropertyEditor(unsuppress.getDescriptor()).createComponents(unsuppress.getDescriptor(), context);

        pageUI.add(createTitleLabel("Display Settings"));
        pageUI.add(autoShowNavigationComponents[0]);
        pageUI.add(showNewBandsComponents[0]);
        pageUI.add(showOnlyDisplayedComponents[0]);
        pageUI.add(tableLayout.createHorizontalSpacer());
        pageUI.add(createTitleLabel("Message Settings"));
        pageUI.add(checkVersionComponents[0]);
        pageUI.add(unsuppressComponents[0]);
        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(100), BorderLayout.EAST);
        return parent;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("ui-behavior");
    }

    static class UiBehaviorBean {

        @ConfigProperty(label = "Show navigation window when image views are opened",
                key = PROPERTY_KEY_AUTO_SHOW_NAVIGATION)
        boolean autoShowNavigation = true;

        @ConfigProperty(label = "Open image view for new (virtual) bands",
                key = PROPERTY_KEY_AUTO_SHOW_NEW_BANDS)
        boolean autoShowNewBands = true;

        @ConfigProperty(label = "Show only pixel values of loaded or displayed bands",
                key = PixelInfoView.PROPERTY_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES)
        boolean showOnlyLoadedOrDisplayedBandPixels = PixelInfoView.PROPERTY_DEFAULT_SHOW_DISPLAYED_BAND_PIXEL_VALUES;

        @ConfigProperty(label = "Check for new version on startup",
                key = PROPERTY_KEY_VERSION_CHECK_ENABLED)
        boolean checkEnabled = true;

        @ConfigProperty(label = "Show all suppressed tips and messages again",
                key = PROPERTY_KEY_UNSUPPRESS)
        boolean unsuppress = false;
    }

}
