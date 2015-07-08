/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.ui.tooladapter.actions;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.tooladapter.dialogs.ToolAdaptersManagementDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * Action for launching the form that manages the existing
 * tool adapters.
 *
 * @author Lucian Barbulescu
 */
@ActionID(category = "Tools", id = "ToolAdapterAction")
@ActionRegistration(displayName = "#CTL_ToolAdapterOperatorAction_Text", lazy = false)
@ActionReference(path = "Menu/Tools", position = 610, separatorBefore = 600)
@NbBundle.Messages({
        "CTL_ToolAdapterOperatorAction_Text=Manage External Tools",
        "CTL_ToolAdapterOperatorAction_Description=Define adapters for external processes.",
        "CTL_ExternalOperatorsEditorDialog_Title=External Tools"
})
public class ManageToolAdaptersAction extends AbstractSnapAction {

    public ManageToolAdaptersAction() {
        putValue(NAME, Bundle.CTL_ToolAdapterOperatorAction_Text());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_ToolAdapterOperatorAction_Description());
    }

    /**
     * Open the external tools selection window
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        ToolAdaptersManagementDialog.showDialog(getAppContext(), event.getActionCommand());
    }

}
