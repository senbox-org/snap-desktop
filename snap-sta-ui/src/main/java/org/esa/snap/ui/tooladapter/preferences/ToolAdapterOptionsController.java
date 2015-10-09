/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.ui.tooladapter.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.NbPreferences;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by kraftek on 10/8/2015.
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_STAOptions",
        keywords = "#Options_Keywords_STAOptions",
        keywordsCategory = "Tool Adapter",
        id = "STA",
        position = 7)
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_STAOptions=Tool Adapter",
        "Options_Keywords_STAOptions=adapter, tool"
})
public class ToolAdapterOptionsController extends DefaultConfigController {

    public static final String PREFERENCE_KEY_TABBED_WINDOW = "sta.use.tabs";
    public static final String PREFERENCE_KEY_VALIDATE_PATHS = "sta.validate.paths";
    public static final String PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING = "sta.warn.no.product";
    public static final String PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT = "sta.display.output";
    public static final boolean DEFAULT_VALUE_TABBED_WINDOW = false;
    public static final boolean DEFAULT_VALUE_VALIDATE_PATHS = true;
    public static final boolean DEFAULT_VALUE_SHOW_EMPTY_PRODUCT_WARINING = true;
    public static final boolean DEFAULT_VALUE_SHOW_EXECUTION_OUTPUT = false;
    private static final String DECISION_SUFFIX = ".decision";
    private BindingContext context;

    @Override
    protected PropertySet createPropertySet() {
        return createPropertySet(new STAOptionsBean());
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        this.context = context;
        TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(4, 10);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowWeightY(4, 1.0);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        Property useTabsControl = context.getPropertySet().getProperty(PREFERENCE_KEY_TABBED_WINDOW);
        Property pathValidationControl = context.getPropertySet().getProperty(PREFERENCE_KEY_VALIDATE_PATHS);
        Property noProductWarningControl = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING);
        Property displayOutputControl = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT);

        JComponent[] useTabsComponents = registry.findPropertyEditor(useTabsControl.getDescriptor()).createComponents(useTabsControl.getDescriptor(), context);
        JComponent[] pathValidationComponents = registry.findPropertyEditor(pathValidationControl.getDescriptor()).createComponents(pathValidationControl.getDescriptor(), context);
        JComponent[] noProductWarningComponents = registry.findPropertyEditor(noProductWarningControl.getDescriptor()).createComponents(noProductWarningControl.getDescriptor(), context);
        JComponent[] displayOutputComponents = registry.findPropertyEditor(displayOutputControl.getDescriptor()).createComponents(displayOutputControl.getDescriptor(), context);

        tableLayout.setRowPadding(0, new Insets(10, 80, 10, 4));
        pageUI.add(useTabsComponents[0]);
        pageUI.add(pathValidationComponents[0]);
        pageUI.add(noProductWarningComponents[0]);
        pageUI.add(displayOutputComponents[0]);
        pageUI.add(tableLayout.createVerticalSpacer());

        return pageUI;
    }

    @Override
    public void update() {
        // module preferences
        Preferences preferences = NbPreferences.forModule(SnapDialogs.class);
        Property property = context.getPropertySet().getProperty(PREFERENCE_KEY_TABBED_WINDOW);
        if (property != null) {
            preferences.put(PREFERENCE_KEY_TABBED_WINDOW, property.getValueAsText());
        }
        property = context.getPropertySet().getProperty(PREFERENCE_KEY_VALIDATE_PATHS);
        if (property != null) {
            preferences.put(PREFERENCE_KEY_VALIDATE_PATHS, property.getValueAsText());
        }
        property = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT);
        if (property != null) {
            preferences.put(PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT, property.getValueAsText());
        }
        // decision preferences
        property = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING);
        if (property != null) {
            if (Boolean.parseBoolean(property.getValueAsText())) {
                preferences.remove(PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING + DECISION_SUFFIX);
            } else {
                preferences.put(PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING + DECISION_SUFFIX, "no");
            }
        }
        try {
            preferences.flush();
        } catch (BackingStoreException ignored) {
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("sta_editor");
    }

    static class STAOptionsBean {
        @Preference(label = "Use tabs to display tool adapter details", key = PREFERENCE_KEY_TABBED_WINDOW)
        boolean useTabs = DEFAULT_VALUE_TABBED_WINDOW;
        @Preference(label = "Validate tool paths and variables on save", key = PREFERENCE_KEY_VALIDATE_PATHS)
        boolean validatePaths = DEFAULT_VALUE_VALIDATE_PATHS;
        @Preference(label = "Display warning when source products are missing", key = PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING)
        boolean warnNoProduct = DEFAULT_VALUE_SHOW_EMPTY_PRODUCT_WARINING;
        @Preference(label = "Display execution output", key = PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT)
        boolean displayOutput = DEFAULT_VALUE_SHOW_EXECUTION_OUTPUT;
    }
}
