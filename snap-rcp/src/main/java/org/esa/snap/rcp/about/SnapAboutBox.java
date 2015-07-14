/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.util.SystemUtils;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Norman
 */
@AboutBox(displayName = "SNAP", position = 0)
public class SnapAboutBox extends JPanel {

    public SnapAboutBox() {
        super(new BorderLayout(4, 4));
        ModuleInfo desktopModuleInfo = Modules.getDefault().ownerOf(SnapAboutBox.class);
        ModuleInfo engineModuleInfo = Modules.getDefault().ownerOf(Product.class);
        ImageIcon image = new ImageIcon(SnapAboutBox.class.getResource("SNAP_Banner.png"));
        JLabel banner = new JLabel(image);
        JLabel infoText = new JLabel("<html>"
                                             + "<b>ESA SNAP </b>" + getVersionString() + "<br><br>"
                                             + "This program is free software: you can redistribute it and/or modify\n"
                                            + "    it under the terms of the GNU General Public License as published by\n"
                                            + "    the Free Software Foundation, either version 3 of the License, or\n"
                                            + "    (at your option) any later version.<br><br>"
                + "<br>"
                                             + "<b>SNAP Desktop version: </b>" + desktopModuleInfo.getImplementationVersion() + "<br>"
                                             + "<b>SNAP Engine version: </b>" + engineModuleInfo.getImplementationVersion() + "<br>"
                                              + "<b>Home directory: </b>" + SystemUtils.getApplicationHomeDir() + "<br>"
                                              + "<b>User directory: </b>" + SystemUtils.getApplicationDataDir() + "<br>"
                                              + "<b>Cache directory: </b>" + SystemUtils.getCacheDir() + "<br>"
                                              + "<b>JRE: </b>" + System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version") + "<br>"
                                              + "<b>Java VM: </b>" + System.getProperty("java.vm.name") + "<br>"
                                              + "<b>Memory: </b>" + Math.round(Runtime.getRuntime().maxMemory() / 1024. / 1024.) + " MiB<br>"
        );

        add(banner, BorderLayout.WEST);
        add(infoText, BorderLayout.CENTER);
        setPreferredSize(new Dimension(image.getIconWidth() + 200, image.getIconHeight()));

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
        if (version != null) {
            return version;
        }
        return "<i>not available</i>";
    }
}
