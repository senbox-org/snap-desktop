package org.esa.snap.rcp.spectrum;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.multilevel.MultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.placemark.PlacemarkUtils;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.locationtech.jts.geom.Point;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ChartUpdater {

    private final static int domain_axis_index = 0;
    private final static int range_axis_index = 1;
    private final static double relativePlotInset = 0.05;

    private final SpectrumTopComponent spectrumTopComponent;
    private final Map<Placemark, Map<Band, Double>> pinToEnergies;
    private boolean showsValidCursorSpectra;
    private boolean pixelPosInRasterBounds;
    private int rasterPixelX;
    private int rasterPixelY;
    private int rasterLevel;
    private final Range[] plotBounds;
    private XYSeriesCollection dataset;
    private Point2D modelP;

    ChartUpdater(SpectrumTopComponent spectrumTopComponent) {
        this.spectrumTopComponent = spectrumTopComponent;
        pinToEnergies = new HashMap<>();
        plotBounds = new Range[2];
        invalidatePlotBounds();
    }

    void invalidatePlotBounds() {
        plotBounds[domain_axis_index] = null;
        plotBounds[range_axis_index] = null;
    }

    void setPosition(int pixelX, int pixelY, int level, boolean pixelPosInRasterBounds) {
        rasterPixelX = pixelX;
        rasterPixelY = pixelY;
        rasterLevel = level;
        this.pixelPosInRasterBounds = pixelPosInRasterBounds;
        final AffineTransform i2m = spectrumTopComponent.getCurrentView().getBaseImageLayer().getImageToModelTransform(level);
        modelP = i2m.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), new Point2D.Double());
    }

    void updateData(JFreeChart chart, List<DisplayableSpectrum> spectra) {
        dataset = new XYSeriesCollection();
        if (rasterLevel >= 0) {
            fillDatasetWithPinSeries(spectra, dataset, chart);
            fillDatasetWithCursorSeries(spectra, dataset, chart);
        }
    }

    void updateChart(JFreeChart chart, List<DisplayableSpectrum> spectra) {
        final XYPlot plot = chart.getXYPlot();
        if (!spectrumTopComponent.getChartHandler().isAutomaticDomainAdjustmentSet() && !spectrumTopComponent.isDomainAxisAdjustmentIsFrozen()) {
            spectrumTopComponent.setCodeInducedAxisChange(true);
            updatePlotBounds(dataset.getDomainBounds(true), plot.getDomainAxis(), domain_axis_index);
            spectrumTopComponent.setCodeInducedAxisChange(false);
        }
        if (!spectrumTopComponent.getChartHandler().isAutomaticRangeAdjustmentSet() && !spectrumTopComponent.isRangeAxisAdjustmentIsFrozen()) {
            spectrumTopComponent.setCodeInducedAxisChange(true);
            updatePlotBounds(dataset.getRangeBounds(true), plot.getRangeAxis(), range_axis_index);
            spectrumTopComponent.setCodeInducedAxisChange(false);
        }
        plot.setDataset(dataset);
        setPlotUnit(spectra, plot);
    }

    private void setPlotUnit(List<DisplayableSpectrum> spectra, XYPlot plot) {
        String unitToBeDisplayed = "";
        if (!spectra.isEmpty()) {
            unitToBeDisplayed = spectra.getFirst().getUnit();
            int i = 1;
            while (i < spectra.size() && !unitToBeDisplayed.equals(DisplayableSpectrum.MIXED_UNITS)) {
                DisplayableSpectrum displayableSpectrum = spectra.get(i);
                i++;
                if (displayableSpectrum.hasSelectedBands() && !unitToBeDisplayed.equals(displayableSpectrum.getUnit())) {
                    unitToBeDisplayed = DisplayableSpectrum.MIXED_UNITS;
                }
            }
        }
        spectrumTopComponent.setCodeInducedAxisChange(true);
        plot.getRangeAxis().setLabel(unitToBeDisplayed);
        spectrumTopComponent.setCodeInducedAxisChange(false);
    }

    private void updatePlotBounds(Range newBounds, ValueAxis axis, int index) {
        if (newBounds != null) {
            final Range axisBounds = axis.getRange();
            final Range oldBounds = plotBounds[index];
            plotBounds[index] = getNewRange(newBounds, plotBounds[index], axisBounds);
            if (oldBounds != plotBounds[index]) {
                axis.setRange(getNewPlotBounds(plotBounds[index]));
            }
        }
    }

    private Range getNewRange(Range newBounds, Range currentBounds, Range plotBounds) {
        if (currentBounds == null) {
            currentBounds = newBounds;
        } else {
            if (plotBounds.getLowerBound() > 0 && newBounds.getLowerBound() < currentBounds.getLowerBound() ||
                    newBounds.getUpperBound() > currentBounds.getUpperBound()) {
                currentBounds = new Range(Math.min(currentBounds.getLowerBound(), newBounds.getLowerBound()),
                        Math.max(currentBounds.getUpperBound(), newBounds.getUpperBound()));
            }
        }
        return currentBounds;
    }

    private Range getNewPlotBounds(Range bounds) {
        double range = bounds.getLength();
        double delta = range * relativePlotInset;
        return new Range(Math.max(0, bounds.getLowerBound() - delta),
                bounds.getUpperBound() + delta);
    }

    private void fillDatasetWithCursorSeries(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart) {
        showsValidCursorSpectra = false;
        if (modelP == null) {
            return;
        }
        if (spectrumTopComponent.isShowingCursorSpectrum() && spectrumTopComponent.getCurrentView() != null) {
            for (DisplayableSpectrum spectrum : spectra) {
                XYSeries series = new XYSeries(spectrum.getName());
                final Band[] spectralBands = spectrum.getSelectedBands();
                if (!spectrumTopComponent.getCurrentProduct().isMultiSize()) {
                    for (Band spectralBand : spectralBands) {
                        final float wavelength = spectralBand.getSpectralWavelength();
                        if (pixelPosInRasterBounds && isPixelValid(spectralBand, rasterPixelX, rasterPixelY, rasterLevel)) {
                            addToSeries(spectralBand, rasterPixelX, rasterPixelY, rasterLevel, series, wavelength);
                            showsValidCursorSpectra = true;
                        }
                    }
                } else {
                    for (Band spectralBand : spectralBands) {
                        final float wavelength = spectralBand.getSpectralWavelength();
                        final AffineTransform i2m = spectralBand.getImageToModelTransform();
                        if (i2m.equals(spectrumTopComponent.getCurrentView().getRaster().getImageToModelTransform())) {
                            if (pixelPosInRasterBounds && isPixelValid(spectralBand, rasterPixelX, rasterPixelY, rasterLevel)) {
                                addToSeries(spectralBand, rasterPixelX, rasterPixelY, rasterLevel, series, wavelength);
                                showsValidCursorSpectra = true;
                            }
                        } else {
                            //todo [Multisize_products] use scenerastertransform here
                            final PixelPos rasterPos = new PixelPos();
                            final MultiLevelModel multiLevelModel = spectralBand.getMultiLevelModel();
                            int level = getLevel(multiLevelModel);
                            multiLevelModel.getModelToImageTransform(level).transform(modelP, rasterPos);
                            final int rasterX = (int) rasterPos.getX();
                            final int rasterY = (int) rasterPos.getY();
                            if (coordinatesAreInRasterBounds(spectralBand, rasterX, rasterY, level) &&
                                    isPixelValid(spectralBand, rasterX, rasterY, level)) {
                                addToSeries(spectralBand, rasterX, rasterY, level, series, wavelength);
                                showsValidCursorSpectra = true;
                            }
                        }
                    }
                }
                updateRenderer(dataset.getSeriesCount(), spectrum.getColor(), spectrum, chart);
                dataset.addSeries(series);
            }
        }
    }

    private void addToSeries(Band spectralBand, int x, int y, int level, XYSeries series, double wavelength) {
        final double energy = ProductUtils.getGeophysicalSampleAsDouble(spectralBand, x, y, level);
        if (energy != spectralBand.getGeophysicalNoDataValue()) {
            series.add(wavelength, energy);
        }
    }

    //todo code duplication with pixelinfoviewmodelupdater - move to single class - tf 20151119
    private boolean coordinatesAreInRasterBounds(RasterDataNode raster, int x, int y, int level) {
        final RenderedImage levelImage = raster.getSourceImage().getImage(level);
        return x >= 0 && y >= 0 && x < levelImage.getWidth() && y < levelImage.getHeight();
    }

    private void fillDatasetWithPinSeries(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart) {
        Placemark[] pins = spectrumTopComponent.getDisplayedPins();
        for (Placemark pin : pins) {
            List<XYSeries> pinSeries = createXYSeriesFromPin(pin, dataset.getSeriesCount(), spectra, chart);
            pinSeries.forEach(dataset::addSeries);
        }
    }

    private List<XYSeries> createXYSeriesFromPin(Placemark pin, int seriesIndex, List<DisplayableSpectrum> spectra, JFreeChart chart) {
        List<XYSeries> pinSeries = new ArrayList<>();
        Color pinColor = PlacemarkUtils.getPlacemarkColor(pin, spectrumTopComponent.getCurrentView());
        int numberOfSpectra = spectra.size();
        for (DisplayableSpectrum spectrum : spectra) {
            String seriesKey;
            if (numberOfSpectra == 1) {
                seriesKey = spectrum.getName() + "_" + pin.getLabel();
            } else {
                seriesKey = "[" + seriesIndex + "]" + spectrum.getName() + "_" + pin.getLabel();
            }
            XYSeries series = new XYSeries(seriesKey);
            final Band[] spectralBands = spectrum.getSelectedBands();
            Map<Band, Double> bandToEnergy;
            if (pinToEnergies.containsKey(pin)) {
                bandToEnergy = pinToEnergies.get(pin);
            } else {
                bandToEnergy = new HashMap<>();
                pinToEnergies.put(pin, bandToEnergy);
            }
            for (Band spectralBand : spectralBands) {
                double energy;
                if (bandToEnergy.containsKey(spectralBand)) {
                    energy = bandToEnergy.get(spectralBand);
                } else {
                    energy = readEnergy(pin, spectralBand);
                    bandToEnergy.put(spectralBand, energy);
                }
                final float wavelength = spectralBand.getSpectralWavelength();
                if (energy != spectralBand.getGeophysicalNoDataValue()) {
                    series.add(wavelength, energy);
                }
            }
            updateRenderer(seriesIndex, pinColor, spectrum, chart);
            seriesIndex++;
            pinSeries.add(series);
        }
        return pinSeries;
    }


    private void updateRenderer(int seriesIndex, Color seriesColor, DisplayableSpectrum spectrum, JFreeChart chart) {
        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();

        final Stroke lineStyle = spectrum.getLineStyle();
        renderer.setSeriesStroke(seriesIndex, lineStyle);

        Shape symbol = spectrum.getScaledShape();
        renderer.setSeriesShape(seriesIndex, symbol);
        renderer.setSeriesShapesVisible(seriesIndex, true);

        renderer.setSeriesPaint(seriesIndex, seriesColor);
    }

    private double readEnergy(Placemark pin, Band spectralBand) {
        //todo [Multisize_products] use scenerastertransform here
        final Object pinGeometry = pin.getFeature().getDefaultGeometry();
        if (pinGeometry == null || !(pinGeometry instanceof org.locationtech.jts.geom.Point)) {
            return spectralBand.getGeophysicalNoDataValue();
        }
        final Point2D.Double modelPoint = new Point2D.Double(((org.locationtech.jts.geom.Point) pinGeometry).getCoordinate().x,
                ((Point) pinGeometry).getCoordinate().y);
        final MultiLevelModel multiLevelModel = spectralBand.getMultiLevelModel();
        int level = getLevel(multiLevelModel);
        final AffineTransform m2iTransform = multiLevelModel.getModelToImageTransform(level);
        final PixelPos pinLevelRasterPos = new PixelPos();
        m2iTransform.transform(modelPoint, pinLevelRasterPos);
        int pinLevelRasterX = (int) Math.floor(pinLevelRasterPos.getX());
        int pinLevelRasterY = (int) Math.floor(pinLevelRasterPos.getY());
        if (coordinatesAreInRasterBounds(spectralBand, pinLevelRasterX, pinLevelRasterY, level) &&
                isPixelValid(spectralBand, pinLevelRasterX, pinLevelRasterY, level)) {
            return ProductUtils.getGeophysicalSampleAsDouble(spectralBand, pinLevelRasterX, pinLevelRasterY, level);
        }
        return spectralBand.getGeophysicalNoDataValue();
    }

    void removePinInformation(Placemark pin) {
        pinToEnergies.remove(pin);
    }

    void removeBandinformation(Band band) {
        for (Placemark pin : pinToEnergies.keySet()) {
            Map<Band, Double> bandToEnergiesMap = pinToEnergies.get(pin);
            bandToEnergiesMap.remove(band);
        }
    }

    public boolean showsValidCursorSpectra() {
        return showsValidCursorSpectra;
    }

    void removeCursorSpectraFromDataset() {
        modelP = null;
        if (showsValidCursorSpectra) {
            int numberOfSelectedSpectra = spectrumTopComponent.getSelectedSpectra().size();
            int numberOfPins = spectrumTopComponent.getDisplayedPins().length;
            int numberOfDisplayedGraphs = numberOfPins * numberOfSelectedSpectra;
            while (dataset.getSeriesCount() > numberOfDisplayedGraphs) {
                dataset.removeSeries(dataset.getSeriesCount() - 1);
            }
        }
    }

    public boolean isDatasetEmpty() {
        return dataset == null || dataset.getSeriesCount() == 0;
    }

    //todo code duplication with pixelinfoviewmodelupdater - move to single class - tf 20151119
    private boolean isPixelValid(RasterDataNode raster, int pixelX, int pixelY, int level) {
        if (raster.isValidMaskUsed()) {
            PlanarImage image = ImageManager.getInstance().getValidMaskImage(raster, level);
            Raster data = getRasterTile(image, pixelX, pixelY);
            return data.getSample(pixelX, pixelY, 0) != 0;
        } else {
            return true;
        }
    }

    //todo code duplication with pixelinfoviewmodelupdater - move to single class - tf 20151119
    private Raster getRasterTile(PlanarImage image, int pixelX, int pixelY) {
        final int tileX = image.XToTileX(pixelX);
        final int tileY = image.YToTileY(pixelY);
        return image.getTile(tileX, tileY);
    }

    //todo code duplication with pixelinfoviewmodelupdater - move to single class - tf 20151119
    private int getLevel(MultiLevelModel multiLevelModel) {
        if (rasterLevel < multiLevelModel.getLevelCount()) {
            return rasterLevel;
        }
        return ImageLayer.getLevel(multiLevelModel, spectrumTopComponent.getCurrentView().getViewport());
    }

    Map<Placemark, Map<Band, Double>> getPinToEnergies() {
        return pinToEnergies;
    }
}
