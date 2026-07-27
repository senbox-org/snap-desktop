package org.esa.snap.rcp.spectrum;

import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.rcp.placemark.PlacemarkUtils;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;

class SpectrumLegendItemSource implements LegendItemSource {

    private final SpectrumTopComponent spectrumTopComponent;

    SpectrumLegendItemSource(SpectrumTopComponent spectrumTopComponent) {
        this.spectrumTopComponent = spectrumTopComponent;
    }

    @Override
    public LegendItemCollection getLegendItems() {
        LegendItemCollection itemCollection = new LegendItemCollection();
        final Placemark[] displayedPins = spectrumTopComponent.getDisplayedPins();
        final List<DisplayableSpectrum> spectra = spectrumTopComponent.getSelectedSpectra();
        for (Placemark pin : displayedPins) {
            Paint pinPaint = PlacemarkUtils.getPlacemarkColor(pin, spectrumTopComponent.getCurrentView());
            spectra.stream().filter(DisplayableSpectrum::hasSelectedBands).forEach(spectrum -> {
                String legendLabel = pin.getLabel() + "_" + spectrum.getName();
                LegendItem item = createLegendItem(spectrum, pinPaint, legendLabel);
                itemCollection.add(item);
            });
        }
        if (spectrumTopComponent.isShowingCursorSpectrum()
                && spectrumTopComponent.showsValidCursorSpectra()
                && !spectrumTopComponent.isPixelPosNotAvailable()) {
            spectra.stream().filter(DisplayableSpectrum::hasSelectedBands).forEach(spectrum -> {
                LegendItem item = createLegendItem(spectrum, spectrum.getColor(), spectrum.getName());
                itemCollection.add(item);
            });
        }
        return itemCollection;
    }

    private LegendItem createLegendItem(DisplayableSpectrum spectrum, Paint paint, String legendLabel) {
        Stroke outlineStroke = new BasicStroke();
        Line2D lineShape = new Line2D.Double(0, 5, 40, 5);
        Stroke lineStyle = spectrum.getLineStyle();
        Shape symbol = spectrum.getScaledShape();
        return new LegendItem(legendLabel, legendLabel, legendLabel, legendLabel,
                true, symbol, false,
                paint, true, paint, outlineStroke,
                true, lineShape, lineStyle, paint);
    }

}
