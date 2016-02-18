/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.BrowserUtils;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Norman
 */
public class SnapAboutBox extends JPanel {

    private final static String releaseNotesHTTP = "https://github.com/senbox-org/snap-desktop/blob/master/ReleaseNotes.md";
    private JLabel versionText;

    public SnapAboutBox() {
        super(new BorderLayout(4, 4));
        ModuleInfo desktopModuleInfo = Modules.getDefault().ownerOf(SnapAboutBox.class);
        ModuleInfo engineModuleInfo = Modules.getDefault().ownerOf(Product.class);
        ImageIcon image = new ImageIcon(SnapAboutBox.class.getResource("SNAP_Banner.png"));
        JLabel banner = new JLabel(image);
        versionText = new JLabel("<html><b>SNAP " + getReleaseVersion() + "</b>");

        JLabel infoText = new JLabel("<html>"
                                             + "This program is free software: you can redistribute it and/or modify it<br>"
                                             + "under the terms of the <b>GNU General Public License</b> as published by<br>"
                                             + "the Free Software Foundation, either version 3 of the License, or<br>"
                                             + "(at your option) any later version.<br>"
                                             + "<br>"
                                             + "<b>SNAP Desktop implementation version: </b>" + desktopModuleInfo.getImplementationVersion() + "<br>"
                                             + "<b>SNAP Engine implementation version: </b>" + engineModuleInfo.getImplementationVersion() + "<br>"
                /*
                                             + "<b>Home directory: </b>" + SystemUtils.getApplicationHomeDir() + "<br>"
                                             + "<b>User directory: </b>" + SystemUtils.getApplicationDataDir() + "<br>"
                                             + "<b>Cache directory: </b>" + SystemUtils.getCacheDir() + "<br>"
                */
                                             + "<b>JRE: </b>" + System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version") + "<br>"
                                             + "<b>JVM: </b>" + System.getProperty("java.vm.name") + " by " + System.getProperty("java.vendor") + "<br>"
                                             + "<b>Memory: </b>" + Math.round(Runtime.getRuntime().maxMemory() / 1024. / 1024.) + " MiB<br>"
        );

        Font font = versionText.getFont();
        if (font != null) {
            infoText.setFont(font.deriveFont(font.getSize() * 0.9f));
        }

        JPanel innerPanel = new JPanel(new BorderLayout(4, 4));
        innerPanel.add(createVersionPanel(), BorderLayout.NORTH);
        innerPanel.add(infoText, BorderLayout.SOUTH);

        add(banner, BorderLayout.WEST);
        add(innerPanel, BorderLayout.CENTER);
/*
        final Properties properties = System.getProperties();
        for (String name : properties.stringPropertyNames()) {
            System.out.println(name + " = " + properties.getProperty(name));
        }
*/
    }

    private String getReleaseVersion() {
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
        return "[no version info, missing ${SNAP_HOME}/VERSION.txt]";
    }

    private JPanel createVersionPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(versionText);
        final URI releaseNotesURI = getReleaseNotesURI();
        if (releaseNotesURI != null) {
            final JLabel releaseNoteLabel = new JLabel("<html><a href=\"" + releaseNotesURI.toString() + "\">Release Notes</a>");
            releaseNoteLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            releaseNoteLabel.addMouseListener(new BrowserUtils.URLClickAdaptor(releaseNotesHTTP));
            panel.add(releaseNoteLabel);
        }
        return panel;
    }

    private URI getReleaseNotesURI() {
        try {
            return new URI(releaseNotesHTTP);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
