package org.esa.snap.rcp.spectrum;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.LineBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;

import java.awt.*;
import java.util.List;
import java.util.Map;

class ChartHandler {

    private static final String MESSAGE_NO_SPECTRAL_BANDS = "No spectral bands available";   /*I18N*/
    private static final String MESSAGE_NO_PRODUCT_SCENE_VIEW_SELECTED = "No product scene view selected";
    private static final String MESSAGE_NO_SPECTRA_SELECTED = "No spectra selected";
    private static final String MESSAGE_COLLECTING_SPECTRAL_INFORMATION = "Collecting spectral information...";

    private final SpectrumTopComponent spectrumTopComponent;
    private final JFreeChart chart;
    private final ChartUpdater chartUpdater;

    ChartHandler(SpectrumTopComponent spectrumTopComponent, JFreeChart chart) {
        this.spectrumTopComponent = spectrumTopComponent;
        chartUpdater = new ChartUpdater(this.spectrumTopComponent);
        this.chart = chart;
        setLegend(chart);
        setAutomaticRangeAdjustments(false);
        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
        renderer.setDefaultLinesVisible(true);
        renderer.setDefaultShapesFilled(false);
        setPlotMessage(MESSAGE_NO_PRODUCT_SCENE_VIEW_SELECTED);
    }

    void setAutomaticRangeAdjustments(boolean automaticRangeAdjustment) {
        final XYPlot plot = chart.getXYPlot();
        boolean adjustmentHasChanged = false;
        if (automaticRangeAdjustment) {
            if (!spectrumTopComponent.isAutomaticAdjustment()) {
                spectrumTopComponent.setAutomaticAdjustment(true);
                if (!isAutomaticDomainAdjustmentSet()) {
                    plot.getDomainAxis().setAutoRange(true);
                    spectrumTopComponent.setDomainAxisAdjustmentIsFrozen(false);
                    adjustmentHasChanged = true;
                }
                if (!isAutomaticRangeAdjustmentSet()) {
                    plot.getRangeAxis().setAutoRange(true);
                    spectrumTopComponent.setRangeAxisAdjustmentIsFrozen(false);
                    adjustmentHasChanged = true;
                }
            }
        } else {
            if (spectrumTopComponent.isAutomaticAdjustment()) {
                spectrumTopComponent.setAutomaticAdjustment(false);
                if (isAutomaticDomainAdjustmentSet()) {
                    plot.getDomainAxis().setAutoRange(false);
                    spectrumTopComponent.setDomainAxisAdjustmentIsFrozen(false);
                    adjustmentHasChanged = true;
                }
                if (isAutomaticRangeAdjustmentSet()) {
                    plot.getRangeAxis().setAutoRange(false);
                    spectrumTopComponent.setRangeAxisAdjustmentIsFrozen(false);
                    adjustmentHasChanged = true;
                }
            }
        }
        if (adjustmentHasChanged) {
            chartUpdater.invalidatePlotBounds();
        }
    }

    boolean isAutomaticDomainAdjustmentSet() {
        return chart.getXYPlot().getDomainAxis().isAutoRange();
    }

    boolean isAutomaticRangeAdjustmentSet() {
        return chart.getXYPlot().getRangeAxis().isAutoRange();
    }

    private void setLegend(JFreeChart chart) {
        chart.removeLegend();
        final LegendTitle legend = new LegendTitle(new SpectrumLegendItemSource(spectrumTopComponent));
        legend.setPosition(RectangleEdge.BOTTOM);
        LineBorder border = new LineBorder(Color.BLACK, new BasicStroke(), new RectangleInsets(2, 2, 2, 2));
        legend.setFrame(border);
        chart.addLegend(legend);
    }

    void setPosition(int pixelX, int pixelY, int level, boolean pixelPosInRasterBounds) {
        chartUpdater.setPosition(pixelX, pixelY, level, pixelPosInRasterBounds);
    }

    void updateChart() {
        if (chartUpdater.isDatasetEmpty()) {
            setEmptyPlot();
            return;
        }
        java.util.List<DisplayableSpectrum> spectra = spectrumTopComponent.getSelectedSpectra();
        chartUpdater.updateChart(chart, spectra);
        chart.getXYPlot().clearAnnotations();
    }

    void updateData() {
        List<DisplayableSpectrum> spectra = spectrumTopComponent.getSelectedSpectra();
        chartUpdater.updateData(chart, spectra);
    }

    void setEmptyPlot() {
        chart.getXYPlot().setDataset(null);
        if (spectrumTopComponent.getCurrentView() == null) {
            setPlotMessage(MESSAGE_NO_PRODUCT_SCENE_VIEW_SELECTED);
        } else if (!chartUpdater.showsValidCursorSpectra()) {
        } else if (spectrumTopComponent.getAllSpectra().length == 0) {
            setPlotMessage(MESSAGE_NO_SPECTRA_SELECTED);
        } else {
            setPlotMessage(MESSAGE_NO_SPECTRAL_BANDS);
        }
    }

    void setGridVisible(boolean visible) {
        chart.getXYPlot().setDomainGridlinesVisible(visible);
        chart.getXYPlot().setRangeGridlinesVisible(visible);
    }

    void removePinInformation(Placemark pin) {
        chartUpdater.removePinInformation(pin);
    }

    void removeBandInformation(Band band) {
        chartUpdater.removeBandinformation(band);
    }

    void setPlotMessage(String messageText) {
        chart.getXYPlot().clearAnnotations();
        if (messageText != null && !messageText.trim().isEmpty()) {
            TextTitle tt = new TextTitle(messageText);
            tt.setTextAlignment(HorizontalAlignment.RIGHT);
            tt.setFont(chart.getLegend().getItemFont());
            tt.setBackgroundPaint(new Color(200, 200, 255, 50));
            tt.setFrame(new BlockBorder(Color.white));
            tt.setPosition(RectangleEdge.BOTTOM);
            XYTitleAnnotation message = new XYTitleAnnotation(0.5, 0.5, tt, RectangleAnchor.CENTER);
            chart.getXYPlot().addAnnotation(message);
        }
    }

    public boolean showsValidCursorSpectra() {
        return chartUpdater.showsValidCursorSpectra();
    }

    public void removeCursorSpectraFromDataset() {
        chartUpdater.removeCursorSpectraFromDataset();
    }

    public void setCollectingSpectralInformationMessage() {
        setPlotMessage(MESSAGE_COLLECTING_SPECTRAL_INFORMATION);
    }

    public Map<Placemark, Map<Band, Double>> getPinToEnergies() {
        return chartUpdater.getPinToEnergies();
    }
}
