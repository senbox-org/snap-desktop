/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.worldwind.preferences;

import gov.nasa.worldwind.WorldWind;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.runtime.Config;
import org.esa.snap.worldwind.LocalDemTerrain;
import org.esa.snap.worldwind.WWWorldViewToolView;
import org.openide.awt.Mnemonics;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class WorldWindOptionsPanel extends JPanel {

    private final JCheckBox useFlatEarthCheckBox = new JCheckBox();
    private final JCheckBox offlineCheckBox = new JCheckBox("Work offline: show only map tiles already in the cache");
    private final JTextField cacheDirField = new JTextField(40);
    private final JLabel cacheLabel = new JLabel(" ");
    private final JButton clearCacheButton = new JButton("Clear Cache...");
    private final JCheckBox localDemCheckBox =
            new JCheckBox("Use DEM GeoTIFFs as terrain (lat/lon grids, e.g. the Copernicus tiles SNAP downloads)");
    private final JTextField localDemDirField = new JTextField(40);
    private final JTextArea wmsServersArea = new JTextArea(4, 50);

    WorldWindOptionsPanel(final WorldWindOptionsPanelController controller) {
        super(new GridBagLayout());
        initComponents();
        // listen to changes in form fields and call controller.changed()
        useFlatEarthCheckBox.addItemListener(e -> controller.changed());
        offlineCheckBox.addItemListener(e -> controller.changed());
        localDemCheckBox.addItemListener(e -> {
            localDemDirField.setEnabled(localDemCheckBox.isSelected());
            controller.changed();
        });
        final DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                controller.changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                controller.changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                controller.changed();
            }
        };
        for (JTextComponent field : new JTextComponent[]{cacheDirField, localDemDirField, wmsServersArea}) {
            field.getDocument().addDocumentListener(documentListener);
        }
        clearCacheButton.addActionListener(e -> clearCache());
    }

    private void initComponents() {
        Mnemonics.setLocalizedText(useFlatEarthCheckBox, "Use flat Earth projection");

        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(2, 2, 2, 2);
        add(useFlatEarthCheckBox, c);

        addSection(c, offlineCheckBox);
        addRow(c, new JLabel("Tile cache folder (empty for WorldWind's default; used after a restart):"));
        addRow(c, withBrowseButton(cacheDirField));
        final JPanel cachePanel = new JPanel(new BorderLayout(4, 0));
        cachePanel.add(cacheLabel, BorderLayout.CENTER);
        cachePanel.add(clearCacheButton, BorderLayout.EAST);
        addRow(c, cachePanel);

        addSection(c, localDemCheckBox);
        addRow(c, withBrowseButton(localDemDirField));

        addSection(c, new JLabel("WMS servers of \"Add WMS\" in the Analysis view, one address per line:"));
        addRow(c, new JScrollPane(wmsServersArea));

        // push everything to the top
        c.gridy++;
        c.weighty = 1.0;
        add(new JPanel(), c);
    }

    private void addSection(final GridBagConstraints c, final JComponent component) {
        c.gridy++;
        c.insets = new Insets(12, 2, 2, 2);
        add(component, c);
        c.insets = new Insets(2, 2, 2, 2);
    }

    private void addRow(final GridBagConstraints c, final JComponent component) {
        c.gridy++;
        add(component, c);
    }

    private JPanel withBrowseButton(final JTextField field) {
        final JButton browseButton = new JButton("...");
        browseButton.addActionListener(e -> {
            final JFileChooser chooser = new JFileChooser(field.getText().trim().isEmpty() ? null : field.getText().trim());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        final JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.add(field, BorderLayout.CENTER);
        panel.add(browseButton, BorderLayout.EAST);
        return panel;
    }

    private void updateCacheLabel() {
        final File location = TileCache.getLocation();
        cacheLabel.setText("Current cache: " + location + " (computing size...)");
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() {
                return TileCache.sizeOf(location);
            }

            @Override
            protected void done() {
                try {
                    cacheLabel.setText("Current cache: " + location + " (" + TileCache.formatSize(get()) + ")");
                } catch (Exception e) {
                    cacheLabel.setText("Current cache: " + location);
                }
            }
        }.execute();
    }

    private void clearCache() {
        final File location = TileCache.getLocation();
        final Dialogs.Answer answer = Dialogs.requestDecision("Clear Tile Cache",
                "Delete all map tiles downloaded to\n" + location + "?\nThey are downloaded again when needed.",
                false, null);
        if (answer != Dialogs.Answer.YES) {
            return;
        }
        clearCacheButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                TileCache.clear(location);
                return null;
            }

            @Override
            protected void done() {
                clearCacheButton.setEnabled(true);
                try {
                    get();
                } catch (Exception e) {
                    Dialogs.showError("Clear Tile Cache", "Unable to clear the cache: " + e.getMessage());
                }
                updateCacheLabel();
            }
        }.execute();
    }

    void load() {
        final Preferences preferences = Config.instance().preferences();
        useFlatEarthCheckBox.setSelected(preferences.getBoolean(WWWorldViewToolView.useFlatEarth, false));
        offlineCheckBox.setSelected(WorldWindPreferences.isOffline());
        final File cacheDir = WorldWindPreferences.getCacheDir();
        cacheDirField.setText(cacheDir != null ? cacheDir.getPath() : "");
        localDemCheckBox.setSelected(WorldWindPreferences.isLocalDemEnabled());
        localDemDirField.setText(WorldWindPreferences.getLocalDemDir().getPath());
        localDemDirField.setEnabled(localDemCheckBox.isSelected());
        wmsServersArea.setText(String.join("\n", WorldWindPreferences.getWmsServers()));
        updateCacheLabel();
    }

    void store() {
        final Preferences preferences = Config.instance().preferences();
        preferences.putBoolean(WWWorldViewToolView.useFlatEarth, useFlatEarthCheckBox.isSelected());

        preferences.putBoolean(WorldWindPreferences.OFFLINE, offlineCheckBox.isSelected());
        WorldWind.setOfflineMode(offlineCheckBox.isSelected());
        preferences.put(WorldWindPreferences.CACHE_DIR, cacheDirField.getText().trim());

        final boolean demChanged = localDemCheckBox.isSelected() != WorldWindPreferences.isLocalDemEnabled() ||
                !new File(localDemDirField.getText().trim()).equals(WorldWindPreferences.getLocalDemDir());
        preferences.putBoolean(WorldWindPreferences.LOCAL_DEM, localDemCheckBox.isSelected());
        preferences.put(WorldWindPreferences.LOCAL_DEM_DIR, localDemDirField.getText().trim());

        WorldWindPreferences.setWmsServers(WorldWindPreferences.parseLines(wmsServersArea.getText()));

        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            SnapApp.getDefault().getLogger().severe(e.getMessage());
        }
        if (demChanged) {
            LocalDemTerrain.reload();
        }
    }

    boolean valid() {
        // Check whether form is consistent and complete
        return true;
    }
}
