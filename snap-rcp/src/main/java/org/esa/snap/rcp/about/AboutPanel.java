/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import org.esa.snap.util.SystemUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;
import org.openide.util.ImageUtilities;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * The UI component displayed by the {@link AboutAction}. Processes {@code AboutBox} file objects generated from
 * {@link AboutBox} annotations.
 *
 * @author Norman Fomferra
 */
class AboutPanel extends JPanel {

    public AboutPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel infoLabel = new JLabel("<html>"
                                              + "<b>SNAP version: </b>" + getVersionString() + "<p>"
                                              + "<b>Home directory: </b>" + SystemUtils.getApplicationHomeDir() + "<br>"
                                              + "<b>User directory: </b>" + SystemUtils.getApplicationDataDir() + "<br>"
                                              + "<b>Cache directory: </b>" + SystemUtils.getCacheDir() + "<br>"
                                              + "<b>JRE: </b>" + System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version") + "<br>"
                                              + "<b>Java VM: </b>" + System.getProperty("java.vm.name") + "<br>"
                                              + "<b>Memory: </b>" + Math.round(Runtime.getRuntime().maxMemory() / 1024. / 1024.) + " MiB<br>"
        );

        add(infoLabel, BorderLayout.SOUTH);

        FileObject configFile = FileUtil.getConfigFile("AboutBox");
        if (configFile != null) {
            JTabbedPane tabbedPane = new JTabbedPane();
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
            add(tabbedPane, BorderLayout.CENTER);
        }


        /*
        final Properties properties = System.getProperties();
        for (String name : properties.stringPropertyNames()) {
            System.out.println(name + " = " + properties.getProperty(name));
        }
        */

    }

    private String getVersionString() {
        String version = null;
        Path versionFile = SystemUtils.getApplicationHomeDir().toPath().resolve("VERSION.txt");
        if (Files.exists(versionFile)) {
            try {
                List<String> versionInfo = Files.readAllLines(versionFile);
                if (!versionInfo.isEmpty()) {
                    version = versionInfo.get(0);
                }
            } catch (IOException e) {
                SystemUtils.LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
        if (version == null) {
            //Collection<ModuleInfo> modules = Lookup.getDefault().lookupAll(ModuleInfo.class);
            ModuleInfo moduleInfo = Modules.getDefault().ownerOf(AboutPanel.class);
            version = moduleInfo.getImplementationVersion() + ", build " + moduleInfo.getBuildVersion();
        }
        return version;
    }
}
