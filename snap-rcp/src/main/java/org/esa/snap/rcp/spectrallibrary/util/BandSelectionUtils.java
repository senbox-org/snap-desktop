package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


public final class BandSelectionUtils {


    public static List<Band> getSpectralBands(Product product) {
        return getSpectralBands(product, null);
    }


    public static List<Band> getSpectralBands(Product product, Set<String> allowedBandNames) {
        if (product == null) {
            return List.of();
        }

        boolean filter = allowedBandNames != null && !allowedBandNames.isEmpty();

        List<Band> out = new ArrayList<>();
        for (Band band : product.getBands()) {
            if (band == null) {
                continue;
            }
            if (band.isFlagBand()) {
                continue;
            }
            if (band.getSpectralWavelength() <= 0.0f) {
                continue;
            }

            if (filter && !allowedBandNames.contains(band.getName())) {
                continue;
            }
            out.add(band);
        }

        out.sort(Comparator.comparingDouble(Band::getSpectralWavelength));
        return out;
    }
}

