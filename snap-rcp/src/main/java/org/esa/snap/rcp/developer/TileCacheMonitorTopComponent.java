/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.developer;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;

/**
 * Displays the state of SNAP's global image tile cache.
 *
 * @author Norman Fomferra
 */
@TopComponent.Description(
        preferredID = "TileCacheMonitorTopComponent",
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "rightSlidingSide", openAtStartup = false, position = 200, roles={"developer"})
@ActionID(category = "Window", id = "org.esa.snap.rcp.developer.TileCacheMonitorTopComponent")
@ActionReference(path = "Menu/View/Tool Windows/Developer", position = 20)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_TileCacheMonitorTopComponentName",
        preferredID = "TileCacheMonitorTopComponent"
)
@NbBundle.Messages({
        "CTL_TileCacheMonitorTopComponentName=Tile Cache Monitor",
        "CTL_TileCacheMonitorTopComponentDescription=Displays the state SNAP's global image tile cache",
})
public class TileCacheMonitorTopComponent extends TopComponent {
    public static final String ID = TileCacheMonitorTopComponent.class.getName();
    private Timer timer;
    private TileCacheMonitor tileCacheMonitor;

    public TileCacheMonitorTopComponent() {
        setLayout(new BorderLayout());
        setName(Bundle.CTL_TileCacheMonitorTopComponentName());
        setToolTipText(Bundle.CTL_TileCacheMonitorTopComponentDescription());
        tileCacheMonitor = new TileCacheMonitor();
        JPanel panel = tileCacheMonitor.createPanel();
        timer = new Timer(2000, e -> {
            if (isVisible()) {
                tileCacheMonitor.updateState();
            }
        });
        timer.setRepeats(true);
        timer.start();
        add(panel, BorderLayout.CENTER);
    }

    /**
     * The default implementation does nothing.
     * <p>Clients shall not call this method directly.</p>
     */
    @Override
    public void componentOpened() {
        timer.restart();
    }

    /**
     * The default implementation does nothing.
     * <p>Clients shall not call this method directly.</p>
     */
    @Override
    public void componentClosed() {
        timer.stop();
    }
}
