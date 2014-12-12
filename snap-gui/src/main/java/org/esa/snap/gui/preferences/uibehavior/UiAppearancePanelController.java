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
        "AdvancedOption_DisplayName_UiAppearance=UI Appearance",
        "AdvancedOption_Keywords_UiAppearance=UI"
})
@OptionsPanelController.SubRegistration(location = "SnapPreferences",
        displayName = "#AdvancedOption_DisplayName_UiAppearance",
        keywords = "#AdvancedOption_Keywords_UiAppearance",
        keywordsCategory = "Advanced, UiAppearance",
        position = 1,
        id = "UiAppearance")
public final class UiAppearancePanelController extends DefaultConfigController {

    public static final String PROPERTY_KEY_USER_NAME = "visat.username";
    public static final String PROPERTY_KEY_IS_ADMIN = "visat.isadmin";
    public static final String PROPERTY_KEY_AGE = "visat.age";

    @Override
    protected Object createBean() {
        return new UiAppearanceBean();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("ui-appearance");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class UiAppearanceBean {

        @ConfigProperty(label = "User name", key = PROPERTY_KEY_USER_NAME, validatorClass = NotNullAndNotEmptyValidator.class)
        String userName;

        @ConfigProperty(label = "User is admin", key = PROPERTY_KEY_IS_ADMIN)
        boolean isAdmin = false;

        @ConfigProperty(label = "Age of the user", key = PROPERTY_KEY_AGE, validatorClass = PositiveIntValidator.class)
        int userAge;
    }

}
