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
import org.esa.snap.core.util.VersionChecker;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

/**
 * Preferences tab for handling the UI behavior preferences. Sub-level panel to the "Miscellaneous"-panel.
 *
 * @author thomas
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#AdvancedOption_DisplayName_Other",
        keywords = "#AdvancedOption_Keywords_Other",
        keywordsCategory = "General, Other",
        id = "Other",
        position = 1000)
@org.openide.util.NbBundle.Messages({
        "AdvancedOption_DisplayName_Other=Other",
        "AdvancedOption_Keywords_Other=other"
})
public final class OtherOptionsController extends DefaultConfigController {

    @Override
    protected PropertySet createPropertySet() {
        return createPropertySet(new OtherBean());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("options-other");
    }

    static class OtherBean {

        @Preference(label = "Release check interval", config = "snap",
                key = VersionChecker.PK_CHECK_INTERVAL)
        VersionChecker.CHECK checkInterval = VersionChecker.CHECK.WEEKLY;
    }

}
