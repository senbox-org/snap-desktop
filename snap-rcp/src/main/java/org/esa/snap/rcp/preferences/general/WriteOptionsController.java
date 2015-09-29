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

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Panel for write options. Sub-level panel to the "Miscellaneous"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_WriteOptions",
        keywords = "#Options_Keywords_WriteOptions",
        keywordsCategory = "Write Options",
        id = "WriteOptions",
        position = 5)
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_WriteOptions=Write Options",
        "Options_Keywords_WriteOptions=write, writing, save, header, MPH, SPH, history, annotation, incremental"
})
public final class WriteOptionsController extends DefaultConfigController {

    /**
     * Preferences key for save product headers (MPH, SPH) or not
     */
    public static final String PREFERENCE_KEY_SAVE_PRODUCT_HEADERS = "save.product.headers";
    /**
     * Preferences key for save product history or not
     */
    public static final String PREFERENCE_KEY_SAVE_PRODUCT_HISTORY = "save.product.history";
    /**
     * Preferences key for save product annotations (ADS) or not
     */
    public static final String PREFERENCE_KEY_SAVE_PRODUCT_ANNOTATIONS = "save.product.annotations";
    /**
     * Preferences key for incremental mode at save
     */
    public static final String PREFERENCE_KEY_SAVE_INCREMENTAL = "save.incremental";

    /**
     * default value for preference save product headers (MPH, SPH) or not
     */
    public static final boolean DEFAULT_VALUE_SAVE_PRODUCT_HEADERS = true;
    /**
     * default value for preference save product history (History) or not
     */
    public static final boolean DEFAULT_VALUE_SAVE_PRODUCT_HISTORY = true;
    /**
     * default value for preference save product annotations (ADS) or not
     */
    public static final boolean DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS = false;
    /**
     * default value for preference incremental mode at save
     */
    public static final boolean DEFAULT_VALUE_SAVE_INCREMENTAL = true;

    protected PropertySet createPropertySet() {
        return createPropertySet(new WriteOptionsBean());
    }

    @Override
    protected void configure(BindingContext context) {
        Enablement enablement = context.bindEnabledState(PREFERENCE_KEY_SAVE_PRODUCT_ANNOTATIONS, false, new Enablement.Condition() {
            @Override
            public boolean evaluate(BindingContext bindingContext) {
                return !((Boolean) bindingContext.getPropertySet().getProperty(PREFERENCE_KEY_SAVE_PRODUCT_HEADERS).getValue());
            }
        });
        context.getPropertySet().getProperty(PREFERENCE_KEY_SAVE_PRODUCT_HEADERS).addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                enablement.apply();
                if (!((Boolean) evt.getNewValue())) {
                    try {
                        context.getPropertySet().getProperty(PREFERENCE_KEY_SAVE_PRODUCT_ANNOTATIONS).setValue(false);
                    } catch (ValidationException e) {
                        e.printStackTrace(); // very basic exception handling because exception is not expected to be thrown
                    }
                }
            }
        });
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("write-options");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class WriteOptionsBean {

        @Preference(label = "Save product header (MPH, SPH, Global_Attributes)", key = PREFERENCE_KEY_SAVE_PRODUCT_HEADERS)
        boolean saveProductHeaders = DEFAULT_VALUE_SAVE_PRODUCT_HEADERS;

        @Preference(label = "Save product history (History)", key = PREFERENCE_KEY_SAVE_PRODUCT_HISTORY)
        boolean saveProductHistory = DEFAULT_VALUE_SAVE_PRODUCT_HISTORY;

        @Preference(label = "Save product annotation datasets (ADS)", key = PREFERENCE_KEY_SAVE_PRODUCT_ANNOTATIONS)
        boolean saveProductAds = DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS;

        @Preference(label = "Use incremental save (only save modified items)", key = PREFERENCE_KEY_SAVE_INCREMENTAL)
        boolean saveIncremental = DEFAULT_VALUE_SAVE_INCREMENTAL;
    }

}
