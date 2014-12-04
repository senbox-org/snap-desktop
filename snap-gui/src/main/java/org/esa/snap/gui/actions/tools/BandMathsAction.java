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

package org.esa.snap.gui.actions.tools;

import org.esa.snap.gui.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Properties;

@ActionID(
        category = "Tools",
        id = "org.esa.snap.gui.actions.tools.BandMathsAction"
)
@ActionRegistration(
        displayName = "#CTL_BandMathsActionText",
        popupText = "#CTL_BandMathsActionPopupText",
        iconBase = "org/esa/snap/gui/icons/BandMaths24.gif"
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Tools",
                position = 110
        ),
        @ActionReference(
                path = "Toolbars/Tools",
                position = 100
        ),
        @ActionReference(
                path = "Shortcuts",
                name = "D-M"
        ),
        @ActionReference(
                path = "Context/Product/Band",
                position = 200
        ),
        @ActionReference(
                path = "Context/Product/TPGrid",
                position = 200
        )}
)
@Messages({
        "CTL_BandMathsActionText=Create Band from Math Expression...",
        "CTL_BandMathsActionPopupText=Create Band from Math Expression..."
})
public class BandMathsAction extends AbstractAction {

    public BandMathsAction() {
        putValue(Action.SHORT_DESCRIPTION, "Create a new band using an arbitrary mathematical expression");
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("icons/BAritmethic16.gif", false));
        putValue(Action.LARGE_ICON_KEY, ImageUtilities.loadImageIcon("icons/BAritmethic24.gif", false));

        // TODO process following action properties correctly
        putValue("helpId", "bandArithmetic");
        putValue("context", "image,band,tiePointGrid");
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        SnapApp.getInstance().showInfoDialog("Yes, you can do here some band maths!", null);
    }
}