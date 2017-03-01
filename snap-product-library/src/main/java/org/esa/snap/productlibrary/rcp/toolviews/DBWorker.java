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
import org.esa.snap.engine_utilities.db.ProductDB;

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

    private final ProgressMonitor pm;
    private final List<DBWorkerListener> listenerList = new ArrayList<>(1);

    private enum TYPE {REMOVE, QUERY}

    /**
     * @param pm       the progress monitor
     */
    public DBWorker(final ProgressMonitor pm) {
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

    /**
     * @param database the database
     * @param baseDir  the basedir to remove. If null, all entries will be removed
     */
    public void removeProducts(final ProductDB database, final File baseDir) {
        operationType = TYPE.REMOVE;
        this.db = database;
        this.baseDir = baseDir;

        execute();
    }

    /**
     * @param database the database
     * @param baseDir  the basedir to remove. If null, all entries will be removed
     */
    public void queryProducts(final DatabasePane dbPane) {
        operationType = TYPE.QUERY;
        this.dbPane = dbPane;

        execute();
    }

    @Override
    protected Boolean doInBackground() throws Exception {

        try {
            if(operationType.equals(TYPE.REMOVE)) {
                removeProducts();
            } else if(operationType.equals(TYPE.QUERY)) {
                dbPane.fullQuery(pm);
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
