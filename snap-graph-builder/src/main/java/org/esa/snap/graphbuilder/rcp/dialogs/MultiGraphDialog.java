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
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.esa.snap.graphbuilder.rcp.progress.LabelBarProgressMonitor;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Provides the dialog for excuting multiple graph from one user interface
 */
public abstract class MultiGraphDialog extends ModelessDialog implements LabelBarProgressMonitor.ProgressBarListener {

    protected final AppContext appContext;
    protected final IOPanel ioPanel;
    protected final List<GraphExecuter> graphExecuterList = new ArrayList<>(3);

    private final JPanel mainPanel;
    protected final JTabbedPane tabbedPane;
    private final JLabel statusLabel;
    private final JPanel progressPanel;
    private final JProgressBar progressBar;
    private LabelBarProgressMonitor progBarMonitor = null;

    private boolean isProcessing = false;

    protected static final String TMP_FILENAME = "tmp_intermediate";

    public MultiGraphDialog(final AppContext theAppContext, final String title, final String helpID,
                            final boolean useSourceSelector) {
        super(theAppContext.getApplicationWindow(), title, ID_APPLY_CLOSE_HELP, helpID);
        appContext = theAppContext;

        mainPanel = new JPanel(new BorderLayout(4, 4));

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                validateAllNodes();
            }
        });
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        ioPanel = new IOPanel(appContext, tabbedPane, useSourceSelector);

        // status
        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(255, 0, 0));
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        // progress Bar
        progressBar = new JProgressBar();
        progressBar.setName(getClass().getName() + "progressBar");
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout(2, 2));
        progressPanel.add(progressBar, BorderLayout.CENTER);

        progBarMonitor = new LabelBarProgressMonitor(progressBar);
        progBarMonitor.addListener(this);

        final JButton progressCancelBtn = new JButton("Cancel");
        progressCancelBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                cancelProcessing();
            }
        });
        progressPanel.add(progressCancelBtn, BorderLayout.EAST);
        progressPanel.setVisible(false);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);

        getButton(ID_APPLY).setText("Run");

        super.getJDialog().setMinimumSize(new Dimension(500, 300));
    }

    @Override
    public int show() {
        ioPanel.initProducts();
        setContent(mainPanel);
        initGraphs();
        return super.show();
    }

    @Override
    public void hide() {
        ioPanel.releaseProducts();
        super.hide();
    }

    @Override
    protected void onApply() {

        if (isProcessing) return;

        ioPanel.onApply();

        try {
            doProcessing();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
        }
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
        }
    }

    /**
     * Validates the input and then call the GPF to execute the graph
     *
     */
    private void doProcessing() {

        if (validateAllNodes()) {

            SystemUtils.freeAllMemory();

            progressBar.setValue(0);

            final SwingWorker processThread = new ProcessThread(progBarMonitor);
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

    private void cancelProcessing() {
        if (progBarMonitor != null)
            progBarMonitor.setCanceled(true);
    }

    private void deleteGraphs() {
        for (GraphExecuter gex : graphExecuterList) {
            gex.clearGraph();
        }
        graphExecuterList.clear();
    }

    /**
     * Loads a new graph from a file
     *
     * @param executer the GraphExcecuter
     * @param file     the graph file to load
     */
    public void loadGraph(final GraphExecuter executer, final File file) {
        try {
            executer.loadGraph(new FileInputStream(file), file, true, true);

        } catch (Exception e) {
            showErrorDialog(e.getMessage());
        }
    }

    abstract void createGraphs() throws GraphException;

    abstract void assignParameters() throws GraphException;

    abstract void cleanUpTempFiles();

    private boolean validateAllNodes() {
        if (isProcessing) return false;
        if (ioPanel == null || graphExecuterList.isEmpty())
            return false;

        boolean result;
        statusLabel.setText("");
        try {
            // check the all files have been saved
            final Product srcProduct = ioPanel.getSelectedSourceProduct();
            if (srcProduct != null && (srcProduct.isModified() || srcProduct.getFileLocation() == null)) {
                throw new OperatorException("The source product has been modified. Please save it before using it in " + getTitle());
            }
            assignParameters();
            // first graph must pass
            result = graphExecuterList.get(0).initGraph();

        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
            result = false;
        }
        return result;
    }

    private void openTargetProducts(final List<Product> products) {
        if (!products.isEmpty()) {
            for (final Product product : products) {
                appContext.getProductManager().addProduct(product);
            }
        }
    }

    protected IOPanel getIOPanel() {
        return ioPanel;
    }

    public void setTargetProductNameSuffix(final String suffix) {
        ioPanel.setTargetProductNameSuffix(suffix);
    }

    /**
     * For running graphs in unit tests
     *
     * @throws Exception when failing validation
     */
    public void testRunGraph() throws Exception {
        ioPanel.initProducts();
        initGraphs();

        if (validateAllNodes()) {

            for (GraphExecuter graphEx : graphExecuterList) {
                final String desc = graphEx.getGraphDescription();
                if (desc != null && !desc.isEmpty())
                    System.out.println("Processing " + graphEx.getGraphDescription());

                graphEx.initGraph();

                graphEx.executeGraph(ProgressMonitor.NULL);
                graphEx.disposeGraphContext();
            }

            cleanUpTempFiles();
        } else {
            throw new OperatorException(statusLabel.getText());
        }
    }

    /////

    private class ProcessThread extends SwingWorker<Boolean, Object> {

        private final ProgressMonitor pm;
        private Date executeStartTime = null;
        private boolean errorOccured = false;

        public ProcessThread(final ProgressMonitor pm) {
            this.pm = pm;
        }

        @Override
        protected Boolean doInBackground() throws Exception {

            pm.beginTask("Processing Graph...", 100 * graphExecuterList.size());
            try {
                executeStartTime = Calendar.getInstance().getTime();
                isProcessing = true;

                for (GraphExecuter graphEx : graphExecuterList) {
                    final String desc = graphEx.getGraphDescription();
                    if (desc != null && !desc.isEmpty())
                        statusLabel.setText("Processing " + graphEx.getGraphDescription());

                    graphEx.initGraph();

                    graphEx.executeGraph(SubProgressMonitor.create(pm, 100));
                }

            } catch (Exception e) {
                System.out.print(e.getMessage());
                if (e.getMessage() != null && !e.getMessage().isEmpty())
                    statusLabel.setText(e.getMessage());
                else
                    statusLabel.setText(e.toString());
                errorOccured = true;
            } finally {
                isProcessing = false;
                pm.done();
                if (SnapApp.getDefault().getPreferences().getBoolean(GPF.BEEP_AFTER_PROCESSING_PROPERTY, false)) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
            return true;
        }

        @Override
        public void done() {
            if (!errorOccured) {
                final Date now = Calendar.getInstance().getTime();
                final long diff = (now.getTime() - executeStartTime.getTime()) / 1000;
                if (diff > 120) {
                    final float minutes = diff / 60f;
                    statusLabel.setText("Processing completed in " + minutes + " minutes");
                } else {
                    statusLabel.setText("Processing completed in " + diff + " seconds");
                }

                SystemUtils.freeAllMemory();

                if (ioPanel.isOpenInAppSelected()) {
                    final GraphExecuter graphEx = graphExecuterList.get(graphExecuterList.size() - 1);
                    openTargetProducts(graphEx.getProductsToOpen());
                }
            }
            cleanUpTempFiles();

            for (GraphExecuter graphEx : graphExecuterList) {
                graphEx.disposeGraphContext();
            }
        }
    }

}
