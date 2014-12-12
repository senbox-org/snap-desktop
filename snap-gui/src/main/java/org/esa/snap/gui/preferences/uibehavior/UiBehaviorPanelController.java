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

package org.esa.snap.gui.preferences.uibehavior;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

/**
 * TODO fill out or delete
 *
 * @author thomas
 */
@org.openide.util.NbBundle.Messages({
        "AdvancedOption_DisplayName_UiBehavior=UI Behavior",
        "AdvancedOption_Keywords_UiBehavior=UIb"
})
@OptionsPanelController.SubRegistration(location = "SnapPreferences",
        displayName = "#AdvancedOption_DisplayName_UiBehavior",
        keywords = "#AdvancedOption_Keywords_UiBehavior",
        keywordsCategory = "Advanced, UiBehavior",
        position = 2,
        id = "UiBehavior")
public final class UiBehaviorPanelController extends DefaultConfigController {

    /**
     * Preferences key for automatically showing navigation
     */
    public static final String PROPERTY_KEY_AUTO_SHOW_NAVIGATION = "visat.autoshownavigation.enabled";

    protected Object createBean() {
        return new UiBehaviorBean();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("ui-behavior");
    }

    static class UiBehaviorBean {

        @ConfigProperty(label = "Show navigation window when image views are opened", key = PROPERTY_KEY_AUTO_SHOW_NAVIGATION)
        boolean autoShowNavigation;
    }

}
