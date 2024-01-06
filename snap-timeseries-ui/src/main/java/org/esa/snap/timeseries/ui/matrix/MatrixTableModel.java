package org.esa.snap.timeseries.ui.matrix;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;

import javax.swing.table.AbstractTableModel;
import java.awt.*;

class MatrixTableModel extends AbstractTableModel {

    private int size;
    private Band band;
    private int centerPixelX;
    private int centerPixelY;

    MatrixTableModel() {
        size = 0;
        band = null;
        centerPixelX = Integer.MIN_VALUE;
        centerPixelY = Integer.MIN_VALUE;
    }

    @Override
    public int getRowCount() {
        return size;
    }

    @Override
    public int getColumnCount() {
        return size;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Double.class;
    }

    @Override
    public Double getValueAt(int rowIndex, int columnIndex) {
        if (unableToFetchValues()) {
            return null;
        }

        final int centerOffset = MathUtils.floorInt(size / 2.0);
        int pixelX = centerPixelX - centerOffset + columnIndex;
        int pixelY = centerPixelY - centerOffset + rowIndex;
        final Rectangle imageRectangle = new Rectangle(band.getRasterWidth(), band.getRasterHeight());
        if (imageRectangle.contains(pixelX, pixelY)) {
            if (band.isPixelValid(pixelX, pixelY)) {
                return ProductUtils.getGeophysicalSampleAsDouble(band, pixelX, pixelY, 0);
            } else {
                return Double.NaN;
            }
        }
        return null;
    }

    public void setMatrixSize(int matrixSize) {
        if (this.size != matrixSize) {
            this.size = matrixSize;
            fireTableStructureChanged();
        }
    }

    public void setBand(Band band) {
        if (this.band != band) {
            this.band = band;
            fireTableDataChanged();
        }
    }

    public Band getBand() {
        return band;
    }

    public void setCenterPixel(int pixelX, int pixelY) {
        if (this.centerPixelX != pixelX || this.centerPixelY != pixelY) {
            this.centerPixelX = pixelX;
            this.centerPixelY = pixelY;
            fireTableDataChanged();
        }
    }

    public void clearMatrix() {
        setCenterPixel(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private boolean unableToFetchValues() {
        return band == null || centerPixelX == Integer.MIN_VALUE || centerPixelY == Integer.MIN_VALUE;
    }
}
