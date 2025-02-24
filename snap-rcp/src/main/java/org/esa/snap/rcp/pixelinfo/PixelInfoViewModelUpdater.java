/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.pixelinfo;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.multilevel.MultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.dataop.maptransf.MapTransform;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.PlanarImage;
import javax.swing.SwingUtilities;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Calendar;
import java.util.Vector;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.5.2
 */
class PixelInfoViewModelUpdater {

    private static final String INVALID_POS_TEXT = "Invalid pos.";

    private final PixelInfoViewTableModel positionModel;
    private final PixelInfoViewTableModel timeModel;
    private final PixelInfoViewTableModel bandModel;
    private final PixelInfoViewTableModel tiePointModel;
    private final PixelInfoViewTableModel flagModel;

    private volatile Product currentProduct;
    private volatile RasterDataNode currentRaster;
    private volatile ProductSceneView currentView;
    private Band[] currentFlagBands;

    private int pixelX;
    private int pixelY;
    private int rasterLevel;
    private int levelZeroRasterX;
    private int levelZeroRasterY;
    private double sceneX;
    private double sceneY;
    private int levelZeroSceneX;
    private int levelZeroSceneY;
    private boolean pixelPosValidInRaster;

    private final PixelInfoView pixelInfoView;

    PixelInfoViewModelUpdater(PixelInfoView pixelInfoView,
                              PixelInfoViewTableModel positionModel,
                              PixelInfoViewTableModel timeModel,
                              PixelInfoViewTableModel bandModel,
                              PixelInfoViewTableModel tiePointModel,
                              PixelInfoViewTableModel flagModel) {
        this.pixelInfoView = pixelInfoView;
        this.positionModel = positionModel;
        this.timeModel = timeModel;
        this.bandModel = bandModel;
        this.tiePointModel = tiePointModel;
        this.flagModel = flagModel;
    }

    Product getCurrentProduct() {
        return currentProduct;
    }

    RasterDataNode getCurrentRaster() {
        return currentRaster;
    }

    void update(PixelInfoState state) {
        update(state.view, state.pixelX, state.pixelY, state.level, state.pixelPosValid);
    }

    private void update(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
        Guardian.assertNotNull("view", view);
        boolean clearRasterTableSelection = false;
        RasterDataNode raster = view.getRaster();
        final Product product = raster.getProduct();
        if (product == currentProduct && view.isRGB()) {
            resetBandTableModel();
        }
        if (product != currentProduct) {
            ProductNodeListener productNodeListener = pixelInfoView.getProductNodeListener();
            if (currentProduct != null) {
                currentProduct.removeProductNodeListener(productNodeListener);
            }
            product.addProductNodeListener(productNodeListener);
            currentProduct = product;
        }
        if (raster != currentRaster) {
            currentRaster = raster;
            registerFlagDatasets();
            resetTableModels();
        }
        if (bandModel.getRowCount() != getBandRowCount()) {
            resetTableModels();
        }
        if (view != currentView) {
            currentView = view;
            resetTableModels();
            clearRasterTableSelection = true;
        }
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.rasterLevel = level;
        this.pixelPosValidInRaster = pixelPosValid;
        AffineTransform i2mTransform = currentView.getBaseImageLayer().getImageToModelTransform(level);
        Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), null);
        try {
            final Point2D sceneP = currentView.getRaster().getModelToSceneTransform().transform(modelP, new Point2D.Double());
            sceneX = sceneP.getX();
            sceneY = sceneP.getY();
        } catch (TransformException e) {
            sceneX = Double.NaN;
            sceneY = Double.NaN;
        }
        AffineTransform m2iTransform = view.getBaseImageLayer().getModelToImageTransform();
        Point2D levelZeroP = m2iTransform.transform(modelP, null);
        levelZeroRasterX = floor(levelZeroP.getX());
        levelZeroRasterY = floor(levelZeroP.getY());
        //todo [multisize_products] ask for different imagetomodeltransforms - tf 20151113
        if (product.isMultiSize()) {
            try {
                final GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
                if (sceneGeoCoding != null) {
                    final MathTransform imageToMapTransform = sceneGeoCoding.getImageToMapTransform();
                    if (imageToMapTransform instanceof AffineTransform) {
                        final MathTransform modelToImage = imageToMapTransform.inverse();
                        final DirectPosition2D pos = new DirectPosition2D(sceneX, sceneY);
                        final DirectPosition position = modelToImage.transform(pos, pos);
                        levelZeroSceneX = floor(position.getCoordinate()[0]);
                        levelZeroSceneY = floor(position.getCoordinate()[1]);
                    } else {
                        levelZeroSceneX = floor(sceneX);
                        levelZeroSceneY = floor(sceneY);
                    }
                }
            } catch (TransformException e) {
                levelZeroSceneX = levelZeroRasterX;
                levelZeroSceneY = levelZeroRasterY;
            }
        }
        updateDataDisplay(clearRasterTableSelection);
    }

    void resetTableModels() {
        resetPositionTableModel();
        resetTimeTableModel();
        resetBandTableModel();
        resetTiePointGridTableModel();
        resetFlagTableModel();
    }

    private void fireTableChanged(final boolean clearRasterTableSelection) {
        SwingUtilities.invokeLater(() -> {
            if (clearRasterTableSelection) {
                pixelInfoView.clearSelectionInRasterTables();
            }
            positionModel.fireTableDataChanged();
            timeModel.fireTableDataChanged();
            bandModel.fireTableDataChanged();
            tiePointModel.fireTableDataChanged();
            flagModel.fireTableDataChanged();
        });
    }

    private void updateDataDisplay(boolean clearRasterTableSelection) {
        if (currentRaster == null) {
            return;
        }
        if (pixelInfoView.isCollapsiblePaneVisible(PixelInfoView.POSITION_INDEX)) {
            updatePositionValues();
        }
        if (pixelInfoView.isCollapsiblePaneVisible(PixelInfoView.TIME_INDEX)) {
            updateTimeValues();
        }
        if (pixelInfoView.isCollapsiblePaneVisible(PixelInfoView.BANDS_INDEX)) {
            updateBandPixelValues();
        }
        if (pixelInfoView.isCollapsiblePaneVisible(PixelInfoView.TIE_POINT_GRIDS_INDEX)) {
            updateTiePointGridPixelValues();
        }
        if (pixelInfoView.isCollapsiblePaneVisible(PixelInfoView.FLAGS_INDEX)) {
            updateFlagPixelValues();
        }
        fireTableChanged(clearRasterTableSelection);
    }

    private void resetPositionTableModel() {
        positionModel.clear();
        if (currentRaster != null) {
            final GeoCoding geoCoding = currentRaster.getGeoCoding();
            positionModel.addRow("Image-X", "", "pixel");
            positionModel.addRow("Image-Y", "", "pixel");
            //todo [Multisize_products] ask for something else than multisize (scenetomodeltransform)
            if (getCurrentProduct().isMultiSize()) {
                positionModel.addRow("Scene-X", "", "pixel");
                positionModel.addRow("Scene-Y", "", "pixel");
            }
            if (geoCoding != null) {
                positionModel.addRow("Longitude", "", "degree");
                positionModel.addRow("Latitude", "", "degree");
                if (geoCoding instanceof MapGeoCoding) {
                    final MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
                    final String mapUnit = mapGeoCoding.getMapInfo().getMapProjection().getMapUnit();

                    positionModel.addRow("Map-X", "", mapUnit);
                    positionModel.addRow("Map-Y", "", mapUnit);
                } else if (geoCoding instanceof CrsGeoCoding) {
                    String xAxisUnit = geoCoding.getMapCRS().getCoordinateSystem().getAxis(0).getUnit().toString();
                    String yAxisUnit = geoCoding.getMapCRS().getCoordinateSystem().getAxis(1).getUnit().toString();
                    positionModel.addRow("Map-X", "", xAxisUnit);
                    positionModel.addRow("Map-Y", "", yAxisUnit);
                }
            }
        }
    }

    private void updatePositionValues() {
        final boolean availableInRaster = pixelPosValidInRaster &&
                coordinatesAreInRasterBounds(currentRaster, pixelX, pixelY, rasterLevel);
        final boolean availableInScene = isSampleValueAvailableInScene();
        final double offset = 0.5 + (pixelInfoView.getShowPixelPosOffset1() ? 1.0 : 0.0);
        final double pX = levelZeroRasterX + offset;
        final double pY = levelZeroRasterY + offset;

        String tix, tiy, tsx, tsy, tmx, tmy, tgx, tgy;
        tix = tiy = tsx = tsy = tmx = tmy = tgx = tgy = INVALID_POS_TEXT;
        GeoCoding geoCoding = currentRaster.getGeoCoding();
        if (availableInRaster) {
            if (pixelInfoView.getShowPixelPosDecimal()) {
                tix = String.valueOf(pX);
                tiy = String.valueOf(pY);
            } else {
                tix = String.valueOf((int) Math.floor(pX));
                tiy = String.valueOf((int) Math.floor(pY));
            }
        }
        if (getCurrentProduct().isMultiSize()) {
            if (!availableInScene) {
                tsx = PixelInfoViewModelUpdater.INVALID_POS_TEXT;
                tsy = PixelInfoViewModelUpdater.INVALID_POS_TEXT;
            } else {
                double sX = levelZeroSceneX + offset;
                double sY = levelZeroSceneY + offset;
                if (pixelInfoView.getShowPixelPosDecimal()) {
                    tsx = String.valueOf(sX);
                    tsy = String.valueOf(sY);
                } else {
                    tsx = String.valueOf((int) Math.floor(sX));
                    tsy = String.valueOf((int) Math.floor(sY));
                }
            }
        }
        if (availableInRaster && geoCoding != null) {
            PixelPos pixelPos = new PixelPos(pX, pY);
            GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
            if (pixelInfoView.getShowGeoPosDecimals()) {
                tgx = String.format("%.6f", geoPos.getLon());
                tgy = String.format("%.6f", geoPos.getLat());
            } else {
                tgx = geoPos.getLonString();
                tgy = geoPos.getLatString();
            }
            if (geoCoding instanceof MapGeoCoding) {
                final MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
                final MapTransform mapTransform = mapGeoCoding.getMapInfo().getMapProjection().getMapTransform();
                Point2D mapPoint = mapTransform.forward(geoPos, null);
                tmx = String.valueOf(MathUtils.round(mapPoint.getX(), 10000.0));
                tmy = String.valueOf(MathUtils.round(mapPoint.getY(), 10000.0));
            } else if (geoCoding instanceof CrsGeoCoding) {
                MathTransform transform = geoCoding.getImageToMapTransform();
                try {
                    DirectPosition position = transform.transform(new DirectPosition2D(pX, pY), null);
                    double[] coordinate = position.getCoordinate();
                    tmx = String.valueOf(coordinate[0]);
                    tmy = String.valueOf(coordinate[1]);
                } catch (TransformException ignore) {
                }
            }
        }
        int rowCount = 0;
        positionModel.updateValue(tix, rowCount++);
        positionModel.updateValue(tiy, rowCount++);
        if (getCurrentProduct().isMultiSize()) {
            positionModel.updateValue(tsx, rowCount++);
            positionModel.updateValue(tsy, rowCount++);
        }
        if (geoCoding != null) {
            positionModel.updateValue(tgx, rowCount++);
            positionModel.updateValue(tgy, rowCount++);
            if (geoCoding instanceof MapGeoCoding || geoCoding instanceof CrsGeoCoding) {
                positionModel.updateValue(tmx, rowCount++);
                positionModel.updateValue(tmy, rowCount);
            }
        }
    }

    private void resetTimeTableModel() {
        timeModel.clear();
        if (currentRaster != null) {
            timeModel.addRow("Date", "", "YYYY-MM-DD");
            timeModel.addRow("Time (UTC)", "", "HH:MM:SS:mm [AM/PM]");
        }
    }

    private void updateTimeValues() {
        final ProductData.UTC utcStartTime = currentProduct.getStartTime();
        final ProductData.UTC utcEndTime = currentProduct.getEndTime();

        boolean isAvailable = currentProduct.isMultiSize() ? isSampleValueAvailableInScene() : isSampleValueAvailableInRaster();
        if (utcStartTime == null || utcEndTime == null || !isAvailable) {
            timeModel.updateValue("No date information", 0);
            timeModel.updateValue("No time information", 1);
        } else {
            final ProductData.UTC utcCurrentLine;
            if (currentProduct.isMultiSize()) {
                utcCurrentLine = ProductUtils.getPixelScanTime(currentProduct, levelZeroSceneX + 0.5, levelZeroSceneY + 0.5);
            } else {
                utcCurrentLine = ProductUtils.getPixelScanTime(currentRaster, levelZeroRasterX + 0.5, levelZeroRasterY + 0.5);
            }
            Assert.notNull(utcCurrentLine, "utcCurrentLine");
            final Calendar currentLineTime = utcCurrentLine.getAsCalendar();

            final String dateString = String.format("%1$tF", currentLineTime);
            final String timeString = String.format("%1$tI:%1$tM:%1$tS:%1$tL %1$Tp", currentLineTime);

            timeModel.updateValue(dateString, 0);
            timeModel.updateValue(timeString, 1);
        }
    }

    private void resetBandTableModel() {
        bandModel.clear();
        if (currentRaster != null) {
            final int numBands = currentProduct.getNumBands();
            for (int i = 0; i < numBands; i++) {
                final Band band = currentProduct.getBandAt(i);
                if (shouldDisplayBand(band)) {
                    bandModel.addRow(band.getName(), "", band.getUnit());
                }
            }
        }
    }

    private void updateBandPixelValues() {
        for (int i = 0; i < bandModel.getRowCount(); i++) {
            final String bandName = (String) bandModel.getValueAt(i, 0);
            bandModel.updateValue(getPixelString(currentProduct.getBand(bandName)), i);
        }
    }

    private int getBandRowCount() {
        int rowCount = 0;
        if (currentProduct != null) {
            Band[] bands = currentProduct.getBands();
            for (final Band band : bands) {
                if (shouldDisplayBand(band)) {
                    rowCount++;
                }
            }
        }
        return rowCount;
    }

    private boolean shouldDisplayBand(final Band band) {
        PixelInfoView.DisplayFilter displayFilter = pixelInfoView.getDisplayFilter();
        if (displayFilter != null) {
            return displayFilter.accept(band);
        }
        return band.hasRasterData();
    }

    private void resetTiePointGridTableModel() {
        tiePointModel.clear();
        if (currentRaster != null) {
            final int numTiePointGrids = currentProduct.getNumTiePointGrids();
            for (int i = 0; i < numTiePointGrids; i++) {
                final TiePointGrid tiePointGrid = currentProduct.getTiePointGridAt(i);
                tiePointModel.addRow(tiePointGrid.getName(), "", tiePointGrid.getUnit());
            }
        }
    }

    private void updateTiePointGridPixelValues() {
        for (int i = 0; i < tiePointModel.getRowCount(); i++) {
            final TiePointGrid grid = currentProduct.getTiePointGrid((String) tiePointModel.getValueAt(i, 0));
            tiePointModel.updateValue(getPixelString(grid), i);
        }
    }

    private void resetFlagTableModel() {
        flagModel.clear();
        if (currentRaster != null) {
            for (Band band : currentFlagBands) {  // currentFlagBands is already filtered for "equals size" in registerFlagDatasets
                final FlagCoding flagCoding = band.getFlagCoding();
                if (flagCoding != null) {
                    final int numFlags = flagCoding.getNumAttributes();
                    final String bandNameDot = band.getName() + ".";
                    for (int j = 0; j < numFlags; j++) {
                        String name = bandNameDot + flagCoding.getAttributeAt(j).getName();
                        flagModel.addRow(name, "", "");
                    }
                }
            }
        }
    }

    private void updateFlagPixelValues() {
        if (flagModel.getRowCount() != getFlagRowCount()) {
            resetFlagTableModel();
        }
        int rowIndex = 0;
        for (Band band : currentFlagBands) {
            long pixelValue;
            boolean available;
            if (band.getImageToModelTransform().equals(currentRaster.getImageToModelTransform())
                    && band.getSceneToModelTransform().equals(currentRaster.getSceneToModelTransform())) {
                available = pixelPosValidInRaster;
                pixelValue = available ? ProductUtils.getGeophysicalSampleAsLong(band, pixelX, pixelY, rasterLevel) : 0;
            } else {
                PixelPos rasterPos = new PixelPos();
                final Point2D.Double scenePos = new Point2D.Double(sceneX, sceneY);
                final Point2D modelPos;
                try {
                    modelPos = band.getSceneToModelTransform().transform(scenePos, new Point2D.Double());
                    final MultiLevelModel multiLevelModel = band.getMultiLevelModel();
                    final int level = getLevel(multiLevelModel);
                    multiLevelModel.getModelToImageTransform(level).transform(modelPos, rasterPos);
                    final int rasterX = (int) Math.floor(rasterPos.getX());
                    final int rasterY = (int) Math.floor(rasterPos.getY());
                    available = coordinatesAreInRasterBounds(band, rasterX, rasterY, level);
                    pixelValue = available ? ProductUtils.getGeophysicalSampleAsLong(band, rasterX, rasterY, level) : 0;
                } catch (TransformException e) {
                    available = false;
                    pixelValue = -1;
                }
            }
            for (int j = 0; j < band.getFlagCoding().getNumAttributes(); j++) {
                if (available) {
                    MetadataAttribute attribute = band.getFlagCoding().getAttributeAt(j);
                    final ProductData flagData = attribute.getData();
                    final long flagMask;
                    final long flagValue;
                    if (flagData.getNumElems() == 2) {
                        flagMask = flagData.getElemUIntAt(0);
                        flagValue = flagData.getElemUIntAt(1);
                    } else {
                        flagMask = flagValue = flagData.getElemUInt();
                    }
                    flagModel.updateValue(String.valueOf((pixelValue & flagMask) == flagValue), rowIndex);
                } else {
                    flagModel.updateValue(INVALID_POS_TEXT, rowIndex);
                }
                rowIndex++;
            }
        }
    }

    private void registerFlagDatasets() {
        Vector<Band> flagBandsVector = new Vector<>();
        if (currentProduct != null) {
            final Band[] bands = currentProduct.getBands();
            for (Band band : bands) {
                if (isFlagBand(band)) {
                    flagBandsVector.add(band);
                }
            }
        }
        currentFlagBands = flagBandsVector.toArray(new Band[flagBandsVector.size()]);
    }

    private boolean isFlagBand(final Band band) {
        return band.getFlagCoding() != null;
    }

    private int getFlagRowCount() {
        int rowCount = 0;
        for (Band band : currentFlagBands) {
            rowCount += band.getFlagCoding().getNumAttributes();
        }
        return rowCount;
    }

    private String getPixelString(RasterDataNode raster) {
        if (raster.getImageToModelTransform().equals(currentRaster.getImageToModelTransform())
                && raster.getSceneToModelTransform().equals(currentRaster.getSceneToModelTransform())) {
            if (!pixelPosValidInRaster) {
                return RasterDataNode.INVALID_POS_TEXT;
            }
            return getPixelString(raster, pixelX, pixelY, rasterLevel);
        }
        final Point2D.Double scenePos = new Point2D.Double(sceneX, sceneY);
        Point2D.Double modelPos = new Point2D.Double();
        try {
            raster.getSceneToModelTransform().transform(scenePos, modelPos);
            if (Double.isNaN(modelPos.getX()) || Double.isNaN(modelPos.getY())) {
                return PixelInfoViewModelUpdater.INVALID_POS_TEXT;
            }
        } catch (TransformException e) {
            return PixelInfoViewModelUpdater.INVALID_POS_TEXT;
        }
        final MultiLevelModel multiLevelModel = raster.getMultiLevelModel();
        final int level = getLevel(multiLevelModel);
        final PixelPos rasterPos = (PixelPos) multiLevelModel.getModelToImageTransform(level).transform(modelPos, new PixelPos());
        final int rasterX = floor(rasterPos.getX());
        final int rasterY = floor(rasterPos.getY());
        if (!coordinatesAreInRasterBounds(raster, rasterX, rasterY, level)) {
            return RasterDataNode.INVALID_POS_TEXT;
        }
        return getPixelString(raster, rasterX, rasterY, level);
    }

    //todo code duplication with spectrumtopcomponent - move to single class - tf 20151119
    private int getLevel(MultiLevelModel multiLevelModel) {
        if (rasterLevel < multiLevelModel.getLevelCount()) {
            return rasterLevel;
        }
        return ImageLayer.getLevel(multiLevelModel, currentView.getViewport());
    }

    private String getPixelString(RasterDataNode raster, int x, int y, int level) {
        if (isPixelValid(raster, x, y, level)) {
            if (raster.isScalingApplied() || ProductData.isFloatingPointType(raster.getDataType())) {
                int dataType = raster.getGeophysicalDataType();
                if (dataType == ProductData.TYPE_FLOAT64) {
                    double pixel = ProductUtils.getGeophysicalSampleAsDouble(raster, x, y, level);
                    return String.format("%.10f", pixel);
                } else if (dataType == ProductData.TYPE_FLOAT32) {
                    double pixel = ProductUtils.getGeophysicalSampleAsDouble(raster, x, y, level);
                    return String.format("%.5f", pixel);
                }
            }
            return String.valueOf(ProductUtils.getGeophysicalSampleAsLong(raster, x, y, level));
        } else {
            return RasterDataNode.NO_DATA_TEXT;
        }
    }

    //todo code duplication with spectrumtopcomponent - move to single class - tf 20151119
    private boolean isPixelValid(RasterDataNode raster, int pixelX, int pixelY, int level) {
        if (raster.isValidMaskUsed()) {
            PlanarImage image = ImageManager.getInstance().getValidMaskImage(raster, level);
            Raster data = getRasterTile(image, pixelX, pixelY);
            return data.getSample(pixelX, pixelY, 0) != 0;
        } else {
            return true;
        }
    }

    //todo code duplication with spectrumtopcomponent - move to single class - tf 20151119
    private Raster getRasterTile(PlanarImage image, int pixelX, int pixelY) {
        final int tileX = image.XToTileX(pixelX);
        final int tileY = image.YToTileY(pixelY);
        return image.getTile(tileX, tileY);
    }

    //todo code duplication with spectrumtopcomponent - move to single class - tf 20151119
    private boolean coordinatesAreInRasterBounds(RasterDataNode raster, int x, int y, int level) {
        final RenderedImage levelImage = raster.getSourceImage().getImage(level);
        return x >= 0 && y >= 0 && x < levelImage.getWidth() && y < levelImage.getHeight();
    }

    /**
     * Convenience method that gives the largest integer smaller than the double value or
     * -1 if value is Doubl.NaN.
     */
    private int floor(double value) {
        if (Double.isNaN(value)) {
            return -1;
        }
        return (int) Math.floor(value);
    }

    private boolean isSampleValueAvailableInScene() {
        return levelZeroSceneX >= 0
                && levelZeroSceneY >= 0
                && levelZeroSceneX < currentProduct.getSceneRasterWidth()
                && levelZeroSceneY < currentProduct.getSceneRasterHeight();
    }

    private boolean isSampleValueAvailableInRaster() {
        return levelZeroRasterX >= 0
                && levelZeroRasterY >= 0
                && levelZeroRasterX < currentRaster.getRasterWidth()
                && levelZeroRasterY < currentRaster.getRasterHeight();
    }

    void clearProductNodeRefs() {
        currentProduct = null;
        currentRaster = null;
        currentView = null;
        currentFlagBands = new Band[0];
    }
}
