/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.gui.preferences;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

import javax.swing.JComponent;
import java.beans.PropertyChangeListener;

/**
 * @author Thomas Storm
 */
//@OptionsPanelController.TopLevelRegistration(
//        categoryName = "#OptionsCategory_Name_Miau",
//        iconBase = "org/esa/snap/gui/icons/RsBandAsSwath24.gif",
//        keywords = "#OptionsCategory_Keywords_Miau",
//        keywordsCategory = "Miau"
//)
//@org.openide.util.NbBundle.Messages({
//                                            "OptionsCategory_Name_Miau=SeNtinel Application Platform",
//                                            "OptionsCategory_Keywords_Miau=sentinel, application, platform"
//                                    })
public final class SnapOptionsPanelController extends OptionsPanelController {

    @Override
    public void update() {
        System.out.println("SnapOptionsPanelController.update");
    }

    @Override
    public void applyChanges() {
        System.out.println("SnapOptionsPanelController.applyChanges");
    }

    @Override
    public void cancel() {
        System.out.println("SnapOptionsPanelController.cancel");
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isChanged() {
        return false;
    }

    @Override
    public JComponent getComponent(Lookup lookup) {
        System.out.println("SnapOptionsPanelController.getComponent");
        return new SnapOptionsPanel(this);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {

    }
}
