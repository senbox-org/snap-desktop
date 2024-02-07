/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.engine_utilities.gpf.ProcessTimeMonitor;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.esa.snap.graphbuilder.rcp.dialogs.support.*;
import org.esa.snap.graphbuilder.rcp.progress.LabelBarProgressMonitor;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.remote.execution.operator.RemoteExecutionDialog;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.FileChooserFactory;
import org.esa.snap.ui.ModelessDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides the dialog for executing a graph on a list of products
 */
public class BatchGraphDialog extends ModelessDialog implements GraphDialog, LabelBarProgressMonitor.ProgressBarListener {

    protected final static Path defaultGraphPath = ResourceUtils.getGraphFolder("");
    protected final List<GraphExecuter> graphExecutorList = new ArrayList<>(10);
    private final AppContext appContext;
    private final String baseTitle;
    private final List<BatchProcessListener> listenerList = new ArrayList<>(1);
    private final boolean closeOnDone;
    protected ProductSetPanel productSetPanel;
    protected File graphFile;
    protected boolean openProcessedProducts;
    private JTabbedPane tabbedPane;
    private JLabel statusLabel, bottomStatusLabel;
    private JPanel progressPanel;
    private JProgressBar progressBar;
    private LabelBarProgressMonitor progBarMonitor;
    private Map<File, File[]> slaveFileMap;
    private boolean skipExistingTargetFiles;
    private boolean replaceWritersWithUniqueTargetProduct;
    private boolean isProcessing;

    public BatchGraphDialog(final AppContext theAppContext, final String title, final String helpID,
                            final boolean closeOnDone) {
        super(theAppContext.getApplicationWindow(), title, ID_YES | ID_APPLY_CLOSE_HELP, getRunRemoteIfWindows(), helpID);
        this.appContext = theAppContext;
        this.baseTitle = title;
        this.closeOnDone = closeOnDone;
        openProcessedProducts = true;

        setContent(createUI());

        if (getJDialog().getJMenuBar() == null) {
            final GraphsMenu operatorMenu = new GraphsMenu(getJDialog(), this);
            getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
        }

        super.getJDialog().setMinimumSize(new Dimension(600, 400));
    }

    private static String[] getRunRemoteIfWindows() {
        if (org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS || org.apache.commons.lang3.SystemUtils.IS_OS_LINUX) {
            return new String[]{"Run remote"};
        }
        return null;
    }

    private static File getFilePath(Component component, String title) {

        final File graphPath = new File(SnapApp.getDefault().getPreferences().get("batch.last_graph_path", defaultGraphPath.toFile().getAbsolutePath()));
        final JFileChooser chooser = FileChooserFactory.getInstance().createFileChooser(graphPath);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(title);
        if (chooser.showDialog(component, "OK") == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            SnapApp.getDefault().getPreferences().put("batch.last_graph_path", file.getAbsolutePath());
            return file;
        }
        return null;
    }

    /**
     * For coregistration
     *
     * @param graphEx      the graph executer
     * @param productSetID the product set reader
     * @param masterFile   master file
     * @param slaveFiles   slave file list
     */
    protected static void setSlaveIO(final GraphExecuter graphEx, final String productSetID,
                                     final File masterFile, final File[] slaveFiles) {
        final GraphNode productSetNode = graphEx.getGraphNodeList().findGraphNodeByOperator(productSetID);
        if (productSetNode != null) {
            StringBuilder str = new StringBuilder(masterFile.getAbsolutePath());
            for (File slaveFile : slaveFiles) {
                str.append(',');
                str.append(slaveFile.getAbsolutePath());
            }
            graphEx.setOperatorParam(productSetNode.getID(), "fileList", str.toString());
        }
    }

    @Override
    protected void onOther() {
        final List<File> sourceProductFiles = getSourceProductFiles();
        if (sourceProductFiles.isEmpty()) {
            showErrorDialog("Please add at least one source product.");
        } else if (this.graphFile == null) {
            showErrorDialog("Please add the graph file.");
        } else {
            final Window parentWindow = getJDialog().getOwner();
            close();
            final RemoteExecutionDialog dialog = new RemoteExecutionDialog(appContext, parentWindow) {
                @Override
                protected void onAboutToShow() {
                    super.onAboutToShow();

                    setData(sourceProductFiles, graphFile, productSetPanel.getTargetFormat());
                }
            };
            dialog.show();
        }
    }

    private JPanel createUI() {
        final JPanel mainPanel = new JPanel(new BorderLayout(4, 4));

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(e -> validateAllNodes());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // status
        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(255, 0, 0));
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        bottomStatusLabel = new JLabel("");
        getButtonPanel().add(bottomStatusLabel, 0);

        // progress Bar
        progressBar = new JProgressBar();
        progressBar.setName(getClass().getName() + "progressBar");
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout(2, 2));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        final JLabel progressMsgLabel = new JLabel();
        progressPanel.add(progressMsgLabel, BorderLayout.NORTH);

        progBarMonitor = new LabelBarProgressMonitor(progressBar, progressMsgLabel);
        progBarMonitor.addListener(this);

        final JButton progressCancelBtn = new JButton("Cancel");
        progressCancelBtn.addActionListener(e -> cancelProcessing());
        progressPanel.add(progressCancelBtn, BorderLayout.EAST);
        progressPanel.setVisible(false);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);

        productSetPanel = new ProductSetPanel(appContext, null, new FileTable(), false, true);
        tabbedPane.add("I/O Parameters", productSetPanel);

        getButton(ID_APPLY).setText("Run");
        getButton(ID_YES).setText("Load Graph");

        return mainPanel;
    }

    @Override
    public int show() {
        return super.show();
    }

    @Override
    public void hide() {
        if (progBarMonitor != null) {
            progBarMonitor.setCanceled(true);
        }
        notifyMSG(BatchProcessListener.BatchMSG.CLOSE);
        super.hide();
    }

    @Override
    public void onApply() {
        if (isProcessing) {
            return;
        }

        productSetPanel.onApply();

        skipExistingTargetFiles = false;
        replaceWritersWithUniqueTargetProduct = true;

        try {
            doProcessing();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
            bottomStatusLabel.setText("");
        }
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void addListener(final BatchProcessListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(final BatchProcessListener listener) {
        listenerList.remove(listener);
    }

    private void notifyMSG(final BatchProcessListener.BatchMSG msg, final String text) {
        for (final BatchProcessListener listener : listenerList) {
            listener.notifyMSG(msg, text);
        }
    }

    private void notifyMSG(final BatchProcessListener.BatchMSG msg) {
        for (final BatchProcessListener listener : listenerList) {
            listener.notifyMSG(msg, productSetPanel.getFileList(), getAllBatchProcessedTargetProducts());
        }
    }

    /**
     * OnLoad
     */
    @Override
    protected void onYes() {
        loadGraph();
    }

    public void setInputFiles(final File[] productFileList) {
        productSetPanel.setProductFileList(productFileList);
    }

    public void setTargetFolder(final File path) {
        productSetPanel.setTargetFolder(path);
    }

    public void loadGraph() {
        if (isProcessing) return;

        final File file = getFilePath(this.getContent(), "Graph File");
        if (file != null) {
            loadGraph(file);
        }
    }

    public void loadGraph(final File file) {
        try {
            graphFile = file;

            initGraphs();
            addGraphTabs("", true);

            setTitle(file.getName());
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to load graph " + file.toString(), e);
        }
    }

    @Override
    public void setTitle(final String title) {
        super.setTitle(baseTitle + " : " + title);
    }

    public boolean canSaveGraphs() {
        return false;
    }

    public void saveGraph() {
    }

    public String getGraphAsString() throws GraphException, IOException {
        if (!graphExecutorList.isEmpty()) {
            return graphExecutorList.get(0).getGraphAsString();
        }
        return "";
    }

    @Override
    protected void onClose() {
        cancelProcessing();

        super.onClose();
    }

    private void initGraphs() {
        try {
            deleteGraphs();
            createGraphs();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
            bottomStatusLabel.setText("");
        }
    }

    /**
     * Validates the input and then call the GPF to execute the graph
     */
    private void doProcessing() {
        if (validateAllNodes()) {

            SystemUtils.freeAllMemory();

            progressBar.setValue(0);

            final SwingWorker<Boolean, Object> processThread = new ProcessThread(progBarMonitor);
            processThread.execute();

        } else {
            if (statusLabel.getText() != null && !statusLabel.getText().isEmpty())
                showErrorDialog(statusLabel.getText());
        }
    }

    public void notifyProgressStart() {
        progressPanel.setVisible(true);
    }

    public void notifyProgressDone() {
        progressPanel.setVisible(false);
    }

    private void cancelProcessing() {
        if (progBarMonitor != null)
            progBarMonitor.setCanceled(true);
    }

    private void deleteGraphs() {
        for (GraphExecuter gex : graphExecutorList) {
            gex.clearGraph();
        }
        graphExecutorList.clear();
    }

    /**
     * Loads a new graph from a file
     *
     * @param executer  the GraphExcecuter
     * @param graphFile the graph file to load
     * @param addUI     add a user interface
     */
    protected void loadGraph(final GraphExecuter executer, final File graphFile, final boolean addUI) {
        try {
            executer.loadGraph(new FileInputStream(graphFile), graphFile, addUI, true);
            ensureWriteNodeTargetReset(executer.getGraphNodes());
        } catch (Exception e) {
            showErrorDialog(e.getMessage());
        }
    }

    static void ensureWriteNodeTargetReset(GraphNode[] graphNodes) {
        for (final GraphNode node : graphNodes) {
            if (node.getID().equals("Write")) {
                Map<String, Object> parameterMap = node.getParameterMap();
                parameterMap.remove("file");
            }
        }
    }

    private boolean validateAllNodes() {
        if (isProcessing) {
            return false;
        }
        if (productSetPanel == null) {
            return false;
        }
        if (graphExecutorList.isEmpty()) {
            return false;
        }

        boolean result;
        statusLabel.setText("");
        try {
            cloneGraphs();
            assignParameters();
            // first graph must pass
            result = graphExecutorList.get(0).initGraph();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
            bottomStatusLabel.setText("");
            result = false;
        }
        return result;
    }

    private List<File> getSourceProductFiles() {
        final File[] sourceFiles = productSetPanel.getFileList();
        final List<File> sourceProductFiles = new ArrayList<>();
        for (final File sourceFile : sourceFiles) {
            if (StringUtils.isNotNullAndNotEmpty(sourceFile.getPath())) {
                sourceProductFiles.add(sourceFile);
            }
        }
        return sourceProductFiles;
    }

    protected ProductSetPanel getProductSetPanel() {
        return productSetPanel;
    }

    public void setTargetProductNameSuffix(final String suffix) {
        productSetPanel.setTargetProductNameSuffix(suffix);
    }

    private void createGraphs() throws GraphException {
        try {
            final GraphExecuter graphEx = new GraphExecuter();
            loadGraph(graphEx, graphFile, true);
            graphExecutorList.add(graphEx);
        } catch (Exception e) {
            throw new GraphException(e.getMessage());
        }
    }

    private void addGraphTabs(final String title, final boolean addUI) {

        if (graphExecutorList.isEmpty()) {
            return;
        }

        tabbedPane.setSelectedIndex(0);
        while (tabbedPane.getTabCount() > 1) {
            tabbedPane.remove(tabbedPane.getTabCount() - 1);
        }

        final GraphExecuter graphEx = graphExecutorList.get(0);
        for (GraphNode n : graphEx.getGraphNodes()) {
            if (n.getOperatorUI() == null)
                continue;
            if (n.getNode().getOperatorName().equals("Read")
                    || (replaceWritersWithUniqueTargetProduct && n.getNode().getOperatorName().equals("Write"))
                    || n.getNode().getOperatorName().equals("ProductSet-Reader")) {
                n.setOperatorUI(null);
                continue;
            }

            if (addUI) {
                String tabTitle = title;
                if (tabTitle.isEmpty())
                    tabTitle = n.getOperatorName();
                tabbedPane.addTab(tabTitle, null,
                        n.getOperatorUI().CreateOpTab(n.getOperatorName(), n.getParameterMap(), appContext),
                        n.getID() + " Operator");
            }
        }
    }

    public void setSlaveFileMap(Map<File, File[]> fileMap) {
        slaveFileMap = fileMap;
    }

    protected void assignParameters() {
        final File targetFolder = productSetPanel.getTargetFolder();
        if (targetFolder != null && !targetFolder.exists()) {
            if (!targetFolder.mkdirs()) {
                final String msg = "Unable to create folders in " + targetFolder;
                SystemUtils.LOG.severe(msg);
                throw new OperatorException(msg);
            }
        }

        final File[] fileList = productSetPanel.getFileList();
        int graphIndex = 0;
        for (File file : fileList) {
            final String name = FileUtils.getFilenameWithoutExtension(file);

            final File targetFile = targetFolder == null ? null : new File(targetFolder, name);
            final String targetFormat = productSetPanel.getTargetFormat();

            setIO(graphExecutorList.get(graphIndex), file, targetFile, targetFormat);
            if (slaveFileMap != null) {
                final File[] slaveFiles = slaveFileMap.get(file);
                if (slaveFiles != null) {
                    setSlaveIO(graphExecutorList.get(graphIndex),
                            "ProductSet-Reader", file, slaveFiles);
                }
            }
            ++graphIndex;
        }
    }

    protected void setIO(final GraphExecuter graphEx,
                         final File readPath, final File writePath, final String format) {
        final GraphNode readNode = graphEx.getGraphNodeList().findGraphNodeByOperator("Read");
        if (readNode != null) {
            graphEx.setOperatorParam(readNode.getID(), "file", readPath.getAbsolutePath());
        }

        if (replaceWritersWithUniqueTargetProduct) {
            final GraphNode[] writeNodes = graphEx.getGraphNodeList().findAllGraphNodeByOperator("Write");
            for (GraphNode writeNode : writeNodes) {
                if (format != null) {
                    graphEx.setOperatorParam(writeNode.getID(), "formatName", format);
                }
                if (writePath != null) {
                    graphEx.setOperatorParam(writeNode.getID(), "file", writePath.getAbsolutePath());
                }
            }
        }
    }

    private void openTargetProducts() {
        final File[] fileList = getAllBatchProcessedTargetProducts();
        for (File file : fileList) {
            try {

                final Product product = CommonReaders.readProduct(file);
                if (product != null) {
                    appContext.getProductManager().addProduct(product);
                }
            } catch (IOException e) {
                showErrorDialog(e.getMessage());
            }
        }
    }

    protected void cloneGraphs() throws Exception {
        final GraphExecuter graphEx = graphExecutorList.get(0);
        for (int graphIndex = 1; graphIndex < graphExecutorList.size(); ++graphIndex) {
            final GraphExecuter cloneGraphEx = graphExecutorList.get(graphIndex);
            cloneGraphEx.clearGraph();
        }
        graphExecutorList.clear();
        graphExecutorList.add(graphEx);

        final File[] fileList = productSetPanel.getFileList();
        for (int graphIndex = 1; graphIndex < fileList.length; ++graphIndex) {

            final GraphExecuter cloneGraphEx = new GraphExecuter();
            loadGraph(cloneGraphEx, graphFile, false);
            graphExecutorList.add(cloneGraphEx);

            // copy UI parameter to clone
            final GraphNode[] cloneGraphNodes = cloneGraphEx.getGraphNodes();
            for (GraphNode cloneNode : cloneGraphNodes) {
                final GraphNode node = graphEx.getGraphNodeList().findGraphNode(cloneNode.getID());
                if (node != null) {
                    cloneNode.setOperatorUI(node.getOperatorUI());
                }
            }
        }
    }

    private File[] getAllBatchProcessedTargetProducts() {
        final List<File> targetFileList = new ArrayList<>();
        for (GraphExecuter graphEx : graphExecutorList) {
            targetFileList.addAll(graphEx.getProductsToOpenInDAT());
        }
        return targetFileList.toArray(new File[0]);
    }

    /////

    public interface BatchProcessListener {

        void notifyMSG(final BatchMSG msg, final File[] inputFileList, final File[] targetFileList);

        void notifyMSG(final BatchMSG msg, final String text);

        enum BatchMSG {DONE, UPDATE, CLOSE}
    }

    private class ProcessThread extends SwingWorker<Boolean, Object> {

        final List<String> errMsgs = new ArrayList<>();
        private final ProgressMonitor pm;
        private final ProcessTimeMonitor timeMonitor = new ProcessTimeMonitor();
        private boolean errorOccured = false;

        public ProcessThread(final ProgressMonitor pm) {
            this.pm = pm;
        }

        @Override
        protected Boolean doInBackground() {

            pm.beginTask("Processing Graph...", graphExecutorList.size());
            try {
                timeMonitor.start();
                isProcessing = true;

                final File[] existingFiles = productSetPanel.getTargetFolder() != null ? productSetPanel.getTargetFolder().listFiles(File::isFile) : null;

                final File[] fileList = productSetPanel.getFileList();
                int graphIndex = 0;
                for (GraphExecuter graphEx : graphExecutorList) {
                    if (pm.isCanceled()) {
                        break;
                    }

                    final String nOfm = (graphIndex + 1) + " of " + graphExecutorList.size() + ' ';
                    final String statusText = nOfm + fileList[graphIndex].getName();

                    try {
                        graphEx.initGraph();
                        graphEx.initGraph();

                        if (shouldSkip(graphEx, existingFiles)) {
                            statusLabel.setText("Skipping " + statusText);
                            notifyMSG(BatchProcessListener.BatchMSG.UPDATE, statusText);

                            pm.worked(1);
                            ++graphIndex;
                            continue;
                        } else {
                            statusLabel.setText("Processing " + statusText);
                            notifyMSG(BatchProcessListener.BatchMSG.UPDATE, statusText);

                            graphEx.executeGraph(SubProgressMonitor.create(pm, 1));

                            graphEx.disposeGraphContext();
                            SystemUtils.freeAllMemory();
                        }

                    } catch (Exception e) {
                        SystemUtils.LOG.severe(e.getMessage());
                        String filename = fileList[graphIndex].getName();
                        errMsgs.add(filename + " -> " + e.getMessage());
                    }

                    ++graphIndex;

                    // calculate time remaining
                    final long duration = timeMonitor.getCurrentDuration();
                    final double timePerGraph = duration / (double) graphIndex;
                    final long timeLeft = (long) (timePerGraph * (graphExecutorList.size() - graphIndex));
                    if (timeLeft > 0) {
                        String remainingStr = "Estimated " + ProcessTimeMonitor.formatDuration(timeLeft) + " remaining";
                        if (!errMsgs.isEmpty())
                            remainingStr += " (Errors occurred)";
                        bottomStatusLabel.setText(remainingStr);
                    }
                }

            } catch (Exception e) {
                SystemUtils.LOG.severe(e.getMessage());
                if (e.getMessage() != null && !e.getMessage().isEmpty())
                    statusLabel.setText(e.getMessage());
                else
                    statusLabel.setText(e.toString());
                errorOccured = true;
            } finally {
                final long duration = timeMonitor.stop();
                statusLabel.setText("Processing completed in " + ProcessTimeMonitor.formatDuration(duration));
                isProcessing = false;
                pm.done();

                if (openProcessedProducts) {
                    bottomStatusLabel.setText("Opening resulting products...");
                }
            }
            return true;
        }

        @Override
        public void done() {
            if (!errorOccured) {
                if (openProcessedProducts) {
                    openTargetProducts();
                }
                bottomStatusLabel.setText("");
            }
            if (!errMsgs.isEmpty()) {
                final StringBuilder msg = new StringBuilder("The following errors occurred:\n");
                for (String errStr : errMsgs) {
                    msg.append(errStr);
                    msg.append('\n');
                }
                showErrorDialog(msg.toString());
            }

            notifyMSG(BatchProcessListener.BatchMSG.DONE);
            if (closeOnDone) {
                close();
            }

            if (SnapApp.getDefault().getPreferences().getBoolean(GPF.BEEP_AFTER_PROCESSING_PROPERTY, false)) {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        private boolean shouldSkip(final GraphExecuter graphEx, final File[] existingFiles) {
            if (skipExistingTargetFiles) {
                if (existingFiles != null) {
                    final File[] targetFiles = graphEx.getPotentialOutputFiles();

                    boolean allTargetsExist = true;
                    for (File targetFile : targetFiles) {
                        final String targetPath = targetFile.getAbsolutePath();
                        boolean fileExists = false;
                        for (File existingFile : existingFiles) {
                            if (existingFile.getAbsolutePath().equalsIgnoreCase(targetPath)) {
                                fileExists = true;
                                break;
                            }
                        }
                        if (!fileExists) {
                            allTargetsExist = false;
                            break;
                        }
                    }
                    return allTargetsExist;
                }
            }
            return false;
        }
    }

}
