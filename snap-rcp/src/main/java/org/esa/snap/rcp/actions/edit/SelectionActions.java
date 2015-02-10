package org.esa.snap.rcp.actions.edit;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Global selection actions (action keys).
 *
 * @author Norman
 */
@Messages({
        "CTL_SelectAllActionName=Select &All",
        "CTL_DeselectAllActionName=&Deselect All"
})
public interface SelectionActions {

    /**
     * The "select-all" action key.
     */
    @ActionID(
            category = "Edit",
            id = "org.esa.snap.rcp.actions.edit.SelectAllAction"
    )
    @ActionRegistration(
            displayName = "#CTL_SelectAllActionName"
    )
    @ActionReferences({
            @ActionReference(path = "Menu/Edit", position = 10000),
            @ActionReference(path = "Shortcuts", name = "D-A")
    })
    String SELECT_ALL = "select-all";

    /**
     * The "deselect-all" action key.
     */
    @ActionID(
            category = "Edit",
            id = "org.esa.snap.rcp.actions.edit.DeselectAllAction"
    )
    @ActionRegistration(
            displayName = "#CTL_DeselectAllActionName"
    )
    @ActionReferences({
            @ActionReference(path = "Menu/Edit", position = 10001),
            @ActionReference(path = "Shortcuts", name = "D-D")
    })
    String DESELECT_ALL = "deselect-all";
}
