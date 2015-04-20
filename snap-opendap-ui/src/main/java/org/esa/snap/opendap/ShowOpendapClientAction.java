package org.esa.snap.opendap;

import org.esa.snap.opendap.ui.OpendapAccessPanel;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.JDialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

@ActionID(
        category = "File",
        id = "org.esa.snap.opendap.ShowOpendapClientAction"
)
@ActionRegistration(
        displayName = "#CTL_ShowOpendapClientAction_Name"
)
@ActionReference(path = "Menu/File", position = 55, separatorBefore = 54, separatorAfter = 56)
@NbBundle.Messages({
        "CTL_ShowOpendapClientAction_Name=OPeNDAP Access"
})
public class ShowOpendapClientAction extends AbstractSnapAction {
    public ShowOpendapClientAction() {
        setHelpId("opendap-client");
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final OpendapAccessPanel opendapAccessPanel = new OpendapAccessPanel(getAppContext(), getHelpId());
        final JDialog dialog = new JDialog(getAppContext().getApplicationWindow(), Bundle.CTL_ShowOpendapClientAction_Name());
        dialog.setContentPane(opendapAccessPanel);
        dialog.pack();
        final Dimension size = dialog.getSize();
        dialog.setPreferredSize(size);
        dialog.setVisible(true);
    }
}
