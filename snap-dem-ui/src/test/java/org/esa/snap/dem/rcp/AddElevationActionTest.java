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
import org.esa.snap.runtime.Config;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import javax.media.jai.TileCache;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


public class AddElevationActionTest {


    private static final String ADD_ELEVATION_TILE_SIZE_KEY = "snap.dem.addElevationTileSize";
    private static final String MAX_DEGREES_PER_ELEVATION_OVERVIEW_TILE_KEY = "snap.dem.maxDegreesPerElevationOverviewTile";
    private static final String ELEVATION_BAND_TILE_CACHE_SIZE_KEY = "snap.dem.elevationBandTileCacheSizeBytes";


    @Test
    @STTM("SNAP-4213")
    public void elevationSourceImageComputesTilesInsteadOfSamples() throws Exception {
        Product product = createProduct(16, 12);

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
        Product product = createProduct(1024, 768);

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

    @Test
    @STTM("SNAP-4213")
    public void elevationSourceImageComputesHigherLevelsInConfiguredChunks() throws Exception {
        Preferences preferences = Config.instance().preferences();
        String previousTileSize = preferences.get(ADD_ELEVATION_TILE_SIZE_KEY, null);
        String previousMaxDegrees = preferences.get(MAX_DEGREES_PER_ELEVATION_OVERVIEW_TILE_KEY, null);
        try {
            preferences.putInt(ADD_ELEVATION_TILE_SIZE_KEY, 4);
            preferences.putDouble(MAX_DEGREES_PER_ELEVATION_OVERVIEW_TILE_KEY, 24.0);

            Product product = createProduct(1024, 1024);
            RecordingElevationModel dem = new RecordingElevationModel(-999.0f);
            addElevationBand(product, dem, "elevation");

            Band elevationBand = product.getBand("elevation");
            RenderedImage levelImage = elevationBand.getSourceImage().getImage(1);

            levelImage.getData(new Rectangle(0, 0, 4, 4));

            TileGeoreferencing tileGeoRef = new TileGeoreferencing(product.getSceneGeoCoding(), 0, 0, 8, 8);
            assertGeoPosEquals(geoPosFor(tileGeoRef, 0, 0), dem.requestedGeoPositions.get(0));
            assertGeoPosEquals(geoPosFor(tileGeoRef, 2, 0), dem.requestedGeoPositions.get(1));
            assertGeoPosEquals(geoPosFor(tileGeoRef, 0, 2), dem.requestedGeoPositions.get(2));
            assertGeoPosEquals(geoPosFor(tileGeoRef, 2, 2), dem.requestedGeoPositions.get(3));
        } finally {
            restorePreference(preferences, ADD_ELEVATION_TILE_SIZE_KEY, previousTileSize);
            restorePreference(preferences, MAX_DEGREES_PER_ELEVATION_OVERVIEW_TILE_KEY, previousMaxDegrees);
        }
    }

    @Test
    @STTM("SNAP-4213")
    public void elevationSourceImagesUseConfiguredPrivateSharedTileCache() throws Exception {
        Preferences preferences = Config.instance().preferences();
        String previousCacheSize = preferences.get(ELEVATION_BAND_TILE_CACHE_SIZE_KEY, null);
        try {
            long cacheSize = 1_234_567L;
            preferences.putLong(ELEVATION_BAND_TILE_CACHE_SIZE_KEY, cacheSize);

            Product product = createProduct(1024, 1024);
            addElevationBand(product, new FakeElevationModel(-999.0f), "elevation");

            Band elevationBand = product.getBand("elevation");
            RasterDataNodeOpImage levelZeroImage = (RasterDataNodeOpImage) elevationBand.getSourceImage().getImage(0);
            RasterDataNodeOpImage overviewImage = (RasterDataNodeOpImage) elevationBand.getSourceImage().getImage(1);

            TileCache tileCache = levelZeroImage.getTileCache();
            assertNotNull(tileCache);
            assertEquals(cacheSize, tileCache.getMemoryCapacity());
            assertSame(tileCache, overviewImage.getTileCache());
        } finally {
            restorePreference(preferences, ELEVATION_BAND_TILE_CACHE_SIZE_KEY, previousCacheSize);
        }
    }

    @Test
    @STTM("SNAP-4213")
    public void elevationSourceImageReusesCachedTilesForRepeatedReads() throws Exception {
        Product product = createProduct(16, 16);
        product.setPreferredTileSize(new Dimension(4, 4));
        CountingElevationModel dem = new CountingElevationModel(-999.0f);
        addElevationBand(product, dem, "elevation");

        RenderedImage levelImage = product.getBand("elevation").getSourceImage().getImage(0);

        levelImage.getTile(levelImage.getMinTileX(), levelImage.getMinTileY());
        int callCountAfterFirstRead = dem.getElevationCallCount();

        levelImage.getTile(levelImage.getMinTileX(), levelImage.getMinTileY());

        assertTrue(callCountAfterFirstRead > 0);
        assertEquals(callCountAfterFirstRead, dem.getElevationCallCount());
    }

    private static Product createProduct(int width, int height) throws Exception {
        Product product = new Product("test", "type", width, height);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                   product.getSceneRasterWidth(),
                                                   product.getSceneRasterHeight(),
                                                   10.0, 50.0,
                                                   0.1, -0.1,
                                                   0.0, 0.0));
        return product;
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

    private static GeoPos geoPosFor(TileGeoreferencing tileGeoRef, int x, int y) {
        GeoPos geoPos = new GeoPos();
        tileGeoRef.getGeoPos(x, y, geoPos);
        return geoPos;
    }

    private static void assertGeoPosEquals(GeoPos expected, GeoPos actual) {
        assertEquals(expected.lat, actual.lat, 1.0e-6);
        assertEquals(expected.lon, actual.lon, 1.0e-6);
    }

    private static void restorePreference(Preferences preferences, String key, String value) {
        if (value == null) {
            preferences.remove(key);
        } else {
            preferences.put(key, value);
        }
    }

    private static class FakeElevationModel implements ElevationModel {
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

    private static final class RecordingElevationModel extends FakeElevationModel {
        private final List<GeoPos> requestedGeoPositions = new ArrayList<>();

        private RecordingElevationModel(float noDataValue) {
            super(noDataValue);
        }

        @Override
        public double getElevation(GeoPos geoPos) {
            requestedGeoPositions.add(new GeoPos(geoPos.lat, geoPos.lon));
            return super.getElevation(geoPos);
        }
    }

    private static final class CountingElevationModel extends FakeElevationModel {
        private final AtomicInteger elevationCallCount = new AtomicInteger();

        private CountingElevationModel(float noDataValue) {
            super(noDataValue);
        }

        @Override
        public double getElevation(GeoPos geoPos) {
            elevationCallCount.incrementAndGet();
            return super.getElevation(geoPos);
        }

        private int getElevationCallCount() {
            return elevationCallCount.get();
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
