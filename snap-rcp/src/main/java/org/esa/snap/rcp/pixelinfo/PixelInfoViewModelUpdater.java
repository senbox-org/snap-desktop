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
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.Calendar;
import java.util.Vector;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.5.2
 */
public class PixelInfoViewModelUpdater {

    private static final String _INVALID_POS_TEXT = "Invalid pos.";

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
    private int level;
    private int levelZeroX;
    private int levelZeroY;
    private boolean pixelPosValid;

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

    void update(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
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
        this.level = level;
        this.pixelPosValid = pixelPosValid;
        AffineTransform i2mTransform = currentView.getBaseImageLayer().getImageToModelTransform(level);
        Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), null);
        AffineTransform m2iTransform = view.getBaseImageLayer().getModelToImageTransform();
        Point2D levelZeroP = m2iTransform.transform(modelP, null);
        levelZeroX = (int) Math.floor(levelZeroP.getX());
        levelZeroY = (int) Math.floor(levelZeroP.getY());

        updateDataDisplay(clearRasterTableSelection);
    }

    private void resetTableModels() {
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
        final boolean available = isSampleValueAvailable(levelZeroX, levelZeroY, pixelPosValid);
        final double offset = 0.5 + (pixelInfoView.getShowPixelPosOffset1() ? 1.0 : 0.0);
        final double pX = levelZeroX + offset;
        final double pY = levelZeroY + offset;

        String tix, tiy, tmx, tmy, tgx, tgy;
        tix = tiy = tmx = tmy = tgx = tgy = _INVALID_POS_TEXT;
        GeoCoding geoCoding = currentRaster.getGeoCoding();
        if (available) {
            PixelPos pixelPos = new PixelPos(pX, pY);
            if (pixelInfoView.getShowPixelPosDecimal()) {
                tix = String.valueOf(pX);
                tiy = String.valueOf(pY);
            } else {
                tix = String.valueOf((int) Math.floor(pX));
                tiy = String.valueOf((int) Math.floor(pY));
            }
            if (geoCoding != null) {
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
        }
        positionModel.updateValue(tix, 0);
        positionModel.updateValue(tiy, 1);
        if (geoCoding != null) {
            positionModel.updateValue(tgx, 2);
            positionModel.updateValue(tgy, 3);
            if (geoCoding instanceof MapGeoCoding || geoCoding instanceof CrsGeoCoding) {
                positionModel.updateValue(tmx, 4);
                positionModel.updateValue(tmy, 5);
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

        if (utcStartTime == null || utcEndTime == null ||
                !isSampleValueAvailable(0, levelZeroY, true) ||
                !equalsViewRasterSize(currentProduct.getSceneRasterSize())) {
            timeModel.updateValue("No date information", 0);
            timeModel.updateValue("No time information", 1);
        } else {
            final ProductData.UTC utcCurrentLine = ProductUtils.getScanLineTime(currentProduct, levelZeroY + 0.5);
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
        final boolean equalSize = equalsViewRasterSize(band.getRasterSize());
        if (displayFilter != null) {
            return displayFilter.accept(band) && equalSize;
        }
        return band.hasRasterData() && equalSize;
    }

    private boolean equalsViewRasterSize(Dimension size) {
        if (currentRaster != null) {
            Dimension viewSize;
            if (currentRaster instanceof TiePointGrid) {
                viewSize = currentRaster.getSceneRasterSize();
            } else {
                viewSize = currentRaster.getRasterSize();
            }
            return viewSize.equals(size);
        }
        return false;
    }

    private void resetTiePointGridTableModel() {
        tiePointModel.clear();
        if (currentRaster != null) {
            final int numTiePointGrids = currentProduct.getNumTiePointGrids();
            for (int i = 0; i < numTiePointGrids; i++) {
                final TiePointGrid tiePointGrid = currentProduct.getTiePointGridAt(i);
                if (equalsViewRasterSize(tiePointGrid.getSceneRasterSize())) {
                    tiePointModel.addRow(tiePointGrid.getName(), "", tiePointGrid.getUnit());
                }
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
                final int numFlags = flagCoding.getNumAttributes();
                final String bandNameDot = band.getName() + ".";
                for (int j = 0; j < numFlags; j++) {
                    String name = bandNameDot + flagCoding.getAttributeAt(j).getName();
                    flagModel.addRow(name, "", "");
                }
            }
        }
    }

    private void updateFlagPixelValues() {
        final boolean available = isSampleValueAvailable(levelZeroX, levelZeroY, pixelPosValid);

        if (flagModel.getRowCount() != getFlagRowCount()) {
            resetFlagTableModel();
        }
        int rowIndex = 0;
        for (Band band : currentFlagBands) {
            long pixelValue = available ? ProductUtils.getGeophysicalSampleAsLong(band, pixelX, pixelY, level) : 0;

            for (int j = 0; j < band.getFlagCoding().getNumAttributes(); j++) {
                if (available) {
                    MetadataAttribute attribute = band.getFlagCoding().getAttributeAt(j);
                    int mask = attribute.getData().getElemInt();
                    flagModel.updateValue(String.valueOf((pixelValue & mask) == mask), rowIndex);
                } else {
                    flagModel.updateValue(_INVALID_POS_TEXT, rowIndex);
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
                if (isFlagBand(band) && equalsViewRasterSize(band.getRasterSize())) {
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
        if (!pixelPosValid) {
            return RasterDataNode.INVALID_POS_TEXT;
        }
        if (isPixelValid(raster, pixelX, pixelY, level)) {
            if (raster.isScalingApplied() || ProductData.isFloatingPointType(raster.getDataType())) {
                int dataType = raster.getGeophysicalDataType();
                if (dataType == ProductData.TYPE_FLOAT64) {
                    double pixel = ProductUtils.getGeophysicalSampleAsDouble(raster, pixelX, pixelY, level);
                    return String.format("%.10f", pixel);
                } else if (dataType == ProductData.TYPE_FLOAT32) {
                    double pixel = ProductUtils.getGeophysicalSampleAsDouble(raster, pixelX, pixelY, level);
                    return String.format("%.5f", pixel);
                }
            }
            return String.valueOf(ProductUtils.getGeophysicalSampleAsLong(raster, pixelX, pixelY, level));
        } else {
            return RasterDataNode.NO_DATA_TEXT;
        }
    }

    private boolean isPixelValid(RasterDataNode raster, int pixelX, int pixelY, int level) {
        if (raster.isValidMaskUsed()) {
            PlanarImage image = ImageManager.getInstance().getValidMaskImage(raster, level);
            Raster data = getRasterTile(image, pixelX, pixelY);
            return data.getSample(pixelX, pixelY, 0) != 0;
        } else {
            return true;
        }
    }

    private Raster getRasterTile(PlanarImage image, int pixelX, int pixelY) {
        final int tileX = image.XToTileX(pixelX);
        final int tileY = image.YToTileY(pixelY);
        return image.getTile(tileX, tileY);
    }


    private boolean isSampleValueAvailable(int pixelX, int pixelY, boolean pixelValid) {
        return currentRaster != null
                && pixelValid
                && pixelX >= 0
                && pixelY >= 0
                && pixelX < currentRaster.getSceneRasterWidth()
                && pixelY < currentRaster.getSceneRasterHeight();
    }

    void clearProductNodeRefs() {
        currentProduct = null;
        currentRaster = null;
        currentView = null;
        currentFlagBands = new Band[0];
    }
}
