/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.action.window;

import com.bc.ceres.core.Assert;
import org.esa.snap.gui.util.WindowUtilities;
import org.esa.snap.gui.window.WorkspaceTopComponent;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.Mode;
import org.openide.windows.WindowManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@ActionID(
        category = "Window",
        id = "org.esa.snap.gui.action.window.NewWorkspaceAction"
)
@ActionRegistration(
        displayName = "#CTL_NewWorkspaceActionName",
        menuText = "#CTL_NewWorkspaceActionMenuText",
        popupText = "#CTL_NewWorkspaceActionMenuText"
)
@ActionReferences({
                          @ActionReference(path = "Menu/Window", position = 20050, separatorAfter = 20075),
                          @ActionReference(path = "Shortcuts", name = "D-W")
                  })
@Messages({
                  "CTL_NewWorkspaceActionName=New Workspace",
                  "CTL_NewWorkspaceActionMenuText=New Workspace...",
                  "LBL_NewWorkspaceActionName=Name:",
                  "VAL_NewWorkspaceActionValue=Workspace"
          })
public final class NewWorkspaceAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        String defaultName = WindowUtilities.getUniqueTitle(Bundle.VAL_NewWorkspaceActionValue(),
                                                            WorkspaceTopComponent.class);
        NotifyDescriptor.InputLine d = new NotifyDescriptor.InputLine(Bundle.LBL_NewWorkspaceActionName(),
                                                                      Bundle.CTL_NewWorkspaceActionName());
        d.setInputText(defaultName);
        Object result = DialogDisplayer.getDefault().notify(d);
        if (NotifyDescriptor.OK_OPTION.equals(result)) {
            WorkspaceTopComponent workspaceTopComponent = new WorkspaceTopComponent(d.getInputText());
            Mode editor = WindowManager.getDefault().findMode("editor");
            Assert.notNull(editor, "editor");
            editor.dockInto(workspaceTopComponent);
            workspaceTopComponent.open();
            workspaceTopComponent.requestActive();
        }
    }
}
