package org.esa.snap.rcp.ctxhelp;

import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Provides context-sensitive web search.
 *
 * @author Norman Fomferra
 */
@ActionID(
        category = "Help",
        id = "org.esa.snap.rcp.ctxhelp.ContextSearchAction"
)
@ActionRegistration(
        displayName = "#CTL_ContextSearchAction_Name",
        lazy = true
)
@ActionReferences({
        @ActionReference(path = "Menu/Help", position = 210),
        @ActionReference(path = "Shortcuts", name = "D-F1")
})
@NbBundle.Messages({
        "CTL_ContextSearchAction_Name=Context Web Search",
        "CTL_ContextSearchAction_ToolTip=Perform a contextual web search on the selected product element."
})
public class ContextWebSearchAction extends AbstractAction implements HelpCtx.Provider {

    public ContextWebSearchAction() {
        super(Bundle.CTL_ContextSearchAction_Name());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ContextWebSearch contextWebSearch = ContextWebSearch.getDefault();
        if (contextWebSearch != null) {
            contextWebSearch.searchForNode(SnapApp.getDefault().getSelectedProductNode());
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("contextWebSearch");
    }
}
