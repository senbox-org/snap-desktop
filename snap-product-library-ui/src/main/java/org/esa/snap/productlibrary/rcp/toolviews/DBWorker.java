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
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.productlibrary.db.ProductDB;
import org.esa.snap.productlibrary.rcp.toolviews.extensions.ProductLibraryActionExt;

import javax.swing.*;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * thread operator on database
 */
public final class DBWorker extends SwingWorker<Boolean, Object> {

    private TYPE operationType;
    private File baseDir;
    private ProductDB db;
    private DatabasePane dbPane;
    private ProductLibraryActionExt action;

    private final ProgressMonitor pm;
    private final List<DBWorkerListener> listenerList = new ArrayList<>(1);

    public enum TYPE {REMOVE, QUERY, EXECUTEACTION }

    /**
     * @param database the database
     * @param baseDir  the basedir to remove. If null, all entries will be removed
     */
    public DBWorker(final TYPE type, final ProductDB database, final File baseDir, final ProgressMonitor pm) {
        this.operationType = type;
        this.db = database;
        this.baseDir = baseDir;
        this.pm = pm;
    }

    /**
     * @param database the database
     * @param baseDir  the basedir to remove. If null, all entries will be removed
     */
    public DBWorker(final TYPE type, final DatabasePane dbPane, final ProgressMonitor pm) {
        this.operationType = type;
        this.dbPane = dbPane;
        this.pm = pm;
    }

    public DBWorker(final TYPE type, final ProductLibraryActionExt action, final ProgressMonitor pm) {
        this.operationType = type;
        this.action = action;
        this.pm = pm;
    }

    public void addListener(final DBWorkerListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    private void notifyMSG(final DBWorkerListener.MSG msg) {
        for (final DBWorkerListener listener : listenerList) {
            listener.notifyMSG(msg);
        }
    }

    @Override
    protected Boolean doInBackground() throws Exception {

        try {
            if(operationType.equals(TYPE.REMOVE)) {
                removeProducts();
            } else if(operationType.equals(TYPE.QUERY)) {
                dbPane.fullQuery(pm);
            } else if(operationType.equals(TYPE.EXECUTEACTION)) {
                action.performAction(pm);
            }
        } catch (Throwable e) {
            SystemUtils.LOG.severe("DB Worker Exception\n" + e.getMessage());
        } finally {
            pm.done();
        }
        return true;
    }

    private void removeProducts() throws SQLException {
        if (baseDir == null) {
            db.removeAllProducts(pm);
        } else {
            db.removeProducts(baseDir, pm);
        }
    }

    @Override
    public void done() {
        notifyMSG(DBWorkerListener.MSG.DONE);
    }

    public interface DBWorkerListener {

        enum MSG {DONE}

        void notifyMSG(final MSG msg);
    }
}
