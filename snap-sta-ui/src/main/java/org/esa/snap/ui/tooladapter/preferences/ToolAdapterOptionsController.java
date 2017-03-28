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
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.rcp.util.Dialogs;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.NbPreferences;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Insets;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Options controller for Standalone Tool Adapter.
 *
 * @author Cosmin Cara
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

    public static final String PREFERENCE_KEY_VALIDATE_ON_SAVE = "sta.validate.save";
    public static final String PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING = "sta.warn.no.product";
    public static final String PREFERENCE_KEY_AUTOCOMPLETE = "sta.autocomplete";
    public static final String PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT = "sta.display.output";
    public static final boolean DEFAULT_VALUE_VALIDATE_PATHS = false;
    public static final boolean DEFAULT_VALUE_SHOW_EMPTY_PRODUCT_WARINING = true;
    public static final boolean DEFAULT_VALUE_SHOW_EXECUTION_OUTPUT = true;
    public static final boolean DEFAULT_VALUE_AUTOCOMPLETE = false;
    private static final String DECISION_SUFFIX = ".decision";
    private BindingContext context;
    private Preferences preferences;

    @Override
    protected PropertySet createPropertySet() {
        return createPropertySet(new STAOptionsBean());
    }

    @Override
    protected JPanel createPanel(BindingContext context) {
        this.context = context;
        preferences = NbPreferences.forModule(Dialogs.class);

        TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(4, 10);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowWeightY(4, 1.0);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();

        Property pathValidationControl = context.getPropertySet().getProperty(PREFERENCE_KEY_VALIDATE_ON_SAVE);
        setPropertyValue(pathValidationControl, DEFAULT_VALUE_VALIDATE_PATHS);

        Property noProductWarningControl = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING);
        setPropertyValue(noProductWarningControl, DEFAULT_VALUE_SHOW_EMPTY_PRODUCT_WARINING);

        Property displayOutputControl = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT);
        setPropertyValue(displayOutputControl, DEFAULT_VALUE_SHOW_EXECUTION_OUTPUT);

        Property autocompleteControl = context.getPropertySet().getProperty(PREFERENCE_KEY_AUTOCOMPLETE);
        setPropertyValue(autocompleteControl, DEFAULT_VALUE_AUTOCOMPLETE);

        JComponent[] pathValidationComponents = registry.findPropertyEditor(pathValidationControl.getDescriptor()).createComponents(pathValidationControl.getDescriptor(), context);
        JComponent[] noProductWarningComponents = registry.findPropertyEditor(noProductWarningControl.getDescriptor()).createComponents(noProductWarningControl.getDescriptor(), context);
        JComponent[] displayOutputComponents = registry.findPropertyEditor(displayOutputControl.getDescriptor()).createComponents(displayOutputControl.getDescriptor(), context);
        JComponent[] autocompleteComponents = registry.findPropertyEditor(autocompleteControl.getDescriptor()).createComponents(autocompleteControl.getDescriptor(), context);

        tableLayout.setRowPadding(0, new Insets(10, 80, 10, 4));
        pageUI.add(pathValidationComponents[0]);
        pageUI.add(noProductWarningComponents[0]);
        pageUI.add(displayOutputComponents[0]);
        pageUI.add(autocompleteComponents[0]);
        pageUI.add(tableLayout.createVerticalSpacer());

        return pageUI;
    }

    @Override
    public void update() {
        Property property = context.getPropertySet().getProperty(PREFERENCE_KEY_VALIDATE_ON_SAVE);
        if (property != null) {
            preferences.put(PREFERENCE_KEY_VALIDATE_ON_SAVE, property.getValueAsText());
        }
        property = context.getPropertySet().getProperty(PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT);
        if (property != null) {
            preferences.put(PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT, property.getValueAsText());
        }
        property = context.getPropertySet().getProperty(PREFERENCE_KEY_AUTOCOMPLETE);
        if (property != null) {
            preferences.put(PREFERENCE_KEY_AUTOCOMPLETE, property.getValueAsText());
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

    private boolean getPropertyValue(String key, boolean defaultValue) {
        if (preferences == null) {
            preferences = NbPreferences.forModule(Dialogs.class);
        }
        return preferences.getBoolean(key, defaultValue);
    }

    private void setPropertyValue(Property property, boolean defaultValue) {
        try {
            property.setValue(getPropertyValue(property.getName(), defaultValue));
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }

    static class STAOptionsBean {
        @Preference(label = "Validate tool paths and variables on save", key = PREFERENCE_KEY_VALIDATE_ON_SAVE)
        boolean validatePaths = DEFAULT_VALUE_VALIDATE_PATHS;
        @Preference(label = "Display warning when source products are missing", key = PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING)
        boolean warnNoProduct = DEFAULT_VALUE_SHOW_EMPTY_PRODUCT_WARINING;
        @Preference(label = "Display execution output", key = PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT)
        boolean displayOutput = DEFAULT_VALUE_SHOW_EXECUTION_OUTPUT;
        @Preference(label = "Use autocomplete of parameters for template editing [experimental]", key = PREFERENCE_KEY_AUTOCOMPLETE)
        boolean autocomplete = DEFAULT_VALUE_AUTOCOMPLETE;
    }
}
