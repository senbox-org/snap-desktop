package org.esa.snap.ui.product.spectrum;

import org.esa.snap.core.datamodel.Band;

public interface Spectrum {

    String getName();

    Band[] getSpectralBands();

    boolean hasBands();

}
