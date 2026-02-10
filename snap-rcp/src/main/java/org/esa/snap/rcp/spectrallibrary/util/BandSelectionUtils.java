package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public final class BandSelectionUtils {


    public static List<Band> getSpectralBands(Product product) {
        if (product == null) {
            return List.of();
        }

        List<Band> out = new ArrayList<>();
        for (Band b : product.getBands()) {
            if (b == null) {
                continue;
            }
            if (b.isFlagBand()) {
                continue;
            }
            if (b.getSpectralWavelength() <= 0.0f) {
                continue;
            }
            out.add(b);
        }

        out.sort(Comparator.comparingDouble(Band::getSpectralWavelength));
        return out;
    }
}

