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

package org.esa.snap.ui.tooladapter.dialogs.progress;

import com.bc.ceres.core.ProgressMonitor;
import org.netbeans.api.progress.ProgressHandle;

/**
 * @author kraftek
 * @date 3/14/2017
 */
public class ProgressHandler implements ProgressMonitor {

    private ProgressHandle progressHandle;
    private boolean isIndeterminate;
    private ConsoleConsumer console;

    public ProgressHandler(ProgressHandle handle, boolean indeterminate) {
        this.progressHandle = handle;
        this.isIndeterminate = indeterminate;
        this.progressHandle.setInitialDelay(10);
    }

    public void setConsumer(ConsoleConsumer consumer) {
        console = consumer;
    }

    @Override
    public void beginTask(String taskName, int totalWork) {
        this.progressHandle.setDisplayName(taskName);
        this.progressHandle.start(totalWork, -1);
        if (this.isIndeterminate) {
            this.progressHandle.switchToIndeterminate();
        }
        if (this.console != null) {
            this.console.setVisible(true);
        }
    }

    @Override
    public void done() {
        this.progressHandle.finish();
    }

    @Override
    public void internalWorked(double work) {
        this.progressHandle.progress((int) work);
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.progressHandle.suspend("Cancelled");
    }

    @Override
    public void setTaskName(String taskName) {
        this.progressHandle.setDisplayName(taskName);
    }

    @Override
    public void setSubTaskName(String subTaskName) {
        this.progressHandle.progress(subTaskName);
    }

    @Override
    public void worked(int work) {
        internalWorked(work);
    }

}
