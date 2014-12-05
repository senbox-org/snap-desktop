package org.esa.snap.gui.util;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.awt.Color;
import java.util.Random;

/**
 * Creates product instances for testing.
 *
 * @author Norman Fomferra
 */
public class TestProducts {

    public static Product[] createProducts() {
        return new Product[]{createProduct1(), createProduct2()};
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
        return product;
    }

    private static double[] createRandomPoints(int n) {
        Random random = new Random();
        double[] pnts = new double[n];
        for (int i = 0; i < pnts.length; i++) {
            pnts[i] = random.nextGaussian();
        }
        return pnts;
    }
}
