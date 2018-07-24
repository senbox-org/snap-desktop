/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.productlibrary.db.*;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.model.repositories.FolderRepository;
import org.esa.snap.productlibrary.rcp.toolviews.model.repositories.RepositoryInterface;
import org.esa.snap.rcp.util.Dialogs;
import org.jdesktop.swingx.JXDatePicker;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
    UI for database query
 */
public final class DatabasePane extends JPanel {

    private final JTextField nameField = new JTextField();
    private final JList<String> missionJList = new JList<>();
    private final JList<String> productTypeJList = new JList<>();
    private final JComboBox<String> acquisitionModeCombo = new JComboBox<>(new String[]{DBQuery.ALL_MODES});
    private final JComboBox<String> passCombo = new JComboBox<>(new String[]{
            DBQuery.ALL_PASSES, DBQuery.ASCENDING_PASS, DBQuery.DESCENDING_PASS});
    private final JTextField trackField = new JTextField();

    private final JXDatePicker startDateBox = new JXDatePicker();
    private final JXDatePicker endDateBox = new JXDatePicker();
    private final JComboBox<String> polarizationCombo = new JComboBox<>(new String[]{
            DBQuery.ANY, DBQuery.QUADPOL, DBQuery.DUALPOL, DBQuery.HHVV, DBQuery.HHHV, DBQuery.VVVH, "HH", "VV", "HV", "VH"});
    private final JComboBox<String> calibrationCombo = new JComboBox<>(new String[]{
            DBQuery.ANY, DBQuery.CALIBRATED, DBQuery.NOT_CALIBRATED});
    private final JComboBox<String> orbitCorrectionCombo = new JComboBox<>(new String[]{
            DBQuery.ANY, DBQuery.ORBIT_PRELIMINARY, DBQuery.ORBIT_PRECISE, DBQuery.ORBIT_VERIFIED});

    private final JTextField cloudCoverField = new JTextField();

    private final JComboBox<String> metadataNameCombo = new JComboBox<>();
    private final JTextField metdataValueField = new JTextField();
    private final JTextArea metadataArea = new JTextArea();
    private final JButton addMetadataButton = new JButton("+");
    private final JTextArea productText = new JTextArea();
    private final JRadioButton bboxInsideButton = new JRadioButton("Inside", true);
    private final JRadioButton bboxIntersectButton = new JRadioButton("Intersect", false);

    private RepositoryInterface repository;
    private ProductQueryInterface productQueryInterface;

    private DBQuery dbQuery = new DBQuery();
    private boolean modifyingCombos = false;

    private final static double MB = 1024 * 1024, GB = 1024, TB = 1024 * 1024;
    private final DecimalFormat df = new DecimalFormat("#.00");

    private final List<DatabaseQueryListener> listenerList = new ArrayList<>(1);

    public DatabasePane() {
        try {
            missionJList.setFixedCellWidth(100);
            createPanel();

            missionJList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    if (modifyingCombos || event.getValueIsAdjusting()) return;
                    updateMissionFields();
                    partialQuery();
                }
            });
            productTypeJList.setFixedCellWidth(100);
            productTypeJList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    if (modifyingCombos || event.getValueIsAdjusting()) return;
                    partialQuery();
                }
            });
            addComboListener(acquisitionModeCombo);
            addComboListener(passCombo);
            addComboListener(polarizationCombo);
            addComboListener(calibrationCombo);
            addComboListener(orbitCorrectionCombo);

            addMetadataButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addMetadataText();
                }
            });

            bboxInsideButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dbQuery.insideSelectionRectangle(bboxInsideButton.isSelected());
                    partialQuery();
                }
            });

            bboxIntersectButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dbQuery.insideSelectionRectangle(bboxInsideButton.isSelected());
                    partialQuery();
                }
            });

        } catch (Throwable t) {
            handleException(t);
        }
    }

    private void addComboListener(final JComboBox<String> combo) {
        combo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (modifyingCombos || event.getStateChange() == ItemEvent.DESELECTED) return;
                partialQuery();
            }
        });
    }

    /**
     * Adds a <code>DatabasePaneListener</code>.
     *
     * @param listener the <code>DatabasePaneListener</code> to be added.
     */
    public void addListener(final DatabaseQueryListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    /**
     * Removes a <code>DatabasePaneListener</code>.
     *
     * @param listener the <code>DatabasePaneListener</code> to be removed.
     */
    public void removeListener(final DatabaseQueryListener listener) {
        listenerList.remove(listener);
    }

    private void notifyQuery() {
        for (final DatabaseQueryListener listener : listenerList) {
            listener.notifyNewEntryListAvailable();
        }
    }

    public interface DatabaseQueryListener {
        void notifyNewEntryListAvailable();
    }

    private static void handleException(Throwable t) {
        SystemUtils.LOG.severe(t.getMessage());
        Dialogs.showError(t.getMessage());
    }

    private void createPanel() {
        setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        JLabel label;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(new JLabel("Mission:"), gbc);
        gbc.gridx = 1;
        this.add(new JLabel("Product Type:"), gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        this.add(new JScrollPane(missionJList), gbc);
        gbc.gridx = 1;
        this.add(new JScrollPane(productTypeJList), gbc);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Product Name:", nameField);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Acquisition Mode:", acquisitionModeCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Pass:", passCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Track:", trackField);
        label.setHorizontalAlignment(JLabel.RIGHT);

        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Start Date:", startDateBox);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "End Date:", endDateBox);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Polarization:", polarizationCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Calibration:", calibrationCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Orbit Correction:", orbitCorrectionCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);

        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Cloud Cover %:", cloudCoverField);
        label.setHorizontalAlignment(JLabel.RIGHT);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        this.add(createFreeSearchPanel(), gbc);

        gbc.gridy++;
        final ButtonGroup group = new ButtonGroup();
        group.add(bboxInsideButton);
        group.add(bboxIntersectButton);
        final JPanel radioPanel = new JPanel(new FlowLayout());
        //radioPanel.add(new JLabel("Bounding Rectangle: "));
        radioPanel.add(bboxInsideButton);
        radioPanel.add(bboxIntersectButton);
        radioPanel.add(new JLabel(" selection rectangle"));
        this.add(radioPanel, gbc);

        gbc.gridy++;
        final JPanel productDetailsPanel = new JPanel(new BorderLayout());
        productDetailsPanel.setBorder(BorderFactory.createTitledBorder("Product Details"));
        productText.setLineWrap(true);
        productText.setRows(4);
        productText.setBackground(getBackground());
        productDetailsPanel.add(productText, BorderLayout.CENTER);
        this.add(productDetailsPanel, gbc);

        //DialogUtils.fillPanel(this, gbc);
    }

    private JPanel createFreeSearchPanel() {
        final JPanel freeSearchPanel = new JPanel(new GridBagLayout());
        freeSearchPanel.setBorder(BorderFactory.createTitledBorder("Metadata SQL Query"));
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        freeSearchPanel.add(metadataNameCombo, gbc);
        metadataNameCombo.setPrototypeDisplayValue("123456789012");

        final String[] metadataNames = MetadataTable.getAllMetadataNames();
        for (String name : metadataNames) {
            metadataNameCombo.insertItemAt(name, metadataNameCombo.getItemCount());
        }

        gbc.gridx = 1;
        freeSearchPanel.add(metdataValueField, gbc);
        metdataValueField.setColumns(10);
        gbc.gridx = 2;
        freeSearchPanel.add(addMetadataButton, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        freeSearchPanel.add(metadataArea, gbc);
        metadataArea.setBorder(new LineBorder(Color.BLACK));
        metadataArea.setLineWrap(true);
        metadataArea.setRows(4);
        metadataArea.setToolTipText("Use AND,OR,NOT and =,<,>,<=,>-");
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        DialogUtils.fillPanel(freeSearchPanel, gbc);
        return freeSearchPanel;
    }

    public void partialQuery() {

        setData();

        try {
            if(productQueryInterface.partialQuery(dbQuery)) {
                notifyQuery();
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private boolean isClientError401(Exception e) {
        return e.getMessage().contains("CLIENT_ERROR") && e.getMessage().contains("401");
    }

    void fullQuery(final ProgressMonitor pm) {

        setData();

        final int numRetries = 1;

        for (int i = 0; i < numRetries + 1; i++) {
            try {
                if (productQueryInterface.fullQuery(dbQuery, pm)) {
                    notifyQuery();
                }
                break;
            } catch (Exception e) {
                if (i < numRetries && isClientError401(e)) {
                    repository.resetCredentials();
                } else if (isClientError401(e)) {
                    handleException(new IOException(e.getMessage() + " (invalid credentials)"));
                    break;
                } else {
                    handleException(e);
                    break;
                }
            }
        }
    }

    public ProductEntry[] getProductEntryList() {
        return productQueryInterface.getProductEntryList();
    }

    void refresh() {
        boolean origState = lockCombos(true);
        try {
            updateMissionCombo();
            updateMissionFields();

        } catch (Throwable t) {
            handleException(t);
        } finally {
            lockCombos(origState);
        }
    }

    private boolean lockCombos(boolean flag) {
        final boolean origState = modifyingCombos;
        modifyingCombos = flag;
        return origState;
    }

    private void updateMissionCombo() throws SQLException {
        missionJList.removeAll();
        missionJList.setListData(SQLUtils.prependString(DBQuery.ALL_MISSIONS, productQueryInterface.getAllMissions()));
    }

    private void updateMissionFields() {
        boolean origState = lockCombos(true);
        try {
            final String[] selectedMissions = toStringArray(missionJList.getSelectedValuesList());
            final String[] missions = StringUtils.contains(selectedMissions, DBQuery.ALL_MISSIONS) ? null : selectedMissions;

            productTypeJList.removeAll();
            productTypeJList.setListData(SQLUtils.prependString(DBQuery.ALL_PRODUCT_TYPES, productQueryInterface.getAllProductTypes(missions)));

            final String[] modeItems = SQLUtils.prependString(DBQuery.ALL_MODES, productQueryInterface.getAllAcquisitionModes(missions));
            acquisitionModeCombo.removeAllItems();
            for (String item : modeItems) {
                acquisitionModeCombo.addItem(item);
            }

            if(!isFolderRepository() && missions != null) {
                boolean isSAR = containsSARMission(missions);
                boolean isOptical = containsOpticalMission(missions);
                polarizationCombo.setEnabled(isSAR);
                if(!isSAR) {
                    polarizationCombo.setSelectedIndex(0);
                }
                cloudCoverField.setEnabled(isOptical);
                if(!isOptical) {
                    cloudCoverField.setText("");
                }
            } else {
                polarizationCombo.setEnabled(true);
                cloudCoverField.setEnabled(false);
                cloudCoverField.setText("");
            }

        } catch (Throwable t) {
            handleException(t);
        } finally {
            lockCombos(origState);
        }
    }

    private boolean containsOpticalMission(final String[] missions) {
        for(String mission : missions) {
            if(mission.equalsIgnoreCase("Sentinel-2") || mission.equalsIgnoreCase("Sentinel-3"))
                return true;
        }
        return false;
    }

    private boolean containsSARMission(final String[] missions) {
        for(String mission : missions) {
            if(mission.equalsIgnoreCase("Sentinel-1"))
                return true;
        }
        return false;
    }

    private static String[] toStringArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    private boolean isFolderRepository() {
        return repository == null || repository instanceof FolderRepository;
    }

    public void setRepository(final RepositoryInterface repo) {
        this.repository = repo;
        this.productQueryInterface = repository.getProductQueryInterface();

        final boolean isFolderRepo = isFolderRepository();
        enableComponents(isFolderRepo);
        if (isFolderRepo) {
            setBaseDir(((FolderRepository) repo).getBaseDir());
        } else {
            setBaseDir(null);
        }
        missionJList.setSelectedIndex(0);
        refresh();
    }

    private void enableComponents(final boolean isFolderRepo) {
        calibrationCombo.setEnabled(isFolderRepo);
        orbitCorrectionCombo.setEnabled(isFolderRepo);
        cloudCoverField.setEnabled(!isFolderRepo);

        metadataNameCombo.setEnabled(isFolderRepo);
        metdataValueField.setEnabled(isFolderRepo);
        metadataArea.setEnabled(isFolderRepo);
        addMetadataButton.setEnabled(isFolderRepo);
    }

    private void setBaseDir(final File dir) {
        dbQuery.setBaseDir(dir);
        partialQuery();
    }

    private void addMetadataText() {
        final String name = (String) metadataNameCombo.getSelectedItem();
        final String value = metdataValueField.getText();
        if (!name.isEmpty() && !value.isEmpty()) {
            if (!metadataArea.getText().isEmpty()) {
                metadataArea.append(" AND ");
            }
            if (value.matches("-?\\d+(\\.\\d+)?")) {     // isNumeric
                metadataArea.append(name + '=' + value + ' ');
            } else {
                metadataArea.append(name + "='" + value + "' ");
            }
        }
    }

    private static Calendar getDate(final JXDatePicker dateField) {
        final Date date = dateField.getDate();
        if (date == null)
            return null;
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    private void setData() {
        dbQuery.setSelectedMissions(toStringArray(missionJList.getSelectedValuesList()));
        dbQuery.setSelectedProductTypes(toStringArray(productTypeJList.getSelectedValuesList()));
        dbQuery.setSelectedName(nameField.getText());
        dbQuery.setSelectedAcquisitionMode((String) acquisitionModeCombo.getSelectedItem());
        dbQuery.setSelectedPass((String) passCombo.getSelectedItem());
        dbQuery.setSelectedTrack(trackField.getText());
        dbQuery.setSelectedCloudCover(cloudCoverField.getText());

        dbQuery.setStartEndDate(getDate(startDateBox), getDate(endDateBox));

        dbQuery.setSelectedPolarization((String) polarizationCombo.getSelectedItem());
        dbQuery.setSelectedCalibration((String) calibrationCombo.getSelectedItem());
        dbQuery.setSelectedOrbitCorrection((String) orbitCorrectionCombo.getSelectedItem());

        dbQuery.clearMetadataQuery();
        dbQuery.setFreeQuery(metadataArea.getText());
    }

    void setSelectionRect(final GeoPos[] selectionBox) {
        dbQuery.setSelectionRect(selectionBox);
        dbQuery.setReturnAllIfNoIntersection(true);
        dbQuery.insideSelectionRectangle(bboxInsideButton.isSelected());
        partialQuery();
    }

    public DBQuery getDBQuery() {
        setData();
        return dbQuery;
    }

    void findSlices(final int dataTakeId) {
        metadataArea.setText(AbstractMetadata.data_take_id + '=' + dataTakeId);

        dbQuery.setSelectionRect(null);
        partialQuery();

        metadataArea.setText("");
    }

    public void setDBQuery(final DBQuery query) throws Exception {
        if (query == null) return;
        dbQuery = query;

        boolean origState = lockCombos(true);
        try {
            missionJList.setSelectedIndices(findIndices(missionJList, dbQuery.getSelectedMissions()));
            updateMissionFields();
            productTypeJList.setSelectedIndices(findIndices(productTypeJList, dbQuery.getSelectedProductTypes()));
            acquisitionModeCombo.setSelectedItem(dbQuery.getSelectedAcquisitionMode());
            passCombo.setSelectedItem(dbQuery.getSelectedPass());
            if (dbQuery.getStartDate() != null) {
                startDateBox.setDate(dbQuery.getStartDate().getTime());
            }
            if (dbQuery.getEndDate() != null) {
                endDateBox.setDate(dbQuery.getEndDate().getTime());
            }
            polarizationCombo.setSelectedItem(dbQuery.getSelectedPolarization());
            calibrationCombo.setSelectedItem(dbQuery.getSelectedCalibration());
            orbitCorrectionCombo.setSelectedItem(dbQuery.getSelectedOrbitCorrection());

            metadataArea.setText(dbQuery.getFreeQuery());
        } finally {
            lockCombos(origState);
        }
    }

    private static int[] findIndices(final JList<String> list, final String[] values) {
        final int size = list.getModel().getSize();
        final List<Integer> indices = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            final String str = list.getModel().getElementAt(i);
            if (StringUtils.contains(values, str)) {
                indices.add(i);
            }
        }
        final int[] intIndices = new int[indices.size()];
        for (int i = 0; i < indices.size(); ++i) {
            intIndices[i] = indices.get(i);
        }
        return intIndices;
    }

    void updateProductSelectionText(final ProductEntry[] selections) {
        if (selections != null && selections.length == 1) {
            final ProductEntry entry = selections[0];
            final StringBuilder text = new StringBuilder(255);

            final File file = entry.getFile();
            if (file != null) {
                text.append("File: " + file.getName() + '\n');
            }
            text.append("Product: " + entry.getName() + '\n');
            text.append('\n');

            text.append("Mission: " + entry.getMission() + '\n');
            text.append("Mode: " + entry.getAcquisitionMode() + '\n');
            text.append("Type: " + entry.getProductType() + '\n');

            final MetadataElement absRoot = entry.getMetadata();
            if(absRoot != null) {
                final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE, AbstractMetadata.NO_METADATA_STRING);
                final ProductData.UTC acqTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
                final String pass = absRoot.getAttributeString(AbstractMetadata.PASS, AbstractMetadata.NO_METADATA_STRING);
                final int absOrbit = absRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT, AbstractMetadata.NO_METADATA);
                final int relOrbit = absRoot.getAttributeInt(AbstractMetadata.REL_ORBIT, AbstractMetadata.NO_METADATA);
                final String map = absRoot.getAttributeString(AbstractMetadata.map_projection, AbstractMetadata.NO_METADATA_STRING).trim();
                final int cal = absRoot.getAttributeInt(AbstractMetadata.abs_calibration_flag, AbstractMetadata.NO_METADATA);
                final int tc = absRoot.getAttributeInt(AbstractMetadata.is_terrain_corrected, AbstractMetadata.NO_METADATA);
                final int ml = absRoot.getAttributeInt(AbstractMetadata.multilook_flag, AbstractMetadata.NO_METADATA);
                final int coreg = absRoot.getAttributeInt(AbstractMetadata.coregistered_stack, AbstractMetadata.NO_METADATA);

                text.append("Date: " + acqTime.format() + '\n');
                text.append("Sample: " + sampleType + '\n');

                text.append("Pass: " + pass + '\n');
                text.append("Orbit: " + absOrbit);
                if (relOrbit != AbstractMetadata.NO_METADATA)
                    text.append("  Track: " + relOrbit);
                text.append('\n');
                text.append("Size: " + getSizeString(entry.getFileSize()) + '\n');
                if (!map.isEmpty()) {
                    text.append(map + '\n');
                }
                if (cal == 1) {
                    text.append("Calibrated ");
                }
                if (ml == 1) {
                    text.append("Multilooked ");
                }
                if (coreg == 1) {
                    text.append("Coregistered ");
                }
                if (tc == 1) {
                    text.append("Terrain Corrected ");
                }
            } else {
                text.append("Date: " + entry.getFirstLineTime() + '\n');
                text.append("Size: " + entry.getFileSizeString() + '\n');
            }

            productText.setText(text.toString());
        } else if (selections != null && selections.length > 1) {
            long totalSize = 0;
            for (ProductEntry entry : selections) {
                totalSize += entry.getFileSize();
            }

            String text = (selections.length + " products\n") + "Total: " + getSizeString(totalSize);

            productText.setText(text);
        } else {
            productText.setText("");
        }
    }

    private String getSizeString(long bytes) {
        double mb = bytes / MB;
        String unit;
        double value;
        if (mb > TB) {
            value = mb / TB;
            unit = "TB";
        } else if (mb > GB) {
            value = mb / GB;
            unit = "GB";
        } else {
            value = mb;
            unit = "MB";
        }
        return df.format(value) + ' ' + unit;
    }
}
