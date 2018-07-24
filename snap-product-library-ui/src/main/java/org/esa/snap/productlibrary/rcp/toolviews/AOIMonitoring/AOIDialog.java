/*
 * Copyright (C) 2017 Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring;


import com.jidesoft.swing.FolderChooser;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.productlibrary.db.DBQuery;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.WorldMapUI;
import org.esa.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOI;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOIManager;
import org.esa.snap.productlibrary.rcp.toolviews.DatabasePane;
import org.esa.snap.productlibrary.rcp.toolviews.model.ProductLibraryConfig;
import org.esa.snap.productlibrary.rcp.toolviews.model.repositories.FolderRepository;
import org.esa.snap.productlibrary.rcp.toolviews.model.repositories.RepositoryInterface;
import org.esa.snap.productlibrary.rcp.toolviews.model.repositories.ScihubRepository;
import org.esa.snap.productlibrary.rcp.toolviews.support.ComboCellRenderer;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

/**
 * AOI Dialog for editing AOIs
 */

public class AOIDialog extends ModalDialog {

    private final JTextField nameField = new JTextField("");
    private final JTextField inputFolderField = new JTextField("");
    private final JButton inputFolderButton = new JButton("...");
    private final JTextField outputFolderField = new JTextField("");
    private final JButton outputFolderButton = new JButton("...");
    private final JTextField graphField = new JTextField("");
    private final JButton graphButton = new JButton("...");

    private final JCheckBox pairsCheckBox = new JCheckBox("Find CCD Slaves");
    private final JTextField maxSlavesField = new JTextField("1");
    private JComboBox<RepositoryInterface> repositoryListCombo;

    private DatabasePane dbPane;
    private WorldMapUI worldMapUI;

    private final AOI aoi;

    public AOIDialog(Window parent, AOI theAOI) throws Exception {
        super(parent, "Area of Interest", ModalDialog.ID_OK_CANCEL, null);
        this.aoi = theAOI;

        inputFolderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                final FolderChooser chooser = new FolderChooser();
                chooser.setDialogTitle("Input folder");
                if (aoi.getInputFolder() != null) {
                    File folder = new File(aoi.getInputFolder());
                    if (folder.exists()) {
                        chooser.setSelectedFolder(folder);
                    }
                }
                final Window window = SwingUtilities.getWindowAncestor((JComponent) e.getSource());
                if (chooser.showDialog(window, "Select") == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = chooser.getSelectedFolder();
                    inputFolderField.setText(selectedFolder.getAbsolutePath());
                    AOIManager.setLastInputPath(selectedFolder.getAbsolutePath());
                }
            }
        });
        outputFolderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                final FolderChooser chooser = new FolderChooser();
                chooser.setDialogTitle("Output folder");
                if (aoi.getOutputFolder() != null) {
                    File folder = new File(aoi.getOutputFolder());
                    if (folder.exists()) {
                        chooser.setSelectedFolder(folder);
                    }
                }
                final Window window = SwingUtilities.getWindowAncestor((JComponent) e.getSource());
                if (chooser.showDialog(window, "Select") == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = chooser.getSelectedFolder();
                    outputFolderField.setText(selectedFolder.getAbsolutePath());
                    AOIManager.setLastOutputPath(selectedFolder.getAbsolutePath());
                }
            }
        });
        graphButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                final File file = Dialogs.requestFileForOpen("AOI Processing Graph", false, null, GraphBuilderDialog.LAST_GRAPH_PATH);
                if (file != null) {
                    graphField.setText(file.getAbsolutePath());
                }
            }
        });
        pairsCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final boolean findPairs = e.getStateChange() == ItemEvent.SELECTED;
                dbPane.setVisible(findPairs);
                if (findPairs) {
                    //dbPane.getDB();
                }
                repositoryListCombo.setVisible(findPairs);
                maxSlavesField.setEnabled(findPairs);

                getJDialog().pack();
            }
        });

        initUI();

        populateRepositoryListCombo(new ProductLibraryConfig(SnapApp.getDefault().getPreferences()));
        populateFields();
    }

    private void initUI() throws Exception {
        final JPanel contentPane = new JPanel(new BorderLayout(4, 4));

        final JPanel northPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(northPane, gbc, "Name:", nameField);
        gbc.gridy++;
        DialogUtils.addComponent(northPane, gbc, "Input Folder:", inputFolderField);
        inputFolderField.setColumns(50);
        gbc.gridx = 2;
        northPane.add(inputFolderButton, gbc);
        gbc.gridy++;
        DialogUtils.addComponent(northPane, gbc, "Output Folder:", outputFolderField);
        gbc.gridx = 2;
        northPane.add(outputFolderButton, gbc);
        gbc.gridy++;
        DialogUtils.addComponent(northPane, gbc, "Processing Graph:", graphField);
        gbc.gridx = 2;
        northPane.add(graphButton, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        northPane.add(pairsCheckBox, gbc);
        gbc.gridx = 1;
        final JLabel maxSlavesLabel = new JLabel("Max Number of Slaves:");
        maxSlavesLabel.setHorizontalAlignment(JLabel.RIGHT);
        northPane.add(maxSlavesLabel, gbc);
        gbc.gridx = 2;
        northPane.add(maxSlavesField, gbc);

        repositoryListCombo = new JComboBox<>();
        repositoryListCombo.setRenderer(new ComboCellRenderer());
        repositoryListCombo.setVisible(false);

        repositoryListCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    final RepositoryInterface repo = (RepositoryInterface)repositoryListCombo.getSelectedItem();
                    dbPane.setRepository(repo);
                }
            }
        });
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        northPane.add(repositoryListCombo, gbc);

        contentPane.add(northPane, BorderLayout.NORTH);

        final MyDatabaseQueryListener dbQueryListener = new MyDatabaseQueryListener();
        dbPane = new DatabasePane();
        dbPane.setDBQuery(aoi.getSlaveDBQuery());
        dbPane.addListener(dbQueryListener);
        dbPane.setVisible(false);
        contentPane.add(dbPane, BorderLayout.WEST);

        worldMapUI = new WorldMapUI();
        worldMapUI.addListener(dbQueryListener);
        contentPane.add(worldMapUI.getWorlMapPane(), BorderLayout.EAST);

        setContent(contentPane);
    }

    private void populateRepositoryListCombo(final ProductLibraryConfig config) {
        // add default repositories
        repositoryListCombo.insertItemAt(new FolderRepository(DBQuery.ALL_FOLDERS, null), 0);
        repositoryListCombo.insertItemAt(new ScihubRepository(), 1);

        // add previously added folder repositories
        final File[] baseDirList = config.getBaseDirs();
        for (File f : baseDirList) {
            repositoryListCombo.insertItemAt(new FolderRepository(f.getAbsolutePath(), f), repositoryListCombo.getItemCount());
        }
        if (baseDirList.length > 0) {
            repositoryListCombo.setSelectedIndex(0);
        }
    }

    private void populateFields() {
        nameField.setText(aoi.getName());
        inputFolderField.setText(aoi.getInputFolder());
        outputFolderField.setText(aoi.getOutputFolder());
        graphField.setText(aoi.getProcessingGraph());
        pairsCheckBox.setSelected(aoi.getFindSlaves());
        maxSlavesField.setEnabled(aoi.getFindSlaves());

        final GeoPos[] aoiPoints = aoi.getPoints();
        if (aoiPoints.length > 2) {
            worldMapUI.setSelectionStart(aoiPoints[0].lat, aoiPoints[0].lon);
            worldMapUI.setSelectionEnd(aoiPoints[2].lat, aoiPoints[2].lon);
        }
    }

    private class MyDatabaseQueryListener implements DatabasePane.DatabaseQueryListener, WorldMapUI.WorldMapUIListener {

        public void notifyNewEntryListAvailable() {
            //ShowRepository(dbPane.getProductEntryList());
        }

        public void notifyNewMapSelectionAvailable() {
            aoi.setPoints(worldMapUI.getSelectionBox());
        }
    }

    public static boolean validateFolder(final File file) {
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Dialogs.showError("Unable to create folder\n" + file.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    protected void onOK() {
        try {
            aoi.setName(nameField.getText());
            final File inputFolder = new File(inputFolderField.getText());
            if (!validateFolder(inputFolder))
                return;
            aoi.setInputFolder(inputFolder.getAbsolutePath());
            final File outputFolder = new File(outputFolderField.getText());
            if (!validateFolder(outputFolder))
                return;
            aoi.setOutputFolder(outputFolder.getAbsolutePath());
            final File graph = new File(graphField.getText());
            if (!graph.exists()) {
                Dialogs.showError("Please select a valid graph file\n" + graph.getAbsolutePath() + " does not exist");
                return;
            }
            aoi.setProcessingGraph(graph.getAbsolutePath());
            aoi.setFindSlaves(pairsCheckBox.isSelected());
            aoi.setMaxSlaves(Integer.parseInt(maxSlavesField.getText()));

            aoi.setSlaveDBQuery(dbPane.getDBQuery());

            aoi.save();
        } catch (Throwable t) {
            Dialogs.showError("Unable to save AOI: " + t.getMessage());
        } finally {
            hide();
        }
    }
}