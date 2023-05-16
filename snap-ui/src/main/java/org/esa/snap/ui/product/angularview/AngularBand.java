package org.esa.snap.ui.product.angularview;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Band;

/**
 */
public class AngularBand {

    private final Band band;
    private boolean isSelected;

    public AngularBand(Band band, boolean isSelected) {
        Assert.notNull(band);
        this.band = band;
        this.isSelected = isSelected;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    String getUnit() {
        return band.getUnit();
    }

    public Band getOriginalBand() {
        return band;
    }

    public String getName() {
        return band.getName();
    }

}
