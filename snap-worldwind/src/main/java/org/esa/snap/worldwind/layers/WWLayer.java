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

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.layers.Layer;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import javax.swing.JPanel;

/**
 * World Wind renderable layer
 */
public interface WWLayer extends Layer {

    BaseLayer.Suitability getSuitability(Product product);

    void addProduct(Product product, WorldWindowGLCanvas wwd);

    void removeProduct(Product product);

    JPanel getControlPanel(WorldWindowGLCanvas wwd);

    void setSelectedProduct(final Product product);

    Product getSelectedProduct();

    void setSelectedRaster(final RasterDataNode raster);

    default void updateInfoAnnotation(final SelectEvent event) {}
}