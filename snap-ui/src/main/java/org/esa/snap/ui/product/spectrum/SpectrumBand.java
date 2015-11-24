package org.esa.snap.ui.product.spectrum;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Band;

/**
 * @author Tonio Fincke
 */
public class SpectrumBand {

    private final Band band;
    private boolean isSelected;

    public SpectrumBand(Band band, boolean isSelected) {
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
