/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import com.bc.ceres.core.runtime.Version;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.BrowserUtils;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * @author Norman
 */
public class SnapAboutBox extends JPanel {

    private String releaseNotesUrlString;
    private final JLabel versionText;
    private final ModuleInfo engineModuleInfo;

    public SnapAboutBox() {
        super(new BorderLayout(4, 4));
        ModuleInfo desktopModuleInfo = Modules.getDefault().ownerOf(SnapAboutBox.class);
        engineModuleInfo = Modules.getDefault().ownerOf(Product.class);

        URL resourceUrl = getResourceUrl("snap-branding", "org.esa.snap.rcp.branding", "About_Banner.jpg");
        if (resourceUrl == null) {
            resourceUrl = SnapAboutBox.class.getResource("SNAP_Banner.jpg");
        }
        ImageIcon image = new ImageIcon(resourceUrl);


        releaseNotesUrlString = SystemUtils.getReleaseNotesUrl();


        JLabel banner = new JLabel(image);
        versionText = new JLabel("<html><b>" + SnapApp.getDefault().getInstanceName() + " " + SystemUtils.getReleaseVersion() + "</b>");

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
        innerPanel.setBorder(new EmptyBorder(8, 4, 8, 4));
        innerPanel.add(createVersionPanel(), BorderLayout.NORTH);
        innerPanel.add(infoText, BorderLayout.SOUTH);

        JPanel bannerPanel = new JPanel(new BorderLayout(4, 4));
        bannerPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        bannerPanel.add(banner);
        add(bannerPanel, BorderLayout.WEST);
        add(innerPanel, BorderLayout.CENTER);
/*
        final Properties properties = System.getProperties();
        for (String name : properties.stringPropertyNames()) {
            System.out.println(name + " = " + properties.getProperty(name));
        }
*/
    }


    private JPanel createVersionPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(versionText);

        Version specVersion = Version.parseVersion(engineModuleInfo.getSpecificationVersion().toString());
        String versionString = String.format("%s.%s.%s", specVersion.getMajor(), specVersion.getMinor(), specVersion.getMicro());
        String changelogUrl = releaseNotesUrlString + versionString;

        final JLabel releaseNoteLabel = new JLabel("<html><a href=\"" + changelogUrl + "\">Release Notes</a>");
        releaseNoteLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        releaseNoteLabel.addMouseListener(new BrowserUtils.URLClickAdaptor(changelogUrl));
        panel.add(releaseNoteLabel);
        return panel;
    }


    // This method acts as a convenience wrapper to the method getResourcePath
    public static URL getResourceUrl(String moduleName, String path, String filename) {
        try {
            String resourcePath = getResourcePath(moduleName, path, filename);

            if (resourcePath != null) {
                File resourceFile = new File(resourcePath);
                if (resourceFile != null && resourceFile.toURI() != null) {
                    if (resourceFile.exists()) {
                        return resourceFile.toURI().toURL();
                    }
                }
            }
        } catch (Exception e) {
        }

        return null;
    }

    // This method returns the resource path when there is no java method available for getting the resource
    // (which is the case with the branding module.)  The assumptions are that org.esa.snap is the directory
    // structure at the parent level within the module and that SnapAboutBox.class.getResource("SNAP_Banner.jpg")
    // returns a value which can be used as a reference to determine the parent directory structure of the target resource.
    public static String getResourcePath(String moduleName, String path, String filename) {

        if (moduleName == null || path == null || filename == null) {
            return null;
        }

        // Get a known resource from module "snap-rcp"
        URL knownResourceUrl = SnapAboutBox.class.getResource("SNAP_Banner.jpg");
        if (knownResourceUrl == null) {
            return null;
        }

        String knownResourcePath = knownResourceUrl.getPath();

        String fileSeparator = System.getProperty("file.separator");

        String orgEsaSnap = "org" + fileSeparator + "esa" + fileSeparator + "snap";

        String[] splitStr = knownResourcePath.split(orgEsaSnap);
        if (splitStr.length <= 1) {
            return null;
        }

        String knownResourceParentPath = splitStr[0];
        String targetResourceParentPath = knownResourceParentPath.replace("snap-rcp", moduleName);

        String[] pathArray = path.split(Pattern.quote("."));
        if (pathArray == null || pathArray.length <= 1) {
            return null;
        }

        StringBuilder sb = new StringBuilder(targetResourceParentPath);
        for (String dir : pathArray) {
            sb.append(dir);
            sb.append(fileSeparator);
        }
        sb.append(filename);

        return sb.toString();
    }
}
