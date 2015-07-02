/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.unmixing.ui;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * Invokes the Spectral Unmixing UI.
 *
 * @author Norman Fomferra
 */
@ActionID(category = "Processors", id = "org.esa.snap.unmixing.ui.SpectralUnmixingAction")
@ActionRegistration(displayName = "#CTL_SpectralUnmixingAction_Text")
@ActionReference(path = "Menu/Optical", position = 10)
@NbBundle.Messages({ "CTL_SpectralUnmixingAction_Text=Spectral Unmixing" })
public class SpectralUnmixingAction extends AbstractSnapAction {
    private SpectralUnmixingDialog dialog;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new SpectralUnmixingDialog(getAppContext());
        }
        dialog.show();
    }
}
