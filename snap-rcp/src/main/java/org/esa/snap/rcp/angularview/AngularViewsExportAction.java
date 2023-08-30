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

package org.esa.snap.rcp.angularview;

import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.diagram.DiagramGraph;
import org.esa.snap.ui.diagram.DiagramGraphIO;
import org.esa.snap.ui.product.angularview.DisplayableAngularview;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

//import org.esa.snap.visat.VisatApp;

class AngularViewsExportAction extends AbstractAction {

    private AngularTopComponent angularViewTopComponent;

    public AngularViewsExportAction(AngularTopComponent angularViewTopComponent) {
        super("exportangularViews");
        this.angularViewTopComponent = angularViewTopComponent;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        exportangularViews();
    }


    private void exportangularViews() {
        final List<DisplayableAngularview> selectedangularViews = angularViewTopComponent.getSelectedAngularViews();
        Placemark[] pins = angularViewTopComponent.getDisplayedPins();
        final List<AngularGraph> angularViewGraphList = new ArrayList<AngularGraph>();
        for (Placemark pin : pins) {
            for (DisplayableAngularview angularViewInDisplay : selectedangularViews) {
                final AngularGraph angularViewGraph = new AngularGraph(pin, angularViewInDisplay.getSelectedBands());
                angularViewGraph.readValues();
                angularViewGraphList.add(angularViewGraph);
            }
        }
        DiagramGraph[] pinGraphs = angularViewGraphList.toArray(new DiagramGraph[0]);
        //todo move diagramgraphio to snap
        DiagramGraphIO.writeGraphs(SwingUtilities.getWindowAncestor(angularViewTopComponent),
                "Export Pin angularViews",
                new SnapFileFilter[]{DiagramGraphIO.SPECTRA_CSV_FILE_FILTER},
                SnapApp.getDefault().getPreferencesPropertyMap(),
                pinGraphs);
    }

}