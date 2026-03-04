package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.speclib.model.SpectralAxis;

import java.util.*;


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

    public static List<Band> sortSpectralBandsByWavelength(List<Band> bands) {
        List<Band> spectral = new ArrayList<>();
        for (Band b : bands) {
            if (b != null && !b.isFlagBand() && b.getSpectralWavelength() > 0.0f) {
                spectral.add(b);
            }
        }

        spectral.sort(Comparator.comparingDouble(Band::getSpectralWavelength));
        if (spectral.isEmpty()) {
            throw new IllegalArgumentException("No spectral bands (wavelength > 0) found");
        }
        return spectral;
    }


    public record AxisBandSelection(List<Band> bandsOrdered, Set<String> bandNames) {}


    public static AxisBandSelection selectAxisBandsUniqueByWavelength(List<Band> bands) {
        Objects.requireNonNull(bands, "bands must not be null");
        LinkedHashMap<Long, Band> bestByWl = new LinkedHashMap<>();

        for (Band b : bands) {
            if (b == null || b.isFlagBand() || b.getSpectralWavelength() <= 0.0f) {
                continue;
            }

            double wl = b.getSpectralWavelength();
            long key = Math.round(wl * 1_000_000d);

            Band cur = bestByWl.get(key);
            if (cur == null || isBetterAxisBand(b, cur)) {
                bestByWl.put(key, b);
            }
        }

        if (bestByWl.isEmpty()) {
            throw new IllegalArgumentException("No spectral bands (wavelength > 0) found");
        }

        List<Band> ordered = new ArrayList<>(bestByWl.values());
        ordered.sort(Comparator.comparingDouble(Band::getSpectralWavelength)
                .thenComparing(Band::getName, Comparator.nullsLast(String::compareTo)));

        Set<String> names = new LinkedHashSet<>();
        for (Band b : ordered) {
            if (b != null && b.getName() != null) {
                names.add(b.getName());
            }
        }

        return new AxisBandSelection(List.copyOf(ordered), Collections.unmodifiableSet(names));
    }

    public static SpectralAxis axisFromOrderedBands(List<Band> orderedSpectralBands) {
        double[] wl = new double[orderedSpectralBands.size()];
        for (int i = 0; i < orderedSpectralBands.size(); i++) {
            wl[i] = orderedSpectralBands.get(i).getSpectralWavelength();
        }
        return new SpectralAxis(wl, "nm");
    }


    private static boolean isBetterAxisBand(Band candidate, Band current) {
        int rc = Integer.compare(axisBandRank(candidate), axisBandRank(current));
        if (rc != 0) {
            return rc > 0;
        }

        rc = Boolean.compare(candidate.isScalingApplied(), current.isScalingApplied());
        if (rc != 0) {
            return rc > 0;
        }

        String cn = candidate.getName();
        String on = current.getName();
        if (cn == null && on == null) {
            return false;
        }
        if (cn == null) {
            return false;
        }
        if (on == null) {
            return true;
        }
        return cn.compareTo(on) < 0;
    }

    private static int axisBandRank(Band b) {
        if (b == null) {
            return Integer.MIN_VALUE;
        }

        return switch (b.getDataType()) {
            case ProductData.TYPE_FLOAT64 -> 4;
            case ProductData.TYPE_FLOAT32 -> 3;
            case ProductData.TYPE_INT32, ProductData.TYPE_UINT32 -> 2;
            case ProductData.TYPE_INT16, ProductData.TYPE_UINT16 -> 1;
            case ProductData.TYPE_INT8, ProductData.TYPE_UINT8 -> 0;
            default -> -1;
        };
    }
}
