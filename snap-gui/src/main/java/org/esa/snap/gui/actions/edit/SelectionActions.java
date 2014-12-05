package org.esa.snap.gui.actions.edit;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * @author Norman
 */
@NbBundle.Messages({
        "CTL_SelectAllActionName=Select &All",
        "CTL_DeselectAllActionName=&Deselect All"
})
public interface SelectionActions {
    /**
     * @author Norman
     */
    @ActionID(
            category = "Edit",
            id = "org.esa.snap.gui.actions.edit.SelectAllAction"
    )
    @ActionRegistration(
            displayName = "#CTL_SelectAllActionName"
    )
    @ActionReferences({
            @ActionReference(path = "Menu/Edit", position = 10000),
            @ActionReference(path = "Shortcuts", name = "D-A")
    })
    String SELECT_ALL = "select-all";

    @ActionID(
            category = "Edit",
            id = "org.esa.snap.gui.actions.edit.DeselectAllAction"
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
