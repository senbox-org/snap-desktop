package org.esa.snap.rcp.util;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.transform.AffineTransform2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Random;

/**
 * Creates product instances for testing.
 *
 * @author Norman Fomferra
 */
public class TestProducts {


    public static Product[] createProducts() {
        return new Product[]{createProduct1(),
                createProduct2(), createProduct3(), createProduct4(),
                createProduct5(), createProduct6()
        };
    }

    public static Product createProduct1() {
        Product product = new Product("Test_Product_1", "Test_Type_1", 2048, 1024);
        product.addTiePointGrid(new TiePointGrid("Grid_A", 32, 16, 0, 0, 2048f / 32, 1024f / 16, createRandomPoints(32 * 16)));
        product.addTiePointGrid(new TiePointGrid("Grid_B", 32, 16, 0, 0, 2048f / 32, 1024f / 16, createRandomPoints(32 * 16)));
        product.addBand("Band_A", "sin(4 * PI * sqrt( sq(X/1000.0 - 1) + sq(Y/500.0 - 1) ))");
        product.addBand("Band_B", "sin(4 * PI * sqrt( 2.0 * abs(X/1000.0 * Y/500.0) ))");
        product.addMask("Mask_A", "Band_A > 0.5", "I am Mask A", Color.ORANGE, 0.5);
        product.addMask("Mask_B", "Band_B < 0.0", "I am Mask B", Color.RED, 0.5);
        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
        product.getMetadataRoot().addElement(new MetadataElement("Local_Attributes"));
        product.setModified(false);
        double sx = 40.0 / product.getSceneRasterWidth();
        AffineTransform at = new AffineTransform();
        at.translate(-80, -30);
        at.rotate(0.3, 20.0, 10.0);
        at.scale(sx, sx);
        product.setSceneGeoCoding(new ATGeoCoding(at));
        return product;
    }

    public static Product createProduct2() {
        Product product = new Product("Test_Product_2", "Test_Type_2", 1024, 2048);
        product.addTiePointGrid(new TiePointGrid("Grid_1", 16, 32, 0, 0, 1024f / 16, 2048f / 32, createRandomPoints(32 * 16)));
        product.addTiePointGrid(new TiePointGrid("Grid_2", 16, 32, 0, 0, 1024f / 16, 2048f / 32, createRandomPoints(32 * 16)));
        product.addBand("Band_1", "cos(X/100)-sin(Y/100)");
        product.addBand("Band_2", "sin(X/100)+cos(Y/100)");
        product.addBand("Band_3", "cos(X/100)*cos(Y/100)");
        product.addBand("Band_1_Unc", "cos(3*X/100)-sin(3*Y/100)");
        product.addBand("Band_2_Unc", "sin(3*X/100)+cos(3*Y/100)");
        product.addBand("Band_3_Unc", "cos(3*X/100)*cos(3*Y/100)");
        product.getBand("Band_1").addAncillaryVariable(product.getBand("Band_1_Unc"), "uncertainty");
        product.getBand("Band_2").addAncillaryVariable(product.getBand("Band_2_Unc"), "uncertainty");
        product.getBand("Band_3").addAncillaryVariable(product.getBand("Band_3_Unc"), "uncertainty");
        product.addMask("Mask_1", "Band_1 > 0.5", "I am Mask 1", Color.GREEN, 0.5);
        product.addMask("Mask_2", "Band_2 < 0.0", "I am Mask 2", Color.CYAN, 0.5);
        product.addMask("Mask_3", "Band_3 > -0.1 && Band_3 < 0.1", "I am Mask 3", Color.BLUE, 0.5);
        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
        product.getMetadataRoot().addElement(new MetadataElement("Local_Attributes"));
        product.setModified(false);
        double sx = 20.0 / product.getSceneRasterWidth();
        AffineTransform at = new AffineTransform();
        at.scale(sx, sx);
        at.rotate(-0.2, 10.0, 10.0);
        product.setSceneGeoCoding(new ATGeoCoding(at));
        product.addBand("A", "Band_1");
        product.addBand("B", "Band_1 + Band_2 + Band_3");
        product.addBand("C", "Band_1 / (2.3 + Band_2 + Band_3)");
        product.addBand("D", "pow(Band_1, 3) / (pow(Band_1, 3) + pow(Band_3, 3))");
        return product;
    }

    public static Product createProduct3() {
        int size = 10 * 1024;
        Product product = new Product("Test_Product_3", "Test_Type_3", size, size);
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
        double sx = 30.0 / product.getSceneRasterWidth();
        AffineTransform at = new AffineTransform();
        at.translate(100, 0.0);
        at.rotate(0.1, 15.0, 15.0);
        at.scale(sx, sx);
        product.setSceneGeoCoding(new ATGeoCoding(at));
        return product;
    }

    public static Product createProduct4() {
        Product product = new Product("Test_Product_4", "Test_Type_4", 512, 512);
        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
        product.getMetadataRoot().addElement(new MetadataElement("Local_Attributes"));
        product.setModified(false);
        double sx = 10.0 / product.getSceneRasterWidth();
        VirtualBand band4 = new VirtualBand("Band_4", ProductData.TYPE_FLOAT64, 512, 512, "cos(ampl((X-256)/100, (Y-256)/100))");
        product.addBand(band4);
        AffineTransform at4 = new AffineTransform();
        at4.scale(0.5 * sx, 0.5 * sx);
        at4.rotate(-0.2, 5.0, 5.0);
        at4.translate(256, 256);
        product.setSceneGeoCoding(new ATGeoCoding(at4));

        return product;
    }

    public static Product createProduct5() {
        try {
            Product product = new Product("Test_Product_5_CRS", "Test_Type_5_CRS", 512, 512);
            product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
            product.getMetadataRoot().addElement(new MetadataElement("Local_Attributes"));
            product.setModified(false);
            product.addBand("Band_A", "sin(4 * PI * sqrt( sq(X/1000.0 - 1) + sq(Y/500.0 - 1) ))");
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 512, 512, 0, 10, 1, 1));

            final String b_expression = "sin(4 * PI * sqrt( 2.0 * abs(X/1000.0 * Y/500.0) ))";
            final VirtualBand band_b = new VirtualBand("Band_B", ProductData.TYPE_FLOAT32, 1024, 256, b_expression);
            band_b.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1024, 256, 0, 10, 0.5, 2.0));
            band_b.setNoDataValueUsed(true);
            product.addBand(band_b);

            return product;
        } catch (FactoryException | TransformException e) {
            return null;
        }
    }

    public static Product createProduct6() {
        try {
            Product product = new Product("Test_Product_6_SceneRasterTransforms", "Test_Type_6_SceneRasterTransforms", 512, 512);
            product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
            product.getMetadataRoot().addElement(new MetadataElement("Local_Attributes"));
            product.setModified(false);
            final String a_expression = "sin(4 * PI * sqrt( sq(X/1000.0 - 1) + sq(Y/500.0 - 1) ))";
            final Band band_a = new VirtualBand("Band_A", ProductData.TYPE_FLOAT32, 2048, 1024, a_expression);
            final AffineTransform a_forward = new AffineTransform();
            a_forward.scale(0.25, 0.5);
            final AffineTransform a_inverse = a_forward.createInverse();
            band_a.setModelToSceneTransform(new AffineTransform2D(a_forward));
            band_a.setSceneToModelTransform(new AffineTransform2D(a_inverse));
            product.addBand(band_a);

            final String b_expression = "sin(4 * PI * sqrt( 2.0 * abs(X/1000.0 * Y/500.0) ))";
            final VirtualBand band_b = new VirtualBand("Band_B", ProductData.TYPE_FLOAT32, 128, 256, b_expression);
            final AffineTransform b_forward = new AffineTransform();
            b_forward.scale(2.0, 2.0);
            b_forward.translate(128, 0);
            final AffineTransform b_inverse = b_forward.createInverse();
            band_b.setModelToSceneTransform(new AffineTransform2D(b_forward));
            band_b.setSceneToModelTransform(new AffineTransform2D(b_inverse));
            band_b.setNoDataValue(Double.NaN);
            band_b.setNoDataValueUsed(true);
            product.addBand(band_b);

            return product;
        } catch (NoninvertibleTransformException e) {
            return null;
        }
    }

//    public static Product createProduct7() {
//        Product product = new Product("Test_Product_3_different_tie_point_geocodings", "Test_Type_3_different_tie_point_geocodings", 1024, 2048);
//        product.addTiePointGrid(new TiePointGrid("Grid_1", 16, 32, 0, 0, 1024f / 16, 2048f / 32, createRandomPoints(32 * 16)));
//        product.addTiePointGrid(new TiePointGrid("Grid_2", 16, 32, 0, 0, 1024f / 16, 2048f / 32, createRandomPoints(32 * 16)));
//        product.addBand("Band_1", "cos(X/100)-sin(Y/100)");
//        product.addMask("Mask_1", "Band_1 > 0.5", "I am Mask 1", Color.GREEN, 0.5);
//        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
//        product.getMetadataRoot().addElement(new MetadataElement("Local_Attributes"));
//        product.setModified(false);
//        double sx = 20.0 / product.getSceneRasterWidth();
//        AffineTransform at = new AffineTransform();
//        at.scale(sx, sx);
//        at.rotate(-0.2, 10.0, 10.0);
//        product.setSceneGeoCoding(new ATGeoCoding(at));
//
//        Band band2 = new Band("Band_2", ProductData.TYPE_FLOAT64, 512, 512);
//        final VirtualBandOpImage image = VirtualBandOpImage.
//                builder("cos(ampl((X-256)/100, (Y-256)/100))", product).
//                sourceSize(band2.getRasterSize()).
//                dataType(band2.getDataType()).
//                create();
//
//        AffineTransform at_model_2 = new AffineTransform();
//        at_model_2.scale(0.5, 0.5);
//        at_model_2.translate(128, 128);
//        final DefaultMultiLevelModel targetModel = new DefaultMultiLevelModel(at_model_2, 512, 512);
//        final DefaultMultiLevelModel targetModel = new DefaultMultiLevelModel(at2, 512, 512);
//        final DefaultMultiLevelModel targetModel = new DefaultMultiLevelModel(new AffineTransform(), 512, 512);
//        final DefaultMultiLevelSource targetMultiLevelSource = new DefaultMultiLevelSource(image, targetModel);
//        band2.setSourceImage(new DefaultMultiLevelImage(targetMultiLevelSource));
//        product.addBand(band2);
//        AffineTransform at2 = new AffineTransform();
//        at2.scale(0.5 * sx, 0.5 * sx);
//        at2.scale(0.5, 0.5);
//        at2.rotate(-0.2, 5.0, 5.0);
//        at2.translate(256, 256);
//        band2.setGeoCoding(new ATGeoCoding(at2));
//        band2.setNoDataValue(-1.0);
//        band2.setNoDataValueUsed(true);
//        return product;
//    }

    private static float[] createRandomPoints(int n) {
        Random random = new Random(987);
        float[] pnts = new float[n];
        for (int i = 0; i < pnts.length; i++) {
            pnts[i] = (float) random.nextGaussian();
        }
        return pnts;
    }

    private static class ATGeoCoding extends AbstractGeoCoding {
        private static final PixelPos INVALID_PIXEL_POS = new PixelPos(Double.NaN, Double.NaN);
        private final AffineTransform affineTransform;

        public ATGeoCoding(AffineTransform affineTransform) {
            this.affineTransform = affineTransform;
        }

        @Override
        public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
            return false;
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            return false;
        }

        @Override
        public boolean canGetPixelPos() {
            return true;
        }

        @Override
        public boolean canGetGeoPos() {
            return true;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            try {
                Point2D p = affineTransform.inverseTransform(new Point2D.Double(geoPos.lon, geoPos.lat), null);
                if (pixelPos == null) {
                    pixelPos = new PixelPos();
                }
                pixelPos.x = p.getX();
                pixelPos.y = p.getY();
                return pixelPos;
            } catch (NoninvertibleTransformException e) {
                return INVALID_PIXEL_POS;
            }
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            Point2D point2D = affineTransform.transform(pixelPos, null);
            if (geoPos == null) {
                geoPos = new GeoPos();
            }
            geoPos.lon = point2D.getX();
            geoPos.lat = point2D.getY();
            return geoPos;
        }

        @Override
        public Datum getDatum() {
            return Datum.WGS_84;
        }

        @Override
        public MathTransform getImageToMapTransform() {
            return new AffineTransform2D(affineTransform);
        }

        @Override
        public void dispose() {
        }
    }

}
