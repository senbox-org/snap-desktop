package org.esa.beam.opendap;

import org.esa.beam.opendap.ui.OpendapAccessPanel;
import org.esa.snap.gui.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/*
    <extension point="snap-ui:actions">
        <action>
            <id>showOpendapClientAction</id>
            <parent>file</parent>
            <class>org.esa.beam.opendap.ShowOpendapClientAction</class>
            <text>OPeNDAP Access</text>
            <smallIcon>icons/RsProduct16.gif</smallIcon>
            <largeIcon>icons/RsProduct24.gif</largeIcon>
            <shortDescr>Download products from OPeNDAP servers</shortDescr>
            <helpId>opendap-client</helpId>
            <placeAfter>reopen</placeAfter>
            <placeBefore>productGrabber</placeBefore>
        </action>
    </extension>
 */

@ActionID(
        category = "File",
        id = "org.esa.beam.opendap.ShowOpendapClientAction"
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
        putValue("ShortDescription", "Download products from OPeNDAP servers.");
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final OpendapAccessPanel opendapAccessPanel = new OpendapAccessPanel(getAppContext(), getHelpId());
        final JDialog dialog = new JDialog(getAppContext().getApplicationWindow(), Bundle.CTL_CloseAllProductsActionName());
        dialog.setContentPane(opendapAccessPanel);
        dialog.pack();
        final Dimension size = dialog.getSize();
        dialog.setPreferredSize(size);
        dialog.setVisible(true);
    }
}
