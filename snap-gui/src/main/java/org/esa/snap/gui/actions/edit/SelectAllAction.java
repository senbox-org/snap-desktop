/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.edit;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallbackSystemAction;

/**
 * @author Norman
 */
@ActionID(
        category = "Edit",
        id = "org.esa.snap.gui.actions.edit.SelectAllAction"
)
@ActionRegistration(
        displayName = "#CTL_SelectAllActionName",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/Edit", position = 10000),
    @ActionReference(path = "Shortcuts", name = "D-A")
})
@NbBundle.Messages({
    "CTL_SelectAllActionName=Select &All"
})
public final class SelectAllAction extends CallbackSystemAction {

    public SelectAllAction() {
    }

    protected void initialize() {
        super.initialize();
    }

    public Object getActionMapKey() {
        return "select-all";
    }

    public String getName() {
        return Bundle.CTL_SelectAllActionName();
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx("select-all");
    }

    protected String iconResource() {
        return null;
    }

    protected boolean asynchronous() {
        return false;
    }
}
