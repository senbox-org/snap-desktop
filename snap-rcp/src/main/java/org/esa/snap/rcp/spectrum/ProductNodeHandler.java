package org.esa.snap.rcp.spectrum;

import eu.esa.snap.core.datamodel.group.BandGroup;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;
import org.esa.snap.ui.product.spectrum.SpectrumBand;

/// //////////////////////////////////////////////////////////////////////
// Product change handling

class ProductNodeHandler extends ProductNodeListenerAdapter {

    private final SpectrumTopComponent spectrumTopComponent;

    public ProductNodeHandler(SpectrumTopComponent spectrumTopComponent) {
        this.spectrumTopComponent = spectrumTopComponent;
    }

    @Override
    public void nodeChanged(final ProductNodeEvent event) {
        boolean chartHasChanged = false;
        if (event.getSourceNode() instanceof Band) {
            final String propertyName = event.getPropertyName();
            if (propertyName.equals(DataNode.PROPERTY_NAME_UNIT)) {
                spectrumTopComponent.updateSpectraUnits();
                chartHasChanged = true;
            } else if (propertyName.equals(Band.PROPERTY_NAME_SPECTRAL_WAVELENGTH)) {
                spectrumTopComponent.setUpSpectra();
                chartHasChanged = true;
            }
        } else if (event.getSourceNode() instanceof Placemark) {
            if ("geoPos".equals(event.getPropertyName()) || "pixelPos".equals(event.getPropertyName())) {
                spectrumTopComponent.getChartHandler().removePinInformation((Placemark) event.getSourceNode());
            }
            if (spectrumTopComponent.isShowingPinSpectra()) {
                chartHasChanged = true;
            }
        } else if (event.getSourceNode() instanceof Product) {
            if ("autoGrouping".equals(event.getPropertyName())) {
                spectrumTopComponent.setUpSpectra();
                chartHasChanged = true;
            }
        }
        if (isActive() && chartHasChanged) {
            spectrumTopComponent.recreateChart();
        }
    }

    @Override
    public void nodeAdded(final ProductNodeEvent event) {
        if (!isActive()) {
            return;
        }
        if (event.getSourceNode() instanceof Band newBand) {
            if (SpectrumTopComponent.isSpectralBand(newBand)) {
                addBandToSpectra((Band) event.getSourceNode());
                spectrumTopComponent.recreateChart();
            }
        } else if (event.getSourceNode() instanceof Placemark) {
            if (spectrumTopComponent.isShowingPinSpectra()) {
                spectrumTopComponent.recreateChart();
            } else {
                spectrumTopComponent.updateUIState();
            }
        }
    }

    @Override
    public void nodeRemoved(final ProductNodeEvent event) {
        if (!isActive()) {
            return;
        }
        if (event.getSourceNode() instanceof Band band) {
            removeBandFromSpectra(band);
            spectrumTopComponent.getChartHandler().removeBandInformation(band);
            spectrumTopComponent.recreateChart();
        } else if (event.getSourceNode() instanceof Placemark) {
            if (spectrumTopComponent.isShowingPinSpectra()) {
                spectrumTopComponent.recreateChart();
            }
        }
    }

    private void addBandToSpectra(Band band) {
        Product bandProduct = band.getProduct();
        if (bandProduct != spectrumTopComponent.getCurrentProduct() && !spectrumTopComponent.getSecondaryProducts().contains(bandProduct)) {
            return;
        }
        DisplayableSpectrum[] allSpectra = spectrumTopComponent.getProductToSpectraMap().get(bandProduct);
        BandGroup autoGrouping = bandProduct.getAutoGrouping();
        if (autoGrouping != null) {
            final int bandIndex = autoGrouping.indexOf(band.getName());
            final DisplayableSpectrum spectrum;
            if (bandIndex != -1) {
                spectrum = allSpectra[bandIndex];
            } else {
                spectrum = allSpectra[allSpectra.length - 1];
            }
            spectrum.addBand(new SpectrumBand(band, spectrum.isSelected()));
        } else {
            allSpectra[0].addBand(new SpectrumBand(band, true));
        }
    }

    private void removeBandFromSpectra(Band band) {
        Product bandProduct = band.getProduct();
        DisplayableSpectrum[] allSpectra = spectrumTopComponent.getProductToSpectraMap().get(bandProduct);
        for (DisplayableSpectrum displayableSpectrum : allSpectra) {
            Band[] spectralBands = displayableSpectrum.getSpectralBands();
            for (int j = 0; j < spectralBands.length; j++) {
                Band spectralBand = spectralBands[j];
                if (spectralBand == band) {
                    displayableSpectrum.remove(j);
                    if (displayableSpectrum.getSelectedBands().length == 0) {
                        displayableSpectrum.setSelected(false);
                    }
                    return;
                }
            }
        }
    }

    private boolean isActive() {
        return spectrumTopComponent.isVisible() && spectrumTopComponent.getCurrentProduct() != null;
    }
}
