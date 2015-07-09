/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import org.esa.snap.rcp.util.BooleanPreferenceKeyAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;


/**
 * @author Norman, Marco
 */
@ActionID(
        category = "View",
        id = "GroupByNodeTypeAction"
)
@ActionRegistration(
        displayName = "#CTL_GroupByNodeTypeActionName",
        lazy = false
)
@ActionReference(
        path = "Context/Product/Product",
        position = 30,separatorAfter = 35,separatorBefore = 25
)
@NbBundle.Messages({
        "CTL_GroupByNodeTypeActionName=Group Nodes by Type"
})
public class GroupByNodeTypeAction extends BooleanPreferenceKeyAction {

    public static final String PREFERENCE_KEY = "group_by_node_type";
    public static final boolean PREFERENCE_DEFAULT_VALUE = true;

    public GroupByNodeTypeAction() {
        super(PREFERENCE_KEY, PREFERENCE_DEFAULT_VALUE);
        putValue(NAME, Bundle.CTL_GroupByNodeTypeActionName());
    }
}
