package org.esa.snap.rcp.actions.tools;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttachPixelGeoCodingActionTest {

    @Test
    public void testGetValidBandCount_none() {
        final Product product = new Product("test", "test_type", 10, 15);

        final int validCount = AttachPixelGeoCodingAction.getValidBandCount(product);
        assertEquals(0, validCount);
    }

    @Test
    public void testGetValidBandCount_one() {
        final Product product = new Product("test", "test_type", 11, 16);
        final Band band = new Band("right", ProductData.TYPE_FLOAT64, 11, 16);
        product.addBand(band);

        final int validCount = AttachPixelGeoCodingAction.getValidBandCount(product);
        assertEquals(1, validCount);
    }

    @Test
    public void testGetValidBandCount_oneRight_oneWrong() {
        final Product product = new Product("test", "test_type", 12, 17);

        Band band = new Band("right", ProductData.TYPE_INT32, 12, 17);
        product.addBand(band);

        band = new Band("wrong", ProductData.TYPE_INT32, 3, 3);
        product.addBand(band);

        final int validCount = AttachPixelGeoCodingAction.getValidBandCount(product);
        assertEquals(1, validCount);
    }

    @Test
    public void testGetValidBandCount_threeRight_oneWrong_clippedAt2() {
        final Product product = new Product("test", "test_type", 13, 18);

        Band band = new Band("right_1", ProductData.TYPE_INT8, 13, 18);
        product.addBand(band);

        band = new Band("wrong", ProductData.TYPE_INT8, 3, 3);
        product.addBand(band);

        band = new Band("right_2", ProductData.TYPE_INT8, 13, 18);
        product.addBand(band);

        band = new Band("right_3", ProductData.TYPE_INT8, 13, 18);
        product.addBand(band);

        final int validCount = AttachPixelGeoCodingAction.getValidBandCount(product);
        assertEquals(2, validCount);
    }

    @Test
    public void testGetRequiredMemory() {
        Product product = new Product("test", "test_type", 100, 180);

        long memSize = AttachPixelGeoCodingAction.getRequiredMemory(product);
        assertEquals(288000L, memSize);

        product = new Product("test", "test_type", 190, 1200);

        memSize = AttachPixelGeoCodingAction.getRequiredMemory(product);
        assertEquals(3648000L, memSize);
    }
}
