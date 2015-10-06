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

package org.esa.snap.binning.operator.ui;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * Action for starting the GPF binning operator user interface.
 *
 * @author Tonio Fincke
 * @author Thomas Storm
 * @author Marco Peters
 */
@ActionID(category = "Processors", id = "org.esa.snap.binning.operator.ui.BinningOperatorAction")
@ActionRegistration(displayName = "#CTL_BinningOperatorAction_Text", lazy = false)
@ActionReference(path = "Menu/Raster/Geometric Operations")
@NbBundle.Messages({
        "CTL_BinningOperatorAction_Text=Level-3 Binning",
        "CTL_BinningOperatorAction_Description=Spatial and temporal aggregation of input products."
})
public class BinningOperatorAction extends AbstractSnapAction {

    private static final String HELP_ID = "binning_overview";
    private static final String OPERATOR_NAME = "Binning";
    private ModelessDialog dialog;

    public BinningOperatorAction() {
        putValue(NAME, Bundle.CTL_BinningOperatorAction_Text());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_BinningOperatorAction_Description());
        setHelpId(HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new BinningDialog(getAppContext(), OPERATOR_NAME, HELP_ID);
        }
        dialog.show();
    }
}
