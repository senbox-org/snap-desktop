package org.esa.s2tbx.dataio.gdal.preferences;

import org.esa.s2tbx.dataio.gdal.GDALLoaderConfig;
import org.esa.s2tbx.dataio.gdal.GDALVersion;
import org.openide.awt.Mnemonics;

import javax.swing.*;
import java.awt.*;

/**
 * GDAL Options Panel for GDAL native library loader.
 * Used for provide a UI to the strategy with loading GDAL native library.
 *
 * @author Adrian Draghici
 */
class GDALOptionsPanel extends JPanel {

    private JRadioButton useInternalGDALLibrary;
    private JRadioButton useInstalledGDALLibrary;
    private JComboBox<String> installedGDALLibraryVersions;

    /**
     * Creates new instance for this class
     *
     * @param controller the GDAL Options Controller instance
     */
    GDALOptionsPanel(final GDALOptionsPanelController controller) {
        initComponents();

        useInternalGDALLibrary.addItemListener(e -> controller.changed());
        useInstalledGDALLibrary.addItemListener(e -> controller.changed());
        installedGDALLibraryVersions.addItemListener(e -> controller.changed());
    }

    /**
     * Updates the UI components when GDAL Library version selected
     */
    private void installedGDALLibraryVersionSelected(JTextField locationInstalledField) {
        String versionName = installedGDALLibraryVersions.getItemAt(installedGDALLibraryVersions.getSelectedIndex());
        String location = "not found";
        if (GDALVersion.getInstalledVersions() !=null && GDALVersion.getInstalledVersions().get(versionName) != null) {
            location = GDALVersion.getInstalledVersions().get(versionName).getLocation();
        }
        locationInstalledField.setText(location);
        Mnemonics.setLocalizedText(useInstalledGDALLibrary, "Use installed GDAL version from Operating System (" + installedGDALLibraryVersions.getItemAt(installedGDALLibraryVersions.getSelectedIndex()) + ")");
    }

    /**
     * Initializes the UI components
     */
    private void initComponents() {
        useInternalGDALLibrary = new JRadioButton();
        useInstalledGDALLibrary = new JRadioButton();
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(useInternalGDALLibrary);
        buttonGroup.add(useInstalledGDALLibrary);

        JTextField locationInternalField = new JTextField();
        locationInternalField.setEditable(false);
        GDALVersion internalVersion = GDALVersion.getInternalVersion();
        Mnemonics.setLocalizedText(useInternalGDALLibrary, "Use internal GDAL version from SNAP (" + internalVersion.getId() + ")");
        locationInternalField.setText(internalVersion.getLocation());
        locationInternalField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        installedGDALLibraryVersions = new JComboBox<>();
        JTextField locationInstalledField = new JTextField();
        locationInstalledField.setEditable(false);
        if (GDALVersion.getInstalledVersions() !=null && GDALVersion.getInstalledVersions().size() > 0) {
            for (String installedGDALLibraryVersion : GDALVersion.getInstalledVersions().keySet()) {
                installedGDALLibraryVersions.addItem(installedGDALLibraryVersion);
            }
            installedGDALLibraryVersions.addItemListener(e -> installedGDALLibraryVersionSelected(locationInstalledField));
        } else {
            String versionName = "not installed";
            installedGDALLibraryVersions.addItem(versionName);
        }
        installedGDALLibraryVersionSelected(locationInstalledField);
        installedGDALLibraryVersions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        locationInstalledField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel label = new JLabel("NOTE: Restart SNAP to take changes effect.");
        JLabel locationInternalLabel = new JLabel("Location: ");
        JPanel locationInternalPanel = new JPanel();
        locationInternalPanel.add(locationInternalLabel);
        locationInternalPanel.add(locationInternalField);
        locationInternalPanel.setLayout(new BoxLayout(locationInternalPanel, BoxLayout.X_AXIS));
        JLabel versionInstalledLabel = new JLabel("Version: ");
        JPanel versionInstalledPanel = new JPanel();
        versionInstalledPanel.add(versionInstalledLabel);
        versionInstalledPanel.add(installedGDALLibraryVersions);
        versionInstalledPanel.setLayout(new BoxLayout(versionInstalledPanel, BoxLayout.X_AXIS));
        JLabel locationInstalledLabel = new JLabel("Location: ");
        JPanel locationInstalledPanel = new JPanel();
        locationInstalledPanel.add(locationInstalledLabel);
        locationInstalledPanel.add(locationInstalledField);
        locationInstalledPanel.setLayout(new BoxLayout(locationInstalledPanel, BoxLayout.X_AXIS));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(useInternalGDALLibrary)
                                        .addGap(0, 512, Short.MAX_VALUE)
                                        .addComponent(locationInternalPanel)
                                        .addGap(0, 512, Short.MAX_VALUE)
                                        .addComponent(useInstalledGDALLibrary)
                                        .addGap(0, 512, Short.MAX_VALUE)
                                        .addComponent(versionInstalledPanel)
                                        .addGap(0, 512, Short.MAX_VALUE)
                                        .addComponent(locationInstalledPanel)
                                        .addGap(0, 512, Short.MAX_VALUE)
                                        .addComponent(label))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(useInternalGDALLibrary)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(locationInternalPanel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(useInstalledGDALLibrary)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(versionInstalledPanel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(locationInstalledPanel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label)
                                .addContainerGap())
        );
    }

    /**
     * Loads the configuration from GDAL Loader Config on UI
     */
    void load() {
        if (GDALLoaderConfig.getInstance().useInstalledGDALLibrary()) {
            useInstalledGDALLibrary.setSelected(true);
        } else {
            useInternalGDALLibrary.setSelected(true);
        }
        String selectedInstalledGDALLibrary = GDALLoaderConfig.getInstance().getSelectedInstalledGDALLibrary();
        if (!selectedInstalledGDALLibrary.contentEquals(GDALLoaderConfig.PREFERENCE_NONE_VALUE_SELECTED_INSTALLED_GDAL)) {
            installedGDALLibraryVersions.setSelectedItem(selectedInstalledGDALLibrary);
        }
    }

    /**
     * Saves the configuration on GDAL Loader Config from UI
     */
    void store() {
        GDALLoaderConfig.getInstance().setUseInstalledGDALLibrary(useInstalledGDALLibrary.isSelected());
        GDALLoaderConfig.getInstance().setSelectedInstalledGDALLibrary(installedGDALLibraryVersions.getItemAt(installedGDALLibraryVersions.getSelectedIndex()));
    }

    /**
     * Checks whether or not form is consistent and complete
     *
     * @return {@code true} if form is consistent and complete
     */
    boolean valid() {
        return true;
    }
}
