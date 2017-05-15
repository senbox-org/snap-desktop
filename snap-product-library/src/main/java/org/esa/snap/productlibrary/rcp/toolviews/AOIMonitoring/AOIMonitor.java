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

import org.esa.snap.graphbuilder.rcp.dialogs.BatchGraphDialog;
import org.esa.snap.graphbuilder.rcp.progress.LabelBarProgressMonitor;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.AOI;
import org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model.DirectoryWatch;
import org.esa.snap.productlibrary.rcp.toolviews.DBScanner;
import org.esa.snap.productlibrary.rcp.toolviews.model.ProductLibraryConfig;
import org.esa.snap.rcp.SnapApp;

import java.io.File;
import java.util.ArrayList;

/**
 *
 */

class AOIMonitor {

    private AOI[] aoiList = new AOI[]{};
    private final ArrayList<AOI> aoiQueue = new ArrayList<>();
    private final DirectoryWatch dirWatch = new DirectoryWatch();
    private final AOIMonitoringToolView aoiMonitoringToolview;
    private final static int WATCH_RATE = 5000;         // time in milliseconds between directory watches
    private boolean isStarted = false;
    private AOI processingAOI = null;
    private final ProductLibraryConfig libConfig = new ProductLibraryConfig(SnapApp.getDefault().getPreferences());

    AOIMonitor(final AOIMonitoringToolView toolview) {
        this.aoiMonitoringToolview = toolview;
        dirWatch.addListener(new DirWatchListener());
    }

    synchronized void start(final AOI[] aoiList) {
        this.aoiList = aoiList;
        for (AOI aoi : aoiList) {
            dirWatch.add(new File(aoi.getInputFolder()));
        }
        dirWatch.start(WATCH_RATE);
        isStarted = true;
    }

    synchronized void stop() {
        dirWatch.stop();
        dirWatch.removeAll();
        isStarted = false;
    }

    boolean isStarted() {
        return isStarted;
    }

    private void monitorAOI() {
        if (aoiQueue.isEmpty()) return;
        dirWatch.stop();

        processingAOI = aoiQueue.get(0);
        aoiQueue.remove(0);

        try {
            final LabelBarProgressMonitor progMon = aoiMonitoringToolview.createNewProgressMonitor();
            libConfig.addBaseDir(new File(processingAOI.getInputFolder()));

            final DBScanner.Options options = new DBScanner.Options(true, false, false);
            final DBScanner scanner = new DBScanner(aoiMonitoringToolview.getProductDatabase(),
                                                    new File(processingAOI.getInputFolder()), options, progMon);
            scanner.addListener(new DatabaseScannerListener(aoiMonitoringToolview.getProductDatabase(), processingAOI, true, true,
                                                            new MonitorBatchProcessListener(),
                                                            aoiMonitoringToolview.getAoiManager()));
            scanner.execute();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private class DirWatchListener implements DirectoryWatch.DirectoryWatchListener {
        public void filesAdded(File[] files) {
            for (File f : files) {
                final String filePath = f.getParent();
                for (AOI aoi : aoiList) {
                    if (!aoiQueue.contains(aoi) && aoi.getInputFolder().equalsIgnoreCase(filePath)) {
                        aoiQueue.add(aoi);
                    }
                }
            }
            monitorAOI();
        }

        public void filesRemoved(File[] files) {
            // don't care
        }
    }

    private class MonitorBatchProcessListener implements BatchGraphDialog.BatchProcessListener {
        public synchronized void notifyMSG(final BatchMSG msg, final File[] inputFileList, final File[] outputFileList) {
            if (msg.equals(BatchGraphDialog.BatchProcessListener.BatchMSG.DONE)) {
                if (processingAOI != null) {
                    for (int i = 0; i < outputFileList.length; ++i) {
                        aoiMonitoringToolview.getAoiManager().setBatchProcessResult(processingAOI,
                                                                                    inputFileList[i], outputFileList[i]);
                    }
                    processingAOI = null;
                }

                if (aoiQueue.isEmpty()) {
                    dirWatch.start(WATCH_RATE);
                } else {
                    monitorAOI();
                }
            }
        }

        public synchronized void notifyMSG(final BatchMSG msg, final String text) {
            if (msg.equals(BatchMSG.CLOSE)) {
                if (aoiQueue.isEmpty()) {
                    dirWatch.start(WATCH_RATE);
                } else {
                    monitorAOI();
                }
            }
        }
    }
}
