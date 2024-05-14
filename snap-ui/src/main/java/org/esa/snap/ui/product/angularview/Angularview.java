package org.esa.snap.ui.product.angularview;

import org.esa.snap.core.datamodel.Band;

public interface Angularview {

    String getName();

    Band[] getAngularBands();

    boolean hasBands();

}
