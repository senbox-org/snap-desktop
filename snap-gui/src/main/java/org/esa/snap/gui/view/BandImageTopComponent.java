/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.view;

import org.esa.snap.core.Band;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;

import javax.swing.JScrollPane;
import java.awt.BorderLayout;

/**
 * @author Norman
 */
@TopComponent.Description(
        preferredID = "BandImageTopComponent",
        persistenceType = TopComponent.PERSISTENCE_NEVER)
@TopComponent.Registration(
        mode = "editor",
        openAtStartup = false)
@Messages({"CTL_BandImageTopComponent=Band Image",
                  "HINT_BandImageTopComponent=This is a band image editor"})
public class BandImageTopComponent extends TopComponent {

    Band band;

    public BandImageTopComponent() {
        initComponents();
        setName(Bundle.CTL_BandImageTopComponent());
        setToolTipText(Bundle.HINT_BandImageTopComponent());
    }

    public BandImageTopComponent(Band band) {
        super(Lookups.fixed(band));
        this.band = band;
        initComponents();
        setName(band.getName());
        setToolTipText(band.getProduct().getName() + " - " + band.getName());
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(new JScrollPane(new BandImagePanel(band)), BorderLayout.CENTER);
    }

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening       
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

}
