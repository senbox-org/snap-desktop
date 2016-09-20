/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.ui.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.core.gpf.GPF;
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
 * @author muhammad.bc.
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#AdvancedOption_DisplayName_GPF_Ui",
        keywords = "#AdvancedOption_Keyboard_GPF_Ui",
        keywordsCategory = "GPF",
        id = "GPF",
        position = 10)
@org.openide.util.NbBundle.Messages({
        "AdvancedOption_DisplayName_GPF_Ui=GPF Settings",
        "AdvancedOption_Keyboard_GPF_Ui=GPF,sound beep"
})
public class GPFController extends DefaultConfigController {

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("gpf.option.controller");
    }

    static class GPFBean {
        @Preference(label = "Beep a sound after completion of process",
                key = GPF.GPF_BEEP_AFTER_PROCESSING)
        boolean beepSound = false;

    }

    protected PropertySet createPropertySet() {
        return createPropertySet(new GPFBean());
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
        Property beepSound = context.getPropertySet().getProperty("snap.gpf.beepAfterProcessing");
        JComponent[] beepSoundComponent = registry.findPropertyEditor(beepSound.getDescriptor()).createComponents(beepSound.getDescriptor(), context);

        pageUI.add(PreferenceUtils.createTitleLabel("Sound Beep"));
        pageUI.add(beepSoundComponent[0]);
        tableLayout.setTableFill(TableLayout.Fill.VERTICAL);
        pageUI.add(tableLayout.createVerticalSpacer());
        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(100), BorderLayout.EAST);
        return parent;
    }
}
