/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.ui.tooladapter.model;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.*;

/**
 * Simple graphical progress worker for a runnable task.
 *
 * @author  Cosmin Cara
 */
public class ProgressWorker extends ProgressMonitorSwingWorker {
    private final static SnapApp snapApp = SnapApp.getDefault();
    private String message;
    private Runnable task;

    public ProgressWorker(String title, String message, Runnable task) {
        super(snapApp.getMainFrame(), title);
        this.message = message;
        this.task = task;
    }

    @Override
    protected Object doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
        try {
            pm.beginTask(message, 1);
            SwingUtilities.invokeLater(() -> {
                snapApp.setStatusBarMessage(message);
                snapApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            });
            if (task != null) {
                task.run();
            }
        } catch (Throwable e) {
            snapApp.handleError("The operation failed.", e); //handleUnknownException(e);
        } finally {
            SwingUtilities.invokeLater(() -> snapApp.getMainFrame().setCursor(Cursor.getDefaultCursor()));
            snapApp.setStatusBarMessage("");
            pm.done();
        }
        return null;
    }
}