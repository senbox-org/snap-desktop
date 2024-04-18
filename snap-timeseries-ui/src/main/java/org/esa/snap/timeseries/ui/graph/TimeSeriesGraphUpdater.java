package org.esa.snap.timeseries.ui.graph;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.timeseries.core.insitu.InsituSource;
import org.esa.snap.timeseries.core.insitu.csv.InsituRecord;
import org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.snap.timeseries.core.timeseries.datamodel.AxisMapping;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeCoding;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;

import javax.swing.*;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

class TimeSeriesGraphUpdater extends SwingWorker<List<TimeSeries>, Void> {

    private final WorkerChainSupport workerChainSupport;
    private final Position cursorPosition;
    private final PositionSupport positionSupport;
    private final TimeSeriesType type;
    private final boolean showCursorTimeSeries;
    private final int version;
    private final AbstractTimeSeries timeSeries;
    private final TimeSeriesDataHandler dataHandler;
    private final VersionSafeDataSources dataSources;
    private final AxisMapping displayAxisMapping;

    TimeSeriesGraphUpdater(AbstractTimeSeries timeSeries, VersionSafeDataSources dataSources,
                           TimeSeriesDataHandler dataHandler, AxisMapping displayAxisMapping,
                           WorkerChainSupport workerChainSupport, Position cursorPosition,
                           PositionSupport positionSupport, TimeSeriesType type, boolean showCursorTimeSeries,
                           int version) {
        super();
        this.timeSeries = timeSeries;
        this.dataHandler = dataHandler;
        this.dataSources = dataSources;
        this.displayAxisMapping = displayAxisMapping;
        this.workerChainSupport = workerChainSupport;
        this.cursorPosition = cursorPosition;
        this.positionSupport = positionSupport;
        this.type = type;
        this.showCursorTimeSeries = showCursorTimeSeries;
        this.version = version;
    }

    @Override
    protected List<TimeSeries> doInBackground() throws Exception {
        if (dataSources.getCurrentVersion() != version) {
            return Collections.emptyList();
        }
        if (type == TimeSeriesType.INSITU) {
            return computeInsituTimeSeries();
        } else {
            return computeRasterTimeSeries();
        }
    }

    @Override
    protected void done() {
        try {
            if (dataSources.getCurrentVersion() != version) {
                return;
            }
            dataHandler.addTimeSeries(get(), type);
        } catch (InterruptedException | ExecutionException ignore) {
            ignore.printStackTrace();
        } finally {
            workerChainSupport.removeWorkerAndStartNext(this);
        }
    }

    private List<TimeSeries> computeRasterTimeSeries() {
        final List<Position> positionsToDisplay = new ArrayList<>();
        final ArrayList<String> positionNames = new ArrayList<>();
        if (type.equals(TimeSeriesType.PIN)) {
            final Placemark[] pinPositionsToDisplay = dataSources.getPinPositionsToDisplay();
            for (Placemark namedGeoPos : pinPositionsToDisplay) {
                positionsToDisplay.add(positionSupport.transformGeoPos(namedGeoPos.getGeoPos()));
                positionNames.add(namedGeoPos.getLabel());
            }
        } else if (showCursorTimeSeries && cursorPosition != null) {
            positionsToDisplay.add(cursorPosition);
            positionNames.add("");
        }

        final Set<String> aliasNames = displayAxisMapping.getAliasNames();
        final List<TimeSeries> rasterTimeSeries = new ArrayList<>();

        for (int i = 0, positionsToDisplaySize = positionsToDisplay.size(); i < positionsToDisplaySize; i++) {
            final Position position = positionsToDisplay.get(i);
            final String positionName = positionNames.get(i);
            for (String aliasName : aliasNames) {
                final List<String> rasterNames = displayAxisMapping.getRasterNames(aliasName);
                for (String rasterName : rasterNames) {
                    final List<Band> bandsForVariable = timeSeries.getBandsForVariable(rasterName);
                    final TimeSeries timeSeries = computeSingleTimeSeries(bandsForVariable, position.pixelX, position.pixelY, position.currentLevel, positionName);
                    rasterTimeSeries.add(dataHandler.getValidatedTimeSeries(timeSeries, rasterName, type));
                }
            }
        }
        return rasterTimeSeries;
    }

    private List<TimeSeries> computeInsituTimeSeries() {
        final InsituSource insituSource = timeSeries.getInsituSource();
        final List<TimeSeries> insituTimeSeries = new ArrayList<>();

        final Set<String> aliasNames = displayAxisMapping.getAliasNames();
        final Placemark[] pinPositionsToDisplay = dataSources.getPinPositionsToDisplay();

        for (Placemark insituPin : pinPositionsToDisplay) {
            for (String aliasName : aliasNames) {
                final List<String> insituNames = displayAxisMapping.getInsituNames(aliasName);
                for (String insituName : insituNames) {
                    // todo
                    final GeoPos insituGeoposFor = timeSeries.getInsituGeoposFor(insituPin);
                    if (insituGeoposFor != null) {
                        InsituRecord[] insituRecords = insituSource.getValuesFor(insituName, insituGeoposFor);
                        final TimeSeries timeSeries = computeSingleTimeSeries(insituRecords, insituName + "_" + insituPin.getLabel());
                        insituTimeSeries.add(dataHandler.getValidatedTimeSeries(timeSeries, insituName, type));
                    }
                }
            }
        }
        return insituTimeSeries;
    }

    private TimeSeries computeSingleTimeSeries(InsituRecord[] insituRecords, String insituName) {
        TimeSeries timeSeries = new TimeSeries(insituName);
        for (InsituRecord insituRecord : insituRecords) {
            final ProductData.UTC startTime = ProductData.UTC.create(insituRecord.time, 0);
            final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                    ProductData.UTC.UTC_TIME_ZONE,
                    Locale.getDefault());
            timeSeries.addOrUpdate(timePeriod, insituRecord.value);
        }
        return timeSeries;
    }

    private TimeSeries computeSingleTimeSeries(final List<Band> bandList, int pixelX, int pixelY, int currentLevel, String positionName) {
        final Band firstBand = bandList.get(0);
        final String firstBandName = firstBand.getName();
        final int lastUnderscore = firstBandName.lastIndexOf("_");
        final String suffix = positionName.isEmpty()?positionName: "_" + positionName;
        final String timeSeriesName = firstBandName.substring(0, lastUnderscore);
        final TimeSeries timeSeries = new TimeSeries(timeSeriesName + suffix);
        for (Band band : bandList) {
            final TimeCoding timeCoding = this.timeSeries.getRasterTimeMap().get(band);
            if (timeCoding != null) {
                final ProductData.UTC startTime = timeCoding.getStartTime();
                final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                        ProductData.UTC.UTC_TIME_ZONE,
                        Locale.getDefault());
                final double value = getValue(band, pixelX, pixelY, currentLevel);
                timeSeries.add(new TimeSeriesDataItem(timePeriod, value));
            }
        }
        return timeSeries;
    }

    private static double getValue(Band band, int pixelX, int pixelY, int currentLevel) {
        final Rectangle pixelRect = new Rectangle(pixelX, pixelY, 1, 1);
        if (band.getValidMaskImage() != null) {
            final RenderedImage validMask = band.getValidMaskImage().getImage(currentLevel);
            final Raster validMaskData = validMask.getData(pixelRect);
            if (validMaskData.getSample(pixelX, pixelY, 0) > 0) {
                return ProductUtils.getGeophysicalSampleAsDouble(band, pixelX, pixelY, currentLevel);
            } else {
                return band.getNoDataValue();
            }
        } else {
            return ProductUtils.getGeophysicalSampleAsDouble(band, pixelX, pixelY, currentLevel);
        }
    }

    static class Position {

        private final int pixelX;
        private final int pixelY;
        private final int currentLevel;

        Position(int pixelX, int pixelY, int currentLevel) {
            this.currentLevel = currentLevel;
            this.pixelY = pixelY;
            this.pixelX = pixelX;
        }
    }

    static interface TimeSeriesDataHandler {

        void addTimeSeries(List<TimeSeries> data, TimeSeriesType type);

        TimeSeries getValidatedTimeSeries(TimeSeries timeSeries, String dataSourceName, TimeSeriesType type);
    }

    static interface WorkerChainSupport {

        void removeWorkerAndStartNext(TimeSeriesGraphUpdater worker);
    }

    static abstract class VersionSafeDataSources {

        private final Placemark[] pinPositionsToDisplay;
        private final int version;

        protected VersionSafeDataSources(Placemark[] pinPositionsToDisplay, final int version) {
            this.pinPositionsToDisplay = pinPositionsToDisplay;
            this.version = version;
        }

        public Placemark[] getPinPositionsToDisplay() {
            if (canReturnValues()) {
                return pinPositionsToDisplay;
            }
            return new Placemark[0];
        }

        protected abstract int getCurrentVersion();

        private boolean canReturnValues() {
            return getCurrentVersion() == version;
        }
    }

    static interface PositionSupport {

        Position transformGeoPos(GeoPos geoPos);
    }
}
