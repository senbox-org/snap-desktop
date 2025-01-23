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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.math.IndexValidator;
import org.esa.snap.core.util.math.Range;
import org.esa.snap.ui.diagram.AbstractDiagramGraph;

import java.util.Arrays;
import java.util.Comparator;


class SpectrumGraph extends AbstractDiagramGraph {

    private Placemark placemark;
    private Band[] bands;
    private double[] energies;
    private double[] wavelengths;
    private final Range energyRange;
    private final Range wavelengthRange;

    SpectrumGraph(Placemark placemark, Band[] bands) {
        Debug.assertNotNull(bands);
        this.placemark = placemark;
        this.bands = bands;
        energyRange = new Range();
        wavelengthRange = new Range();
        setBands(bands);
    }

    SpectrumGraph() {
        // this one is just for testing tb 2024-02-22
        energyRange = new Range();
        wavelengthRange = new Range();
    }

    @Override
    public String getXName() {
        return "Wavelength";
    }

    @Override
    public String getYName() {
        return placemark != null ? placemark.getLabel() : "Cursor";
    }

    @Override
    public int getNumValues() {
        return bands.length;
    }

    @Override
    public double getXValueAt(int index) {
        return wavelengths[index];
    }

    @Override
    public double getYValueAt(int index) {
        if (energies[index] == bands[index].getGeophysicalNoDataValue()) {
            return Double.NaN;
        }
        return energies[index];
    }

    @Override
    public double getXMin() {
        return wavelengthRange.getMin();
    }

    @Override
    public double getXMax() {
        return wavelengthRange.getMax();
    }

    @Override
    public double getYMin() {
        return energyRange.getMin();
    }

    @Override
    public double getYMax() {
        return energyRange.getMax();
    }

    void setBands(Band[] bands) {
        Debug.assertNotNull(bands);
        this.bands = bands.clone();
        Arrays.sort(this.bands, new Comparator<Band>() {
            @Override
            public int compare(Band band1, Band band2) {
                final float v = band1.getSpectralWavelength() - band2.getSpectralWavelength();
                return v < 0.0F ? -1 : v > 0.0F ? 1 : 0;
            }
        });
        if (wavelengths == null || wavelengths.length != this.bands.length) {
            wavelengths = new double[this.bands.length];
        }
        if (energies == null || energies.length != this.bands.length) {
            energies = new double[this.bands.length];
        }
        for (int i = 0; i < wavelengths.length; i++) {
            wavelengths[i] = this.bands[i].getSpectralWavelength();
            energies[i] = 0.0f;
        }
        Range.computeRangeDouble(wavelengths, IndexValidator.TRUE, wavelengthRange, ProgressMonitor.NULL);
        Range.computeRangeDouble(energies, IndexValidator.TRUE, energyRange, ProgressMonitor.NULL);
    }

    void setEnergies(double[] energies) {
        this.energies = energies;
        Range.computeRangeDouble(energies, IndexValidator.TRUE, energyRange, ProgressMonitor.NULL);
    }

    @Override
    public void dispose() {
        placemark = null;
        bands = null;
        energies = null;
        wavelengths = null;
        super.dispose();
    }
}
