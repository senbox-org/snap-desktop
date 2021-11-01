/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import com.bc.ceres.core.runtime.Version;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.BrowserUtils;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Note: this BrandedAboutBox panel is used instead of the SnapAboutBox by setting the following boolean
// context variable in snap.properties:
// snap.branding=true  (or more specifically [context].branding=true)

// The reason for having a separate file for SnapAboutBox and BrandedAboutBox is that the very nature of About has to
// do with the branding, and it's mostly an information panel.  This makes later merges much simpler as you are not merging
// SnapAboutBox changes with your own branding.

public class BrandedAboutBox extends JPanel {

    public BrandedAboutBox() {
        super(new BorderLayout(4, 4));

        JLabel infoText = new JLabel("<html>"
                + "Branded About Box <br>"
                + "See SnapAboutBox for sample code implementation <br>"
        );

        add(infoText);
    }
}
