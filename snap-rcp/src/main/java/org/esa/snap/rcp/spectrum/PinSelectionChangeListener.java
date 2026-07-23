package org.esa.snap.rcp.spectrum;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class PinSelectionChangeListener implements PropertyChangeListener {

    private final SpectrumTopComponent spectrumTopComponent;

    PinSelectionChangeListener(SpectrumTopComponent spectrumTopComponent) {
        this.spectrumTopComponent = spectrumTopComponent;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        spectrumTopComponent.recreateChart();
    }

}
