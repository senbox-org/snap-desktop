package org.esa.snap.framework.ui.product.spectrum;

import org.esa.snap.framework.datamodel.Band;

public interface Spectrum {

    String getName();

    Band[] getSpectralBands();

    boolean hasBands();

}
