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

import org.esa.snap.core.dataop.downloadable.ProgressMonitorList;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.JPanel;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

@ServiceProvider(service = StatusLineElementProvider.class, position = 1)
public class StatusProgress implements StatusLineElementProvider, ProgressMonitorList.Listener
{
    private final JPanel statusPanel = new JPanel();
    private final Map<StatusProgressMonitor, StatusProgressPanel> progressPanelMap = new HashMap<>();

    public StatusProgress() {
        ProgressMonitorList.instance().addListener(this);
    }

    @Override
    public Component getStatusLineElement()
    {
        return statusPanel;
    }

    public void notifyMsg(final ProgressMonitorList.Notification msg, final StatusProgressMonitor pm) {
        if(msg.equals(ProgressMonitorList.Notification.ADD)) {
            final StatusProgressPanel progressPanel = new StatusProgressPanel(pm);
            progressPanelMap.put(pm, progressPanel);

            statusPanel.add(progressPanel);

        } else if(msg.equals(ProgressMonitorList.Notification.REMOVE)) {
            final StatusProgressPanel progressPanel = progressPanelMap.get(pm);

            statusPanel.remove(progressPanel);
            progressPanelMap.remove(pm);
        }
    }
}
