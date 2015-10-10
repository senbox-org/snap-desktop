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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.common.ReadOp;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.db.CommonReaders;
import org.esa.snap.graphbuilder.gpf.ui.ProductSetReaderOpUI;
import org.esa.snap.graphbuilder.gpf.ui.SourceUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphDialog;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphNode;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphPanel;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphsMenu;
import org.esa.snap.graphbuilder.rcp.dialogs.support.ProgressBarProgressMonitor;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;
import org.esa.snap.engine_utilities.util.MemUtils;
import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.openide.util.HelpCtx;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Provides the User Interface for creating, loading and saving Graphs
 */
public class GraphBuilderDialog extends ModelessDialog implements Observer, GraphDialog {

    private static final ImageIcon processIcon = TangoIcons.actions_media_playback_start(TangoIcons.Res.R22);
    private static final ImageIcon saveIcon = TangoIcons.actions_document_save_as(TangoIcons.Res.R22);
    private static final ImageIcon loadIcon = TangoIcons.actions_document_open(TangoIcons.Res.R22);
    private static final ImageIcon clearIcon = TangoIcons.actions_edit_clear(TangoIcons.Res.R22);
    private static final ImageIcon helpIcon = TangoIcons.apps_help_browser(TangoIcons.Res.R22);
    private static final ImageIcon infoIcon = TangoIcons.apps_accessories_text_editor(TangoIcons.Res.R22);

    private final AppContext appContext;
    private GraphPanel graphPanel = null;
    private JLabel statusLabel = null;
    private String lastWarningMsg = "";

    private JPanel progressPanel = null;
    private JProgressBar progressBar = null;
    private ProgressBarProgressMonitor progBarMonitor = null;
    private JLabel progressMsgLabel = null;
    private boolean initGraphEnabled = true;

    private final GraphExecuter graphEx;
    private boolean isProcessing = false;
    private boolean allowGraphBuilding = true;
    private final List<ProcessingListener> listenerList = new ArrayList<>(1);

    private final static String LAST_GRAPH_PATH = "graphbuilder.last_graph_path";

    private JTabbedPane tabbedPanel = null;

    public GraphBuilderDialog(final AppContext theAppContext, final String title, final String helpID) {
        this(theAppContext, title, helpID, true);
    }

    public GraphBuilderDialog(final AppContext theAppContext, final String title, final String helpID, final boolean allowGraphBuilding) {
        super(theAppContext.getApplicationWindow(), title, 0, helpID);

        this.allowGraphBuilding = allowGraphBuilding;
        appContext = theAppContext;
        graphEx = new GraphExecuter();
        graphEx.addObserver(this);

        String lastDir = SnapApp.getDefault().getPreferences().get(LAST_GRAPH_PATH,
                                                                   ResourceUtils.getGraphFolder("").toFile().getAbsolutePath());
        if (new File(lastDir).exists()) {
            SnapApp.getDefault().getPreferences().put(LAST_GRAPH_PATH, lastDir);
        }

        initUI();
    }

    /**
     * Initializes the dialog components
     */
    private void initUI() {
        if (this.allowGraphBuilding) {
            super.getJDialog().setMinimumSize(new Dimension(600, 750));
        } else {
            super.getJDialog().setMinimumSize(new Dimension(600, 500));
        }

        final JPanel mainPanel = new JPanel(new BorderLayout(4, 4));

        // north panel
        final JPanel northPanel = new JPanel(new BorderLayout(4, 4));

        if (allowGraphBuilding) {
            graphPanel = new GraphPanel(graphEx);
            graphPanel.setBackground(Color.WHITE);
            graphPanel.setPreferredSize(new Dimension(1500, 1000));
            final JScrollPane scrollPane = new JScrollPane(graphPanel);
            scrollPane.setPreferredSize(new Dimension(300, 300));
            northPanel.add(scrollPane, BorderLayout.CENTER);

            mainPanel.add(northPanel, BorderLayout.NORTH);
        }

        // mid panel
        final JPanel midPanel = new JPanel(new BorderLayout(4, 4));
        tabbedPanel = new JTabbedPane();
        //tabbedPanel.setTabPlacement(JTabbedPane.LEFT);
        tabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPanel.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                ValidateAllNodes();
            }
        });

        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(255, 0, 0));

        midPanel.add(tabbedPanel, BorderLayout.CENTER);
        midPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(midPanel, BorderLayout.CENTER);

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
        final JButton progressCancelBtn = new JButton("Cancel");
        progressCancelBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                CancelProcessing();
            }
        });
        progressPanel.add(progressCancelBtn, BorderLayout.EAST);

        progressPanel.setVisible(false);
        southPanel.add(progressPanel, BorderLayout.SOUTH);

        mainPanel.add(southPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        if (getJDialog().getJMenuBar() == null && allowGraphBuilding) {
            final GraphsMenu operatorMenu =  new GraphsMenu(getJDialog(), this);

            getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
        }

        setContent(mainPanel);
    }

    private void initButtonPanel(final JPanel panel) {
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        final JButton processButton = DialogUtils.createButton("processButton", "Run", processIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        processButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                DoProcessing();
            }
        });

        final JButton saveButton = DialogUtils.createButton("saveButton", "Save", saveIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                SaveGraph();
            }
        });

        final JButton loadButton = DialogUtils.createButton("loadButton", "Load", loadIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        loadButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                LoadGraph();
            }
        });

        final JButton clearButton = DialogUtils.createButton("clearButton", "Clear", clearIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        clearButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                ClearGraph();
            }
        });

        final JButton infoButton = DialogUtils.createButton("infoButton", "Note", infoIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        infoButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                OnInfo();
            }
        });
        //getClass().getName() + name
        final JButton helpButton = DialogUtils.createButton("helpButton", "Help", helpIcon, panel, DialogUtils.ButtonStyle.TextAndIcon);
        helpButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                OnHelp();
            }
        });

        gbc.weightx = 0;
        if (allowGraphBuilding) {
            panel.add(loadButton, gbc);
            panel.add(saveButton, gbc);
            panel.add(clearButton, gbc);
            panel.add(infoButton, gbc);
        }
        panel.add(helpButton, gbc);
        panel.add(processButton, gbc);
    }

    /**
     * Validates the input and then call the GPF to execute the graph
     */
    public void DoProcessing() {

        if (ValidateAllNodes()) {
            if (!checkIfOutputExists()) {
                return;
            }

            MemUtils.freeAllMemory();

            progressBar.setValue(0);
            progBarMonitor = new ProgressBarProgressMonitor(progressBar, progressMsgLabel, progressPanel);
            final SwingWorker processThread = new ProcessThread(progBarMonitor);
            processThread.execute();

        } else {
            showErrorDialog(statusLabel.getText());
        }
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

    private void CancelProcessing() {
        if (progBarMonitor != null)
            progBarMonitor.setCanceled(true);
    }

    private boolean InitGraph() {
        boolean result = true;
        try {
            if (initGraphEnabled) {
                result = graphEx.InitGraph();
            }
            if (!result)
                statusLabel.setText("Graph is incomplete");
        } catch (Exception e) {
            if (e.getMessage() != null)
                statusLabel.setText(e.getMessage());
            else
                statusLabel.setText(e.toString());
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
    public void SaveGraph() {

        //if(ValidateAllNodes()) {
        try {
            final File file = graphEx.saveGraph();
            if(file != null) {
                setTitle(file.getName());
            }
        } catch (GraphException e) {
            showErrorDialog(e.getMessage());
        }
        //} else {
        //    showErrorDialog(statusLabel.getText());
        //}
    }

    @Override
    public void setTitle(final String title) {
        super.setTitle("Graph Builder : " + title);
    }

    /**
     * Loads a new graph from a file
     */
    public void LoadGraph() {
        final SnapFileFilter fileFilter = new SnapFileFilter("XML", "xml", "Graph");
        final File graphFile = SnapDialogs.requestFileForOpen("Load Graph", false, fileFilter, LAST_GRAPH_PATH);
        if (graphFile == null) return;

        LoadGraph(graphFile);
    }

    /**
     * Loads a new graph from a file
     *
     * @param file the graph file to load
     */
    public void LoadGraph(final File file) {
        try {
            LoadGraph(new FileInputStream(file));
            if (allowGraphBuilding) {
                setTitle(file.getName());
            }
        } catch (IOException e) {
            SnapApp.getDefault().handleError("Unable to load graph " + file.toString(), e);
        }
    }

    /**
     * Loads a new graph from a file
     *
     * @param fileStream the graph file to load
     */
    public void LoadGraph(final InputStream fileStream) {
        try {
            initGraphEnabled = false;
            tabbedPanel.removeAll();
            graphEx.loadGraph(fileStream, true);
            if (allowGraphBuilding) {
                graphPanel.showRightClickHelp(false);
                graphPanel.repaint();
            }
            initGraphEnabled = true;
        } catch (GraphException e) {
            showErrorDialog(e.getMessage());
        }
    }

    public String getGraphAsString() throws GraphException, IOException {
        return graphEx.getGraphAsString();
    }

    public void EnableInitialInstructions(final boolean flag) {
        if (this.allowGraphBuilding) {
            graphPanel.showRightClickHelp(flag);
        }
    }

    /**
     * Removes all tabs and clears the graph
     */
    private void ClearGraph() {

        initGraphEnabled = false;
        tabbedPanel.removeAll();
        graphEx.ClearGraph();
        graphPanel.repaint();
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
            ProductSetReaderOpUI ui = (ProductSetReaderOpUI) productSetNode.GetOperatorUI();
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
            SourceUI ui = (SourceUI) readerNode.GetOperatorUI();
            ui.setSourceProduct(product);

            ValidateAllNodes();
        }
    }

    /**
     * Call Help
     */
    private void OnHelp() {
        new HelpCtx(getHelpID()).display();
    }

    /**
     * Call description dialog
     */
    private void OnInfo() {
        final PromptDialog dlg = new PromptDialog("Graph Description", "Description", graphEx.getGraphDescription(), true);
        dlg.show();
        if (dlg.IsOK()) {
            graphEx.setGraphDescription(dlg.getValue());
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
    boolean ValidateAllNodes() {

        if (isProcessing) return false;

        boolean isValid = true;
        final StringBuilder errorMsg = new StringBuilder(100);
        final StringBuilder warningMsg = new StringBuilder(100);
        for (GraphNode n : graphEx.GetGraphNodes()) {
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

        statusLabel.setForeground(new Color(255, 0, 0));
        statusLabel.setText("");
        final String warningStr = warningMsg.toString();
        if (!isValid) {
            statusLabel.setText(errorMsg.toString());
            return false;
        } else if (!warningStr.isEmpty()) {
            if (warningStr.length() > 100 && !warningStr.equals(lastWarningMsg)) {
                SnapDialogs.showWarning(warningStr);
                lastWarningMsg = warningStr;
            } else {
                statusLabel.setForeground(new Color(0, 100, 255));
                statusLabel.setText("Warning: " + warningStr);
            }
        }

        return InitGraph();
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
            final String opID = node.getID();
            if (event.getEventType() == GraphExecuter.events.ADD_EVENT) {

                tabbedPanel.addTab(opID, null, CreateOperatorTab(node), opID + " Operator");
            } else if (event.getEventType() == GraphExecuter.events.REMOVE_EVENT) {

                int index = tabbedPanel.indexOfTab(opID);
                tabbedPanel.remove(index);
            } else if (event.getEventType() == GraphExecuter.events.SELECT_EVENT) {

                int index = tabbedPanel.indexOfTab(opID);
                tabbedPanel.setSelectedIndex(index);
            } else if (event.getEventType() == GraphExecuter.events.CONNECT_EVENT) {

                ValidateAllNodes();
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if(msg == null || msg.isEmpty()) {
                msg = e.toString();
            }
            statusLabel.setText(msg);
        }
    }

    private JComponent CreateOperatorTab(final GraphNode node) {

        return node.GetOperatorUI().CreateOpTab(node.getOperatorName(), node.getParameterMap(), appContext);
    }

    private class ProcessThread extends SwingWorker<GraphExecuter, Object> {

        private final ProgressMonitor pm;
        private Date executeStartTime = null;
        private boolean errorOccured = false;

        public ProcessThread(final ProgressMonitor pm) {
            this.pm = pm;
        }

        @Override
        protected GraphExecuter doInBackground() throws Exception {

            pm.beginTask("Processing Graph...", 10);
            try {
                executeStartTime = Calendar.getInstance().getTime();
                isProcessing = true;
                graphEx.executeGraph(pm);

            } catch (Throwable e) {
                System.out.print(e.getMessage());
                if (e.getMessage() != null && !e.getMessage().isEmpty())
                    statusLabel.setText(e.getMessage());
                else
                    statusLabel.setText(e.getCause().toString());
                errorOccured = true;
            } finally {
                isProcessing = false;
                graphEx.disposeGraphContext();
                // free cache
                MemUtils.freeAllMemory();

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
                final File[] files = fileList.toArray(new File[fileList.size()]);
                notifyMSG(ProcessingListener.MSG.DONE, files);

                ProcessingStats stats = openTargetProducts(files);
                statusLabel.setText(ProductFunctions.getProcessingStatistics(totalSeconds, stats.totalBytes, stats.totalPixels));
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
}
