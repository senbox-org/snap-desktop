/*
 * Copyright (C) 2026 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.mask;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.junit.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RangeThresholdSliderPanelTest {


    @Test
    @STTM("SNAP-4243")
    public void testIsRangeMask() {
        Mask rangeMask = createRangeMask();
        Mask bandMathsMask = Mask.BandMathsType.create("maths", "B > 1.0", 10, 10, "B > 1.0", Color.YELLOW, 0.5);

        assertTrue(RangeThresholdSliderPanel.isRangeMask(rangeMask));
        assertFalse(RangeThresholdSliderPanel.isRangeMask(bandMathsMask));
        assertFalse(RangeThresholdSliderPanel.isRangeMask(null));
    }

    @Test
    @STTM("SNAP-4243")
    public void testComputeRelativeThreshold() {
        assertEquals(10.0, RangeThresholdSliderPanel.computeRelativeThreshold(10.0, 0.0, 20.0, 0), 1.0e-8);
        assertEquals(0.0, RangeThresholdSliderPanel.computeRelativeThreshold(10.0, 0.0, 20.0, -1000), 1.0e-8);
        assertEquals(20.0, RangeThresholdSliderPanel.computeRelativeThreshold(10.0, 0.0, 20.0, 1000), 1.0e-8);

        double smallMove = RangeThresholdSliderPanel.computeRelativeThreshold(10.0, 0.0, 20.0, 100);
        double largeMove = RangeThresholdSliderPanel.computeRelativeThreshold(10.0, 0.0, 20.0, 900);
        assertTrue(smallMove > 10.0);
        assertTrue(largeMove > smallMove);
        assertTrue(smallMove - 10.0 < 1.0);
    }

    @Test
    @STTM("SNAP-4243")
    public void testApplyThresholdUpdatesRangeMask() {
        Mask mask = createRangeMask();

        RangeThresholdSliderPanel.applyThreshold(mask, true, 0.25);
        assertEquals(0.25, Mask.RangeType.getMinimum(mask), 1.0e-8);
        assertEquals("B >= 0.25 && B <= 1.0", mask.getDescription());

        RangeThresholdSliderPanel.applyThreshold(mask, false, 0.75);
        assertEquals(0.75, Mask.RangeType.getMaximum(mask), 1.0e-8);
        assertEquals("B >= 0.25 && B <= 0.75", mask.getDescription());
    }

    @Test
    @STTM("SNAP-4243")
    public void testApplyThresholdKeepsMinimumBelowMaximum() {
        Mask mask = createRangeMask();

        RangeThresholdSliderPanel.applyThreshold(mask, true, 2.0);
        assertEquals(1.0, Mask.RangeType.getMinimum(mask), 1.0e-8);

        RangeThresholdSliderPanel.applyThreshold(mask, false, 0.0);
        assertEquals(1.0, Mask.RangeType.getMaximum(mask), 1.0e-8);
    }

    @Test
    @STTM("SNAP-4243")
    public void testApplyThresholdClampsAgainstOtherThreshold() {
        Mask mask = createRangeMask();
        Mask.RangeType.setMinimum(mask, 5.0);
        Mask.RangeType.setMaximum(mask, 20.0);

        RangeThresholdSliderPanel.applyThreshold(mask, true, 25.0);
        assertEquals(20.0, Mask.RangeType.getMinimum(mask), 1.0e-8);

        RangeThresholdSliderPanel.applyThreshold(mask, false, 10.0);
        assertEquals(20.0, Mask.RangeType.getMaximum(mask), 1.0e-8);
    }

    @Test
    @STTM("SNAP-4243")
    public void testGetReferencedRasterResolvesExternalName() {
        Product product = new Product("P", "T", 10, 10);
        RasterDataNode raster = product.addBand("B band", ProductData.TYPE_UINT8);
        Mask mask = createRangeMask("'B band'");

        assertEquals(raster, RangeThresholdSliderPanel.getReferencedRaster(mask, product));
    }

    @Test
    @STTM("SNAP-4243")
    public void testApplyThresholdUsesRasterRange() {
        Product product = new Product("P", "T", 2, 1);
        RasterDataNode raster = product.addBand("B", ProductData.TYPE_UINT8);
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setSample(0, 0, 0, 10);
        image.getRaster().setSample(1, 0, 0, 20);
        raster.setSourceImage(image);

        Mask mask = createRangeMask();
        RangeThresholdSliderPanel.applyThreshold(mask, true, -100.0, product);
        RangeThresholdSliderPanel.applyThreshold(mask, false, 100.0, product);

        assertEquals(10.0, Mask.RangeType.getMinimum(mask), 1.0e-8);
        assertEquals(20.0, Mask.RangeType.getMaximum(mask), 1.0e-8);
    }

    private static Mask createRangeMask() {
        return createRangeMask("B");
    }

    private static Mask createRangeMask(String rasterName) {
        Mask mask = new Mask("range", 10, 10, Mask.RangeType.INSTANCE);
        Mask.RangeType.setRasterName(mask, rasterName);
        Mask.RangeType.setMinimum(mask, 0.0);
        Mask.RangeType.setMaximum(mask, 1.0);
        mask.setDescription(Mask.RangeType.getExpression(mask));
        return mask;
    }
}
