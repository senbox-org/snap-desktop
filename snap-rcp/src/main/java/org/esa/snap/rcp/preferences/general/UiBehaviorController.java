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
import org.esa.snap.rcp.pixelinfo.PixelInfoView;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.rcp.preferences.PreferenceUtils;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Insets;

/**
 * Preferences tab for handling the UI behavior preferences. Sub-level panel to the "Miscellaneous"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#AdvancedOption_DisplayName_UiBehavior",
        keywords = "#AdvancedOption_Keywords_UiBehavior",
        keywordsCategory = "Advanced, UiBehavior",
        id = "UiBehavior",
        position = 1)
@org.openide.util.NbBundle.Messages({
        "AdvancedOption_DisplayName_UiBehavior=UI Behaviour",
        "AdvancedOption_Keywords_UiBehavior=UI, behavior"
})
public final class UiBehaviorController extends DefaultConfigController {

    /**
     * Preferences key for automatically showing navigation
     */
    public static final String PREFERENCE_KEY_AUTO_SHOW_NAVIGATION = "autoshownavigation.enabled";
    /**
     * Preferences key for automatically showing new bands
     */
    public static final String PREFERENCE_KEY_AUTO_SHOW_NEW_BANDS = "autoshowbands.enabled";
    /**
     * Preferences key for on-line version check
     */
    public static final String PREFERENCE_KEY_VERSION_CHECK_ENABLED =
            "versionCheck.enabled";
    /**
     * Preferences key for showing a message after writing a GPF-processed product.
     */
    public static final String PREFERENCE_KEY_SAVE_INFO = "saveInfo";
    /**
     * Preferences key for showing a message after opening a GPF-processed product in the application.
     */
    public static final String PREFERENCE_KEY_OPEN_IN_APP_INFO = "openInAppInfo";
    /**
     * Preferences key for showing a message after writing and opening a GPF-processed product.
     */
    public static final String PREFERENCE_KEY_SAVE_AND_OPEN_IN_APP_INFO = "saveAndOpenInAppInfo";

    /**
     * Preferences key to set the maximum number of file in the list to reopen.
     */
    public static final String PREFERENCE_KEY_LIST_FILES_TO_REOPEN = "filesToReopen";

    protected PropertySet createPropertySet() {
        return createPropertySet(new UiBehaviorBean());
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
        Property autoShowNavigation = context.getPropertySet().getProperty(PREFERENCE_KEY_AUTO_SHOW_NAVIGATION);
        Property showNewBands = context.getPropertySet().getProperty(PREFERENCE_KEY_AUTO_SHOW_NEW_BANDS);
        Property showOnlyDisplayed = context.getPropertySet().getProperty(PixelInfoView.PREFERENCE_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES);
        Property checkVersion = context.getPropertySet().getProperty(PREFERENCE_KEY_VERSION_CHECK_ENABLED);
        Property saveInfo = context.getPropertySet().getProperty(PREFERENCE_KEY_SAVE_INFO);
        Property openInApp = context.getPropertySet().getProperty(PREFERENCE_KEY_OPEN_IN_APP_INFO);
        Property saveAndOpenInApp = context.getPropertySet().getProperty(PREFERENCE_KEY_SAVE_AND_OPEN_IN_APP_INFO);
        Property listOfFilesToReopen = context.getPropertySet().getProperty(PREFERENCE_KEY_LIST_FILES_TO_REOPEN);


        JComponent[] autoShowNavigationComponents = registry.findPropertyEditor(autoShowNavigation.getDescriptor()).createComponents(autoShowNavigation.getDescriptor(), context);
        JComponent[] showNewBandsComponents = registry.findPropertyEditor(showNewBands.getDescriptor()).createComponents(showNewBands.getDescriptor(), context);
        JComponent[] showOnlyDisplayedComponents = registry.findPropertyEditor(showOnlyDisplayed.getDescriptor()).createComponents(showOnlyDisplayed.getDescriptor(), context);
        JComponent[] checkVersionComponents = registry.findPropertyEditor(checkVersion.getDescriptor()).createComponents(checkVersion.getDescriptor(), context);
        JComponent[] saveInfoComponents = registry.findPropertyEditor(saveInfo.getDescriptor()).createComponents(saveInfo.getDescriptor(), context);
        JComponent[] openInAppComponents = registry.findPropertyEditor(openInApp.getDescriptor()).createComponents(openInApp.getDescriptor(), context);
        JComponent[] saveAndOpenInAppComponents = registry.findPropertyEditor(saveAndOpenInApp.getDescriptor()).createComponents(saveAndOpenInApp.getDescriptor(), context);

        JComponent[] listOfFilesToReopenComponent = registry.findPropertyEditor(listOfFilesToReopen.getDescriptor()).createComponents(listOfFilesToReopen.getDescriptor(), context);

        pageUI.add(PreferenceUtils.createTitleLabel("Display Settings"));
        pageUI.add(autoShowNavigationComponents[0]);
        pageUI.add(showNewBandsComponents[0]);
        pageUI.add(showOnlyDisplayedComponents[0]);


        // Adding the number of file that can be reopenn
        TableLayout layout = new TableLayout(2);
        JPanel panel = new JPanel(layout);
        layout.setTablePadding(new Insets(1, 10, 0, 0));
        panel.add(listOfFilesToReopenComponent[1]);
        panel.add(listOfFilesToReopenComponent[0]);
        tableLayout.setTableFill(TableLayout.Fill.VERTICAL);
        pageUI.add(panel);

        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.EAST);
        pageUI.add(tableLayout.createHorizontalSpacer());
        tableLayout.setColumnCount(1);
        pageUI.add(PreferenceUtils.createTitleLabel("Message Settings"));
        pageUI.add(checkVersionComponents[0]);
        pageUI.add(saveInfoComponents[0]);
        pageUI.add(openInAppComponents[0]);
        pageUI.add(saveAndOpenInAppComponents[0]);
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

        @Preference(label = "Show navigation window when image views are opened",
                key = PREFERENCE_KEY_AUTO_SHOW_NAVIGATION)
        boolean autoShowNavigation = true;

        @Preference(label = "Open image view for new (virtual) bands",
                key = PREFERENCE_KEY_AUTO_SHOW_NEW_BANDS)
        boolean autoShowNewBands = true;

        @Preference(label = "Show only pixel values of loaded or displayed bands",
                key = PixelInfoView.PREFERENCE_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES)
        boolean showOnlyLoadedOrDisplayedBandPixels = PixelInfoView.PREFERENCE_DEFAULT_SHOW_DISPLAYED_BAND_PIXEL_VALUES;

        @Preference(label = "Check for new version on startup",
                key = PREFERENCE_KEY_VERSION_CHECK_ENABLED)
        boolean checkEnabled = true;

        @Preference(label = "Show target product writing success and duration information",
                key = PREFERENCE_KEY_SAVE_INFO)
        boolean saveInfo = true;

        @Preference(label = "Show target product opening information",
                key = PREFERENCE_KEY_OPEN_IN_APP_INFO)
        boolean openInAppInfo = true;

        @Preference(label = "Show target product writing and opening information",
                key = PREFERENCE_KEY_SAVE_AND_OPEN_IN_APP_INFO)
        boolean saveAndOpenInAppInfo = true;


        @Preference(label = "Maximum recent file list",
                key = PREFERENCE_KEY_LIST_FILES_TO_REOPEN, interval = "[1,20]")
        int fileReopen = 10;
    }

}
