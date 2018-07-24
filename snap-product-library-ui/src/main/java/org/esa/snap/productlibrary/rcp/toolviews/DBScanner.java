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
package org.esa.snap.productlibrary.rcp.toolviews;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.productlibrary.db.ProductDB;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.engine_utilities.gpf.ThreadManager;
import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.esa.snap.engine_utilities.util.ZipUtils;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans folders for products to add or update into the database
 */
public final class DBScanner extends SwingWorker {
    private final ProductDB db;

    private final File baseDir;
    private final Options options;
    private final ProgressMonitor pm;
    private final List<DBScannerListener> listenerList = new ArrayList<>(1);
    private final List<ErrorFile> errorList = new ArrayList<>();

    public DBScanner(final ProductDB database, final File baseDir, final Options options,
                     final ProgressMonitor pm) {
        this.db = database;
        this.pm = pm;
        this.baseDir = baseDir;
        this.options = options;
    }

    public void addListener(final DBScannerListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(final DBScannerListener listener) {
        listenerList.remove(listener);
    }

    private void notifyMSG(final DBScannerListener.MSG msg) {
        for (final DBScannerListener listener : listenerList) {
            listener.notifyMSG(this, msg);
        }
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        errorList.clear();

        final List<File> dirList = new ArrayList<>(20);
        dirList.add(baseDir);
        if (options.doRecursive) {
            final File[] subDirs = collectAllSubDirs(baseDir, 0, pm);
            dirList.addAll(Arrays.asList(subDirs));
        }

        final ProductFunctions.ValidProductFileFilter fileFilter = new ProductFunctions.ValidProductFileFilter(false);
        final List<File> fileList = new ArrayList<>(dirList.size());
        for (File file : dirList) {
            final File[] files = file.listFiles(fileFilter);
            if (files != null) {
                fileList.addAll(Arrays.asList(files));
            }
            pm.setTaskName("Collecting " + fileList.size() + " files...");
        }

        final List<Product> qlProducts = new ArrayList<>(fileList.size());
        final ProductEntry[] entriesInPath = db.getProductEntryInPath(baseDir);
        final Map<File, ProductEntry> fileMap = new ConcurrentHashMap<>(entriesInPath.length);
        for (ProductEntry entry : entriesInPath) {
            fileMap.put(entry.getFile(), entry);
        }

        final int total = fileList.size();
        pm.beginTask("Scanning Files...", total);
        int i = 0;
        int prodCount = 0;
        try {
            for (File file : fileList) {
                ++i;
                String taskMsg = "Scanning " + i + " of " + total + " files ";
                if (prodCount > 0)
                    taskMsg += "(" + prodCount + " new products)";
                pm.setTaskName(taskMsg);
                pm.worked(1);

                if (pm.isCanceled())
                    break;

                if (options.validateZips) {
                    if (ZipUtils.isZip(file) && !ZipUtils.isValid(file)) {
                        errorList.add(new ErrorFile(file, ErrorFile.CORRUPT_ZIP));
                        continue;
                    }
                }

                // check if already exists in db
                final ProductEntry existingEntry = fileMap.get(file);

                if (existingEntry != null) {
                    // check for missing quicklook
                    if (options.generateQuicklooks && !existingEntry.quickLookExists()) {
                        final Product sourceProduct = CommonReaders.readProduct(file);
                        if (sourceProduct != null) {
                            qlProducts.add(sourceProduct);
                        }
                    }
                    existingEntry.dispose();
                    continue;
                }

                try {
                    // quick test for common readers
                    final Product sourceProduct = CommonReaders.readProduct(file);
                    if (sourceProduct != null) {
                        final ProductEntry entry = db.saveProduct(sourceProduct);
                        ++prodCount;
                        if (!sourceProduct.getDefaultQuicklook().hasCachedImage()) {
                            qlProducts.add(sourceProduct);
                            // product to be freed later
                        } else {
                            // free now
                            sourceProduct.dispose();
                        }
                        entry.dispose();
                    } else if (!file.isDirectory()) {
                        SystemUtils.LOG.warning("No reader for " + file.getAbsolutePath());
                    }
                } catch (Throwable e) {
                    errorList.add(new ErrorFile(file, ErrorFile.UNREADABLE));
                    SystemUtils.LOG.warning("Unable to read " + file.getAbsolutePath() + '\n' + e.getMessage());
                }
            }

            db.cleanUpRemovedProducts(pm);

            notifyMSG(DBScannerListener.MSG.FOLDERS_SCANNED);

            if (options.generateQuicklooks) {
                final int numQL = qlProducts.size();
                pm.beginTask("Generating Quicklooks...", numQL);
                final ThreadManager threadManager = new ThreadManager();
                threadManager.setNumConsecutiveThreads(Math.min(threadManager.getNumConsecutiveThreads(), 4));

                for (int j = 0; j < numQL; ++j) {
                    pm.setTaskName("Generating Quicklook... " + (j + 1) + " of " + numQL);
                    pm.worked(1);
                    if (pm.isCanceled())
                        break;

                    final Product product = qlProducts.get(j);

                    final StatusProgressMonitor qlPM = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
                    qlPM.beginTask("Creating quicklook " + product.getName() + "... ", 100);

                    final Thread worker = new Thread() {

                        @Override
                        public void run() {
                            try {
                                product.getDefaultQuicklook().getImage(qlPM);
                            } catch (Throwable e) {
                                SystemUtils.LOG.warning("Unable to create quicklook for " + product.getName() + '\n' + e.getMessage());
                            } finally {
                                product.dispose();
                                qlPM.done();
                            }
                        }
                    };
                    threadManager.add(worker);

                    notifyMSG(DBScannerListener.MSG.QUICK_LOOK_GENERATED);
                }
                threadManager.finish();
            }
            pm.setTaskName("");

        } catch (Throwable e) {
            SystemUtils.LOG.severe("Scanning Exception\n" + e.getMessage());
        } finally {
            pm.done();
        }
        return true;
    }

    @Override
    public void done() {
        notifyMSG(DBScannerListener.MSG.DONE);
    }

    private static File[] collectAllSubDirs(final File dir, int count, final ProgressMonitor pm) {
        final List<File> dirList = new ArrayList<>(20);
        final ProductFunctions.DirectoryFileFilter dirFilter = new ProductFunctions.DirectoryFileFilter();

        final File[] subDirs = dir.listFiles(dirFilter);
        if (subDirs != null) {
            count += subDirs.length;
            pm.setTaskName("Collecting " + count + " folders...");

            for (final File subDir : subDirs) {
                dirList.add(subDir);
                final File[] dirs = collectAllSubDirs(subDir, count, pm);
                dirList.addAll(Arrays.asList(dirs));
            }
        }
        return dirList.toArray(new File[dirList.size()]);
    }

    public List<ErrorFile> getErrorList() {
        return errorList;
    }

    public static class ErrorFile {
        public final File file;
        public final String message;
        public final static String CORRUPT_ZIP = "Corrupt zip file";
        public final static String CORRUPT_IMAGE = "Corrupt Image";
        public final static String UNREADABLE = "Product unreadable";

        public ErrorFile(final File file, final String msg) {
            this.file = file;
            this.message = msg;
        }
    }

    public static class Options {
        private final boolean doRecursive;
        private final boolean validateZips;
        private final boolean generateQuicklooks;

        public Options(final boolean doRecursive, final boolean validateZips, final boolean generateQuicklooks) {
            this.doRecursive = doRecursive;
            this.validateZips = validateZips;
            this.generateQuicklooks = generateQuicklooks;
        }
    }

    public interface DBScannerListener {

        enum MSG {DONE, FOLDERS_SCANNED, QUICK_LOOK_GENERATED}

        void notifyMSG(final DBScanner dbScanner, final MSG msg);
    }
}
