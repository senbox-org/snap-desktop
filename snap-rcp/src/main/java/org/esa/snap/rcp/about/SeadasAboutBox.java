/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.BrowserUtils;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * @author Daniel Knowles
 */
public class SeadasAboutBox extends JPanel {

    private static String seadasVersion;

    private String releaseNotesUrl;
    private String releaseNotesUrlName;

    private final static String OCEAN_COLOR_WEB_URL = "https://oceancolor.gsfc.nasa.gov/";
    private final static String OCEAN_COLOR_WEB_URL_NAME = "NASA Ocean Color Web";

    private final static String SEADAS_WEB_URL = "https://seadas.gsfc.nasa.gov/";
    private final static String SEADAS_WEB_URL_NAME = "SeaDAS Web";

    private final ModuleInfo engineModuleInfo;


    public SeadasAboutBox() {

        super(new BorderLayout());


        releaseNotesUrl = SystemUtils.getReleaseNotesUrl();

        ModuleInfo desktopModuleInfo = Modules.getDefault().ownerOf(SnapAboutBox.class);
        engineModuleInfo = Modules.getDefault().ownerOf(Product.class);

        SnapApp app = SnapApp.getDefault();

        // Get SeaDAS version from here ... unless better location is determined
        seadasVersion = SystemUtils.getReleaseVersion();


        releaseNotesUrlName = seadasVersion + " Release Notes";


//
//        // todo Testing
//        System.out.println("******** getApplicationHomepageUrl=" + SystemUtils.getApplicationHomepageUrl());
//        System.out.println("******** getReleaseVersion=" + SystemUtils.getReleaseVersion());
//        System.out.println("******** getApplicationName=" + SystemUtils.getApplicationName());
//        System.out.println("******** getApplicationContextId=" + SystemUtils.getApplicationContextId());
//        System.out.println("******** getApplicationRemoteVersionUrl=" + SystemUtils.getApplicationRemoteVersionUrl());
//        System.out.println("******** getApplicationDataDir=" + SystemUtils.getApplicationDataDir());
//        System.out.println("******** getApplicationHomeDir=" + SystemUtils.getApplicationHomeDir());
//        System.out.println("******** getUserHomeDir=" + SystemUtils.getUserHomeDir());



        URL resourceUrl = SnapAboutBox.getResourceUrl("snap-branding", "org.esa.snap.rcp.branding", "About_Banner.png");
        if (resourceUrl == null) {
            resourceUrl = SnapAboutBox.class.getResource("SNAP_Banner.jpg");
        }
        ImageIcon image = new ImageIcon(resourceUrl);

        JLabel banner = new JLabel(image);


        JLabel infoText = new JLabel("<html>"
                + "SeaDAS is a scientific data visualisation, analysis, and processing software application<br>"
                + "for remote-sensing satellite data.  SeaDAS comprises two components: the <i>SeaDAS Application<br>"
                + "Platform</i> and the <i>SeaDAS-Toolbox</i>.  The <i>SeaDAS Application Platform</i> is a collaborative<br>"
                + "integration with the <i>SNAP Sentinel Application Platform</i>, the primary difference being the <br>"
                + "GUI layout and some minor default behavior.  The <i>SeaDAS-Toolbox</i> contains all the NASA <br>"
                + "Ocean Biology science processing science tools as well as some additional GUI tools <br>"
                + "related to ocean sciences.<br><br>"
                + "</html>"
        );


        JLabel infoText2 = new JLabel("<html><hr>"
                + "This program is free software: you can redistribute it and/or modify it under the terms of<br>"
                + "the <i>GNU General Public License</i> as published by the Free Software Foundation, either<br>"
                + "version 3 of the License, or (at your option) any later version.<br>&nbsp;<br>"
                + "<b>SeaDAS version: </b>" + seadasVersion + "<br>"
                + "<b>SNAP Desktop implementation version: </b>" + desktopModuleInfo.getImplementationVersion() + "* (SeaDAS Modified)<br>"
                + "<b>SNAP Engine implementation version: </b>" + engineModuleInfo.getImplementationVersion() + "* (SeaDAS Modified)<br>"
//                + "<b>SNAP Desktop implementation version: </b>SEADAS-8.2.0 (branded from SNAP 8.0.9)<br>"
                + "<b>SNAP Desktop git repository tag: </b>https://github.com/senbox-org/snap-desktop/releases/tag/SEADAS-" + seadasVersion + "<br>"
//                + "<b>SNAP Desktop implementation version: </b>" + desktopModuleInfo.getImplementationVersion() + "-seadas" + SEADAS_VERSION +"<br>"
//                + "<b>SNAP Engine implementation version: </b>SEADAS-8.2.0 (branded from SNAP 8.0.9)<br>"
                + "<b>SNAP Engine git repository tag: </b>https://github.com/senbox-org/snap-desktop/releases/tag/SEADAS-" + seadasVersion + "<br>"

//                + "<b>SNAP Engine implementation version: </b>" + engineModuleInfo.getImplementationVersion() + "-seadas" + SEADAS_VERSION +"<br>"
                + "<b>Resource directory: </b>" + SystemUtils.getApplicationDataDir() + "<br>"
                + "<b>JRE: </b>" + System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version") + "<br>"
                + "<b>JVM: </b>" + System.getProperty("java.vm.name") + " by " + System.getProperty("java.vendor") + "<br>"
                + "<b>Memory: </b>" + Math.round(Runtime.getRuntime().maxMemory() / 1024. / 1024.) + " MiB"
                + "<br><hr>"
                + "</html>"
        );


        Font font = infoText.getFont();
        if (font != null) {
            infoText2.setFont(font.deriveFont(font.getSize() * 0.9f));
        }


        GridBagConstraints gbc = new GridBagConstraints();
        JPanel jPanel = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets.left = 5;
        gbc.insets.top = 5;

        gbc.gridy = 0;
        infoText.setMinimumSize(infoText.getPreferredSize());
        jPanel.add(infoText, gbc);

        gbc.gridy = 1;
        gbc.insets.left = 15;
        jPanel.add(getUrlJLabel(releaseNotesUrl, releaseNotesUrlName), gbc);

        gbc.gridy = 2;
        jPanel.add(getUrlJLabel(SEADAS_WEB_URL, SEADAS_WEB_URL_NAME), gbc);

        gbc.gridy = 3;
        jPanel.add(getUrlJLabel(OCEAN_COLOR_WEB_URL, OCEAN_COLOR_WEB_URL_NAME), gbc);

        gbc.gridy = 4;
        gbc.insets.left = 5;
        jPanel.add(infoText2, gbc);

        gbc.gridy = 5;
        jPanel.add(banner, gbc);

        gbc.gridy = 6;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        jPanel.add(new JLabel(), gbc);

        add(jPanel, BorderLayout.WEST);

    }


    private JLabel getUrlJLabel(String url, String name) {
        final JLabel jLabel = new JLabel("<html> " +
                "<a href=\"" + url + "\">" + name + "</a></html>");
        jLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jLabel.addMouseListener(new BrowserUtils.URLClickAdaptor(url));
        return jLabel;
    }


}
