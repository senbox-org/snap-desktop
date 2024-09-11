/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.ShapeAttributes;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.awt.*;
import java.util.List;

/**
 * Base layer class
 */
public abstract class BaseLayer extends RenderableLayer {
    protected Product selectedProduct = null;
    protected RasterDataNode selectedRaster = null;

    protected final static Material RED_MATERIAL = new Material(Color.RED);
    protected final static Material ORANGE_MATERIAL = new Material(Color.ORANGE);
    protected final static Material GREEN_MATERIAL = new Material(Color.GREEN);
    protected final static Material WHITE_MATERIAL = new Material(Color.WHITE);

    public void setSelectedProduct(final Product product) {
        selectedProduct = product;
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedRaster(final RasterDataNode raster) {
        selectedRaster = raster;
    }

    public Path createPath(final List<Position> positions,
                           final Material normalMaterial, final Material highlightMaterial) {
        Path polyLine = new Path(positions);
        polyLine.setSurfacePath(true);
        polyLine.setFollowTerrain(true);

        ShapeAttributes pathAttributes = new BasicShapeAttributes();
        pathAttributes.setOutlineMaterial(normalMaterial);
        pathAttributes.setEnableAntialiasing(true);
        polyLine.setAttributes(pathAttributes);

        ShapeAttributes highlightAttributes = new BasicShapeAttributes();
        highlightAttributes.setOutlineMaterial(highlightMaterial);
        highlightAttributes.setEnableAntialiasing(true);
        polyLine.setHighlightAttributes(highlightAttributes);

        return polyLine;
    }
}
