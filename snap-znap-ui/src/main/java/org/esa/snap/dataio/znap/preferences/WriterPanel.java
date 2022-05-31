/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.znap.preferences;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.DEFAULT_COMPRESSION_LEVEL;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.DEFAULT_COMPRESSOR_ID;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.DEFAULT_USE_ZIP_ARCHIVE;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_BINARY_FORMAT;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_COMPRESSION_LEVEL;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_COMPRESSOR_ID;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.ZLIB_COMPRESSION_LEVELS;

final class WriterPanel extends javax.swing.JPanel {

    private static final String ZARR_FORMAT_NAME = "Zarr (default)";
    private static final String COMPRESSOR_NULL = "null";
    private static final String COMPRESSOR_ZLIB = "zlib";
    private static final String DEFAULT_EXTENSION = " (default)";
    private static final String[] AVAILABLE_COMPRESSORS = {COMPRESSOR_NULL, COMPRESSOR_ZLIB};

    static {
        for (int i = 0; i < AVAILABLE_COMPRESSORS.length; i++) {
            AVAILABLE_COMPRESSORS[i] = appendDefaultExtensionIfItIsTheDefaultCompressorId(AVAILABLE_COMPRESSORS[i]);
        }
    }

    private final WriterOptionsPanelController controller;

    private JComponent createZipArchiveLabel;
    private JCheckBox createZipArchiveCheck;
    private JComponent binaryFormatLabel;
    private JComboBox<String> binaryFormatCombo;
    private JComponent compressorLabel;
    private JComboBox<String> compressorCombo;
    private JComponent compressionLevelLabel;
    private JComboBox<String> compressionLevelCombo;

    WriterPanel(WriterOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        addListenerToComponents(controller);
    }

    void load() {
        Preferences preferences = Config.instance("snap").load().preferences();

        final boolean useZipArchive = preferences.getBoolean(PROPERTY_NAME_USE_ZIP_ARCHIVE, DEFAULT_USE_ZIP_ARCHIVE);
        createZipArchiveCheck.setSelected(useZipArchive);

        String binaryFormat = preferences.get(PROPERTY_NAME_BINARY_FORMAT, ZARR_FORMAT_NAME);
        binaryFormatCombo.setSelectedItem(binaryFormat);

        String compressorId = preferences.get(PROPERTY_NAME_COMPRESSOR_ID, DEFAULT_COMPRESSOR_ID);
        final String entryToBeSelected = appendDefaultExtensionIfItIsTheDefaultCompressorId(compressorId);
        compressorCombo.setSelectedItem(entryToBeSelected);

        int compressionLevel = preferences.getInt(PROPERTY_NAME_COMPRESSION_LEVEL, DEFAULT_COMPRESSION_LEVEL);
        int idx = Arrays.asList(ZLIB_COMPRESSION_LEVELS).indexOf(compressionLevel);
        compressionLevelCombo.setSelectedIndex(idx);
    }

    void store() {
        Preferences preferences = Config.instance("snap").load().preferences();
        try {
            final boolean useZipArchive = createZipArchiveCheck.isSelected();
            if (useZipArchive != DEFAULT_USE_ZIP_ARCHIVE) {
                preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, String.valueOf(useZipArchive));
            } else {
                preferences.remove(PROPERTY_NAME_USE_ZIP_ARCHIVE);
            }

            String selectedFormat = binaryFormatCombo.getItemAt(binaryFormatCombo.getSelectedIndex());
            final boolean isNotZarrFormatName = !ZARR_FORMAT_NAME.equals(selectedFormat);
            if (binaryFormatCombo.isEnabled() && isNotZarrFormatName) {
                preferences.put(PROPERTY_NAME_BINARY_FORMAT, selectedFormat);
            } else {
                preferences.remove(PROPERTY_NAME_BINARY_FORMAT);
            }

            String compressorId = compressorCombo.getItemAt(compressorCombo.getSelectedIndex());
            compressorId = removeDefaultExtension(compressorId);
            final boolean isDefaultCompressorId = DEFAULT_COMPRESSOR_ID.equals(compressorId);
            if (compressorCombo.isEnabled() && !isDefaultCompressorId) {
                preferences.put(PROPERTY_NAME_COMPRESSOR_ID, compressorId);
            } else {
                preferences.remove(PROPERTY_NAME_COMPRESSOR_ID);
            }

            if (COMPRESSOR_NULL.equals(compressorId)) {
                preferences.remove(PROPERTY_NAME_COMPRESSION_LEVEL);
                return;
            }

            int selectedIndex = compressionLevelCombo.getSelectedIndex();
            int compressionLevel = ZLIB_COMPRESSION_LEVELS[selectedIndex];
            final boolean isDefaultCompressionLevel = compressionLevel == DEFAULT_COMPRESSION_LEVEL;
            if (compressionLevelCombo.isEnabled() && !isDefaultCompressionLevel) {
                preferences.putInt(PROPERTY_NAME_COMPRESSION_LEVEL, compressionLevel);
            } else {
                preferences.remove(PROPERTY_NAME_COMPRESSION_LEVEL);
            }
        } finally {
            try {
                preferences.flush();
            } catch (BackingStoreException e) {
                SnapApp.getDefault().getLogger().severe(e.getMessage());
            }
        }
    }

    private static String appendDefaultExtensionIfItIsTheDefaultCompressorId(String compressorId) {
        return DEFAULT_COMPRESSOR_ID.equals(compressorId) ? compressorId + DEFAULT_EXTENSION : compressorId;
    }

    private static String removeDefaultExtension(String comboBoxEntry) {
        return comboBoxEntry.replace(DEFAULT_EXTENSION, "");
    }

    boolean valid() {
        return true;
    }

    private void initComponents() {
        binaryFormatLabel = new JLabel("Binary format:");
        Vector<String> formatNames = new Vector<>();
        formatNames.add(ZARR_FORMAT_NAME);
        formatNames.add("GeoTIFF");
        formatNames.add("GeoTIFF-BigTIFF");
        formatNames.add("ENVI");
        formatNames.add("NetCDF4-CF");
        binaryFormatCombo = new JComboBox<>(formatNames);

        compressorLabel = new JLabel("Compressor:");

        compressorCombo = new JComboBox<>(AVAILABLE_COMPRESSORS);

        compressionLevelLabel = new JLabel("Compression level:");
        compressionLevelCombo = new JComboBox<>();
        for (Integer zlibCompressionLevel : ZLIB_COMPRESSION_LEVELS) {
            String item = zlibCompressionLevel.toString();
            if (zlibCompressionLevel == DEFAULT_COMPRESSION_LEVEL) {
                item += " (default)";
            }
            compressionLevelCombo.addItem(item);
        }

        createZipArchiveLabel = new JLabel("Create zip archive:");
        createZipArchiveCheck = new JCheckBox();

        setBorder(new TitledBorder("ZNAP Options"));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        setLayout(layout);
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(createZipArchiveLabel)
                                        .addComponent(createZipArchiveCheck))
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(binaryFormatLabel)
                                        .addComponent(binaryFormatCombo))
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(compressorLabel)
                                        .addComponent(compressorCombo))
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(compressionLevelLabel)
                                        .addComponent(compressionLevelCombo))
                        .addGap(0, 2000, Short.MAX_VALUE)
        );
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(binaryFormatLabel)
                                        .addComponent(compressorLabel)
                                        .addComponent(compressionLevelLabel)
                                        .addComponent(createZipArchiveLabel))
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(binaryFormatCombo)
                                        .addComponent(compressorCombo)
                                        .addComponent(compressionLevelCombo)
                                        .addComponent(createZipArchiveCheck))
                        .addGap(0, 2000, Short.MAX_VALUE)
        );

    }

    private void addListenerToComponents(WriterOptionsPanelController controller) {
        ItemListener itemListener = e -> {
            controller.changed();
            updateState();
        };
        binaryFormatCombo.addItemListener(itemListener);
        compressorCombo.addItemListener(itemListener);
        compressionLevelCombo.addItemListener(itemListener);
        createZipArchiveCheck.addItemListener(itemListener);
    }

    private void updateState() {
        final boolean noZipArchive = !createZipArchiveCheck.isSelected();
        binaryFormatLabel.setEnabled(noZipArchive);
        binaryFormatCombo.setEnabled(noZipArchive);

        boolean zarrFormat = binaryFormatCombo.getSelectedIndex() == 0;
        final boolean noArchiveAndZarrFormat = noZipArchive && zarrFormat;
        compressorCombo.setEnabled(noArchiveAndZarrFormat);
        compressorLabel.setEnabled(noArchiveAndZarrFormat);

        int selectedIndex = compressorCombo.getSelectedIndex();
        String selectedItem = compressorCombo.getItemAt(selectedIndex);
        final String selectedCompressorID = removeDefaultExtension(selectedItem);
        boolean compressorIsSelected = !COMPRESSOR_NULL.equals(selectedCompressorID);
        boolean levelEnabled = noArchiveAndZarrFormat && compressorIsSelected;
        compressionLevelCombo.setEnabled(levelEnabled);
        compressionLevelLabel.setEnabled(levelEnabled);
    }
}
