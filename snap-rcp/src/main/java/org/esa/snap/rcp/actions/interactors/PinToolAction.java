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

package org.esa.snap.rcp.actions.interactors;

import org.esa.snap.rcp.placemark.InsertPinInteractor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

import java.awt.*;

@ActionID(category = "Interactors", id = "org.esa.snap.rcp.action.interactors.PinToolAction")
@ActionRegistration(displayName = "#CTL_PinToolActionText", lazy = false)
@ActionReference(path = "Toolbars/Tools", position = 130)
@Messages({"CTL_PinToolActionText=Pin Tool", "CTL_PinToolActionDescription=Pin placing tool"})
public class PinToolAction extends ToolAction {

    private InsertPinInteractor pinInteractor;
    private PinToolSplitButton pinToolSplitButton;

    @SuppressWarnings("UnusedDeclaration")
    public PinToolAction() {
        this(null);
    }

    public PinToolAction(Lookup lookup) {
        super(lookup);
        putValue(NAME, Bundle.CTL_PinToolActionText());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_PinToolActionDescription());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/PinTool24.gif", false));

        pinInteractor = new InsertPinInteractor();
        setInteractor(pinInteractor);
        pinToolSplitButton = new PinToolSplitButton(this);
        
        // Set initial color on the pin interactor
        pinInteractor.setCurrentColor(pinToolSplitButton.getCurrentColor());
        
        // Add listener for color changes
        pinToolSplitButton.addPropertyChangeListener(PinToolSplitButton.COLOR_PROPERTY, evt -> {
            Color newColor = (Color) evt.getNewValue();
            pinInteractor.setCurrentColor(newColor);
        });
    }

    @Override
    public Component getToolbarPresenter() {
        return pinToolSplitButton;
    }

    @Override
    public HelpCtx getHelpCtx() {
        // TODO: Make sure help page is available for ID
        return new HelpCtx("pinTool");
    }
}