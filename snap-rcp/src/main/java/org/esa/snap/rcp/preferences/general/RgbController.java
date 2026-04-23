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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.ui.RGBImageProfilePane;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.JPanel;

/**
 * The controller for RGB product profile preferences. Sub-level panel to the "Product Profile"-panel.
 *
 * @author thomas
 */
//@org.openide.util.NbBundle.Messages({
//        "Options_DisplayName_RGB=RGB-Image Profiles",
//        "Options_Keywords_RGB=RGB, profile"
//})
//@OptionsPanelController.SubRegistration(
//        location = "GeneralPreferences",
//        displayName = "#Options_DisplayName_RGB",
//        keywords = "#Options_Keywords_RGB",
//        keywordsCategory = "RGB",
//        id = "rgb-image-profiles",
//        position = 4
//)
//public final class RgbController extends DefaultConfigController {
//
//    @Override
//    protected PropertySet createPropertySet() {
//        return new PropertyContainer();
//    }
//
//    @Override
//    protected JPanel createPanel(BindingContext context) {
//        return new RGBImageProfilePane(new DefaultPropertyMap());
//    }
//
//    @Override
//    public HelpCtx getHelpCtx() {
//        return new HelpCtx("options-rgb");
//    }
//}
