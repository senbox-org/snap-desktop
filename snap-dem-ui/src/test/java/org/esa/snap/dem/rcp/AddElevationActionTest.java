package org.esa.snap.dem.rcp;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.image.RasterDataNodeOpImage;
import org.esa.snap.core.image.RasterDataNodeSampleOpImage;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class AddElevationActionTest {


    @Test
    @STTM("SNAP-4213")
    public void elevationSourceImageComputesTilesInsteadOfSamples() throws Exception {
        Product product = new Product("test", "type", 16, 12);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                   product.getSceneRasterWidth(),
                                                   product.getSceneRasterHeight(),
                                                   10.0, 50.0,
                                                   0.1, -0.1,
                                                   0.0, 0.0));

        addElevationBand(product, new FakeElevationModel(-999.0f), "elevation");

        Band elevationBand = product.getBand("elevation");
        assertNotNull(elevationBand);
        RenderedImage levelImage = elevationBand.getSourceImage().getImage(0);

        assertTrue(levelImage instanceof RasterDataNodeOpImage);
        assertFalse(levelImage instanceof RasterDataNodeSampleOpImage);

        Rectangle rectangle = new Rectangle(2, 3, 4, 2);
        TileGeoreferencing tileGeoRef = new TileGeoreferencing(product.getSceneGeoCoding(), rectangle.x, rectangle.y,
                                                               rectangle.width, rectangle.height);
        Raster raster = levelImage.getData(rectangle);
        GeoPos expectedGeoPos = new GeoPos();
        tileGeoRef.getGeoPos(2, 3, expectedGeoPos);
        assertEquals(elevationFor(expectedGeoPos),
                     raster.getSampleDouble(2, 3, 0), 1.0e-4);
    }

    @Test
    @STTM("SNAP-4213")
    public void elevationSourceImageComputesHigherLevelsFromTileGeoreferencing() throws Exception {
        Product product = new Product("test", "type", 1024, 768);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                   product.getSceneRasterWidth(),
                                                   product.getSceneRasterHeight(),
                                                   10.0, 50.0,
                                                   0.1, -0.1,
                                                   0.0, 0.0));

        addElevationBand(product, new FakeElevationModel(-999.0f), "elevation");

        Band elevationBand = product.getBand("elevation");
        RenderedImage levelImage = elevationBand.getSourceImage().getImage(1);

        Rectangle rectangle = new Rectangle(1, 1, 3, 2);
        Raster raster = levelImage.getData(rectangle);

        TileGeoreferencing tileGeoRef = new TileGeoreferencing(product.getSceneGeoCoding(), 2, 2, 6, 4);
        GeoPos expectedGeoPos = new GeoPos();
        tileGeoRef.getGeoPos(2, 2, expectedGeoPos);
        assertEquals(elevationFor(expectedGeoPos),
                     raster.getSampleDouble(1, 1, 0), 1.0e-4);
    }

    private static void addElevationBand(Product product, ElevationModel dem, String elevationBandName) throws Exception {
        Method method = AddElevationAction.class.getDeclaredMethod("addElevationBand", Product.class,
                                                                   ElevationModel.class, String.class);
        method.setAccessible(true);
        method.invoke(null, product, dem, elevationBandName);
    }

    private static double elevationFor(GeoPos geoPos) {
        return geoPos.lat * 10.0 + geoPos.lon;
    }

    private static final class FakeElevationModel implements ElevationModel {
        private final ElevationModelDescriptor descriptor;

        private FakeElevationModel(float noDataValue) {
            descriptor = new FakeDescriptor(noDataValue);
        }

        @Override
        public ElevationModelDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public double getElevation(GeoPos geoPos) {
            return elevationFor(geoPos);
        }

        @Override
        public PixelPos getIndex(GeoPos geoPos) {
            return new PixelPos((float) geoPos.lon, (float) geoPos.lat);
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos) {
            return new GeoPos(pixelPos.y, pixelPos.x);
        }

        @Override
        public double getSample(double x, double y) {
            return y * 10.0 + x;
        }

        @Override
        public boolean getSamples(int[] x, int[] y, double[][] samples) {
            for (int row = 0; row < y.length; row++) {
                for (int col = 0; col < x.length; col++) {
                    samples[row][col] = getSample(x[col], y[row]);
                }
            }
            return true;
        }

        @Override
        public Resampling getResampling() {
            return Resampling.NEAREST_NEIGHBOUR;
        }

        @Override
        public void dispose() {
        }
    }

    private static final class FakeDescriptor implements ElevationModelDescriptor {
        private final float noDataValue;

        private FakeDescriptor(float noDataValue) {
            this.noDataValue = noDataValue;
        }

        @Override
        public String getName() {
            return "Fake";
        }

        @Override
        public float getNoDataValue() {
            return noDataValue;
        }

        @Override
        public int getRasterWidth() {
            return 360;
        }

        @Override
        public int getRasterHeight() {
            return 180;
        }

        @Override
        public int getTileWidthInDegrees() {
            return 1;
        }

        @Override
        public int getTileWidth() {
            return 1;
        }

        @Override
        public int getNumXTiles() {
            return 360;
        }

        @Override
        public int getNumYTiles() {
            return 180;
        }

        @Override
        public ElevationModel createDem(Resampling resampling) {
            return null;
        }

        @Override
        public boolean canBeDownloaded() {
            return false;
        }

        @Override
        public File getDemInstallDir() {
            return new File(".");
        }
    }
}
