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
package org.esa.snap.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.geom.AffineTransform;

import static org.esa.snap.core.util.Debug.assertTrue;
import static org.junit.Assert.*;

public class ProductSceneViewTest {

    private Product product;
    private VirtualBand r;
    private VirtualBand g;
    private VirtualBand b;

    @Before
    public void setUp() throws Exception {
        product = new Product("x", "y", 2, 3);

        r = new VirtualBand("r", ProductData.TYPE_FLOAT32, 2, 3, "0");
        g = new VirtualBand("g", ProductData.TYPE_FLOAT32, 2, 3, "0");
        b = new VirtualBand("b", ProductData.TYPE_FLOAT32, 2, 3, "0");

        product.addBand(r);
        product.addBand(g);
        product.addBand(b);

        r.ensureRasterData();
        g.ensureRasterData();
        b.ensureRasterData();
    }

    @After
    public void tearDown() throws Exception {
        r = null;
        g = null;
        b = null;
        product = null;
    }

    @Test
    public void testIsRGB() {
        ProductSceneView view;

        view = new ProductSceneView(new ProductSceneImage(r, new DefaultPropertyMap(), ProgressMonitor.NULL));
        assertFalse(view.isRGB());

        view = new ProductSceneView(new ProductSceneImage("RGB", r, g, b, new DefaultPropertyMap(), ProgressMonitor.NULL));
        assertTrue(view.isRGB());
    }

    @Test
    public void testDispose() {
        final ProductSceneView view = new ProductSceneView(new ProductSceneImage(r, new DefaultPropertyMap(), ProgressMonitor.NULL));
        view.dispose();
        assertNull(view.getSceneImage());
    }

    @Test
    public void testDoesNotCreateMaskCollectionLayerWithoutOverlayMasks() {
        product.getMaskGroup().add(createMask("mask"));

        final ProductSceneImage sceneImage = new ProductSceneImage(r, new DefaultPropertyMap(), ProgressMonitor.NULL);
        final ProductSceneView view = new ProductSceneView(sceneImage);

        try {
            assertNull(sceneImage.getMaskCollectionLayer(false));
            assertFalse(view.isMaskOverlayEnabled());
        } finally {
            view.dispose();
        }
    }

    @Test
    public void testCreatesMaskCollectionLayerWithOverlayMasks() {
        final Mask mask = createMask("mask");
        product.getMaskGroup().add(mask);
        r.getOverlayMaskGroup().add(mask);

        final ProductSceneImage sceneImage = new ProductSceneImage(r, new DefaultPropertyMap(), ProgressMonitor.NULL);
        final ProductSceneView view = new ProductSceneView(sceneImage);

        try {
            assertNotNull(sceneImage.getMaskCollectionLayer(false));
            assertTrue(view.isMaskOverlayEnabled());
        } finally {
            view.dispose();
        }
    }

    @Test
    public void testAffineTransformReplacesManualCalculation() {
        double modelOffsetX = 37.8;
        double modelOffsetY = -54.1;
        double viewScale = 2.5;

        final AffineTransform transform = new AffineTransform();
        transform.scale(viewScale, viewScale);
        transform.translate(-modelOffsetX, -modelOffsetY);

        double modelX = 10.4;
        double modelY = 2.9;

        double viewX = (modelX - modelOffsetX) * viewScale;
        double viewY = (modelY - modelOffsetY) * viewScale;

        final double[] result = new double[2];
        transform.transform(new double[]{modelX, modelY}, 0, result, 0, 1);
        assertEquals(viewX, result[0], 1e-10);
        assertEquals(viewY, result[1], 1e-10);
    }

    private Mask createMask(String name) {
        return Mask.BandMathsType.create(name, "description", r.getRasterWidth(), r.getRasterHeight(),
                                         "r > 0", Color.RED, 0.5);
    }
}
