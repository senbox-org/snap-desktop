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

package org.esa.snap.collocation.visat;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * Geographic collocation action.
 *
 * @author Ralf Quast
 * @author Marco Peters
 */
@ActionID(category = "Processors", id = "org.esa.snap.collocation.visat.CollocationAction")
@ActionRegistration(displayName = "#CTL_CollocationAction_Text", lazy = false)
@ActionReference(path = "Menu/Raster/Geometric Operations", position = 10000)
@NbBundle.Messages({
        "CTL_CollocationAction_Text=Collocation",
        "CTL_CollocationAction_Description=Geographic collocation of two data products."
})
public class CollocationAction extends AbstractSnapAction {

    private ModelessDialog dialog;

    public CollocationAction() {
        putValue(NAME, Bundle.CTL_CollocationAction_Text());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_CollocationAction_Description());
        setHelpId(CollocationDialog.HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new CollocationDialog(getAppContext());
        }
        dialog.show();
    }

}
