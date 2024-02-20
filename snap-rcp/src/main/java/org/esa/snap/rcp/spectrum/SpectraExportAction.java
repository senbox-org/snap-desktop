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

package org.esa.snap.rcp.spectrum;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.util.PreferencesPropertyMap;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.diagram.DiagramGraph;
import org.esa.snap.ui.diagram.DiagramGraphIO;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

//import org.esa.snap.visat.VisatApp;

class SpectraExportAction extends AbstractAction {

    private SpectrumTopComponent spectrumTopComponent;

    public SpectraExportAction(SpectrumTopComponent spectrumTopComponent) {
        super("exportSpectra");
        this.spectrumTopComponent = spectrumTopComponent;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        exportSpectra();
    }


    private void exportSpectra() {
        final List<DisplayableSpectrum> selectedSpectra = spectrumTopComponent.getSelectedSpectra();
        Placemark[] pins = spectrumTopComponent.getDisplayedPins();
        Map<Placemark, Map<Band, Double>> energiesMap = spectrumTopComponent.getPinToEnergies();
        final List<SpectrumGraph> spectrumGraphList = new ArrayList<SpectrumGraph>();
        for (Placemark pin : pins) {
            for (DisplayableSpectrum spectrumInDisplay : selectedSpectra) {
                final SpectrumGraph spectrumGraph = new SpectrumGraph(pin, spectrumInDisplay.getSelectedBands());
                spectrumGraph.readValues();
                spectrumGraphList.add(spectrumGraph);
            }
        }
        DiagramGraph[] pinGraphs = spectrumGraphList.toArray(new DiagramGraph[0]);

        final Preferences preferences = SnapApp.getDefault().getPreferences();
        final PreferencesPropertyMap preferencesPropertyMap = new PreferencesPropertyMap(preferences);
        DiagramGraphIO.writeGraphs(SwingUtilities.getWindowAncestor(spectrumTopComponent),
                                   "Export Pin Spectra",
                                   new SnapFileFilter[]{DiagramGraphIO.SPECTRA_CSV_FILE_FILTER},
                                   preferencesPropertyMap,
                                   pinGraphs);
    }

}
