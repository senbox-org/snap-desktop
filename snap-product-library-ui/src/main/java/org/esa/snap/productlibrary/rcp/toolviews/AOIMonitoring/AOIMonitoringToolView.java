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


import com.jidesoft.swing.JideSplitPane;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.productlibrary.db.GeoPosList;
import org.esa.snap.productlibrary.db.ProductDB;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.WorldMapUI;
import org.esa.snap.graphbuilder.rcp.dialogs.BatchGraphDialog;
import org.esa.snap.graphbuilder.rcp.progress.LabelBarProgressMonitor;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOI;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOIManager;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOISortingDecorator;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOITableModel;
import org.esa.snap.productlibrary.rcp.toolviews.DBScanner;
import org.esa.snap.productlibrary.rcp.toolviews.model.ProductLibraryConfig;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;


@TopComponent.Description(
        preferredID = "AOIMonitoringTopComponent",
        iconBase = "org/esa/snap/productlibrary/icons/aoi.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "rightSlidingSide",
        openAtStartup = false,
        position = 4
)
@ActionID(category = "Window", id = "AOIMonitoringToolView")
@ActionReferences({
        @ActionReference(path = "Menu/Tools", position = 330, separatorAfter = 399),
        @ActionReference(path = "Toolbars/Processing", position = 30)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_AOIMonitoringTopComponentName",
        preferredID = "AOIMonitoringTopComponent"
)
@NbBundle.Messages({
        "CTL_AOIMonitoringTopComponentName=AOI Monitoring",
        "CTL_AOIMonitoringTopComponentDescription=AOI Monitoring Batch Processing",
})
public class AOIMonitoringToolView extends ToolTopComponent {

    private static ImageIcon processIcon, processRolloverIcon;
    private static ImageIcon stopIcon, stopRolloverIcon;
    private static ImageIcon monitorStartIcon, monitorStartRolloverIcon;
    private static ImageIcon monitorStopIcon, monitorStopRolloverIcon;
    private static ImageIcon clearIcon;
    private static ImageIcon helpIcon;

    private final static SnapFileFilter aoiFileFilter = new SnapFileFilter("AOI", new String[]{".xml"}, "SNAP AOI files");

    private JTable aoiTable;
    private JLabel statusLabel;
    private JPanel mainPanel, progressPanel;
    private JButton newButton, addButton, removeButton, processButton, monitorButton, clearButton, helpButton;

    private LabelBarProgressMonitor progMon;
    private JProgressBar progressBar;
    private final ProductLibraryConfig libConfig = new ProductLibraryConfig(SnapApp.getDefault().getPreferences());
    private static final String helpId = "AOIMonitoring";

    private WorldMapUI worldMapUI;

    private final AOIManager aoiManager = new AOIManager();
    private ProductDB db;
    private AOI processingAOI;

    private final AOIMonitor aoiMonitor = new AOIMonitor(this);

    public AOIMonitoringToolView() {
        setLayout(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        setDisplayName("AOI Monitoring");
        add(createControls(), BorderLayout.CENTER);
    }

    private JComponent createControls() {

        loadIcons();
        initUI();
        mainPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentHidden(final ComponentEvent e) {
                if (progMon != null)
                    progMon.setCanceled(true);
            }
        });
        applyConfig();

        return mainPanel;
    }

    private static void loadIcons() {
        processIcon = UIUtils.loadImageIcon("icons/Play24.png");
        processRolloverIcon = ToolButtonFactory.createRolloverIcon(processIcon);
        stopIcon = UIUtils.loadImageIcon("icons/Stop24.gif");
        stopRolloverIcon = ToolButtonFactory.createRolloverIcon(stopIcon);
        monitorStartIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/run24.png", AOIMonitoringToolView.class);
        monitorStartRolloverIcon = ToolButtonFactory.createRolloverIcon(monitorStartIcon);
        monitorStopIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/run-red24.png", AOIMonitoringToolView.class);
        monitorStopRolloverIcon = ToolButtonFactory.createRolloverIcon(monitorStopIcon);
        clearIcon = TangoIcons.actions_edit_clear(TangoIcons.Res.R22);
        helpIcon = TangoIcons.apps_help_browser(TangoIcons.Res.R22);
    }

    private void initUI() {

        // East Panel
        final JPanel eastPanel = new JPanel(new BorderLayout(4, 4));
        eastPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // South Panel
        final JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        final JPanel openPanel = new JPanel(new BorderLayout(4, 4));
        southPanel.add(openPanel, BorderLayout.WEST);

        statusLabel = new JLabel("");
        southPanel.add(statusLabel, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(progressBar);
        progressPanel.setVisible(false);
        southPanel.add(progressPanel, BorderLayout.EAST);

        mainPanel = new JPanel(new BorderLayout(4, 4));
        mainPanel.add(eastPanel, BorderLayout.EAST);
        mainPanel.add(createCentrePanel(), BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    private JPanel createCentrePanel() {
        final JideSplitPane splitPane1V = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);

        aoiTable = new JTable();
        aoiTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        aoiTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        aoiTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                final int clickCount = e.getClickCount();
                if (clickCount == 2) {
                    performOpenAction();
                } else if (clickCount == 1) {
                    performSelectAction();
                }
            }
        });
        splitPane1V.add(new JScrollPane(aoiTable));

        worldMapUI = new WorldMapUI();
        splitPane1V.add(worldMapUI.getWorlMapPane());

        return splitPane1V;
    }

    private JPanel createHeaderPanel() {
        final JPanel headerBar = new JPanel();
        headerBar.setLayout(new GridLayout(10, 1));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        newButton = createToolButton("newButton", UIUtils.loadImageIcon("icons/New24.gif"));
        newButton.setToolTipText("Create New AOI");
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performNewAction();
            }
        });
        headerBar.add(newButton, gbc);

        addButton = createToolButton("addButton", UIUtils.loadImageIcon("icons/Plus24.gif"));
        addButton.setToolTipText("Add existing AOI");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                addAOI();
            }
        });
        headerBar.add(addButton, gbc);

        removeButton = createToolButton("removeButton", UIUtils.loadImageIcon("icons/Minus24.gif"));
        removeButton.setToolTipText("Remove AOI");
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                removeAOI();
            }
        });
        headerBar.add(removeButton, gbc);

        clearButton = createToolButton("clearButton", clearIcon);
        clearButton.setToolTipText("Clear all AOIs");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                clearAll();
            }
        });
        headerBar.add(clearButton, gbc);

        helpButton = createToolButton("helpButton", helpIcon);
        helpButton.setToolTipText("Show help");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                new HelpCtx(helpId).display();
            }
        });
        headerBar.add(helpButton, gbc);

        processButton = createToolButton("processButton", processIcon);
        processButton.setToolTipText("Process selected AOI");
        processButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
        processButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals(LabelBarProgressMonitor.stopCommand)) {
                    processButton.setEnabled(false);
                    mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    if (progMon != null)
                        progMon.setCanceled(true);
                } else {
                    final int row = aoiTable.getSelectedRow();
                    if (row >= 0) {
                        processingAOI = aoiManager.getAOIAt(row);
                        try {
                            processAOI(processingAOI, true);
                        } catch (Exception ex) {
                            SnapApp.getDefault().handleError("AOI Error", ex);
                        }
                    }
                }
            }
        });
        headerBar.add(processButton, gbc);

        monitorButton = createToolButton("monitorButton", monitorStartIcon);
        monitorButton.setToolTipText("Begin monitoring all AOI folders");
        monitorButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
        monitorButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals(LabelBarProgressMonitor.stopCommand)) {
                    mainPanel.setCursor(Cursor.getDefaultCursor());
                    aoiMonitor.stop();
                    if (progMon != null)
                        progMon.setCanceled(true);
                    toggleMonitorButton(LabelBarProgressMonitor.updateCommand);
                } else {
                    aoiMonitor.start(aoiManager.getAOIList());
                    toggleMonitorButton(LabelBarProgressMonitor.stopCommand);
                }
            }
        });
        headerBar.add(monitorButton, gbc);

        return headerBar;
    }

    private JButton createToolButton(final String name, final ImageIcon icon) {
        JButton button;
        if (icon == null)
            button = new JButton(name);
        else {
            button = (JButton) ToolButtonFactory.createButton(icon, false);
            button.setName(getClass().getName() + name);
        }
        return button;
    }

    private void applyConfig() {
        final File[] baseDirList = aoiManager.getBaseDirs();
        for (File file : baseDirList) {
            if (file.exists()) {
                final AOI aoi = aoiManager.createAOI(file);
            } else {
                aoiManager.removeBaseDir(file);
            }
        }
        UpdateUI();
    }

    private void UpdateUI() {
        ShowAOIs(aoiManager.getAOIList());
        //updateWorldMap();

        final boolean hasAOIs = aoiManager.getAOIList().length > 0;
        clearButton.setEnabled(hasAOIs);
        monitorButton.setEnabled(hasAOIs);
        final AOI[] selectedAOIs = getSelectedAOIs();
        enableOnSelection(selectedAOIs.length > 0);
    }

    AOIManager getAoiManager() {
        return aoiManager;
    }

    ProductDB getProductDatabase() throws Exception {
        if (db == null) {
            db = ProductDB.instance();
        }
        return db;
    }

    private void enableOnSelection(final boolean flag) {
        removeButton.setEnabled(flag);
        processButton.setEnabled(flag);
    }

    private void performSelectAction() {
        updateStatusLabel();

        final AOI[] selectedAOIs = getSelectedAOIs();
        enableOnSelection(selectedAOIs.length > 0);

        final GeoPos[][] geoBoundaries = new GeoPos[selectedAOIs.length][4];
        int i = 0;
        for (GeoPosList aoi : selectedAOIs) {
            geoBoundaries[i++] = aoi.getPoints();
        }
        worldMapUI.setSelectedGeoBoundaries(geoBoundaries);
    }

    private void performNewAction() {
        try {
            final File aoiFile = Dialogs.requestFileForSave("New Area of Interest", false, aoiFileFilter, AOI.EXT,
                                                            aoiManager.getNewAOIFile().getAbsolutePath(), null, AOIManager.LAST_AOI_PATH);
            if (aoiFile != null) {
                final AOI aoi = aoiManager.createAOI(aoiFile);
                final AOIDialog dlg = new AOIDialog(SnapApp.getDefault().getMainFrame(), aoi);
                dlg.show();
                aoiManager.addBaseDir(aoi.getFile());
                UpdateUI();
            }
        } catch (Exception e) {
            Dialogs.showError("Unable to create new AOI: " + e.getMessage());
        }
    }

    private void performOpenAction() {
        try {
            final int row = aoiTable.getSelectedRow();
            if (row >= 0) {
                final AOI aoi = aoiManager.getAOIAt(row);
                final AOIDialog dlg = new AOIDialog(SnapApp.getDefault().getMainFrame(), aoi);
                dlg.show();
                UpdateUI();
                performSelectAction();
            }
        } catch (Exception e) {
            Dialogs.showError("Unable to open AOI: " + e.getMessage());
        }
    }

    private AOI[] getSelectedAOIs() {
        final int[] selectedRows = aoiTable.getSelectedRows();
        final AOI[] selectedEntries = new AOI[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = aoiTable.getValueAt(selectedRows[i], 0);
            if (entry instanceof AOI) {
                selectedEntries[i] = (AOI) entry;
            }
        }
        return selectedEntries;
    }

    private void addAOI() {
        final File aoiFile = Dialogs.requestFileForSave("Add Area of Interest", false, aoiFileFilter, AOI.EXT,
                                                        aoiManager.getAOIFolder().getAbsolutePath(), null, AOIManager.LAST_AOI_PATH);
        if (aoiFile != null) {
            aoiManager.createAOI(aoiFile);
            aoiManager.addBaseDir(aoiFile);
            UpdateUI();
        }
    }

    LabelBarProgressMonitor createNewProgressMonitor() {
        progMon = new LabelBarProgressMonitor(progressBar, statusLabel);
        progMon.addListener(new MyProgressBarListener());
        return progMon;
    }

    private void processAOI(final AOI aoi, final boolean doRecursive) throws Exception {
        progMon = createNewProgressMonitor();
        libConfig.addBaseDir(new File(aoi.getInputFolder()));

        final DBScanner.Options options = new DBScanner.Options(doRecursive, false, false);
        final DBScanner scanner = new DBScanner(getProductDatabase(), new File(aoi.getInputFolder()), options, progMon);
        scanner.addListener(new DatabaseScannerListener(getProductDatabase(), aoi, false, false,
                                                        new MyBatchProcessListener(), aoiManager));
        scanner.execute();
    }

    private void removeAOI() {
        final int row = aoiTable.getSelectedRow();
        if (row >= 0) {
            final AOI aoi = aoiManager.getAOIAt(row);
            aoiManager.removeBaseDir(aoi.getFile());
            aoiManager.removeAOI(aoi);
            UpdateUI();
        }
    }

    private void clearAll() {
        final AOI[] aoiList = aoiManager.getAOIList();
        for (AOI aoi : aoiList) {
            aoiManager.removeBaseDir(aoi.getFile());
            aoiManager.removeAOI(aoi);
        }
        UpdateUI();
    }

    private void toggleProcessButton(final String command) {
        if (command.equals(LabelBarProgressMonitor.stopCommand)) {
            processButton.setIcon(stopIcon);
            processButton.setRolloverIcon(stopRolloverIcon);
            processButton.setActionCommand(LabelBarProgressMonitor.stopCommand);
            processButton.setToolTipText("Stop processing");
            newButton.setEnabled(false);
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
            clearButton.setEnabled(false);
            monitorButton.setEnabled(false);
        } else {
            processButton.setIcon(processIcon);
            processButton.setRolloverIcon(processRolloverIcon);
            processButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
            processButton.setToolTipText("Process selected AOI");
            newButton.setEnabled(true);
            addButton.setEnabled(true);
            UpdateUI();
        }
    }

    private void toggleMonitorButton(final String command) {
        if (command.equals(LabelBarProgressMonitor.stopCommand)) {
            monitorButton.setIcon(monitorStopIcon);
            monitorButton.setRolloverIcon(monitorStopRolloverIcon);
            monitorButton.setActionCommand(LabelBarProgressMonitor.stopCommand);
            monitorButton.setToolTipText("Stop monitoring AOIs");
            newButton.setEnabled(false);
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
            clearButton.setEnabled(false);
            processButton.setEnabled(false);
        } else {
            monitorButton.setIcon(monitorStartIcon);
            monitorButton.setRolloverIcon(monitorStartRolloverIcon);
            monitorButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
            monitorButton.setToolTipText("Begin monitoring all AOI folders");
            newButton.setEnabled(true);
            addButton.setEnabled(true);
            UpdateUI();
        }
    }

    private void updateStatusLabel() {
        //todo
        //   String selectedText = "";
        //   final int selecteRows = aoiTable.getSelectedRowCount();
        //   if(selecteRows >= 0)
        //       selectedText = ", "+selecteRows+" Selected";
        //   statusLabel.setText(aoiTable.getRowCount() + " Products"+ selectedText);
    }

    private void ShowAOIs(final AOI[] aoiList) {
        final AOITableModel tableModel = new AOITableModel(aoiList);
        final AOISortingDecorator sortedModel = new AOISortingDecorator(tableModel, aoiTable.getTableHeader());
        aoiTable.setModel(sortedModel);
        aoiTable.setColumnModel(tableModel.getColumnModel());
        updateStatusLabel();

        final GeoPos[][] geoBoundaries = new GeoPos[aoiList.length][4];
        int i = 0;
        for (GeoPosList aoi : aoiList) {
            geoBoundaries[i++] = aoi.getPoints();
        }
        worldMapUI.setAdditionalGeoBoundaries(geoBoundaries);
    }

    private class MyProgressBarListener implements LabelBarProgressMonitor.ProgressBarListener {
        public void notifyProgressStart() {
            progressPanel.setVisible(true);
            if (!aoiMonitor.isStarted())
                toggleProcessButton(LabelBarProgressMonitor.stopCommand);
        }

        public void notifyProgressDone() {
            progressPanel.setVisible(false);
            if (!aoiMonitor.isStarted())
                toggleProcessButton(LabelBarProgressMonitor.updateCommand);
            mainPanel.setCursor(Cursor.getDefaultCursor());
        }
    }

    private class MyBatchProcessListener implements BatchGraphDialog.BatchProcessListener {
        public void notifyMSG(final BatchGraphDialog.BatchProcessListener.BatchMSG msg, final File[] inputFileList, final File[] outputFileList) {
            if (msg.equals(BatchMSG.DONE) && processingAOI != null) {
                for (int i = 0; i < outputFileList.length; ++i) {
                    aoiManager.setBatchProcessResult(processingAOI, inputFileList[i], outputFileList[i]);
                }
                processingAOI = null;
            }
        }

        public void notifyMSG(final BatchMSG msg, final String text) {
            if (msg.equals(BatchMSG.UPDATE)) {

            }
        }
    }
}