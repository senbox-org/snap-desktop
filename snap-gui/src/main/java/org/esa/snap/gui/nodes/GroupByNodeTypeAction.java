/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.snap.gui.util.BooleanPreferenceKeyAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;


/**
 * @author Norman, Marco
 */
@ActionID(
        category = "View",
        id = "org.esa.snap.gui.nodes.GroupByNodeTypeAction"
)
@ActionRegistration(
        displayName = "#CTL_GroupByNodeTypeActionName",
        lazy = false
)
@ActionReference(
        path = "Context/Product/Product",
        position = 300
)
@NbBundle.Messages({
        "CTL_GroupByNodeTypeActionName=Ungroup Nodes"
})
public class GroupByNodeTypeAction extends BooleanPreferenceKeyAction {

    public static final String PREFERENCE_KEY = "group_by_node_type";

    public GroupByNodeTypeAction() {
        super(PREFERENCE_KEY);
        putValue(NAME, Bundle.CTL_GroupByNodeTypeActionName());
    }
}
