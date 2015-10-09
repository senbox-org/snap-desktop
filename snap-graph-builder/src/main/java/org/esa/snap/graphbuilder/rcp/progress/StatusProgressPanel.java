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
package org.esa.snap.graphbuilder.rcp.progress;

import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class StatusProgressPanel extends JPanel implements StatusProgressMonitor.Listener
{
    private final StatusProgressMonitor pm;

    ProgressHandle p;

    public StatusProgressPanel(final StatusProgressMonitor pm) {
        this.pm = pm;
        pm.addListener(this);

        p = ProgressHandleFactory.createHandle(pm.getName());
        p.start(100);
        p.switchToDeterminate(100);
    }

    private void update() {
        runInUI(new Runnable() {
            public void run() {
                if (pm != null) {
                    p.progress(pm.getText(), pm.getPercentComplete());
                }
            }
        });
    }

    private void runInUI(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public void notifyMsg(final StatusProgressMonitor.Notification msg) {
        if (msg.equals(StatusProgressMonitor.Notification.UPDATE)) {
            update();
        } else if (msg.equals(StatusProgressMonitor.Notification.DONE)) {
            p.finish();
        }
    }
}
