package org.esa.snap.opendap.ui;

import com.jidesoft.swing.FolderChooser;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.SimpleScrollPane;
import org.esa.snap.core.ui.AppContext;
import org.esa.snap.core.ui.GridBagUtils;
import org.esa.snap.core.ui.tool.ToolButtonFactory;
import org.esa.snap.opendap.datamodel.OpendapLeaf;
import org.esa.snap.opendap.utils.OpendapUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.OpenProductAction;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.util.StringUtils;
import org.openide.modules.Places;
import org.openide.util.HelpCtx;
import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvDataset;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class OpendapAccessPanel extends JPanel implements CatalogTree.UIContext {

    private static final Logger LOG = Logger.getLogger(OpendapAccessPanel.class.getName());
    private static final String PROPERTY_KEY_SERVER_URLS = "opendap.server.urls";
    private static final String PROPERTY_KEY_DOWNLOAD_DIR = "opendap.download.dir";
    private final static int DDS_AREA_INDEX = 0;
    private final static int DAS_AREA_INDEX = 1;

    private JComboBox<String> urlField;
    private AbstractButton refreshButton;
    private CatalogTree catalogTree;

    private JTabbedPane metaInfoArea;
    private JCheckBox useDatasetNameFilter;

    private FilterComponent datasetNameFilter;
    private JCheckBox useTimeRangeFilter;

    private FilterComponent timeRangeFilter;
    private JCheckBox useRegionFilter;

    private FilterComponent regionFilter;
    private JCheckBox useVariableFilter;

    private VariableFilter variableFilter;

    private JCheckBox openInVisat;
    private JPanel statusBar;

    private double currentDataSize = 0.0;
    private final Preferences preferences;
    private final String helpId;
    private JTextField folderTextField;
    private JProgressBar progressBar;
    private JLabel preMessageLabel;
    private JLabel postMessageLabel;
    private Map<Integer, JTextArea> textAreas;
    private JButton downloadButton;
    private AppContext appContext;
    private JButton cancelButton;
    private JLabel statusBarMessage;

    public OpendapAccessPanel(AppContext appContext, String helpId) {
        super();
        this.preferences = SnapApp.getDefault().getPreferences();
        this.helpId = helpId;
        this.appContext = appContext;
        initComponents();
        initContentPane();
    }

    private void initComponents() {
        urlField = new JComboBox<>();
        urlField.setEditable(true);
        urlField.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    refreshButton.doClick();
                }
            }
        });
        updateUrlField();
        refreshButton = ToolButtonFactory.createButton(
                TangoIcons.actions_view_refresh(TangoIcons.Res.R22),
                false);
        refreshButton.addActionListener(e -> {
            final boolean usingUrl = refresh();
            if (usingUrl) {
                final String urls = preferences.get(PROPERTY_KEY_SERVER_URLS, "");
                final String currentUrl = urlField.getSelectedItem().toString();
                if (!urls.contains(currentUrl)) {
                    preferences.put(PROPERTY_KEY_SERVER_URLS, urls + "\n" + currentUrl);
                    updateUrlField();
                }
            }
        });
        metaInfoArea = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        JTextArea ddsArea = new JTextArea(10, 40);
        JTextArea dasArea = new JTextArea(10, 40);

        ddsArea.setEditable(false);
        dasArea.setEditable(false);

        textAreas = new HashMap<>();
        textAreas.put(DAS_AREA_INDEX, dasArea);
        textAreas.put(DDS_AREA_INDEX, ddsArea);

        metaInfoArea.addTab("DDS", new JScrollPane(ddsArea));
        metaInfoArea.addTab("DAS", new JScrollPane(dasArea));
        metaInfoArea.setToolTipTextAt(DDS_AREA_INDEX, "Dataset Descriptor Structure: description of dataset variables");
        metaInfoArea.setToolTipTextAt(DAS_AREA_INDEX, "Dataset Attribute Structure: description of dataset attributes");

        metaInfoArea.addChangeListener(e -> {
            if (catalogTree.getSelectedLeaf() != null) {
                setMetadataText(metaInfoArea.getSelectedIndex(), catalogTree.getSelectedLeaf());
            }
        });

        catalogTree = new CatalogTree(new DefaultLeafSelectionListener(), appContext, this);
        useDatasetNameFilter = new JCheckBox("Use dataset name filter");
        useTimeRangeFilter = new JCheckBox("Use time range filter");
        useRegionFilter = new JCheckBox("Use region filter");
        useVariableFilter = new JCheckBox("Use variable name filter");

        DefaultFilterChangeListener filterChangeListener = new DefaultFilterChangeListener();
        datasetNameFilter = new DatasetNameFilter(useDatasetNameFilter);
        datasetNameFilter.addFilterChangeListener(filterChangeListener);
        timeRangeFilter = new TimeRangeFilter(useTimeRangeFilter);
        timeRangeFilter.addFilterChangeListener(filterChangeListener);
        regionFilter = new RegionFilter(useRegionFilter);
        regionFilter.addFilterChangeListener(filterChangeListener);
        variableFilter = new VariableFilter(useVariableFilter, catalogTree);
        variableFilter.addFilterChangeListener(filterChangeListener);

        catalogTree.addCatalogTreeListener(new CatalogTree.CatalogTreeListener() {
            @Override
            public void leafAdded(OpendapLeaf leaf, boolean hasNestedDatasets) {
                if (hasNestedDatasets) {
                    return;
                }
                if (leaf.getDataset().getGeospatialCoverage() != null) {
                    useRegionFilter.setEnabled(true);
                }
                filterLeaf(leaf);
            }

            @Override
            public void catalogElementsInsertionFinished() {
            }
        });

        openInVisat = new JCheckBox("Open in SNAP");
        statusBarMessage = new JLabel("Ready.");
        statusBarMessage.setText("Ready.");
        preMessageLabel = new JLabel();
        postMessageLabel = new JLabel();
        progressBar = new JProgressBar(0, 100);

        statusBar = new JPanel();
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        statusBar.add(statusBarMessage);
        statusBar.add(Box.createHorizontalStrut(4));
        statusBar.add(preMessageLabel);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(progressBar);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(postMessageLabel);

        useRegionFilter.setEnabled(false);
    }


    private void setMetadataText(int componentIndex, OpendapLeaf leaf) {
        String text = null;
        try {
            if (leaf.isDapAccess()) {
                if (metaInfoArea.getSelectedIndex() == DDS_AREA_INDEX) {
                    text = OpendapUtils.getResponse(leaf.getDdsUri());
                } else if (metaInfoArea.getSelectedIndex() == DAS_AREA_INDEX) {
                    text = OpendapUtils.getResponse(leaf.getDasUri());
                }
            } else if (leaf.isFileAccess()) {
                if (metaInfoArea.getSelectedIndex() == DDS_AREA_INDEX) {
                    text = "No DDS information for file '" + leaf.getName() + "'.";
                } else if (metaInfoArea.getSelectedIndex() == DAS_AREA_INDEX) {
                    text = "No DAS information for file '" + leaf.getName() + "'.";
                }
            }
        } catch (IOException e) {
            LOG.warning("Unable to retrieve meta information for file '" + leaf.getName() + "'.");
        }

        setResponseText(componentIndex, text);
    }

    private void setResponseText(int componentIndex, String response) {
        JTextArea textArea = textAreas.get(componentIndex);
        if (response.length() > 100000) {
            response = response.substring(0, 10000) + "\nCut remaining file content";
        }
        textArea.setText(response);
        textArea.setCaretPosition(0);
    }

    @Override
    public void updateStatusBar(String message) {
        statusBarMessage.setText(message);
    }

    private void filterLeaf(OpendapLeaf leaf) {
        if ((!useDatasetNameFilter.isSelected() || datasetNameFilter.accept(leaf)) &&
            (!useTimeRangeFilter.isSelected() || timeRangeFilter.accept(leaf)) &&
            (!useRegionFilter.isSelected() || regionFilter.accept(leaf)) &&
            (!useVariableFilter.isSelected() || variableFilter.accept(leaf))) {
            catalogTree.setLeafVisible(leaf, true);
        } else {
            catalogTree.setLeafVisible(leaf, false);
        }
    }

    private void updateUrlField() {
        final String urlsProperty = preferences.get(PROPERTY_KEY_SERVER_URLS, "");
        final String[] urls = urlsProperty.split("\n");
        for (String url : urls) {
            if (StringUtils.isNotNullAndNotEmpty(url) && !contains(urlField, url)) {
                urlField.addItem(url);
            }
        }
    }

    private static boolean contains(JComboBox<String> urlField, String url) {
        for (int i = 0; i < urlField.getItemCount(); i++) {
            if (urlField.getItemAt(i).equals(url)) {
                return true;
            }
        }
        return false;
    }

    private void initContentPane() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets.right = 5;
        final JPanel urlPanel = new JPanel(layout);
        urlPanel.add(new JLabel("Root URL:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        urlPanel.add(urlField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        urlPanel.add(refreshButton, gbc);
        gbc.gridx = 3;
        gbc.insets.right = 0;
        final AbstractButton helpButton = ToolButtonFactory.createButton(TangoIcons.apps_help_browser(TangoIcons.Res.R22), false);
        helpButton.addActionListener(e -> new HelpCtx(helpId).display());
        urlPanel.add(helpButton, gbc);

        final JPanel variableInfo = new JPanel(new BorderLayout(5, 5));
        variableInfo.setBorder(new EmptyBorder(10, 0, 0, 0));
        variableInfo.add(metaInfoArea, BorderLayout.CENTER);

        final JScrollPane openDapTree = new JScrollPane(catalogTree.getComponent());
        final JSplitPane centerLeftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, openDapTree, variableInfo);
        centerLeftPane.setResizeWeight(1);
        centerLeftPane.setContinuousLayout(true);

        final JPanel filterPanel = new JPanel(new GridBagLayout());
        final JComponent datasetNameFilterUI = datasetNameFilter.getUI();
        final JComponent timeRangeFilterUI = timeRangeFilter.getUI();
        final JComponent regionFilterUI = regionFilter.getUI();
        final JComponent variableFilterUI = variableFilter.getUI();
        GridBagUtils.addToPanel(filterPanel, new TitledPanel(useDatasetNameFilter, datasetNameFilterUI, true, true), gbc,
                                "gridx=0,gridy=0,anchor=NORTHWEST,weightx=1,weighty=0,,fill=BOTH");
        GridBagUtils.addToPanel(filterPanel, new TitledPanel(useTimeRangeFilter, timeRangeFilterUI, true, true), gbc, "gridy=1");
        GridBagUtils.addToPanel(filterPanel, new TitledPanel(useRegionFilter, regionFilterUI, true, true), gbc, "gridy=2");
        GridBagUtils.addToPanel(filterPanel, new TitledPanel(useVariableFilter, variableFilterUI, true, true), gbc, "gridy=3");
        GridBagUtils.addToPanel(filterPanel, new JLabel(), gbc, "gridy=4,weighty=1");
        filterPanel.setPreferredSize(new Dimension(460, 800));
        filterPanel.setMinimumSize(new Dimension(460, 120));
        filterPanel.setMaximumSize(new Dimension(460, 800));

        final JPanel downloadButtonPanel = new JPanel(new BorderLayout(8, 5));
        downloadButtonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        cancelButton = new JButton("Cancel");
        final DownloadProgressBarPM pm = new DownloadProgressBarPM(progressBar, preMessageLabel, postMessageLabel, cancelButton);
        progressBar.setVisible(false);
        File downloadDir = new File(preferences.get(PROPERTY_KEY_DOWNLOAD_DIR, Places.getUserDirectory().getAbsolutePath()));
        if (!downloadDir.isDirectory()) {
            downloadDir = new File(Places.getUserDirectory().getAbsolutePath());
        }
        folderTextField = new JTextField(downloadDir.getAbsolutePath());
        JButton folderChooserButton = new JButton("...");
        folderChooserButton.addActionListener(e -> fetchDownloadDirectory());
        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);
        final DownloadAction downloadAction = createDownloadAction(pm);
        downloadButton.addActionListener(downloadAction);
        downloadButtonPanel.add(openInVisat, BorderLayout.NORTH);
        downloadButtonPanel.add(folderTextField);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(folderChooserButton);
        buttonPanel.add(downloadButton);
        cancelButton.setEnabled(false);
        downloadButton.addActionListener(e -> cancelButton.setEnabled(true));
        cancelButton.addActionListener(e -> {
            downloadAction.cancel();
            cancelButton.setEnabled(false);
        });
        buttonPanel.add(cancelButton);
        downloadButtonPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel centerRightPane = new JPanel(new BorderLayout());
        final SimpleScrollPane simpleScrollPane = new SimpleScrollPane(filterPanel, JideScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                                       JideScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        simpleScrollPane.setBorder(BorderFactory.createEmptyBorder());
        centerRightPane.add(simpleScrollPane, BorderLayout.CENTER);
        centerRightPane.add(downloadButtonPanel, BorderLayout.SOUTH);

        final JSplitPane centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerLeftPane, centerRightPane);
        centerPanel.setResizeWeight(1);
        centerPanel.setContinuousLayout(true);

        this.setLayout(new BorderLayout(15, 15));
        this.setBorder(new EmptyBorder(8, 8, 8, 8));
        this.add(urlPanel, BorderLayout.NORTH);
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(statusBar, BorderLayout.SOUTH);
    }

    private File fetchDownloadDirectory() {
        FolderChooser folderChooser = new FolderChooser();
        folderChooser.setRecentListVisible(false);
        folderChooser.setCurrentDirectory(new File(folderTextField.getText()));
        folderChooser.setNavigationFieldVisible(true);
        folderChooser.setFileHidingEnabled(true);
        int result = folderChooser.showOpenDialog(OpendapAccessPanel.this);
        if (FolderChooser.APPROVE_OPTION == result) {
            String downloadDirPath = folderChooser.getSelectedFile().getAbsolutePath();
            preferences.put(PROPERTY_KEY_DOWNLOAD_DIR, downloadDirPath);
            folderTextField.setText(downloadDirPath);
            return folderChooser.getSelectedFile();
        } else {
            return null;
        }
    }

    private DownloadAction createDownloadAction(DownloadProgressBarPM pm) {
        return new DownloadAction(pm, new ParameterProviderImpl(), new DownloadAction.DownloadHandler() {

            @Override
            public void handleException(Exception e) {
                SnapApp.getDefault().handleError("Unable to perform download. Reason: " + e.getMessage(), e);
            }

            @Override
            public void handleDownloadFinished(File downloadedFile) {
                if (openInVisat.isSelected()) {
                    OpenProductAction openProductAction = new OpenProductAction();
                    openProductAction.setFile(downloadedFile);
                    openProductAction.execute();
                }
            }
        });
    }

    private boolean refresh() {
        String url;
        if (urlField.getSelectedItem() == null) {
            url = urlField.getEditor().getItem().toString();
        } else {
            url = urlField.getSelectedItem().toString();
        }
        url = checkCatalogURLString(url);
        final InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(true);
        final InvCatalog catalog = factory.readXML(url);
        final List<InvDataset> datasets = catalog.getDatasets();

        if (datasets.size() == 0) {
            JOptionPane.showMessageDialog(this, "Cannnot find THREDDS catalog service xml at '" + url + "'");
            return false;
        }
        urlField.setSelectedItem(url);
        catalogTree.setNewRootDatasets(datasets);
        variableFilter.stopFiltering();
        return true;
    }

    private String checkCatalogURLString(String url) {
        if (url.endsWith("catalog.xml")) {
            return url;
        } else if (url.endsWith("catalog.html")) {
            return url.substring(0, url.lastIndexOf("h")).concat("xml");
        } else if (url.endsWith("/")) {
            return url.concat("catalog.xml");
        } else {
            return url.concat("/catalog.xml");
        }
    }

    private class DefaultFilterChangeListener implements FilterChangeListener {

        @Override
        public void filterChanged() {
            final OpendapLeaf[] leaves = catalogTree.getLeaves();
            for (OpendapLeaf leaf : leaves) {
                filterLeaf(leaf);
            }
        }
    }

    private class ParameterProviderImpl implements DownloadAction.ParameterProvider {

        Map<String, Boolean> dapURIs = new HashMap<>();
        List<String> fileURIs = new ArrayList<>();
        private boolean mayAlwaysOverwrite = false;
        private boolean mayNeverOverwrite = false;

        @Override
        public Map<String, Boolean> getDapURIs() {
            if (dapURIs.isEmpty() && fileURIs.isEmpty()) {
                collectURIs();
            }
            return new HashMap<>(dapURIs);
        }

        @Override
        public List<String> getFileURIs() {
            if (dapURIs.isEmpty() && fileURIs.isEmpty()) {
                collectURIs();
            }
            return new ArrayList<>(fileURIs);
        }

        private void collectURIs() {
            final TreePath[] selectionPaths = ((JTree) catalogTree.getComponent()).getSelectionModel().getSelectionPaths();
            if (selectionPaths == null || selectionPaths.length <= 0) {
                return;
            }

            for (TreePath selectionPath : selectionPaths) {
                final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                if (CatalogTreeUtils.isDapNode(treeNode) || CatalogTreeUtils.isFileNode(treeNode)) {
                    final OpendapLeaf leaf = (OpendapLeaf) treeNode.getUserObject();
                    if (leaf.isDapAccess()) {
                        dapURIs.put(leaf.getDapUri(), leaf.getFileSize() >= 2 * 1024 * 1024);
                    } else if (leaf.isFileAccess()) {
                        fileURIs.add(leaf.getFileUri());
                    }
                }
            }
        }

        @Override
        public void reset() {
            dapURIs.clear();
            fileURIs.clear();
            mayAlwaysOverwrite = false;
            mayNeverOverwrite = false;
        }

        @Override
        public double getDatasizeInKb() {
            return currentDataSize;
        }

        @Override
        public File getTargetDirectory() {
            final File targetDirectory;
            if (folderTextField.getText() == null || folderTextField.getText().equals("")) {
                targetDirectory = fetchDownloadDirectory();
            } else {
                targetDirectory = new File(folderTextField.getText());
            }
            return targetDirectory;
        }

        @Override
        public boolean inquireOverwritePermission(String filename) {
            if (mayAlwaysOverwrite) {
                return true;
            }
            if (mayNeverOverwrite) {
                return false;
            }
            boolean mayOverwrite = false;
            String[] options = {
                    "Overwrite",
                    "Always overwrite",
                    "Skip this file",
                    "Never overwrite"
            };
            int result = JOptionPane.showOptionDialog(OpendapAccessPanel.this,
                                                      "Target file '" + filename + "' already exists.",
                                                      "Target file already exists",
                                                      JOptionPane.DEFAULT_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE,
                                                      null, options, "Yes");
            switch (result) {
                case 0:
                    mayOverwrite = true;
                    break;
                case 1:
                    mayOverwrite = true;
                    mayAlwaysOverwrite = true;
                    break;
                case 2:
                    mayOverwrite = false;
                    break;
                case 3:
                    mayOverwrite = false;
                    mayNeverOverwrite = true;
                    break;
            }
            return mayOverwrite;
        }
    }

    private class DefaultLeafSelectionListener implements CatalogTree.LeafSelectionListener {

        @Override
        public void dapLeafSelected(final OpendapLeaf leaf) {
            setText(leaf);
        }

        @Override
        public void fileLeafSelected(OpendapLeaf leaf) {
            setText(leaf);
        }

        private void setText(final OpendapLeaf leaf) {
            new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {
                    updateStatusBar("Retrieving metadata...");
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    setMetadataText(metaInfoArea.getSelectedIndex(), leaf);
                    return null;
                }

                @Override
                protected void done() {
                    updateStatusBar("Ready.");
                    setCursor(Cursor.getDefaultCursor());
                }

            }.execute();
        }

        @Override
        public void leafSelectionChanged(boolean isSelected, OpendapLeaf dapObject) {
            int dataSize = dapObject.getFileSize();
            currentDataSize += isSelected ? dataSize : -dataSize;
            if (currentDataSize <= 0) {
                updateStatusBar("Ready.");
                downloadButton.setEnabled(false);
            } else {
                downloadButton.setEnabled(true);
                double dataSizeInMB = currentDataSize / 1024.0;
                updateStatusBar("Total size of currently selected files: " + OpendapUtils.format(dataSizeInMB) + " MB");
            }
        }
    }
}
