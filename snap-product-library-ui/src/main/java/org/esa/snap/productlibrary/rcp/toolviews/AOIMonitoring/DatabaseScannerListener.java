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


import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.productlibrary.db.DBQuery;
import org.esa.snap.productlibrary.db.ProductDB;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.graphbuilder.rcp.dialogs.BatchGraphDialog;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOI;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOIManager;
import org.esa.snap.productlibrary.rcp.toolviews.DBScanner;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**

 */

public class DatabaseScannerListener implements DBScanner.DBScannerListener {

    private final ProductDB db;
    private final AOI processingAOI;
    private final boolean autoApply;
    private final boolean closeOnDone;
    private final BatchGraphDialog.BatchProcessListener batchListener;
    private final AOIManager aoiManager;

    DatabaseScannerListener(final ProductDB database, final AOI aoi,
                                   final boolean automaticallyApply, final boolean closeOnDone,
                                   final BatchGraphDialog.BatchProcessListener listener,
                                   final AOIManager aoiManager) {
        this.db = database;
        this.processingAOI = aoi;
        this.autoApply = automaticallyApply;
        this.closeOnDone = closeOnDone;
        this.batchListener = listener;
        this.aoiManager = aoiManager;
    }

    public void notifyMSG(final DBScanner dbScanner, final MSG msg) {
        if (msg == DBScanner.DBScannerListener.MSG.FOLDERS_SCANNED && processingAOI != null) {
            ProductEntry[] productEntryList = findInputProducts(db, processingAOI);
            if (productEntryList == null) return;
            if (productEntryList.length == 0) {
                if (!closeOnDone) {
                    Dialogs.showWarning("No input products found matching the selected criteria\nor all products have already been processed");
                }
                batchListener.notifyMSG(BatchGraphDialog.BatchProcessListener.BatchMSG.CLOSE, "");
                return;
            }
            final Map<File, File[]> slaveFileMap = new HashMap<>();
            if (processingAOI.getFindSlaves()) {
                final ArrayList<ProductEntry> toProcessEntries = new ArrayList<>();
                for (ProductEntry entry : productEntryList) {
                    final File[] slaveFiles = findCCDPairs(db, entry, processingAOI);
                    if (slaveFiles != null && slaveFiles.length > 0 && entry.getFile() != null) {
                        slaveFileMap.put(entry.getFile(), slaveFiles);
                        toProcessEntries.add(entry);
                    }
                }

                productEntryList = toProcessEntries.toArray(new ProductEntry[toProcessEntries.size()]);
            }

            if (productEntryList.length == 0) {
                if (!closeOnDone) {
                    Dialogs.showWarning("No slave products founds");
                }
                batchListener.notifyMSG(BatchGraphDialog.BatchProcessListener.BatchMSG.CLOSE, "");
                return;
            }
            batchProcess(productEntryList, slaveFileMap,
                         new File(processingAOI.getOutputFolder()),
                         new File(processingAOI.getProcessingGraph()));
        }
    }

    private void batchProcess(final ProductEntry[] productEntryList, final Map<File, File[]> slaveFileMap,
                              final File outputFolder, final File graphFile) {
        final BatchGraphDialog batchDlg = new BatchGraphDialog(SnapApp.getDefault().getAppContext(),
                                                               "Batch Processing", "batchProcessing", closeOnDone);
        batchDlg.addListener(batchListener);
        batchDlg.setInputFiles(productEntryList);
        batchDlg.setTargetFolder(outputFolder);
        batchDlg.setSlaveFileMap(slaveFileMap);
        if (graphFile != null) {
            batchDlg.LoadGraph(graphFile);
        }
        batchDlg.show();
        if (autoApply)
            batchDlg.onApply();
    }

    private ProductEntry[] findInputProducts(final ProductDB db, final AOI aoi) {
        final DBQuery dbQuery = new DBQuery();
        dbQuery.setBaseDir(new File(aoi.getInputFolder()));
        dbQuery.setSelectionRect(aoi.getPoints());
        try {
            final ProductEntry[] queryResult = dbQuery.queryDatabase(db);
            final ArrayList<ProductEntry> inputEntries = new ArrayList<>();
            for (ProductEntry entry : queryResult) {
                if (!aoiManager.hasBeenBatchedProcessed(aoi, entry.getFile()))
                    inputEntries.add(entry);
            }
            return inputEntries.toArray(new ProductEntry[inputEntries.size()]);

        } catch (Throwable t) {
            Dialogs.showError("Query database error:"+t.getMessage());
            return null;
        }
    }

    private static File[] findCCDPairs(final ProductDB db, final ProductEntry master, final AOI aoi) {
        DBQuery dbQuery = aoi.getSlaveDBQuery();
        if (dbQuery == null)
            dbQuery = new DBQuery();
        dbQuery.setExcludeDir(new File(aoi.getInputFolder()));
        dbQuery.setFreeQuery(AbstractMetadata.PRODUCT + " <> '" + master.getName() + "'");
        dbQuery.setSelectionRect(master.getGeoBoundary());
        try {
            final ProductEntry[] entries = dbQuery.queryDatabase(db);
            return ProductEntry.getFileList(getClosestDatePairs(entries, master, aoi));
        } catch (Throwable t) {
            Dialogs.showError("Query database error:"+t.getMessage());
            return null;
        }
    }

    private static ProductEntry[] getClosestDatePairs(final ProductEntry[] entries,
                                                      final ProductEntry master, final AOI aoi) {
        final double masterTime = master.getFirstLineTime().getMJD();
        double cutoffTime = masterTime;
        final DBQuery slaveQuery = aoi.getSlaveDBQuery();
        if (slaveQuery != null && slaveQuery.getEndDate() != null) {
            final double endTime = ProductData.UTC.create(slaveQuery.getEndDate().getTime(), 0).getMJD();
            if (endTime > masterTime)
                cutoffTime = endTime;
        }

        final ArrayList<ProductEntry> resultList = new ArrayList<>(aoi.getMaxSlaves());
        final Map<Double, ProductEntry> timesMap = new HashMap<>();
        final ArrayList<Double> diffList = new ArrayList<>();
        // find all before masterTime
        for (ProductEntry entry : entries) {
            final double entryTime = entry.getFirstLineTime().getMJD();
            if (entryTime < cutoffTime) {
                final double diff = Math.abs(masterTime - entryTime);
                timesMap.put(diff, entry);
                diffList.add(diff);
            }
        }
        Collections.sort(diffList);
        // select only the closest up to maxPairs
        for (Double diff : diffList) {
            resultList.add(timesMap.get(diff));
            if (resultList.size() >= aoi.getMaxSlaves())
                break;
        }

        return resultList.toArray(new ProductEntry[resultList.size()]);
    }
}
