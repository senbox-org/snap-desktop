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

import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.tooladapter.dialogs.ToolAdapterExecutionDialog;

import java.awt.event.ActionEvent;

/**
 * Action to be performed when a toll adapter menu entry is invoked.
 *
 * @author Cosmin Cara
 */
public class ExecuteToolAdapterAction extends AbstractSnapAction {

    public ExecuteToolAdapterAction() {
        super();
    }

    public ExecuteToolAdapterAction(String label) {
        putValue(NAME, label);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ToolAdapterOperatorDescriptor operatorDescriptor = ToolAdapterActionRegistrar.getActionMap().get(getValue(NAME));
        if (operatorDescriptor != null) {
            final ToolAdapterExecutionDialog operatorDialog = new ToolAdapterExecutionDialog(operatorDescriptor, getAppContext(), operatorDescriptor.getLabel());
            operatorDialog.show();
        }
    }
}
