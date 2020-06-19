/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.snap.core.datamodel.quicklooks.QuicklookGenerator;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

/**
 * Panel for quicklook options. Sub-level panel to the General-panel.
 *
 * @author Luis Veci
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_QuicklookOptions",
        keywords = "#Options_Keywords_QuicklookOptions",
        keywordsCategory = "Quicklook",
        id = "QuicklookOptions",
        position = 6)
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_QuicklookOptions=Quicklooks",
        "Options_Keywords_QuicklookOptions=Quicklook"
})
public final class QuicklookOptionsController extends DefaultConfigController {

    protected PropertySet createPropertySet() {
        return createPropertySet(new QuicklookOptionsBean());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("options-quicklook");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class QuicklookOptionsBean {

        @Preference(label = "Save quicklooks with product where possible", config = "snap",
                key = QuicklookGenerator.PREFERENCE_KEY_QUICKLOOKS_SAVE_WITH_PRODUCT)
        boolean saveWithProduct = QuicklookGenerator.DEFAULT_VALUE_QUICKLOOKS_SAVE_WITH_PRODUCT;

        @Preference(label = "Max quicklook width in pixels", config = "snap",
                key = QuicklookGenerator.PREFERENCE_KEY_QUICKLOOKS_MAX_WIDTH)
        int maxWidth = QuicklookGenerator.DEFAULT_VALUE_QUICKLOOKS_MAX_WIDTH;
    }

}
