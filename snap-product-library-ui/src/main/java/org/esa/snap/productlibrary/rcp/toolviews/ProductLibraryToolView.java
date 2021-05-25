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
import org.esa.snap.graphbuilder.rcp.dialogs.CheckListDialog;
import org.esa.snap.productlibrary.db.DBProductQuery;
import org.esa.snap.productlibrary.db.DBQuery;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.productlibrary.opensearch.CopernicusProductQuery;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.WorldMapUI;
import org.esa.snap.graphbuilder.rcp.progress.LabelBarProgressMonitor;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.extensions.ProductLibraryActionExt;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ListView;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ProductEntryList;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ProductEntryTable;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ThumbnailView;
import org.esa.snap.productlibrary.rcp.toolviews.model.DatabaseStatistics;
import org.esa.snap.productlibrary.rcp.toolviews.model.ProductLibraryConfig;
import org.esa.snap.productlibrary.rcp.toolviews.model.repositories.FolderRepository;
import org.esa.snap.productlibrary.rcp.toolviews.model.repositories.RepositoryInterface;
import org.esa.snap.productlibrary.rcp.toolviews.model.repositories.ScihubRepository;
import org.esa.snap.productlibrary.rcp.toolviews.support.ComboCellRenderer;
import org.esa.snap.productlibrary.rcp.toolviews.support.SortingDecorator;
import org.esa.snap.productlibrary.rcp.toolviews.timeline.TimelinePanel;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.PackageDefaults;
import org.esa.snap.ui.UIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/* DISABLE old Product Library & AOI Monitoring GUI
@TopComponent.Description(
        preferredID = "ProductLibraryTopComponent",
        iconBase = "org/esa/snap/productlibrary/icons/search.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = PackageDefaults.PRODUCT_LIBRARY_MODE,
        openAtStartup = PackageDefaults.PRODUCT_LIBRARY_OPEN,
        position = PackageDefaults.PRODUCT_LIBRARY_POSITION
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
        "CTL_ProductLibraryTopComponentName=" + PackageDefaults.PRODUCT_LIBRARY_NAME,
        "CTL_ProductLibraryTopComponentDescription=Product Library",
})
*/
public class ProductLibraryToolView extends ToolTopComponent implements LabelBarProgressMonitor.ProgressBarListener,
        DatabasePane.DatabaseQueryListener, WorldMapUI.WorldMapUIListener, ListView.ListViewListener {

    private static ImageIcon updateIcon, searchIcon, stopIcon, helpIcon;
    private static ImageIcon addButtonIcon, removeButtonIcon;

    private final static String LAST_ERROR_OUTPUT_DIR_KEY = "snap.lastErrorOutputDir";

    private JPanel mainPanel;
    private JComboBox<RepositoryInterface> repositoryListCombo;
    private ProductEntryTable productEntryTable;
    private ProductEntryList productEntryList;
    private ThumbnailView thumbnailView;
    private ListView currentListView;

    private JLabel statusLabel;
    private JPanel progressPanel;
    private JScrollPane listViewPane, tableViewPane, thumbnailPane;
    private JSplitPane splitPaneV;
    private JButton addButton, removeButton, updateButton, searchButton;
    private final static String RESCAN = "Rescan folder";

    private LabelBarProgressMonitor progMon;
    private JProgressBar progressBar;
    private JButton stopButton;
    private ProductLibraryConfig libConfig;

    private static final String helpId = "productLibrary";

    private WorldMapUI worldMapUI = null;
    private DatabasePane dbPane;
    private ProductLibraryActions productLibraryActions;

    private boolean initialized = false;
    private int repositoryFolderStartIndex;

    public ProductLibraryToolView() {
        setDisplayName("Product Library");
    }

    protected void componentShowing() {
        if (!initialized) {
            initialize();
        }
    }

    /*
    These don't seem to be required for things to work. In fact, componentHidden() and componentDeactivated() are
    emptying the list which requires componentActivated() to repopulate it.

    protected void componentHidden() {
        //System.out.println("ProductLibraryToolView.componentHidden empties currentListView " + currentListView.getClass());
        //currentListView.setProductEntryList(new ProductEntry[]{});
    }

    protected void componentActivated() {
        //System.out.println("ProductLibraryToolView.componentActivated fills currentListView " + currentListView.getClass());
        //showRepository(dbPane.getProductEntryList());
    }

    protected void componentDeactivated() {
        //System.out.println("ProductLibraryToolView.componentDeactivated empties currentListView " + currentListView.getClass());
        //currentListView.setProductEntryList(new ProductEntry[]{});
    }
    */

    private synchronized void initialize() {
        initDatabase();
        initUI();

        initialized = true;
    }

    private void initDatabase() {
        libConfig = new ProductLibraryConfig(SnapApp.getDefault().getPreferences());

        dbPane = new DatabasePane();
        dbPane.addListener(this);
    }

    private static void loadIcons() {
        updateIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/refresh24.png", ProductLibraryToolView.class);
        searchIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/search24.png", ProductLibraryToolView.class);
        stopIcon = UIUtils.loadImageIcon("icons/Stop24.gif");
        addButtonIcon = UIUtils.loadImageIcon("icons/Plus24.gif");
        removeButtonIcon = UIUtils.loadImageIcon("icons/Minus24.gif");
        helpIcon = UIUtils.loadImageIcon("icons/Help24.gif");
    }

    private void initUI() {

        loadIcons();

        final JPanel northPanel = createHeaderPanel();
        final JPanel centrePanel = createCentrePanel();
        final JPanel southPanel = createStatusPanel();

        ProductLibraryActionExt.listenerList.add(new ProductLibraryActionExtListener());

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

        populateRepositoryListCombo(libConfig);

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

        setUIComponentsEnabled();

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public class ProductLibraryActionExtListener implements ProductLibraryActionExt.ActionExtListener {

        public void notifyMSG(ProductLibraryActionExt action, MSG msg) {
            SystemUtils.LOG.info("ProductLibraryTool: got notification MSG " + msg.name() + " " + action.getNewRepoFolder().getName());
            switch (msg) {
                case NEW_REPO:
                    DBScanner.Options options = new DBScanner.Options(false, false, false);
                    addRepository(action.getNewRepoFolder(), options);
                    break;
                default:
                    break;
            }
        }
    }

    private JPanel createHeaderPanel() {
        final JPanel headerBar = new JPanel();
        headerBar.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        updateButton = DialogUtils.createButton("updateButton", RESCAN, updateIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        updateButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
        updateButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals("stop")) {
                    updateButton.setEnabled(false);
                    mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    if (progMon != null) {
                        progMon.setCanceled(true);
                    }
                } else {
                    final RescanOptions dlg = new RescanOptions();
                    dlg.show();
                    if (dlg.IsOK()) {
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

        repositoryListCombo = new JComboBox<>();
        repositoryListCombo.setRenderer(new ComboCellRenderer());

        repositoryListCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    final RepositoryInterface repo = (RepositoryInterface)repositoryListCombo.getSelectedItem();
                    dbPane.setRepository(repo);

                    SystemUtils.LOG.info("ProductLibraryToolView: selected " + repo.getName());

                    setUIComponentsEnabled();
                }
            }
        });
        headerBar.add(repositoryListCombo, gbc);
        gbc.weightx = 0;

        searchButton = DialogUtils.createButton("searchButton", "Apply Search Query", searchIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        searchButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                search();
            }
        });
        headerBar.add(searchButton, gbc);

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

        final JButton helpButton = DialogUtils.createButton("helpButton", "Help", helpIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        HelpCtx.setHelpIDString(helpButton, helpId);
        helpButton.addActionListener(e -> new HelpCtx(helpId).display());
        headerBar.add(helpButton, gbc);

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
        stopButton = DialogUtils.createButton("stopButton", "Stop", stopIcon, progressPanel, DialogUtils.ButtonStyle.Icon);
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (progMon != null) {
                    progMon.setCanceled(true);
                }
            }
        });
        progressPanel.add(stopButton, BorderLayout.EAST);
        progressPanel.setVisible(false);
        southPanel.add(progressPanel, BorderLayout.EAST);

        return southPanel;
    }

    private JPanel createCentrePanel() {

        final JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setMinimumSize(new Dimension(200, 577));
        leftPanel.add(dbPane, BorderLayout.NORTH);

        productLibraryActions = new ProductLibraryActions(this);

        productEntryTable = new ProductEntryTable(productLibraryActions);
        productEntryTable.addListener(this);
        productEntryList = new ProductEntryList(productLibraryActions);
        productEntryList.addListener(this);
        thumbnailView = new ThumbnailView(productLibraryActions);
        thumbnailView.addListener(this);

        currentListView = productEntryTable;

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

    private void populateRepositoryListCombo(final ProductLibraryConfig config) {
        // add default repositories
        repositoryFolderStartIndex = 0;
        repositoryListCombo.insertItemAt(new FolderRepository(DBQuery.ALL_FOLDERS, null), repositoryFolderStartIndex++);
        repositoryListCombo.insertItemAt(new ScihubRepository(), repositoryFolderStartIndex++);

        // add previously added folder repositories
        final File[] baseDirList = config.getBaseDirs();
        for (File f : baseDirList) {
            repositoryListCombo.insertItemAt(new FolderRepository(f.getAbsolutePath(), f), repositoryListCombo.getItemCount());
        }
        repositoryListCombo.setSelectedIndex(0);
    }

    private RepositoryInterface getRepositoryFromListCombo(final File file) {

        int total = repositoryListCombo.getItemCount();

        for (int i = 0; i < total; i++) {
            final RepositoryInterface repo = repositoryListCombo.getItemAt(i);
            if (file.getAbsolutePath().equals(repo.getName())) {
                return repo;
            }
        }

        return null;
    }

    private RepositoryInterface addToRepositoryListCombo(final File baseDir) {
        RepositoryInterface repo = getRepositoryFromListCombo(baseDir);
        if (repo == null) {
            repo = new FolderRepository(baseDir.getAbsolutePath(), baseDir);
            repositoryListCombo.insertItemAt(repo, repositoryListCombo.getItemCount());
        }
        return repo;
    }

    private void addRepository(final File baseDir, final DBScanner.Options options) {

        libConfig.addBaseDir(baseDir);  // verified that it won't be added again if already there
        final RepositoryInterface repo = addToRepositoryListCombo(baseDir);

        setUIComponentsEnabled();

        updateRepostitory(repo, options);  // This is needed
    }

    private void addRepository() {
        final File baseDir = productLibraryActions.promptForRepositoryBaseDir();
        if (baseDir == null) {
            return;
        }

        final RescanOptions dlg = new RescanOptions();
        dlg.show();

        if (dlg.IsOK()) {
            DBScanner.Options options = new DBScanner.Options(dlg.shouldDoRecusive(),
                    dlg.shouldValidateZips(),
                    dlg.shouldDoQuicklooks());
            addRepository(baseDir, options);
        }
    }

    private void removeRepository() {

        final Object selectedItem = repositoryListCombo.getSelectedItem();
        final int index = repositoryListCombo.getSelectedIndex();
        if (index == 0) {
            final Dialogs.Answer status = Dialogs.requestDecision("Remove folders",
                                                                  "This will remove all folders and products from the database.\n" +
                                                                          "Are you sure you wish to continue?", true, null);
            if (status == Dialogs.Answer.YES) {

                while (repositoryListCombo.getItemCount() > repositoryFolderStartIndex) {
                    final FolderRepository repo = (FolderRepository) repositoryListCombo.getItemAt(repositoryFolderStartIndex);
                    libConfig.removeBaseDir(repo.getBaseDir());
                    repositoryListCombo.removeItemAt(repositoryFolderStartIndex);
                }
                removeProducts(null); // remove all
                UpdateUI();
            }
        } else if (selectedItem instanceof FolderRepository) {
            final FolderRepository repo = (FolderRepository) selectedItem;
            final Dialogs.Answer status = Dialogs.requestDecision("Remove products",
                                                                  "This will remove all products within " +
                                                                          repo.getBaseDir().getAbsolutePath() + " from the database\n" +
                                                                          "Are you sure you wish to continue?", true, null);
            if (status == Dialogs.Answer.YES) {
                libConfig.removeBaseDir(repo.getBaseDir());
                repositoryListCombo.removeItemAt(index);
                removeProducts(repo);
                UpdateUI();
            }
        }
    }

    private boolean doRepositoriesExist() {
        return repositoryListCombo.getItemCount() > repositoryFolderStartIndex;
    }

    LabelBarProgressMonitor getLabelBarProgressMonitor() {
        if (progMon == null) {
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

    ProductEntry getEntryOverMouse() {
        return currentListView.getEntryOverMouse();
    }

    public void sort(final SortingDecorator.SORT_BY sortBy) {
        currentListView.sort(sortBy);
    }

    public void selectAll() {
        currentListView.selectAll();
        notifySelectionChanged();
    }

    void selectNone() {
        currentListView.clearSelection();
        notifySelectionChanged();
    }

    public void setSelectedRepositoryToSciHub() {
        for (int i = 0; i < repositoryListCombo.getItemCount(); i++) {
            RepositoryInterface repository = repositoryListCombo.getItemAt(i);
            if (repository instanceof ScihubRepository) {
                repositoryListCombo.setSelectedItem(repository);
            }
        }
    }

    private synchronized void search() {
        progMon = getLabelBarProgressMonitor();
        final DBWorker dbWorker = new DBWorker(DBWorker.TYPE.QUERY, dbPane, progMon);
        dbWorker.addListener(new DBWorkerListener());
        dbWorker.execute();
    }

    private synchronized void updateRepostitory(final RepositoryInterface repo, final DBScanner.Options options) {
        if(repo instanceof FolderRepository) {
            final FolderRepository folderRepo = (FolderRepository) repo;
            if(folderRepo.getBaseDir() == null) {
                return;
            }
            progMon = getLabelBarProgressMonitor();
            final DBScanner scanner = new DBScanner(((DBProductQuery)folderRepo.getProductQueryInterface()).getDB(),
                                                    folderRepo.getBaseDir(), options, progMon);
            scanner.addListener(new MyDatabaseScannerListener(repo));
            scanner.execute();
        }
    }

    private synchronized void removeProducts(final RepositoryInterface repo) {
        progMon = getLabelBarProgressMonitor();
        if (repo == null) {
            // Remove all
            final DBWorker remover = new DBWorker(DBWorker.TYPE.REMOVE,
                    ((DBProductQuery) repositoryListCombo.getItemAt(0).getProductQueryInterface()).getDB(),
                    null, progMon);
            remover.addListener(new DBWorkerListener());
            remover.execute();
        } else if(repo instanceof FolderRepository) {
            final FolderRepository folderRepo = (FolderRepository) repo;
            final DBWorker remover = new DBWorker(DBWorker.TYPE.REMOVE,
                                                  ((DBProductQuery)folderRepo.getProductQueryInterface()).getDB(),
                                                  folderRepo.getBaseDir(), progMon);
            remover.addListener(new DBWorkerListener());
            remover.execute();
        }
    }

    private void setUIComponentsEnabled() {

        final RepositoryInterface repo = (RepositoryInterface)repositoryListCombo.getSelectedItem();

        // "Rescan folder" and "Remove folder" are not applicable for "ESA SciHub" or when there are no
        // other folders.
        final boolean isSciHub = repo.getProductQueryInterface() instanceof CopernicusProductQuery;

        final boolean enable = !isSciHub && doRepositoriesExist();
        updateButton.setEnabled(enable);
        removeButton.setEnabled(enable);

        // "Apply Search Query" should always be sensitive for "ESA SciHub". For other folders, it should
        // be sensitive only if there are folders.
        searchButton.setEnabled(isSciHub || doRepositoriesExist());
    }

    private void UpdateUI() {
        dbPane.refresh();

        final RepositoryInterface repo = (RepositoryInterface)repositoryListCombo.getSelectedItem();
        final boolean isSciHub = repo.getProductQueryInterface() instanceof CopernicusProductQuery;

        // After remove all folders, we stay at All_Folders, so need to clear list and world map
        if (!doRepositoriesExist() && !isSciHub) {

            // clear the list
            currentListView.setProductEntryList(new ProductEntry[]{}); // force to empty

            // clear the world map
            dbPane.setSelectionRect(null);
        }

        currentListView.updateUI();
    }

    public ListView getCurrentListView() {
        return currentListView;
    }

    public synchronized void changeView() {
        //System.out.println("PublicLibraryToolView.changeView empties currentListView " + currentListView.getClass());
        currentListView.setProductEntryList(new ProductEntry[]{}); // force to empty

        if (currentListView instanceof ProductEntryList) {
            currentListView = productEntryTable;
            splitPaneV.setLeftComponent(tableViewPane);
        } else if (currentListView instanceof ProductEntryTable) {
            currentListView = thumbnailView;
            splitPaneV.setLeftComponent(thumbnailPane);
        } else if (currentListView instanceof ThumbnailView) {
            currentListView = productEntryList;
            splitPaneV.setLeftComponent(listViewPane);
        }
        notifyNewEntryListAvailable();
    }

    public void findSlices(int dataTakeId) {
        dbPane.findSlices(dataTakeId);
    }

    private void rescanFolder(final DBScanner.Options options) {
        if (repositoryListCombo.getSelectedIndex() == 0) {
            // Switch to each folder and rescan it
            for (int i = repositoryFolderStartIndex; i < repositoryListCombo.getItemCount(); ++i) {
                updateRepostitory(repositoryListCombo.getItemAt(i), options);
            }
        } else {
            updateRepostitory((RepositoryInterface)repositoryListCombo.getSelectedItem(), options);
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

    private void showRepository(final ProductEntry[] productEntryList) {
        //System.out.println("ProductLibraryToolView.showRepository: productEntryList " + (productEntryList == null ? "is null" : productEntryList.length) + " " + currentListView.getClass());
        if(productEntryList == null)
            return;

        currentListView.setProductEntryList(productEntryList);
        notifySelectionChanged();

        final List<GeoPos[]> geoBoundaryList = new ArrayList<>();
        for (ProductEntry entry : productEntryList) {
            geoBoundaryList.add(entry.getGeoBoundary());
        }
        worldMapUI.setAdditionalGeoBoundaries(geoBoundaryList);
        worldMapUI.setSelectedGeoBoundaries(null);
    }

    private static void handleErrorList(final java.util.List<DBScanner.ErrorFile> errorList) {
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

            final File file = Dialogs.requestFileForSave("Save as...", false,
                    new SnapFileFilter("Text File", new String[]{".txt"}, "Text File"),
                    ".txt", "ProductErrorList", null, LAST_ERROR_OUTPUT_DIR_KEY);
            if (file != null) {
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
            if (p != null) {
                p.close();
            }
        }
    }

    public void notifyProgressStart() {
        if (progMon.isCanceled()) {
            return;
        }
        progressPanel.setVisible(true);
        //toggleUpdateButton(LabelBarProgressMonitor.stopCommand);
    }

    public void notifyProgressDone() {
        progressPanel.setVisible(false);
        //toggleUpdateButton(LabelBarProgressMonitor.updateCommand);
        updateButton.setEnabled(true);
        mainPanel.setCursor(Cursor.getDefaultCursor());
    }

    public void notifyNewEntryListAvailable() {
        showRepository(dbPane.getProductEntryList());
    }

    public void notifyNewMapSelectionAvailable() {
        dbPane.setSelectionRect(worldMapUI.getSelectionBox());
    }

    public void notifySelectionChanged() {
        updateStatusLabel();
        final ProductEntry[] selections = getSelectedProductEntries();
        productLibraryActions.selectionChanged(selections);

        productLibraryActions.updateContextMenu(selections);
        dbPane.updateProductSelectionText(selections);

        if (selections != null) {
            final List<GeoPos[]> geoBoundaryList = new ArrayList<>();
            for (ProductEntry entry : selections) {
                geoBoundaryList.add(entry.getGeoBoundary());
            }
            worldMapUI.setSelectedGeoBoundaries(geoBoundaryList);
        } else {
            worldMapUI.setSelectedGeoBoundaries(null);
        }
    }

    public void notifyOpenAction() {
        productLibraryActions.performOpenAction();
    }

    private class MyDatabaseScannerListener implements DBScanner.DBScannerListener {

        RepositoryInterface repo;

        MyDatabaseScannerListener(final RepositoryInterface repo) {
            this.repo = repo;
        }

        public void notifyMSG(final DBScanner dbScanner, final MSG msg) {
            if (msg.equals(DBScanner.DBScannerListener.MSG.DONE)) {
                final java.util.List<DBScanner.ErrorFile> errorList = dbScanner.getErrorList();
                if (!errorList.isEmpty()) {
                    handleErrorList(errorList);
                }
                //System.out.println("ProductLibraryToolView.MyDatabaseScannerListener: DBScanner DONE");
                repositoryListCombo.setSelectedItem(repo);
            }
            UpdateUI();

            // This will update the product list after rescan
            notifyNewMapSelectionAvailable();
        }
    }

    DBWorker.DBWorkerListener createDBListener() {
        return new DBWorkerListener();
    }

    private class DBWorkerListener implements DBWorker.DBWorkerListener {

        public void notifyMSG(final MSG msg) {
            if (msg.equals(DBWorker.DBWorkerListener.MSG.DONE)) {
                setUIComponentsEnabled();
                UpdateUI();
            }
        }
    }

    ProductFileHandler.ProductFileHandlerListener createProductFileHandlerListener() {
        return new FileHandlerListener();
    }

    private class FileHandlerListener implements ProductFileHandler.ProductFileHandlerListener {

        public void notifyMSG(final ProductFileHandler fileHandler, final MSG msg) {
            if (msg.equals(ProductFileHandler.ProductFileHandlerListener.MSG.DONE)) {
                final java.util.List<DBScanner.ErrorFile> errorList = fileHandler.getErrorList();
                if (!errorList.isEmpty()) {
                    handleErrorList(errorList);
                }
                if (fileHandler.getOperationType().equals(ProductFileHandler.TYPE.MOVE_TO) ||
                        fileHandler.getOperationType().equals(ProductFileHandler.TYPE.DELETE)) {
                    rescanFolder(new DBScanner.Options(true, false, false));
                    UpdateUI();
                }
            }
            UpdateUI();
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

        boolean shouldDoRecusive() {
            return items.get(SEARCH_RECURSIVELY);
        }

        boolean shouldValidateZips() {
            return items.get(VERIFY_ZIP_FILES);
        }

        boolean shouldDoQuicklooks() {
            return items.get(GENERATE_QUICKLOOKS);
        }
    }
}
