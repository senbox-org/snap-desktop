/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.ui.crs;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValueSet;
import org.esa.snap.core.datamodel.ImageGeometry;
import org.esa.snap.core.datamodel.Product;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
public class OutputGeometryFormModel {

    public static final int REFERENCE_PIXEL_USER = 2;
    public static final int REFERENCE_PIXEL_UPPER_LEFT = 0;
    public static final int REFERENCE_PIXEL_SCENE_CENTER = 1;
    private static final int REFERENCE_PIXEL_DEFAULT = REFERENCE_PIXEL_SCENE_CENTER;
    private static final boolean FIT_PRODUCT_SIZE_DEFAULT = true;

    public int referencePixelLocation;
    public boolean fitProductSize;

    private transient Product sourceProduct;
    private transient CoordinateReferenceSystem targetCrs;
    private transient PropertySet propertyContainer;

    public OutputGeometryFormModel(PropertySet sourcePropertySet) {
        init(null,
                null,
                getFitProductSize(null, sourcePropertySet, targetCrs),
                getReferencePixelLocation(sourcePropertySet),
                sourcePropertySet);
    }

    public OutputGeometryFormModel(Product sourceProduct, Product collocationProduct) {
        this(sourceProduct, ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct));
    }

    public OutputGeometryFormModel(Product sourceProduct, CoordinateReferenceSystem targetCrs) {
        this(sourceProduct, ImageGeometry.createTargetGeometry(sourceProduct, targetCrs,
                null, null, null, null,
                null, null, null, null, null));
    }

    public OutputGeometryFormModel(Product sourceProduct, CoordinateReferenceSystem targetCrs, PropertySet ps) {
        this(sourceProduct, ImageGeometry.createTargetGeometry(sourceProduct, targetCrs,
                ps.getValue("pixelSizeX"),
                ps.getValue("pixelSizeY"),
                ps.getValue("width"),
                ps.getValue("height"),
                ps.getValue("orientation"),
                ps.getValue("easting"),
                ps.getValue("northing"),
                ps.getValue("referencePixelX"),
                ps.getValue("referencePixelY")));
    }

    public OutputGeometryFormModel(OutputGeometryFormModel formModel) {
        init(formModel.sourceProduct,
                formModel.targetCrs,
                formModel.fitProductSize,
                formModel.referencePixelLocation,
                formModel.getPropertySet());
    }

    private OutputGeometryFormModel(Product sourceProduct, ImageGeometry imageGeometry) {
        init(sourceProduct,
                imageGeometry.getMapCrs(),
                FIT_PRODUCT_SIZE_DEFAULT,
                REFERENCE_PIXEL_DEFAULT,
                PropertyContainer.createObjectBacked(imageGeometry));
    }

    private void init(Product sourceProduct, CoordinateReferenceSystem targetCrs, boolean fitProductSize, int referencePixelLocation,
                      PropertySet sourcePropertySet) {
        this.sourceProduct = sourceProduct;
        this.targetCrs = targetCrs;
        this.fitProductSize = getFitProductSize(sourceProduct, sourcePropertySet, targetCrs);
        this.referencePixelLocation = getReferencePixelLocation(sourcePropertySet);

        this.propertyContainer = PropertyContainer.createValueBacked(ImageGeometry.class);
        configurePropertyContainer(propertyContainer);

        Property[] properties = sourcePropertySet.getProperties();
        for (Property property : properties) {
            if (propertyContainer.isPropertyDefined(property.getName())) {
                propertyContainer.setValue(property.getName(), property.getValue());
            }
        }

    }

    public PropertySet getPropertySet() {
        return propertyContainer;
    }

    public void setSourceProduct(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
        updateProductSize();
    }

    public void setTargetCrs(CoordinateReferenceSystem targetCrs) {
        this.targetCrs = targetCrs;
        updateProductSize();
        setAxisUnits(propertyContainer);
    }

    public void resetToDefaults(ImageGeometry ig) {
        PropertyContainer pc = PropertyContainer.createObjectBacked(ig);
        Property[] properties = pc.getProperties();
        for (Property property : properties) {
            propertyContainer.setValue(property.getName(), property.getValue());
        }
        propertyContainer.setValue("referencePixelLocation", REFERENCE_PIXEL_DEFAULT);
        propertyContainer.setValue("fitProductSize", FIT_PRODUCT_SIZE_DEFAULT);
    }

    private void configurePropertyContainer(PropertySet ps) {
        PropertySet thisPS = PropertyContainer.createObjectBacked(this);
        ps.addProperties(thisPS.getProperties());

        ps.getDescriptor("referencePixelLocation").setValueSet(new ValueSet(new Integer[]{0, 1, 2}));
        setAxisUnits(ps);
        ps.getDescriptor("orientation").setUnit("°");

        ps.addPropertyChangeListener(new ChangeListener());
    }

    private void setAxisUnits(PropertySet pc) {
        if (targetCrs != null) {
            String crsAxisUnit = targetCrs.getCoordinateSystem().getAxis(0).getUnit().toString();
            pc.getDescriptor("easting").setUnit(crsAxisUnit);
            pc.getDescriptor("northing").setUnit(crsAxisUnit);
            pc.getDescriptor("pixelSizeX").setUnit(crsAxisUnit);
            pc.getDescriptor("pixelSizeY").setUnit(crsAxisUnit);
        }
    }

    private void updateProductSize() {
        if (targetCrs != null && sourceProduct != null) {
            Double pixelSizeX = propertyContainer.getValue("pixelSizeX");
            Double pixelSizeY = propertyContainer.getValue("pixelSizeY");
            Rectangle productSize = ImageGeometry.calculateProductSize(sourceProduct, targetCrs, pixelSizeX, pixelSizeY);
            propertyContainer.setValue("width", productSize.width);
            propertyContainer.setValue("height", productSize.height);
        }
    }

    private void updateReferencePixel() {
        double referencePixelX = propertyContainer.getValue("referencePixelX");
        double referencePixelY = propertyContainer.getValue("referencePixelY");
        if (referencePixelLocation == REFERENCE_PIXEL_UPPER_LEFT) {
            referencePixelX = 0.5;
            referencePixelY = 0.5;
        } else if (referencePixelLocation == REFERENCE_PIXEL_SCENE_CENTER) {
            referencePixelX = 0.5 * (Integer) propertyContainer.getValue("width");
            referencePixelY = 0.5 * (Integer) propertyContainer.getValue("height");
        }
        propertyContainer.setValue("referencePixelX", referencePixelX);
        propertyContainer.setValue("referencePixelY", referencePixelY);
    }

    private static boolean getFitProductSize(Product sourceProduct, PropertySet sourcePropertySet, CoordinateReferenceSystem targetCrs) {
        // if either width or height is set then don't set fit product size
        if (sourceProduct != null) {
            int width = sourcePropertySet.getValue("width");
            int height =  sourcePropertySet.getValue("height");

            ImageGeometry iGeometry;
            iGeometry = ImageGeometry.createTargetGeometry(sourceProduct, targetCrs,
                    null, null, null, null,
                    null, null, null, null,
                    null);

            // }
            Rectangle imageRect = iGeometry.getImageRect();

            if (width == imageRect.width && height == imageRect.height) {
                return true;
            } else {
                return false;
            }
        } else {
            return !(sourcePropertySet.getProperty("width") != null || sourcePropertySet.getProperty("height") != null);
        }
    }

    private static int getReferencePixelLocation(PropertySet sourcePropertySet) {
        if (sourcePropertySet != null) {
            double referencePixelX = sourcePropertySet.getValue("referencePixelX");
            double referencePixelY = sourcePropertySet.getValue("referencePixelY");
            int width = sourcePropertySet.getValue("width");
            int height = sourcePropertySet.getValue("height");
            if (referencePixelX == 0.5 && referencePixelY == 0.5) {
                return REFERENCE_PIXEL_UPPER_LEFT;
            } else if (referencePixelX == width / 2.0 && referencePixelY == height / 2.0) {
                return REFERENCE_PIXEL_SCENE_CENTER;
            } else {
                return REFERENCE_PIXEL_USER;
            }
        } else {
            return REFERENCE_PIXEL_DEFAULT;
        }
    }

    private class ChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            String propertyName = event.getPropertyName();

            if (fitProductSize) {
                if (propertyName.startsWith("pixelSize") || propertyName.startsWith("fitProductSize")) {
                    updateProductSize();
                    updateReferencePixel();
                }
            } else if (propertyName.startsWith("referencePixelLocation")
                    || ((propertyName.startsWith("width") || propertyName.startsWith("height"))
                    && referencePixelLocation == REFERENCE_PIXEL_SCENE_CENTER)) {
                updateReferencePixel();
            }
        }
    }
}
