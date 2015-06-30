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

package org.esa.snap.rcp.reproject;

import org.esa.snap.framework.ui.ModelessDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * Geographic collocation action.
 *
 * @author Norman Fomferra
 */
@ActionID(category = "Operators", id = "org.esa.snap.rcp.reproject.OrthorectificationAction")
@ActionRegistration(displayName = "#CTL_OrthorectificationAction_Name")
@ActionReference(path = "Menu/Raster/Geometric Operations")
@NbBundle.Messages("CTL_OrthorectificationAction_Name=Orthorectification")
public class OrthorectificationAction extends AbstractSnapAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new ReprojectionDialog(true, Bundle.CTL_OrthorectificationAction_Name(),
                                            "orthorectificationAction", getAppContext());
        }
        dialog.show();
    }

}
