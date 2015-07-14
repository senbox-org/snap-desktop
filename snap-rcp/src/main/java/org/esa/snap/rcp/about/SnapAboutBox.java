/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import org.esa.snap.framework.datamodel.Product;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * @author Norman
 */
@AboutBox(displayName = "SNAP", position = 0)
public class SnapAboutBox extends JPanel {

    public SnapAboutBox() {
        super(new BorderLayout(4, 4));
        ModuleInfo desktopModuleInfo = Modules.getDefault().ownerOf(SnapAboutBox.class);
        ModuleInfo engineModuleInfo = Modules.getDefault().ownerOf(Product.class);
        JLabel banner = new JLabel(new ImageIcon(SnapAboutBox.class.getResource("SNAP_Banner.png")));
        JLabel infoText = new JLabel("<html>" +
                                             "<b>SNAP Desktop version: </b>"
                                             + desktopModuleInfo.getImplementationVersion() + "<br>" +
                                             "<b>SNAP Engine version: </b>"
                                             + engineModuleInfo.getImplementationVersion() + "<br>" +
                                             "");
        add(banner, BorderLayout.WEST);
        add(infoText, BorderLayout.CENTER);
    }
}
