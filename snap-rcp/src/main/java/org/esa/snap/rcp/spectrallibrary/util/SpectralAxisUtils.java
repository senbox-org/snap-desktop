package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.model.SpectralAxis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


public class SpectralAxisUtils {


    public static SpectralAxis axisFromBands(List<Band> bands) {
        Objects.requireNonNull(bands, "bands must not be null");

        List<Band> spectralBands = new ArrayList<>();
        for (Band b : bands) {
            if (b == null) {
                continue;
            }
            if (b.isFlagBand()) {
                continue;
            }
            if (b.getSpectralWavelength() <= 0.0f) {
                continue;
            }
            spectralBands.add(b);
        }

        spectralBands.sort(Comparator.comparingDouble(Band::getSpectralWavelength));

        if (spectralBands.isEmpty()) {
            throw new IllegalArgumentException("No spectral bands (wavelength > 0) found");
        }

        double[] wl = new double[spectralBands.size()];
        for (int i = 0; i < spectralBands.size(); i++) {
            wl[i] = spectralBands.get(i).getSpectralWavelength();
        }

        return new SpectralAxis(wl, "nm");
    }


    public static String defaultYUnitFromBands(List<Band> bands) {
        Objects.requireNonNull(bands, "bands must not be null");

        String yUnit = null;
        for (Band b : bands) {
            if (b == null || b.isFlagBand() || b.getSpectralWavelength() <= 0.0f) continue;
            String u = b.getUnit();
            if (u != null && !u.isBlank()) {
                yUnit = u;
                break;
            }
        }
        return yUnit;
    }

    public static SpectralAxis axisFromReferenceSpectralGroup(List<Band> bands) {
        List<Band> spectral = new ArrayList<>();
        for (Band b : bands) {
            if (b == null || b.isFlagBand() || b.getSpectralWavelength() <= 0.0f) {
                continue;
            }
            spectral.add(b);
        }
        spectral.sort(Comparator.comparingDouble(Band::getSpectralWavelength));
        if (spectral.isEmpty()) {
            throw new IllegalArgumentException("No spectral bands (wavelength > 0) found");
        }

        List<Double> uniqueWl = new ArrayList<>();
        double last = Double.NaN;
        for (Band b : spectral) {
            double wl = b.getSpectralWavelength();
            if (uniqueWl.isEmpty() || Double.compare(wl, last) != 0) {
                uniqueWl.add(wl);
                last = wl;
            }
        }

        double[] wl = new double[uniqueWl.size()];
        for (int i = 0; i < uniqueWl.size(); i++) {
            wl[i] = uniqueWl.get(i);
        }
        return new SpectralAxis(wl, "nm");
    }
}
