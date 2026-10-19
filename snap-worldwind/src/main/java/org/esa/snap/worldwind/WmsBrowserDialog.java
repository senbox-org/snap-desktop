/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.worldwind;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwindx.examples.WMSLayersPanel;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.worldwind.preferences.WorldWindPreferences;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Browses the layers of WMS servers and adds the checked ones to a globe. The servers come from the WorldWind
 * preferences; a server typed into the dialog is added to them.
 */
class WmsBrowserDialog extends JDialog {

    private static final Dimension PANEL_SIZE = new Dimension(450, 500);

    private final WorldWindow wwd;
    private final Consumer<Layer> layerAdded;
    private final JComboBox<String> serverCombo;
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<String, SnapWMSLayersPanel> panels = new HashMap<>();

    WmsBrowserDialog(final Window owner, final WorldWindow wwd, final Consumer<Layer> layerAdded) {
        super(owner, "Add WMS Layer", ModalityType.MODELESS);
        this.wwd = wwd;
        this.layerAdded = layerAdded;

        serverCombo = new JComboBox<>(WorldWindPreferences.getWmsServers().toArray(new String[0]));
        serverCombo.setEditable(true);
        serverCombo.setPrototypeDisplayValue("https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi");
        final JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());

        final JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        serverPanel.add(new JLabel("Server:"));
        serverPanel.add(serverCombo);
        serverPanel.add(connectButton);

        final JPanel content = new JPanel(new BorderLayout(4, 4));
        content.add(serverPanel, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }

    private void connect() {
        final Object selected = serverCombo.getSelectedItem();
        final String server = selected != null ? selected.toString().trim() : "";
        if (server.isEmpty()) {
            return;
        }
        final SnapWMSLayersPanel existing = panels.get(server);
        if (existing != null) {
            tabs.setSelectedComponent(existing);
            return;
        }
        final SnapWMSLayersPanel panel;
        try {
            // the capabilities are loaded in the panel's own thread
            panel = new SnapWMSLayersPanel(wwd, server, PANEL_SIZE, layerAdded);
        } catch (URISyntaxException e) {
            Dialogs.showError("Add WMS Layer", "Invalid server address: " + server);
            return;
        }
        panels.put(server, panel);
        tabs.addTab(panel.getServerDisplayString(), panel);
        tabs.setSelectedComponent(panel);
        pack();
        panel.whenLoaded(() -> loaded(server, panel));
    }

    /**
     * WorldWind's panel only prints a failure to the console and keeps showing its progress bar; report it, and
     * remember the server only once it has answered with layers.
     */
    private void loaded(final String server, final SnapWMSLayersPanel panel) {
        if (!panel.hasLayers()) {
            tabs.remove(panel);
            panels.remove(server);
            pack();
            Dialogs.showError("Add WMS Layer", "No layers from " + server + ".\n" +
                                               "The server cannot be reached or is not a WMS server.");
            return;
        }
        final List<String> servers = new ArrayList<>(WorldWindPreferences.getWmsServers());
        if (!servers.contains(server)) {
            servers.add(server);
            WorldWindPreferences.setWmsServers(servers);
            serverCombo.addItem(server);
        }
    }

    /**
     * WorldWind's WMS layer panel, adding its layers below SNAP's overlays: the original inserts them before the
     * place name layer, which SNAP's globes do not have, and so at the bottom of the layer list under the base maps.
     */
    static class SnapWMSLayersPanel extends WMSLayersPanel {

        private final transient Consumer<Layer> layerAdded;

        SnapWMSLayersPanel(final WorldWindow wwd, final String server, final Dimension size,
                           final Consumer<Layer> layerAdded) throws URISyntaxException {
            super(wwd, server, size);
            this.layerAdded = layerAdded;
        }

        boolean hasLayers() {
            return !layerInfos.isEmpty();
        }

        /**
         * Runs a task on the EDT once the capabilities have been loaded or failed to load.
         */
        void whenLoaded(final Runnable task) {
            final Thread waiter = new Thread(() -> {
                try {
                    loadingThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                SwingUtilities.invokeLater(task);
            }, "WMS capabilities " + serverURI);
            waiter.setDaemon(true);
            waiter.start();
        }

        @Override
        protected void updateComponent(final Object component, final boolean enable) {
            if (!(component instanceof Layer)) {
                super.updateComponent(component, enable);
                return;
            }
            final Layer layer = (Layer) component;
            final LayerList layers = wwd.getModel().getLayers();
            layer.setEnabled(enable);
            if (enable && !layers.contains(layer)) {
                LayerConfiguration.insertBelowOverlays(layers, layer);
                if (layerAdded != null) {
                    layerAdded.accept(layer);
                }
                firePropertyChange("LayersPanelUpdated", null, layer);
            } else if (!enable) {
                layers.remove(layer);
                firePropertyChange("LayersPanelUpdated", layer, null);
            }
            wwd.redraw();
        }
    }
}
