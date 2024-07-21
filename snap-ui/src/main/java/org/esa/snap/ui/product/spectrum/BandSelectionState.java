package org.esa.snap.ui.product.spectrum;

class BandSelectionState {

    private int selectCount;

    BandSelectionState() {
        selectCount = 0;
    }

    boolean isSelected() {
        return selectCount > 0;
    }

    public void setSelected(boolean selected) {
        if (selected) {
            selectCount++;
        } else {
            selectCount--;
        }
    }
}
