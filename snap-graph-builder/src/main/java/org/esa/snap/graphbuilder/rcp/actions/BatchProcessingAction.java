/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.rcp.actions;

import org.esa.snap.graphbuilder.rcp.dialogs.BatchGraphDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Tools",
        id = "BatchProcessingAction"
)
@ActionRegistration(
        displayName = "#CTL_BatchProcessingAction_MenuText",
        popupText = "#CTL_BatchProcessingAction_MenuText",
        iconBase = "org/esa/snap/graphbuilder/icons/batch.png",
        lazy = true
)
@ActionReferences({
        @ActionReference(path = "Menu/Tools", position = 320, separatorAfter = 399),
        @ActionReference(path = "Toolbars/" + PackageDefaults.GPT_BATCH_PROCESSING_TOOLBAR, position = 20)
})
@NbBundle.Messages({
        "CTL_BatchProcessingAction_MenuText=Batch Processing",
        "CTL_BatchProcessingAction_ShortDescription=Batch process several products"
})
public class BatchProcessingAction extends AbstractAction {

    @Override
    public void actionPerformed(final ActionEvent event) {
        final BatchGraphDialog dialog = new BatchGraphDialog(SnapApp.getDefault().getAppContext(),
                "Batch Processing", "batchProcessing", false);
        dialog.show();
    }

}
