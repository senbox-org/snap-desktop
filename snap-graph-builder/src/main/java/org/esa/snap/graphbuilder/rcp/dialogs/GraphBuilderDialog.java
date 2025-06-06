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
package org.esa.snap.graphbuilder.rcp.dialogs;

import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.common.ReadOp;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.converters.JtsGeometryConverter;
import org.esa.snap.core.util.converters.RectangleConverter;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.esa.snap.graphbuilder.gpf.ui.*;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphDialog;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphNode;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphPanel;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphStruct;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphsMenu;
import org.esa.snap.graphbuilder.rcp.progress.LabelBarProgressMonitor;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;
import org.esa.snap.ui.help.HelpDisplayer;
import org.openide.util.HelpCtx;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.plaf.basic.BasicBorders;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

/**
 * Provides the User Interface for creating, loading and saving Graphs
 */
public class GraphBuilderDialog extends ModelessDialog implements Observer, GraphDialog, LabelBarProgressMonitor.ProgressBarListener, HelpCtx.Provider {

    static {
        registerConverters();
    }

    private static final ImageIcon processIcon = TangoIcons.actions_media_playback_start(TangoIcons.Res.R22);
    private static final ImageIcon saveIcon = TangoIcons.actions_document_save_as(TangoIcons.Res.R22);
    private static final ImageIcon loadIcon = TangoIcons.actions_document_open(TangoIcons.Res.R22);
    private static final ImageIcon clearIcon = TangoIcons.actions_edit_clear(TangoIcons.Res.R22);
    private static final ImageIcon helpIcon = TangoIcons.apps_help_browser(TangoIcons.Res.R22);
    private static final ImageIcon infoIcon = TangoIcons.apps_accessories_text_editor(TangoIcons.Res.R22);
    private static final ImageIcon validateIcon = TangoIcons.actions_view_refresh(TangoIcons.Res.R22);

    private final AppContext appContext;
    private GraphPanel graphPanel = null;
    StatusLabel  statusLabel = null;
    private String lastWarningMsg = "";

    JPanel progressPanel = null;
    JProgressBar progressBar = null;
    private LabelBarProgressMonitor progBarMonitor = null;
    private JLabel progressMsgLabel = null;
    private boolean initGraphEnabled = true;

    GraphExecuter graphEx;
    private boolean isProcessing = false;
    private boolean allowGraphBuilding = true;
    private final List<ProcessingListener> listenerList = new ArrayList<>(1);

    private Map<String, Object> selectedConfiguration = null;
    private List<GraphStruct> previousConfiguration = new ArrayList<>();
    private String selectedId = null;

    public final static String LAST_GRAPH_PATH = "graphbuilder.last_graph_path";

    JTabbedPane tabbedPanel = null;
    private GraphNode selectedNode;

    public GraphBuilderDialog(final AppContext theAppContext, final String title, final String helpID) {
        this(theAppContext, title, helpID, true);
    }

    public GraphBuilderDialog(final AppContext theAppContext, final String title, final String helpID, final boolean allowGraphBuilding) {
        super(theAppContext.getApplicationWindow(), title, 0, helpID);

        this.allowGraphBuilding = allowGraphBuilding;
        appContext = theAppContext;
        graphEx = new GraphExecuter();
        graphEx.addObserver(this);

        final Preferences preferences = SnapApp.getDefault().getPreferences();
        String lastDir = preferences.get(LAST_GRAPH_PATH, ResourceUtils.getGraphFolder("").toFile().getAbsolutePath());
        if (new File(lastDir).exists()) {
            preferences.put(LAST_GRAPH_PATH, lastDir);
        }

        initUI();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("addLandCoverBand");
    }

    /**
     * Initializes the dialog components
     */
    private void initUI() {
        if (this.allowGraphBuilding) {
            super.getJDialog().setMinimumSize(new Dimension(700, 750));
        } else {
            super.getJDialog().setMinimumSize(new Dimension(700, 550));
        }

        final JPanel mainPanel = new JPanel(new BorderLayout(4, 4));

        // mid panel
        final JPanel midPanel = new JPanel(new BorderLayout(4, 4));
        tabbedPanel = new JTabbedPane();
        //tabbedPanel.setTabPlacement(JTabbedPane.LEFT);
        tabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        statusLabel = new StatusLabel();

        midPanel.add(tabbedPanel, BorderLayout.CENTER);
        midPanel.add(statusLabel, BorderLayout.SOUTH);

        if (allowGraphBuilding) {
            graphPanel = new GraphPanel(graphEx);
            graphPanel.setBackground(Color.WHITE);
            graphPanel.setPreferredSize(new Dimension(1500, 1000));
            final JScrollPane scrollPane = new JScrollPane(graphPanel);
            scrollPane.setPreferredSize(new Dimension(300, 300));

            final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, midPanel);
            splitPane.setOneTouchExpandable(true);
            splitPane.setResizeWeight(0.4);
            if(!allowGraphBuilding) {
                splitPane.setDividerLocation(0);
            }
            splitPane.setBorder(new BasicBorders.MarginBorder());

            mainPanel.add(splitPane, BorderLayout.CENTER);

        } else {
            mainPanel.add(midPanel, BorderLayout.CENTER);
        }

        // south panel
        final JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        final JPanel buttonPanel = new JPanel();
        initButtonPanel(buttonPanel);
        southPanel.add(buttonPanel, BorderLayout.CENTER);

        // progress Bar
        progressBar = new JProgressBar();
        progressBar.setName(getClass().getName() + "progressBar");
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout(2, 2));
        progressMsgLabel = new JLabel();
        progressPanel.add(progressMsgLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        progBarMonitor = new LabelBarProgressMonitor(progressBar, progressMsgLabel);
        progBarMonitor.addListener(this);

        final JButton progressCancelBtn = new JButton("Cancel");
        progressCancelBtn.addActionListener(e -> cancelProcessing());
        progressPanel.add(progressCancelBtn, BorderLayout.EAST);

        progressPanel.setVisible(false);
        southPanel.add(progressPanel, BorderLayout.SOUTH);

        mainPanel.add(southPanel, BorderLayout.SOUTH);

        if (getJDialog().getJMenuBar() == null && allowGraphBuilding) {
            final GraphsMenu operatorMenu = new GraphsMenu(getJDialog(), this);

            getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
        }

        setContent(mainPanel);
    }

    private static boolean equals(Map<String, Object> a, Map<String, Object> b){
        if (a == null && b == null)
            return true;

        if (a == null || b == null)
            return false;

        if (a.keySet().size() == b.keySet().size()) {
            for (String key : a.keySet()) {
                if (!b.containsKey(key))
                    return false;
                Object objA = a.get(key);
                Object objB = b.get(key);
                if (objA != null && objB != null) {
                    if (!objA.toString().equals(objB.toString()))
                        return false;
                }
                if (objA == null ^ objB == null)
                    return false;
            }
            return true;
        }
        return false;
    }

    /*
    private boolean changesAreDetected() {
        boolean result = false;
        List<GraphStruct> currentStruct = GraphStruct.copyGraphStruct(this.graphEx.getGraphNodes());
        if (this.selectedId != null) {
            if (GraphStruct.deepEqual(currentStruct, previousConfiguration)) {
                result = !equals(this.selectedConfiguration, this.selectedNode.getOperatorUIParameterMap());
            } else {
                // the graph has changed and so you need to reverify
                result = true;
            }
        }
        this.previousConfiguration = currentStruct;
        if (this.tabbedPanel.getSelectedIndex() >= 0){
            this.selectedId = this.tabbedPanel.getTitleAt(this.tabbedPanel.getSelectedIndex());
            for (GraphNode n: this.graphEx.getGraphNodes()) {
                if (n.getID().equals(this.selectedId)) {
                    this.selectedNode = (n);
                    this.selectedConfiguration = new HashMap<>(n.getOperatorUIParameterMap());
                }
            }
            if (this.selectedConfiguration == null) {
                System.err.println("WARNING [org.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog]: Node `"+selectedId+"`not found");
                this.selectedId = null;
            }

        } else {
            this.selectedId = null;
        }
        return result;
    }
     */

    private void initButtonPanel(final JPanel panel) {
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        final AbstractButton processButton = DialogUtils.createButton("processButton", "Run", processIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        processButton.addActionListener(e -> doProcessing());

        final AbstractButton saveButton = DialogUtils.createButton("saveButton", "Save", saveIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        saveButton.addActionListener(e -> saveGraph());

        final AbstractButton loadButton = DialogUtils.createButton("loadButton", "Load", loadIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        loadButton.addActionListener(e -> loadGraph());

        final AbstractButton clearButton = DialogUtils.createButton("clearButton", "Clear", clearIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        clearButton.addActionListener(e -> clearGraph());

        final AbstractButton infoButton = DialogUtils.createButton("infoButton", "Note", infoIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        infoButton.addActionListener(e -> OnInfo());
        //getClass().getName() + name
        final AbstractButton helpButton = DialogUtils.createButton("helpButton", "Help", helpIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        helpButton.addActionListener(e -> OnHelp());

        final AbstractButton validateButton = DialogUtils.createButton("validateButton", "Validate", validateIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        validateButton.addActionListener(e -> validateAllNodes());

        gbc.weightx = 0;
        if (allowGraphBuilding) {
            panel.add(loadButton, gbc);
            panel.add(clearButton, gbc);
            panel.add(infoButton, gbc);
        }
        panel.add(saveButton, gbc);
        panel.add(helpButton, gbc);
        panel.add(validateButton, gbc);
        panel.add(processButton, gbc);
    }

    /**
     * Validates the input and then call the GPF to execute the graph
     */
    public void doProcessing() {

        if (validateAllNodes()) {
            if (!checkIfOutputExists()) {
                return;
            }

            SystemUtils.freeAllMemory();

            progressBar.setValue(0);

            final ProcessThread processThread = new ProcessThread(progBarMonitor);
            processThread.execute();

        } else {
            showErrorDialog(statusLabel.getText());
        }
    }

    public void notifyProgressStart() {
        progressPanel.setVisible(true);
    }

    public void notifyProgressDone() {
        progressPanel.setVisible(false);
    }

    private boolean checkIfOutputExists() {
        final File[] files = graphEx.getPotentialOutputFiles();
        for (File file : files) {
            if (file.exists()) {
                final int answer = JOptionPane.showOptionDialog(getJDialog(),
                                                                "File " + file.getPath() + " already exists.\nWould you like to overwrite?", "Overwrite?",
                                                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

                if (answer == JOptionPane.NO_OPTION) {
                    return false;
                }
            }
        }
        return true;
    }

    private void cancelProcessing() {
        if (progBarMonitor != null)
            progBarMonitor.setCanceled(true);
    }

    private boolean initGraph() {
        boolean result = true;
        try {
            if (initGraphEnabled) {
                result = graphEx.initGraph();
            }
            if (!result && allowGraphBuilding) {
                statusLabel.setErrorMessage("Graph is incomplete");
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                statusLabel.setErrorMessage("Error: " + e.getMessage());
            } else {
                statusLabel.setErrorMessage("Error: " + e);
            }
            result = false;
        }
        return result;
    }

    public boolean canSaveGraphs() {
        return true;
    }

    /**
     * Validates the input and then saves the current graph to a file
     */
    public void saveGraph() {
        try {
            final File file = graphEx.saveGraph();
            if (file != null) {
                setTitle(file.getName());
            }
        } catch (GraphException e) {
            showErrorDialog(e.getMessage());
        }
    }

    @Override
    public void setTitle(final String title) {
        super.setTitle("Graph Builder : " + title);
    }

    /**
     * Loads a new graph from a file
     */
    public void loadGraph() {
        final SnapFileFilter fileFilter = new SnapFileFilter("XML", "xml", "Graph");
        final File graphFile = Dialogs.requestFileForOpen("Load Graph", false, fileFilter, LAST_GRAPH_PATH);
        if (graphFile == null) {
            return;
        }
        refreshGraph();

        loadGraph(graphFile);

        refreshGraph();
    }

    /**
     * Loads a new graph from a file
     *
     * @param file the graph file to load
     */
    public void loadGraph(final File file) {
        try {

            loadGraph(new FileInputStream(file), file);
            if (allowGraphBuilding) {
                setTitle(file.getName());
            }
        } catch (IOException e) {
            SnapApp.getDefault().handleError("Unable to load graph " + file, e);
        }
    }

    /**
     * Loads a new graph from a file
     *
     * @param fileStream the graph file to load
     */
    public void loadGraph(final InputStream fileStream, final File file) {
        try {
            initGraphEnabled = false;
            tabbedPanel.removeAll();
            graphEx.loadGraph(fileStream, file, true, true);
            if (allowGraphBuilding) {
                graphPanel.showRightClickHelp(false);
                refreshGraph();
            }
            initGraphEnabled = true;
            validateAllNodes();
        } catch (GraphException e) {
            showErrorDialog(e.getMessage());
        }
    }

    private void refreshGraph() {
        if(graphPanel != null) {
            graphPanel.repaint();
        }
    }

    public String getGraphAsString() throws GraphException, IOException {
        return graphEx.getGraphAsString();
    }

    public void enableInitialInstructions(final boolean flag) {
        if (this.allowGraphBuilding) {
            graphPanel.showRightClickHelp(flag);
        }
    }

    /**
     * Removes all tabs and clears the graph
     */
    public void clearGraph() {

        initGraphEnabled = false;
        tabbedPanel.removeAll();
        graphEx.clearGraph();
        refreshGraph();
        initGraphEnabled = true;
        statusLabel.setText("");
    }

    /**
     * pass in a file list for a ProductSetReader
     *
     * @param productFileList the product files
     */
    public void setInputFiles(final File[] productFileList) {
        final GraphNode productSetNode = graphEx.getGraphNodeList().findGraphNodeByOperator("ProductSet-Reader");
        if (productSetNode != null) {
            ProductSetReaderOpUI ui = (ProductSetReaderOpUI) productSetNode.getOperatorUI();
            ui.setProductFileList(productFileList);
        }
    }

    /**
     * pass in a file list for a ProductSetReader
     *
     * @param product the product files
     */
    public void setInputFile(final Product product) {
        final GraphNode readerNode = graphEx.getGraphNodeList().findGraphNodeByOperator(
                ReadOp.Spi.getOperatorAlias(ReadOp.class));
        if (readerNode != null) {
            SourceUI ui = (SourceUI) readerNode.getOperatorUI();
            ui.setSourceProduct(product);

            validateAllNodes();
        }
    }

    /**
     * Call Help
     */
    private void OnHelp() {
        HelpDisplayer.show(getHelpID());
    }

    /**
     * Call description dialog
     */
    private void OnInfo() {
        final PromptDialog dlg = new PromptDialog("Graph Description", "Description",
                                                  graphEx.getGraphDescription(), PromptDialog.TYPE.TEXTAREA);
        dlg.show();
        if (dlg.IsOK()) {
            try {
                graphEx.setGraphDescription(dlg.getValue("Description"));
            } catch (Exception ex) {
                Dialogs.showError(ex.getMessage());
            }
        }
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    /**
     * lets all operatorUIs validate their parameters
     * If parameter validation fails then a list of the failures is presented to the user
     *
     * @return true if validation passes
     */
    public boolean validateAllNodes() {
        if (isProcessing) {
            return false;
        }

        boolean isValid = true;
        final StringBuilder errorMsg = new StringBuilder(100);
        final StringBuilder warningMsg = new StringBuilder(100);
        for (GraphNode n : graphEx.getGraphNodes()) {
            try {
                final UIValidation validation = n.validateParameterMap();
                if (validation.getState() == UIValidation.State.ERROR) {
                    isValid = false;
                    errorMsg.append(validation.getMsg()).append('\n');
                } else if (validation.getState() == UIValidation.State.WARNING) {
                    warningMsg.append(validation.getMsg()).append('\n');
                }
            } catch (Exception e) {
                isValid = false;
                errorMsg.append(e.getMessage()).append('\n');
            }
        }


        final String warningStr = warningMsg.toString();
        if (!isValid) {
            statusLabel.setErrorMessage(errorMsg.toString());
            return false;
        }

        if (!warningStr.isEmpty()) {
            if (warningStr.length() > 100 && !warningStr.equals(lastWarningMsg)) {
                Dialogs.showWarning(warningStr);
                lastWarningMsg = warningStr;
            } else {
                statusLabel.setWarningMessage("Warning: " + warningStr);
            }
        } else {
            statusLabel.setOkMessage("Validation OK");
        }

        return initGraph();
    }

    public void addListener(final ProcessingListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(final ProcessingListener listener) {
        listenerList.remove(listener);
    }

    private void notifyMSG(final ProcessingListener.MSG msg, final String text) {
        for (final ProcessingListener listener : listenerList) {
            listener.notifyMSG(msg, text);
        }
    }

    private void notifyMSG(final ProcessingListener.MSG msg, final File[] fileList) {
        for (final ProcessingListener listener : listenerList) {
            listener.notifyMSG(msg, fileList);
        }
    }

    /**
     * Implements the functionality of Observer participant of Observer Design Pattern to define a one-to-many
     * dependency between a Subject object and any number of Observer objects so that when the
     * Subject object changes state, all its Observer objects are notified and updated automatically.
     * <p>
     * Defines an updating interface for objects that should be notified of changes in a subject.
     *
     * @param subject The Observerable subject
     * @param data    optional data
     */
    public void update(Observable subject, Object data) {

        try {
            final GraphExecuter.GraphEvent event = (GraphExecuter.GraphEvent) data;
            final GraphNode node = (GraphNode) event.getData();
            final GraphExecuter.events eventType = event.getEventType();
            switch(eventType) {
                case ADD_EVENT:
                    tabbedPanel.addTab(node.getID(), null, createOperatorTab(node), node.getID() + " Operator");
                    if(node.getOperatorUI().hasError()) {
                        statusLabel.setText(node.getOperatorUI().getErrorMessage());
                    }
                    refreshGraph();
                    break;
                case REMOVE_EVENT:
                    tabbedPanel.remove(tabbedPanel.indexOfTab(node.getID()));
                    refreshGraph();
                    break;
                case SELECT_EVENT:
                    int newTabIndex = tabbedPanel.indexOfTab(node.getID());
                    if(tabbedPanel.getSelectedIndex() != newTabIndex) {
                        tabbedPanel.setSelectedIndex(newTabIndex);
                    }
                    break;
                case CONNECT_EVENT:
                    initGraphOnConnect();
                    break;
                case REFRESH_EVENT:
                    refreshGraph();
                    break;
                default:
                    throw new Exception("Unhandled GraphExecuter event " + eventType.name());
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = e.toString();
            }
            statusLabel.setText(msg);
        }
    }

    void initGraphOnConnect() {
        try {
            boolean prev = initGraphEnabled;
            initGraphEnabled = true;
            initGraph();
            initGraphEnabled = prev;
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Init error: " + e.getMessage());
        }
    }

    private JComponent createOperatorTab(final GraphNode node) {
        final OperatorUI operatorUI = node.getOperatorUI();
        final JComponent opTab = operatorUI.CreateOpTab(node.getOperatorName(), node.getParameterMap(), appContext);
        operatorUI.addSelectionChangeListener(new SourceSelectionChangeListener());
        return opTab;
    }

    private class ProcessThread extends SwingWorker<GraphExecuter, Object> {

        private final ProgressMonitor pm;
        private Date executeStartTime = null;
        private boolean errorOccured = false;

        ProcessThread(final ProgressMonitor pm) {
            this.pm = pm;
        }

        @Override
        protected GraphExecuter doInBackground() {

            pm.beginTask("Processing Graph...", 10);
            try {
                executeStartTime = Calendar.getInstance().getTime();
                isProcessing = true;
                graphEx.executeGraph(pm);

            } catch (Throwable e) {
                if (e.getMessage() != null && !e.getMessage().isEmpty())
                    statusLabel.setText(e.getMessage());
                else
                    statusLabel.setText(e.getCause().toString());
                errorOccured = true;
            } finally {
                isProcessing = false;
                graphEx.disposeGraphContext();
                // free cache
                SystemUtils.freeAllMemory();

                pm.done();
            }
            return graphEx;
        }

        @Override
        public void done() {
            if (!errorOccured) {
                final Date now = Calendar.getInstance().getTime();
                final long totalSeconds = (now.getTime() - executeStartTime.getTime()) / 1000;

                statusLabel.setText(ProductFunctions.getProcessingStatistics(totalSeconds));

                final List<File> fileList = graphEx.getProductsToOpenInDAT();
                final File[] files = fileList.toArray(new File[0]);
                notifyMSG(ProcessingListener.MSG.DONE, files);

                ProcessingStats stats = openTargetProducts(files);
                statusLabel.setText(ProductFunctions.getProcessingStatistics(totalSeconds, stats.totalBytes, stats.totalPixels));
                if (SnapApp.getDefault().getPreferences().getBoolean(GPF.BEEP_AFTER_PROCESSING_PROPERTY, false)) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
    }

    private ProcessingStats openTargetProducts(final File[] fileList) {
        ProcessingStats stats = new ProcessingStats();
        if (fileList.length != 0) {
            for (File file : fileList) {
                try {
                    final Product product = CommonReaders.readProduct(file);
                    if (product != null) {
                        appContext.getProductManager().addProduct(product);

                        stats.totalBytes += ProductFunctions.getRawStorageSize(product);
                        stats.totalPixels = ProductFunctions.getTotalPixels(product);
                    }
                } catch (IOException e) {
                    showErrorDialog(e.getMessage());
                }
            }
        }
        return stats;
    }

    private static class ProcessingStats {
        long totalBytes = 0;
        long totalPixels = 0;
    }

    public static File getInternalGraphFolder() {
        return ResourceUtils.getGraphFolder("internal").toFile();
    }

    public static File getStandardGraphFolder() {
        return ResourceUtils.getGraphFolder("Standard Graphs").toFile();
    }

    public interface ProcessingListener {

        enum MSG {DONE, UPDATE}

        void notifyMSG(final MSG msg, final File[] fileList);

        void notifyMSG(final MSG msg, final String text);
    }

    private static void registerConverters() {
        final ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
        JtsGeometryConverter.registerConverter();

        RectangleConverter rectConverter = new RectangleConverter();
        converterRegistry.setConverter(Rectangle.class, rectConverter);
    }

    private class SourceSelectionChangeListener implements SelectionChangeListener {

        public void selectionChanged(SelectionChangeEvent event) {
            final Object selected = event.getSelection().getSelectedValue();
            if (selected instanceof Product) {
                validateAllNodes();
            }
        }

        public void selectionContextChanged(SelectionChangeEvent event) {
            //nothing to do tb 2025-06-06
        }
    }
}
