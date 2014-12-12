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

import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.ui.RGBImageProfilePane;
import org.esa.beam.util.PropertyMap;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.JPanel;

/**
 * The top-level controller for logging preferences.
 *
 * @author thomas
 */
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_RGB=RGB-Image Profiles",
        "Options_Keywords_RGB=RGB, profile"
})
@OptionsPanelController.TopLevelRegistration(
        categoryName = "#Options_DisplayName_RGB",
        iconBase = "org/esa/snap/gui/icons/RGB32.gif",
        keywords = "#Options_Keywords_RGB",
        keywordsCategory = "RGB",
        id = "rgb-image-profiles")
public final class RgbPanelController extends DefaultConfigController {

    @Override
    protected JPanel createPanel(BindingContext context) {
        return new RGBImageProfilePane(new PropertyMap());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("rgb");
    }
}
