/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import java.awt.BorderLayout;
import java.awt.Image;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.Utilities;

/**
 * The UI component displayed by the {@link AboutAction}. Processes {@code AboutBox} file objects generated from
 * {@link AboutBox} annotations.
 *
 * @author Norman Fomferra
 */
class AboutPanel extends JPanel {

    public AboutPanel() {
        setLayout(new BorderLayout(4,4));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        FileObject configFile = FileUtil.getConfigFile("AboutBox");
        if (configFile != null) {
            JTabbedPane tabbedPane = new JTabbedPane();
            FileObject aboutBoxPanels[] = configFile.getChildren();
            List<FileObject> orderedAboutBoxPanels = FileUtil.getOrder(Arrays.asList(aboutBoxPanels), true);
            for (FileObject aboutBoxFileObject : orderedAboutBoxPanels) {
                JComponent panel = FileUtil.getConfigObject(aboutBoxFileObject.getPath(), JComponent.class);
                String displayName = (String) aboutBoxFileObject.getAttribute("displayName");
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = "About";
                }
                Icon icon = null;
                String iconPath = (String) aboutBoxFileObject.getAttribute("iconPath");
                if (iconPath != null && !iconPath.trim().isEmpty()) {
                    Image image = ImageUtilities.loadImage(iconPath, false);
                    if (image != null) {
                        icon = new ImageIcon(image);
                    }
                }
                tabbedPane.addTab(displayName, icon, panel);
            }
            add(tabbedPane, BorderLayout.CENTER);
        }
        JLabel infoLabel = new JLabel("<html>"
                + "<b>Memory: </b>" + Runtime.getRuntime().totalMemory() + "<br>"
                + "<b>User directory: </b>" + System.getProperty("user.home") + "<br>"
                + "<b>NetBeans RCP: </b>" + System.getProperty("netbeans.productversion") + "<br>"
                + "<b>Java VM: </b>" + System.getProperty("java.vm.name") + "<br>"
                + "<b>JRE: </b>" + System.getProperty("java.runtime.name") + " "+ System.getProperty("java.runtime.version") + "<br>"
        );
        add(infoLabel, BorderLayout.SOUTH);
        
        /*
        final Properties properties = System.getProperties();
        for (String name : properties.stringPropertyNames()) {
            System.out.println(name + " = " + properties.getProperty(name));
        }
        */

    }
}
