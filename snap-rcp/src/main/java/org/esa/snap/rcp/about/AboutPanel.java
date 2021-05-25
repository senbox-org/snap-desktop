/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Image;
import java.util.Arrays;
import java.util.List;

/**
 * The UI component displayed by the {@link AboutAction}. Processes {@code AboutBox} file objects generated from
 * {@link AboutBox} annotations.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 */
class AboutPanel extends JPanel {

    public AboutPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        FileObject configFile = FileUtil.getConfigFile("AboutBox");
        if (configFile != null) {
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.add("SeaDAS", new SeadasAboutBox());
            addAboutBoxPlugins(tabbedPane, configFile);
            tabbedPane.add("Licenses", new LicensesAboutBox());
            add(tabbedPane, BorderLayout.CENTER);
        } else {
            add(new SnapAboutBox(), BorderLayout.CENTER);
        }
    }

    private void addAboutBoxPlugins(JTabbedPane tabbedPane, FileObject configFile) {
        FileObject aboutBoxPanels[] = configFile.getChildren();
        List<FileObject> orderedAboutBoxPanels = FileUtil.getOrder(Arrays.asList(aboutBoxPanels), true);
        for (FileObject aboutBoxFileObject : orderedAboutBoxPanels) {
            JComponent panel = FileUtil.getConfigObject(aboutBoxFileObject.getPath(), JComponent.class);
            if (panel != null) {
                String displayName = (String) aboutBoxFileObject.getAttribute("displayName");
                if (displayName != null && !displayName.trim().isEmpty()) {
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
            }
        }
    }
}
