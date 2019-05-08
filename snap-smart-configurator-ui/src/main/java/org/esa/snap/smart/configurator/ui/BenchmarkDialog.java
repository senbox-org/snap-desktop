/*
 * Copyright (C) 2015 CS SI
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

package org.esa.snap.smart.configurator.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.internal.OperatorContext;
import org.esa.snap.core.gpf.internal.OperatorExecutor;
import org.esa.snap.core.gpf.internal.OperatorProductReader;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.rcp.util.ProgressHandleMonitor;
import org.esa.snap.smart.configurator.Benchmark;
import org.esa.snap.smart.configurator.BenchmarkSingleCalculus;
import org.esa.snap.smart.configurator.ConfigurationOptimizer;
import org.esa.snap.smart.configurator.PerformanceParameters;
import org.esa.snap.smart.configurator.StoredGraphOp;
import org.esa.snap.ui.AppContext;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Cancellable;

import javax.media.jai.JAI;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * Dialog to launch performance parameters benchmark.
 *
 * @author Manuel Campomanes
 */
public class BenchmarkDialog extends DefaultSingleTargetProductDialog {

    /**
     * Benchmark calculus model
     */
    private Benchmark benchmarkModel;


    /**
     * Parent panel
     */
    private PerformancePanel perfPanel;


    /**
     * Constructor
     *
     * @param perfPanel      Parent JPanel
     * @param operatorName   Operator name
     * @param benchmarkModel Benchmark model
     * @param appContext     Application context
     */
    public BenchmarkDialog(PerformancePanel perfPanel, String operatorName, Benchmark benchmarkModel, AppContext appContext) {
        super(operatorName, appContext, "Benchmark " + operatorName, null, false);

        this.benchmarkModel = benchmarkModel;
        this.getJDialog().setModal(true);
        this.perfPanel = perfPanel;
    }


    protected void executeOperator(Product targetProduct, ProgressHandleMonitor pm) throws Exception {
        final TargetProductSelectorModel model = getTargetProductSelector().getModel();

        //To avoid a nullPointerException in model.getProductFile()
        if(model.getProductName()==null) {
            model.setProductName(targetProduct.getName());
        }

        Operator execOp = null;
        if (targetProduct.getProductReader() instanceof OperatorProductReader) {
            final OperatorProductReader opReader = (OperatorProductReader) targetProduct.getProductReader();
            Operator operator = opReader.getOperatorContext().getOperator();
            boolean autoWriteDisabled = operator.getSpi().getOperatorDescriptor().isAutoWriteDisabled();
            if (autoWriteDisabled) {
                execOp = operator;
            }
        }

        if (execOp == null) {
            WriteOp writeOp = new WriteOp(targetProduct, model.getProductFile(), model.getFormatName());
            writeOp.setDeleteOutputOnFailure(true);
            writeOp.setWriteEntireTileRows(true);
            writeOp.setClearCacheAfterRowWrite(false);
            execOp = writeOp;
        }


        SubProgressMonitor pm2 = (SubProgressMonitor) SubProgressMonitor.create(pm, 95);

        //execute
        if(execOp.canComputeTile() || execOp.canComputeTileStack()) {
            final OperatorExecutor executor = OperatorExecutor.create(execOp);
            executor.execute(pm2);
        } else {
            execOp.execute(pm2);
        }
        pm2.done();
    }

    @Override
    protected void onApply() {

        BenchmarkExecutor executor = new BenchmarkExecutor();

        //launch processing with a progress bar
        ProgressHandleMonitor pm = ProgressHandleMonitor.create("Running benchmark", executor);

        executor.setProgressHandleMonitor(pm);

        ProgressUtils.runOffEventThreadWithProgressDialog(executor, "Benchmarking....",
                                                          pm.getProgressHandle(),
                                                          true,
                                                          50,
                                                          1000);
    }


    private class BenchmarkExecutor implements Runnable, Cancellable {

        ProgressHandleMonitor progressHandleMonitor = null;

        BenchmarkSingleCalculus currentBenchmarkSingleCalcul = null;

        private boolean canceled = false;

        private void setProgressHandleMonitor(ProgressHandleMonitor progressHandleMonitor) {
            this.progressHandleMonitor = progressHandleMonitor;
        }

        @Override
        public boolean cancel() {
            //load old params (before benchmark)
            benchmarkModel.loadBenchmarkPerfParams(currentBenchmarkSingleCalcul);

            canceled = true;

            return true;
        }

        @Override
        public void run() {

            canceled = false;

            if (progressHandleMonitor == null) {
                throw new IllegalStateException("Progress Handle Monitor not set");
            }

            //temporary directory for benchmark
            String tmpdirPath = Paths.get(SystemUtils.getCacheDir().toString(), "snap-benchmark-tmp").toString();
            appContext.getPreferences().setPropertyString(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, tmpdirPath);

            //add current performance parameters to benchmark
            PerformanceParameters currentPerformanceParameters = ConfigurationOptimizer.getInstance().getActualPerformanceParameters();
            currentBenchmarkSingleCalcul = new BenchmarkSingleCalculus(
                    currentPerformanceParameters.getDefaultTileSize(), currentPerformanceParameters.getTileHeight(),currentPerformanceParameters.getTileWidth(),
                    currentPerformanceParameters.getCacheSize(),
                    currentPerformanceParameters.getNbThreads());

            if (!benchmarkModel.isAlreadyInList(currentBenchmarkSingleCalcul)) {
                benchmarkModel.addBenchmarkCalcul(currentBenchmarkSingleCalcul);
            }

            try {

                progressHandleMonitor.beginTask("Benchmark running... ", benchmarkModel.getBenchmarkCalculus().size() * 100);

                List<BenchmarkSingleCalculus> benchmarkSingleCalculusList = benchmarkModel.getBenchmarkCalculus();

                int executionOrder = 0;

                for (BenchmarkSingleCalculus benchmarkSingleCalcul : benchmarkSingleCalculusList) {
                    /*progressHandleMonitor.getProgressHandle().progress(
                            String.format("Benchmarking ( tile size:%s , cache size:%d , nb threads:%d )",
                                          benchmarkSingleCalcul.getDimensionString(),
                                          benchmarkSingleCalcul.getCacheSize(),
                                          benchmarkSingleCalcul.getNbThreads()));*/

                    progressHandleMonitor.getProgressHandle().progress(
                            String.format("Benchmarking ( cache size:%d , nb threads:%d )",
                                          benchmarkSingleCalcul.getCacheSize(),
                                          benchmarkSingleCalcul.getNbThreads()));

                    final Product targetProduct;

                    //load performance parameters for current benchmark
                    benchmarkModel.loadBenchmarkPerfParams(benchmarkSingleCalcul);

                    //processing start time
                    long startTime = System.currentTimeMillis();
                    
                    try {
                        targetProduct = createTargetProduct();

                    } catch (Throwable t) {
                        handleInitialisationError(t);
                        throw t;
                    }
                    if (targetProduct == null) {
                        throw new NullPointerException("Target product is null.");
                    }

                    //When the source product is read at the beginning, a preferred tile size is selected (tipically, the tile size of the properties).
                    //There are some operators which do not use the properties for setting the tile size of the product and they use directly the tile size of the inputs.
                    //Since the inputs are loaded only one time, the first tile size is always used.
                    //In the line below, we re-write that preferred tile size in order to generate the output with the benchmark value.
                    //TODO review because getTile from benchmarkSingleCalcul could be null or *...
                    //targetProduct.setPreferredTileSize(new Dimension(Integer.parseInt(benchmarkSingleCalcul.getTileWidth()),Integer.parseInt(benchmarkSingleCalcul.getTileHeight())));

                    executeOperator(targetProduct, progressHandleMonitor);

                    //save execution time
                    long endTime = System.currentTimeMillis();
                    benchmarkSingleCalcul.setExecutionTime(endTime - startTime);
                    benchmarkSingleCalcul.setExecutionOrder(executionOrder);
                    executionOrder++;

                    SystemUtils.LOG.fine(String.format("Start time: %d, end time: %d, diff: %d", startTime, endTime, endTime - startTime));

                    // we remove all tiles
                    //TODO cambiar, esto solo funciona si es cache in memery, pero no si es en file
                    JAI.getDefaultInstance().getTileCache().flush();
                }

                progressHandleMonitor.done();

            } catch (Exception ex) {
                SystemUtils.LOG.severe("Could not perform benchmark: " + ex.getMessage());
            } finally {
                //load old params (before benchmark)
                benchmarkModel.loadBenchmarkPerfParams(currentBenchmarkSingleCalcul);
                //delete benchmark TMP directory
                VirtualDir.deleteFileTree(new File(tmpdirPath));
            }

            managePostBenchmark();
        }


        private void managePostBenchmark() {
            if (!canceled) {
                //sort benchmark results and return the fastest
                BenchmarkSingleCalculus bestBenchmarkSingleCalcul = benchmarkModel.getFasterBenchmarkSingleCalculus();
                //load fastest params?
                benchmarkModel.loadBenchmarkPerfParams(bestBenchmarkSingleCalcul);

                showResults();

                //update parent panel with best values
                perfPanel.updatePerformanceParameters(bestBenchmarkSingleCalcul);
            }

            close();
        }

        private void showResults() {
            // table model
            class BenchmarkTableModel extends AbstractTableModel {
                //final String[] columnNames = benchmarkModel.getColumnsNames();
                //final int[][] data = benchmarkModel.getRowsToShow();

                final String[] columnNames = benchmarkModel.getColumnsNamesWithoutTileDimension();
                final int[][] data = benchmarkModel.getRowsToShowWhitoutTileDimension();


                @Override
                public Class getColumnClass(int column) {
                    return Integer.class;
                }

                public int getColumnCount() {
                    return columnNames.length;
                }

                public int getRowCount() {
                    return data.length;
                }

                public String getColumnName(int col) {
                    return columnNames[col];
                }

                public Object getValueAt(int row, int col) {
                    return data[row][col];
                }
            }

            BenchmarkTableModel tableModel = new BenchmarkTableModel();
            JTable table = new JTable(tableModel);

            // For sorting
            TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(tableModel);
            table.setRowSorter(rowSorter);

            DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
            tcr.setHorizontalAlignment(SwingConstants.CENTER);
            table.getColumnModel().getColumn(table.getColumnCount() - 1).setCellRenderer(tcr);

            JPanel panel = new JPanel(new BorderLayout(4, 4));
            JScrollPane panelTable = new JScrollPane(table);
            panel.add(panelTable, BorderLayout.CENTER);
            NotifyDescriptor d = new NotifyDescriptor(panel, "Benchmark results", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null);
            DialogDisplayer.getDefault().notify(d);
        }
    }
}
