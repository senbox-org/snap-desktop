/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.db.DBQuery;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.WorldMapUI;
import org.esa.snap.graphbuilder.rcp.progress.LabelBarProgressMonitor;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.dialogs.CheckListDialog;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ListView;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ProductEntryList;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ProductEntryTable;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ThumbnailView;
import org.esa.snap.productlibrary.rcp.toolviews.model.DatabaseStatistics;
import org.esa.snap.productlibrary.rcp.toolviews.model.ProductLibraryConfig;
import org.esa.snap.productlibrary.rcp.toolviews.model.SortingDecorator;
import org.esa.snap.productlibrary.rcp.toolviews.timeline.TimelinePanel;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.quicklooks.ThumbnailPanel;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

@TopComponent.Description(
        preferredID = "ProductLibraryTopComponent",
        iconBase = "org/esa/snap/productlibrary/icons/search.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "rightSlidingSide",
        openAtStartup = true,
        position = 0
)
@ActionID(category = "Window", id = "org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryToolView")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Menu/File", position = 15)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProductLibraryTopComponentName",
        preferredID = "ProductLibraryTopComponent"
)
@NbBundle.Messages({
        "CTL_ProductLibraryTopComponentName=Product Library",
        "CTL_ProductLibraryTopComponentDescription=Product Library",
})
public class ProductLibraryToolView extends ToolTopComponent implements LabelBarProgressMonitor.ProgressBarListener,
        DatabasePane.DatabaseQueryListener, WorldMapUI.WorldMapUIListener, ListView.ListViewListener, ProductLibraryActions.ProductLibraryActionListener {

    private static ImageIcon updateIcon, updateRolloverIcon;
    private static ImageIcon stopIcon, stopRolloverIcon;
    private static ImageIcon addButtonIcon, removeButtonIcon;
    private static ImageIcon listViewButtonIcon, tableViewButtonIcon, thumbnailViewButtonIcon;
    private static ImageIcon helpButtonIcon;

    private final static String LAST_ERROR_OUTPUT_DIR_KEY = "snap.lastErrorOutputDir";

    private JPanel mainPanel;
    private JComboBox repositoryListCombo;
    private ProductEntryTable productEntryTable;
    private ProductEntryList productEntryList;
    private ThumbnailView thumbnailView;
    private ListView currentListView;

    private JLabel statusLabel;
    private JPanel progressPanel;
    private JScrollPane listViewPane, tableViewPane, thumbnailPane;
    private JSplitPane splitPaneV;
    private JButton addButton, removeButton, viewButton, updateButton;

    private LabelBarProgressMonitor progMon;
    private JProgressBar progressBar;
    private ProductLibraryConfig libConfig;
    private static final String helpId = "productLibrary";

    private WorldMapUI worldMapUI = null;
    private DatabasePane dbPane;
    private ProductLibraryActions productLibraryActions;

    private boolean initialized = false;

    public ProductLibraryToolView() {
        setDisplayName("Product Library");
    }

    protected void componentShowing() {
        if(!initialized) {
            initialize();
        }
    }

    protected void componentHidden() {
        currentListView.setProductEntryList(new ProductEntry[] {});
    }

    protected void componentDeactivated() {
        currentListView.setProductEntryList(new ProductEntry[] {});
    }

    private synchronized void initialize() {
        initDatabase();
        initUI();
        dbPane.getDB();

        initialized = true;
    }

    private void initDatabase() {
        libConfig = new ProductLibraryConfig(SnapApp.getDefault().getPreferences());

        dbPane = new DatabasePane();
        dbPane.addListener(this);

        productLibraryActions = new ProductLibraryActions(this);
        productLibraryActions.addListener(this);

        productEntryTable = new ProductEntryTable(productLibraryActions);
        productEntryTable.addListener(this);
        productEntryList = new ProductEntryList(productLibraryActions);
        productEntryList.addListener(this);
        thumbnailView  = new ThumbnailView(productLibraryActions);
        thumbnailView.addListener(this);

        currentListView = productEntryTable;
    }

    private static void loadIcons() {
        updateIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/refresh24.png", ProductLibraryToolView.class);
        updateRolloverIcon = ToolButtonFactory.createRolloverIcon(updateIcon);
        stopIcon = UIUtils.loadImageIcon("icons/Stop24.gif");
        stopRolloverIcon = ToolButtonFactory.createRolloverIcon(stopIcon);
        addButtonIcon = UIUtils.loadImageIcon("icons/Plus24.gif");
        removeButtonIcon = UIUtils.loadImageIcon("icons/Minus24.gif");
        listViewButtonIcon = UIUtils.loadImageIcon("/org/esa/snap/rcp/icons/view_list24.png", ThumbnailPanel.class);
        tableViewButtonIcon = UIUtils.loadImageIcon("/org/esa/snap/rcp/icons/view_table24.png", ThumbnailPanel.class);
        thumbnailViewButtonIcon = UIUtils.loadImageIcon("/org/esa/snap/rcp/icons/view_thumbnails24.png", ThumbnailPanel.class);
        helpButtonIcon = UIUtils.loadImageIcon("icons/Help24.gif");
    }

    public void initUI() {

        loadIcons();

        final JPanel northPanel = createHeaderPanel();
        final JPanel centrePanel = createCentrePanel();
        final JPanel southPanel = createStatusPanel();

        final DatabaseStatistics stats = new DatabaseStatistics(dbPane);
        final TimelinePanel timeLinePanel = new TimelinePanel(stats);
        dbPane.addListener(timeLinePanel);
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                    centrePanel, timeLinePanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.99);

        mainPanel = new JPanel(new BorderLayout(4, 4));
        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        mainPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentHidden(final ComponentEvent e) {
                if (progMon != null) {
                    progMon.setCanceled(true);
                }
            }
        });
        applyConfig(libConfig);
        mainPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentMoved(final ComponentEvent e) {
                libConfig.setWindowBounds(e.getComponent().getBounds());
            }

            @Override
            public void componentResized(final ComponentEvent e) {
                libConfig.setWindowBounds(e.getComponent().getBounds());
            }
        });
        setUIComponentsEnabled(repositoryListCombo.getItemCount() > 1);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        final JPanel headerBar = new JPanel();
        headerBar.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        updateButton = DialogUtils.createButton("updateButton", "Rescan folder", updateIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        updateButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
        updateButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals("stop")) {
                    updateButton.setEnabled(false);
                    mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    if(progMon != null) {
                        progMon.setCanceled(true);
                    }
                } else {
                    final RescanOptions dlg = new RescanOptions();
                    dlg.show();
                    if(dlg.IsOK()) {
                        DBScanner.Options options = new DBScanner.Options(dlg.shouldDoRecusive(),
                                dlg.shouldValidateZips(),
                                dlg.shouldDoQuicklooks());
                        rescanFolder(options);
                    }
                }
            }
        });
        headerBar.add(updateButton, gbc);

        headerBar.add(new JLabel("Folder:")); /* I18N */
        gbc.weightx = 99;
        repositoryListCombo = new JComboBox();
        repositoryListCombo.setName(getClass().getName() + "repositoryListCombo");
        repositoryListCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    final Object selectedItem = repositoryListCombo.getSelectedItem();
                    if (selectedItem instanceof File) {
                        dbPane.setBaseDir((File) selectedItem);
                    } else {
                        dbPane.setBaseDir(null);
                    }
                }
            }
        });
        headerBar.add(repositoryListCombo, gbc);
        gbc.weightx = 0;

        addButton = DialogUtils.createButton("addButton", "Add folder", addButtonIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                addRepository();
            }
        });
        headerBar.add(addButton, gbc);

        removeButton = DialogUtils.createButton("removeButton", "Remove folder", removeButtonIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                removeRepository();
            }
        });
        headerBar.add(removeButton, gbc);

        viewButton = DialogUtils.createButton("viewButton", "Change View", thumbnailViewButtonIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        viewButton.addActionListener(new ActionListener() {
            public synchronized void actionPerformed(final ActionEvent e) {
                currentListView.setProductEntryList(new ProductEntry[] {}); // force to empty

                if(currentListView instanceof ProductEntryList) {
                    currentListView = productEntryTable;
                    viewButton.setIcon(thumbnailViewButtonIcon);
                    viewButton.setRolloverIcon(thumbnailViewButtonIcon);
                    splitPaneV.setLeftComponent(tableViewPane);
                } else if(currentListView instanceof ProductEntryTable) {
                    currentListView = thumbnailView;
                    viewButton.setIcon(listViewButtonIcon);
                    viewButton.setRolloverIcon(listViewButtonIcon);
                    splitPaneV.setLeftComponent(thumbnailPane);
                } else if(currentListView instanceof ThumbnailView) {
                    currentListView = productEntryList;
                    viewButton.setIcon(tableViewButtonIcon);
                    viewButton.setRolloverIcon(tableViewButtonIcon);
                    splitPaneV.setLeftComponent(listViewPane);
                }
                notifyNewEntryListAvailable();
            }
        });
        headerBar.add(viewButton, gbc);

        //final JButton helpButton = DialogUtils.createButton("helpButton", "Help", helpButtonIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        //HelpSys.enableHelpOnButton(helpButton, helpId);
        //headerBar.add(helpButton, gbc);

        return headerBar;
    }

    private JPanel createStatusPanel() {

        final JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        statusLabel = new JLabel("");
        statusLabel.setMinimumSize(new Dimension(100, 10));
        southPanel.add(statusLabel, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setName(getClass().getName() + "progressBar");
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.setVisible(false);
        southPanel.add(progressPanel, BorderLayout.EAST);

        return southPanel;
    }

    private JPanel createCentrePanel() {

        final JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setMinimumSize(new Dimension(200, 577));
        leftPanel.add(dbPane, BorderLayout.NORTH);

        final JPanel commandPanel = productLibraryActions.createCommandPanel();

        listViewPane = new JScrollPane(productEntryList);
        listViewPane.setMinimumSize(new Dimension(400, 400));
        tableViewPane = new JScrollPane(productEntryTable);
        tableViewPane.setMinimumSize(new Dimension(400, 400));
        thumbnailPane = new JScrollPane(thumbnailView);
        thumbnailPane.setMinimumSize(new Dimension(400, 400));
        thumbnailPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        worldMapUI = new WorldMapUI();
        worldMapUI.addListener(this);

        splitPaneV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableViewPane, worldMapUI.getWorlMapPane());
        splitPaneV.setOneTouchExpandable(true);
        splitPaneV.setResizeWeight(0.8);

        final JPanel centrePanel = new JPanel(new BorderLayout());
        centrePanel.add(leftPanel, BorderLayout.WEST);
        centrePanel.add(splitPaneV, BorderLayout.CENTER);
        centrePanel.add(commandPanel, BorderLayout.EAST);

        return centrePanel;
    }

    private void applyConfig(final ProductLibraryConfig config) {
        final File[] baseDirList = config.getBaseDirs();
        repositoryListCombo.insertItemAt(DBQuery.ALL_FOLDERS, 0);
        for (File f : baseDirList) {
            repositoryListCombo.insertItemAt(f, repositoryListCombo.getItemCount());
        }
        if (baseDirList.length > 0)
            repositoryListCombo.setSelectedIndex(0);
    }

    private void addRepository() {
        final File baseDir = productLibraryActions.promptForRepositoryBaseDir();
        if (baseDir == null) {
            return;
        }

        final Map<String, Boolean> checkBoxMap = new HashMap<>(3);
        checkBoxMap.put("Generate quicklooks?", false);
        checkBoxMap.put("Search folder recursively?", true);

        final RescanOptions dlg = new RescanOptions();
        dlg.show();

        if(dlg.IsOK()) {
            libConfig.addBaseDir(baseDir);
            final int index = repositoryListCombo.getItemCount();
            repositoryListCombo.insertItemAt(baseDir, index);
            setUIComponentsEnabled(repositoryListCombo.getItemCount() > 1);

            DBScanner.Options options = new DBScanner.Options(dlg.shouldDoRecusive(),
                    dlg.shouldValidateZips(),
                    dlg.shouldDoQuicklooks());
            updateRepostitory(baseDir, options);
        }
    }

    LabelBarProgressMonitor createLabelBarProgressMonitor() {
        if(progMon == null) {
            progMon = new LabelBarProgressMonitor(progressBar, statusLabel);
            progMon.addListener(this);
        }
        return progMon;
    }

    public File[] getSelectedFiles() {
        return currentListView.getSelectedFiles();
    }

    public ProductEntry[] getSelectedProductEntries() {
        return currentListView.getSelectedProductEntries();
    }

    public ProductEntry getEntryOverMouse() {
        return currentListView.getEntryOverMouse();
    }

    public void sort(final SortingDecorator.SORT_BY sortBy) {
        currentListView.sort(sortBy);
    }

    public void selectAll() {
        currentListView.selectAll();
        notifySelectionChanged();
    }

    public void selectNone() {
        currentListView.clearSelection();
        notifySelectionChanged();
    }

    private synchronized void updateRepostitory(final File baseDir, final DBScanner.Options options) {
        if (baseDir == null) return;
        progMon = createLabelBarProgressMonitor();
        final DBScanner scanner = new DBScanner(dbPane.getDB(), baseDir, options, progMon);
        scanner.addListener(new MyDatabaseScannerListener());
        scanner.execute();
    }

    private synchronized void removeProducts(final File baseDir) {
        progMon = createLabelBarProgressMonitor();
        final DBRemover remover = new DBRemover(dbPane.getDB(), baseDir, progMon);
        remover.addListener(new MyDatabaseRemoverListener());
        remover.execute();
    }

    private void removeRepository() {

        final Object selectedItem = repositoryListCombo.getSelectedItem();
        final int index = repositoryListCombo.getSelectedIndex();
        if (index == 0) {
            final Dialogs.Answer status = Dialogs.requestDecision("Remove folders",
                                                              "This will remove all folders and products from the database.\n" +
                                                                      "Are you sure you wish to continue?", true, null);
            if (status == Dialogs.Answer.YES) {

                while (repositoryListCombo.getItemCount() > 1) {
                    final File baseDir = (File) repositoryListCombo.getItemAt(1);
                    libConfig.removeBaseDir(baseDir);
                    repositoryListCombo.removeItemAt(1);
                }
                removeProducts(null); // remove all
                UpdateUI();
            }
        } else if (selectedItem instanceof File) {
            final File baseDir = (File) selectedItem;
            final Dialogs.Answer status = Dialogs.requestDecision("Remove products",
                                                           "This will remove all products within " +
                                                                   baseDir.getAbsolutePath() + " from the database\n" +
                                                                   "Are you sure you wish to continue?", true, null);
            if (status == Dialogs.Answer.YES) {
                libConfig.removeBaseDir(baseDir);
                repositoryListCombo.removeItemAt(index);
                removeProducts(baseDir);
                UpdateUI();
            }
        }
    }

    private void setUIComponentsEnabled(final boolean enable) {
        removeButton.setEnabled(enable);
        updateButton.setEnabled(enable);
        repositoryListCombo.setEnabled(enable);
    }

    private void toggleUpdateButton(final String command) {
        if (command.equals(LabelBarProgressMonitor.stopCommand)) {
            updateButton.setIcon(stopIcon);
            updateButton.setRolloverIcon(stopRolloverIcon);
            updateButton.setActionCommand(LabelBarProgressMonitor.stopCommand);
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
        } else {
            updateButton.setIcon(updateIcon);
            updateButton.setRolloverIcon(updateRolloverIcon);
            updateButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
            addButton.setEnabled(true);
            removeButton.setEnabled(true);
        }
    }

    public void UpdateUI() {
        dbPane.refresh();
        currentListView.updateUI();
    }

    public void findSlices(int dataTakeId) {
        dbPane.findSlices(dataTakeId);
    }

    private void rescanFolder(final DBScanner.Options options) {
        if (repositoryListCombo.getSelectedIndex() != 0) {
            updateRepostitory((File) repositoryListCombo.getSelectedItem(), options);
        } else {
            final File[] baseDirList = libConfig.getBaseDirs();
            for (File f : baseDirList) {
                updateRepostitory(f, options);
            }
        }
    }

    private void updateStatusLabel() {
        String selectedText = "";
        final int selectedCount = currentListView.getSelectionCount();

        if (selectedCount > 0) {
            selectedText = ", " + selectedCount + " Selected";
        } else {
            dbPane.updateProductSelectionText(null);
        }
        statusLabel.setText(currentListView.getTotalCount() + " Products" + selectedText);
    }

    public void ShowRepository(final ProductEntry[] productEntryList) {
        currentListView.setProductEntryList(productEntryList);
        notifySelectionChanged();

        final GeoPos[][] geoBoundaries = new GeoPos[productEntryList.length][4];
        int i = 0;
        for (ProductEntry entry : productEntryList) {
            geoBoundaries[i++] = entry.getGeoBoundary();
        }
        worldMapUI.setAdditionalGeoBoundaries(geoBoundaries);
        worldMapUI.setSelectedGeoBoundaries(null);
    }

    public static void handleErrorList(final java.util.List<DBScanner.ErrorFile> errorList) {
        final StringBuilder str = new StringBuilder();
        int cnt = 1;
        for (DBScanner.ErrorFile err : errorList) {
            str.append(err.message);
            str.append("   ");
            str.append(err.file.getAbsolutePath());
            str.append('\n');
            if (cnt >= 20) {
                str.append("plus " + (errorList.size() - 20) + " other errors...\n");
                break;
            }
            ++cnt;
        }
        final String question = "\nWould you like to save the list to a text file?";
        if (Dialogs.requestDecision("Product Errors",
                                         "The follow files have errors:\n" + str.toString() + question,
                                    false, null) == Dialogs.Answer.YES) {

            File file = Dialogs.requestFileForSave("Save as...", false,
                                                   new SnapFileFilter("Text File", new String[]{".txt"}, "Text File"),
                                                   ".txt", "ProductErrorList", null, LAST_ERROR_OUTPUT_DIR_KEY);
            try {
                writeErrors(errorList, file);
            } catch (Exception e) {
                Dialogs.showError("Unable to save to " + file.getAbsolutePath());
            }
            if (Desktop.isDesktopSupported() && file.exists()) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception e) {
                    SystemUtils.LOG.warning("Unable to open error file: " + e.getMessage());
                }
            }
        }
    }

    private static void writeErrors(final java.util.List<DBScanner.ErrorFile> errorList, final File file) throws Exception {
        if (file == null) return;

        PrintStream p = null; // declare a print stream object
        try {
            final FileOutputStream out = new FileOutputStream(file.getAbsolutePath());
            // Connect print stream to the output stream
            p = new PrintStream(out);

            for (DBScanner.ErrorFile err : errorList) {
                p.println(err.message + "   " + err.file.getAbsolutePath());
            }
        } finally {
            if (p != null)
                p.close();
        }
    }

    public void notifyProgressStart() {
        progressPanel.setVisible(true);
        toggleUpdateButton(LabelBarProgressMonitor.stopCommand);
    }

    public void notifyProgressDone() {
        progressPanel.setVisible(false);
        toggleUpdateButton(LabelBarProgressMonitor.updateCommand);
        updateButton.setEnabled(true);
        mainPanel.setCursor(Cursor.getDefaultCursor());
    }

    public void notifyNewEntryListAvailable() {
        ShowRepository(dbPane.getProductEntryList());
    }

    public void notifyNewMapSelectionAvailable() {
        dbPane.setSelectionRect(worldMapUI.getSelectionBox());
    }

    public void notifyDirectoryChanged() {
        rescanFolder(new DBScanner.Options(true, false, false));
        UpdateUI();
    }

    public void notifySelectionChanged() {
        updateStatusLabel();
        final ProductEntry[] selections = getSelectedProductEntries();
        productLibraryActions.selectionChanged(selections);

        productLibraryActions.updateContextMenu(selections);
        dbPane.updateProductSelectionText(selections);

        if(selections != null) {
            final GeoPos[][] geoBoundaries = new GeoPos[selections.length][4];
            int i = 0;
            for (ProductEntry entry : selections) {
                geoBoundaries[i++] = entry.getGeoBoundary();
            }
            worldMapUI.setSelectedGeoBoundaries(geoBoundaries);
        } else {
            worldMapUI.setSelectedGeoBoundaries(null);
        }
    }

    public void notifyOpenAction() {
        productLibraryActions.performOpenAction();
    }

    private class MyDatabaseScannerListener implements DBScanner.DBScannerListener {

        public void notifyMSG(final DBScanner dbScanner, final MSG msg) {
            if (msg.equals(DBScanner.DBScannerListener.MSG.DONE)) {
                final java.util.List<DBScanner.ErrorFile> errorList = dbScanner.getErrorList();
                if (!errorList.isEmpty()) {
                    handleErrorList(errorList);
                }
            }
            UpdateUI();
        }
    }

    private class MyDatabaseRemoverListener implements DBRemover.DBRemoverListener {

        public void notifyMSG(final MSG msg) {
            if (msg.equals(DBRemover.DBRemoverListener.MSG.DONE)) {
                setUIComponentsEnabled(repositoryListCombo.getItemCount() > 1);
                UpdateUI();
            }
        }
    }

    private static class RescanOptions extends CheckListDialog {
        private final static String TITLE = "Scan Folder Options";
        private final static String SEARCH_RECURSIVELY = "Search folder recursively?";
        private final static String VERIFY_ZIP_FILES = "Test zip files for errors?";
        private final static String GENERATE_QUICKLOOKS = "Generate quicklooks?";

        RescanOptions() {
            super(TITLE);
        }

        @Override
        protected void initContent() {
            items.put(SEARCH_RECURSIVELY, true);
            items.put(VERIFY_ZIP_FILES, false);
            items.put(GENERATE_QUICKLOOKS, false);

            super.initContent();
        }

        public boolean shouldDoRecusive() {
            return items.get(SEARCH_RECURSIVELY);
        }

        public boolean shouldValidateZips() {
            return items.get(VERIFY_ZIP_FILES);
        }

        public boolean shouldDoQuicklooks() {
            return items.get(GENERATE_QUICKLOOKS);
        }
    }
}
