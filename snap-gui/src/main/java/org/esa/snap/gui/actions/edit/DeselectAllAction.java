/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.edit;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallbackSystemAction;

/**
 * @author Norman
 */
@ActionID(
        category = "Edit",
        id = "org.esa.snap.gui.actions.edit.DeselectAllAction"
)
@ActionRegistration(
        displayName = "#CTL_DeselectAllActionName"
        //,key = "D-D"
)
@ActionReference(
        path = "Menu/Edit",
        position = 10001
)
@NbBundle.Messages({
        "CTL_DeselectAllActionName=&Deselect All"
})
public final class DeselectAllAction extends CallbackSystemAction {
    public DeselectAllAction() {
    }

    protected void initialize() {
        super.initialize();
    }

    public Object getActionMapKey() {
        return "deselect-all";
    }

    public String getName() {
        return Bundle.CTL_DeselectAllActionName();
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx("deselect-all");
    }

    protected String iconResource() {
        return null;
    }

    protected boolean asynchronous() {
        return false;
    }
}
