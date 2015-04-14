package org.esa.snap.rcp.util;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.TiePointGrid;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Color;
import java.util.Random;

/**
 * Creates product instances for testing.
 *
 * @author Norman Fomferra
 */
public class TestProducts {

    public static Product[] createProducts() {
        return new Product[]{createProduct1(), createProduct2(), createProduct3()};
    }

    public static Product createProduct1() {
        Product product = new Product("Product_1", "Type_1", 2048, 1024);
        product.addTiePointGrid(new TiePointGrid("Grid_A", 32, 16, 0, 0, 2048f / 32, 1024f / 16, createRandomPoints(32 * 16)));
        product.addTiePointGrid(new TiePointGrid("Grid_B", 32, 16, 0, 0, 2048f / 32, 1024f / 16, createRandomPoints(32 * 16)));
        product.addBand("Band_A", "sin(4 * PI * sqrt( sqr(X/1000.0 - 1) + sqr(Y/500.0 - 1) ))");
        product.addBand("Band_B", "sin(4 * PI * sqrt( 2.0 * abs(X/1000.0 * Y/500.0) ))");
        product.addMask("Mask_A", "Band_A > 0.5", "I am Mask A", Color.ORANGE, 0.5);
        product.addMask("Mask_B", "Band_B < 0.0", "I am Mask B", Color.RED, 0.5);
        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
        product.getMetadataRoot().addElement(new MetadataElement("Local_Attributes"));
        product.setModified(false);
        return product;
    }

    public static Product createProduct2() {
        Product product = new Product("Product_2", "Type_2", 1024, 2048);
        product.addTiePointGrid(new TiePointGrid("Grid_1", 16, 32, 0, 0, 1024f / 16, 2048f / 32, createRandomPoints(32 * 16)));
        product.addTiePointGrid(new TiePointGrid("Grid_2", 16, 32, 0, 0, 1024f / 16, 2048f / 32, createRandomPoints(32 * 16)));
        product.addBand("Band_1", "cos(X/100)-sin(Y/100)");
        product.addBand("Band_2", "sin(X/100)+cos(Y/100)");
        product.addBand("Band_3", "cos(X/100)*cos(Y/100)");
        product.addMask("Mask_1", "Band_1 > 0.5", "I am Mask 1", Color.GREEN, 0.5);
        product.addMask("Mask_2", "Band_2 < 0.0", "I am Mask 2", Color.CYAN, 0.5);
        product.addMask("Mask_3", "Band_3 > -0.1 && Band_3 < 0.1", "I am Mask 3", Color.BLUE, 0.5);
        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
        product.getMetadataRoot().addElement(new MetadataElement("Local_Attributes"));
        product.setModified(false);
        return product;
    }

    public static Product createProduct3() {
        int size = 10 * 1024;
        Product product = new Product("Product_3", "Type_3", size, size);
        product.setPreferredTileSize(512, 512);
        Band band1 = new Band("Big_Band_1", ProductData.TYPE_FLOAT64, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        Band band2 = new Band("Big_Band_2", ProductData.TYPE_FLOAT64, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        Band band3 = new Band("Big_Band_3", ProductData.TYPE_FLOAT64, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        Band band4 = new Band("Big_Band_4", ProductData.TYPE_FLOAT64, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        Band band5 = new Band("Big_Band_5", ProductData.TYPE_FLOAT64, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        band1.setSourceImage(ConstantDescriptor.create(1f * size, 1F * size, new Double[]{1.0}, null));
        band2.setSourceImage(ConstantDescriptor.create(1f * size, 1F * size, new Double[]{2.0}, null));
        band3.setSourceImage(ConstantDescriptor.create(1f * size, 1F * size, new Double[]{3.0}, null));
        band4.setSourceImage(ConstantDescriptor.create(1f * size, 1F * size, new Double[]{4.0}, null));
        band5.setSourceImage(ConstantDescriptor.create(1f * size, 1F * size, new Double[]{5.0}, null));
        product.addBand(band1);
        product.addBand(band2);
        product.addBand(band3);
        product.addBand(band4);
        product.addBand(band5);
        product.setModified(true);
        return product;
    }

    private static float[] createRandomPoints(int n) {
        Random random = new Random();
        float[] pnts = new float[n];
        for (int i = 0; i < pnts.length; i++) {
            pnts[i] = (float) random.nextGaussian();
        }
        return pnts;
    }
}
