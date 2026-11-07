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
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.worldwind.layers.DataFileLayers;
import org.esa.snap.worldwind.preferences.WorldWindPreferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Adds external data to a globe: KML/KMZ, shapefile and GeoJSON files, and layers of WMS servers. Lists the added
 * layers so that they can be removed again.
 */
class ExternalDataPanel extends JPanel {

    private final WorldWindow wwd;
    private final DefaultListModel<Layer> layersModel = new DefaultListModel<>();
    private final JList<Layer> layersList = new JList<>(layersModel);
    // layers of the WMS dialog: owned by its check boxes, which add the same layer again when checked again
    private final Set<Layer> wmsLayers = new HashSet<>();
    private WmsBrowserDialog wmsDialog;

    ExternalDataPanel(final WorldWindow wwd) {
        super(new BorderLayout(2, 2));
        this.wwd = wwd;
        setBorder(BorderFactory.createTitledBorder("External Data"));

        final JButton addFileButton = new JButton("Add File...");
        addFileButton.setToolTipText("Show KML/KMZ, shapefile or GeoJSON files on the globe");
        addFileButton.addActionListener(e -> chooseFiles());
        final JButton addWmsButton = new JButton("Add WMS...");
        addWmsButton.setToolTipText("Show layers of a WMS server on the globe");
        addWmsButton.addActionListener(e -> showWmsDialog());
        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSelected());
        removeButton.setEnabled(false);

        layersList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        layersList.setVisibleRowCount(3);
        layersList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, ((Layer) value).getName(), index, isSelected, cellHasFocus);
            }
        });
        layersList.addListSelectionListener(e -> removeButton.setEnabled(!layersList.isSelectionEmpty()));

        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        buttons.add(addFileButton);
        buttons.add(addWmsButton);
        buttons.add(removeButton);
        add(buttons, BorderLayout.NORTH);
        add(new JScrollPane(layersList), BorderLayout.CENTER);
    }

    private void chooseFiles() {
        final String lastDir = Config.instance().preferences().get(WorldWindPreferences.LAST_DATA_DIR, null);
        final SnapFileChooser chooser = new SnapFileChooser(lastDir != null ? new File(lastDir) : null);
        chooser.setDialogTitle("Add Data File");
        chooser.setMultiSelectionEnabled(true);
        final List<String> allExtensions = new ArrayList<>();
        for (DataFileLayers.Type type : DataFileLayers.Type.values()) {
            allExtensions.addAll(List.of(type.extensions));
        }
        final FileNameExtensionFilter allFilter =
                new FileNameExtensionFilter("KML/KMZ, shapefile, GeoJSON", allExtensions.toArray(new String[0]));
        chooser.addChoosableFileFilter(allFilter);
        for (DataFileLayers.Type type : DataFileLayers.Type.values()) {
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(type.description, type.extensions));
        }
        chooser.setFileFilter(allFilter);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File[] files = chooser.getSelectedFiles();
        if (files.length > 0 && files[0].getParentFile() != null) {
            Config.instance().preferences().put(WorldWindPreferences.LAST_DATA_DIR, files[0].getParent());
        }
        for (File file : files) {
            load(file);
        }
    }

    private void load(final File file) {
        new SwingWorker<Layer, Void>() {
            @Override
            protected Layer doInBackground() throws Exception {
                return DataFileLayers.load(file);
            }

            @Override
            protected void done() {
                try {
                    addLayer(get());
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause() != null ? e.getCause() : e;
                    Dialogs.showError("Add Data File", "Unable to show " + file.getName() + ":\n" + cause.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    void addLayer(final Layer layer) {
        if (!wwd.getModel().getLayers().contains(layer)) {
            LayerConfiguration.insertBelowOverlays(wwd.getModel().getLayers(), layer);
        }
        if (!layersModel.contains(layer)) {
            layersModel.addElement(layer);
        }
        wwd.redraw();
    }

    private void addWmsLayer(final Layer layer) {
        wmsLayers.add(layer);
        addLayer(layer);
    }

    /**
     * A WMS layer unchecked in the dialog.
     */
    private void wmsLayerRemoved(final Layer layer) {
        layersModel.removeElement(layer);
    }

    private void removeSelected() {
        for (Layer layer : layersList.getSelectedValuesList()) {
            wwd.getModel().getLayers().remove(layer);
            layersModel.removeElement(layer);
            if (wmsLayers.contains(layer)) {
                wmsDialog.deselect(layer);
            } else {
                layer.dispose();
            }
        }
        wwd.redraw();
    }

    private void showWmsDialog() {
        if (wmsDialog == null) {
            wmsDialog = new WmsBrowserDialog(SwingUtilities.getWindowAncestor(this), wwd, this::addWmsLayer,
                                             this::wmsLayerRemoved);
        }
        wmsDialog.setVisible(true);
        wmsDialog.toFront();
    }
}
