package org.esa.snap.worldwind;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.LocalElevationModel;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import org.esa.snap.worldwind.model.DemGrid;
import org.esa.snap.worldwind.model.DemGridTestAccess;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LocalDemTerrainTest {

    private static double elevation(final LocalElevationModel model, final double lat, final double lon) {
        return model.getUnmappedElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon));
    }

    @Test
    public void testHeightsReachTheElevationModel() {
        // 3 x 2 grid over lon 10..12, lat 45..46: north row 100 200 300, south row 400 500 600
        final DemGrid grid = DemGridTestAccess.create(45, 46, 10, 12, 3, 2,
                                                      new float[]{100, 200, 300, 400, 500, 600});
        final LocalElevationModel model = new LocalElevationModel();
        LocalDemTerrain.addGrid(model, grid);

        // the first row is the north, the first column the west
        assertEquals(100, elevation(model, 46, 10), 1e-3);
        assertEquals(300, elevation(model, 46, 12), 1e-3);
        assertEquals(400, elevation(model, 45, 10), 1e-3);
        assertEquals(600, elevation(model, 45, 12), 1e-3);
        // bilinear between the samples
        assertEquals(300, elevation(model, 45.5, 10.5), 1e-3);
        assertTrue(model.contains(Angle.fromDegrees(45.5), Angle.fromDegrees(11)));
        assertFalse(model.contains(Angle.fromDegrees(47), Angle.fromDegrees(11)));
    }

    @Test
    public void testNoDataIsMissing() {
        final DemGrid grid = DemGridTestAccess.create(45, 46, 10, 12, 3, 2,
                                                      new float[]{100, 200, 300, 400, Float.NaN, 600});
        final LocalElevationModel model = new LocalElevationModel();
        LocalDemTerrain.addGrid(model, grid);

        // WorldWind treats a lookup touching a missing sample as missing, so the globe's own elevations show there
        assertEquals(model.getMissingDataSignal(), elevation(model, 45.2, 10.8), 0);
    }

    private static LocalElevationModel constant(final double minLat, final double maxLat, final double minLon,
                                                final double maxLon, final int size, final float height) {
        final float[] heights = new float[size * size];
        java.util.Arrays.fill(heights, height);
        final LocalElevationModel model = new LocalElevationModel();
        LocalDemTerrain.addGrid(model, DemGridTestAccess.create(minLat, maxLat, minLon, maxLon, size, size, heights));
        return model;
    }

    @Test
    public void testLocalDemTakesPrecedence() {
        // the globe's own elevations: finer than the local DEM, as WorldWind's 30 m elevations are
        final CompoundElevationModel own = new CompoundElevationModel();
        own.addElevationModel(constant(40, 50, 0, 20, 2001, 100));
        final Earth globe = new Earth();
        globe.setElevationModel(own);
        final LocalElevationModel local = constant(45, 46, 10, 11, 11, 700);

        LocalDemTerrain.addTo(globe, local);
        assertEquals("inside the local DEM", 700, globe.getElevation(Angle.fromDegrees(45.5), Angle.fromDegrees(10.5)), 1e-3);
        assertEquals("elsewhere the globe's own", 100, globe.getElevation(Angle.fromDegrees(48), Angle.fromDegrees(5)), 1e-3);

        LocalDemTerrain.addTo(globe, local);
        assertEquals("added once", 2, ((CompoundElevationModel) globe.getElevationModel()).getElevationModels().size());

        LocalDemTerrain.removeFrom(globe, local);
        assertSame("the original elevation model is restored", own, globe.getElevationModel());
        assertEquals(100, globe.getElevation(Angle.fromDegrees(45.5), Angle.fromDegrees(10.5)), 1e-3);
    }

    @Test
    public void testAddToGlobeWithoutCompoundModel() {
        final Earth globe = new Earth();
        final ZeroElevationModel zero = new ZeroElevationModel();
        globe.setElevationModel(zero);
        final LocalElevationModel local = constant(45, 46, 10, 11, 11, 700);

        LocalDemTerrain.addTo(globe, local);
        assertTrue(globe.getElevationModel() instanceof CompoundElevationModel);
        assertEquals(700, globe.getElevation(Angle.fromDegrees(45.5), Angle.fromDegrees(10.5)), 1e-3);

        LocalDemTerrain.removeFrom(globe, local);
        assertSame(zero, globe.getElevationModel());
        assertFalse(globe.getElevationModel() instanceof CompoundElevationModel);
    }

    @Test
    public void testTessellationUsesTheLocalDem() {
        // the globe's surface is built from getElevations, not from point lookups
        final CompoundElevationModel own = new CompoundElevationModel();
        own.addElevationModel(constant(40, 50, 0, 20, 2001, 100));
        final Earth globe = new Earth();
        globe.setElevationModel(own);
        LocalDemTerrain.addTo(globe, constant(45, 46, 10, 11, 11, 700));

        final Sector sector = Sector.fromDegrees(45.2, 45.8, 9.0, 10.8);
        final List<LatLon> locations = Arrays.asList(LatLon.fromDegrees(45.5, 10.5), LatLon.fromDegrees(45.5, 9.5));
        final double[] heights = new double[2];
        globe.getElevationModel().getElevations(sector, locations, 1.0e-6, heights);
        assertEquals("inside the local DEM", 700, heights[0], 1e-3);
        assertEquals("outside it", 100, heights[1], 1e-3);
    }

    @Test
    public void testInterruptedLoadGivesNothing() throws Exception {
        final java.io.File folder = java.nio.file.Files.createTempDirectory("dem").toFile();
        try {
            assertTrue(new java.io.File(folder, "a.tif").createNewFile());
            Thread.currentThread().interrupt();
            assertNull(LocalDemTerrain.load(folder));
        } finally {
            Thread.interrupted();
            new java.io.File(folder, "a.tif").delete();
            folder.delete();
        }
    }

    @Test
    public void testUnusableFilesGiveNoTerrain() throws Exception {
        final java.io.File folder = java.nio.file.Files.createTempDirectory("dem").toFile();
        final java.io.File notATiff = new java.io.File(folder, "readme.tif");
        try {
            java.nio.file.Files.write(notATiff.toPath(), "not a GeoTIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            assertNull(LocalDemTerrain.load(folder));
            assertTrue("no reader keeps the file open", notATiff.delete());
        } finally {
            notATiff.delete();
            folder.delete();
        }
    }

    /**
     * Writes a 20 x 20 GeoTIFF DEM of 0.05 degree pixels from the given north-west corner, of constant height.
     */
    private static java.io.File writeDem(final java.io.File folder, final String name, final double west,
                                         final double north, final float height) throws Exception {
        final org.esa.snap.core.datamodel.Product product = new org.esa.snap.core.datamodel.Product(name, "DEM", 20, 20);
        product.setSceneGeoCoding(new org.esa.snap.core.datamodel.CrsGeoCoding(
                org.geotools.referencing.crs.DefaultGeographicCRS.WGS84, 20, 20, west, north, 0.05, 0.05, 0.0, 0.0));
        final org.esa.snap.core.datamodel.Band band =
                product.addBand("height", org.esa.snap.core.datamodel.ProductData.TYPE_FLOAT32);
        final float[] heights = new float[400];
        Arrays.fill(heights, height);
        band.setRasterData(org.esa.snap.core.datamodel.ProductData.createInstance(heights));
        final java.io.File file = new java.io.File(folder, name + ".tif");
        org.esa.snap.core.dataio.ProductIO.writeProduct(product, file, "GeoTIFF", false);
        product.dispose();
        return file;
    }

    private static void deleteAll(final java.io.File folder) {
        final java.io.File[] files = folder.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                file.delete();
            }
        }
        folder.delete();
    }

    @Test
    public void testGeoTiffsBecomeTerrainAndAreClosed() throws Exception {
        final java.io.File folder = java.nio.file.Files.createTempDirectory("dem").toFile();
        try {
            final java.io.File a = writeDem(folder, "a", 10.0, 46.0, 500f);
            final java.io.File b = writeDem(folder, "b", 20.0, 46.0, 900f);

            final LocalElevationModel model = LocalDemTerrain.load(folder);

            assertEquals(500, elevation(model, 45.5, 10.5), 1e-3);
            assertEquals(900, elevation(model, 45.5, 20.5), 1e-3);
            // both reads of each file closed it (Windows keeps open files locked)
            assertTrue(a.delete());
            assertTrue(b.delete());
        } finally {
            deleteAll(folder);
        }
    }

    @Test
    public void testFilesNearestTheViewAreUsed() {
        final List<java.io.File> files = Arrays.asList(new java.io.File("far"), new java.io.File("near"),
                                                       new java.io.File("across"));
        final List<double[]> bounds = Arrays.asList(new double[]{0, 1, 0, 1}, new double[]{45, 46, 10, 11},
                                                    new double[]{45, 46, -180, -179});
        final List<java.io.File> nearest = LocalDemTerrain.nearest(files, bounds, LatLon.fromDegrees(45.5, 12), 1);
        assertEquals(Arrays.asList(new java.io.File("near")), nearest);
        // across the antimeridian: -179.5 is 1 degree from 179.5
        assertEquals(Arrays.asList(new java.io.File("across")),
                     LocalDemTerrain.nearest(files, bounds, LatLon.fromDegrees(45.5, 179.5), 1));
        // few enough files: all of them
        assertEquals(files, LocalDemTerrain.nearest(files, bounds, null, 3));
    }

    @Test
    public void testSamplesPerFileWithinTheMemoryBudget() {
        assertEquals(LocalDemTerrain.MAX_SAMPLES, LocalDemTerrain.samplesPerFile(1));
        assertEquals(LocalDemTerrain.MAX_SAMPLES, LocalDemTerrain.samplesPerFile(32));
        assertEquals(LocalDemTerrain.MIN_SAMPLES, LocalDemTerrain.samplesPerFile(LocalDemTerrain.MAX_FILES));
        for (int files : new int[]{1, 33, 100, LocalDemTerrain.MAX_FILES}) {
            final long samples = LocalDemTerrain.samplesPerFile(files);
            assertTrue(files + " files", files * samples * samples * Float.BYTES <= LocalDemTerrain.MAX_BYTES);
        }
    }

    @Test
    public void testFolderWithoutDemsIsReported() throws Exception {
        final java.io.File folder = java.nio.file.Files.createTempDirectory("dem").toFile();
        final java.util.prefs.Preferences preferences = org.esa.snap.runtime.Config.instance().preferences();
        final String enabled = preferences.get(org.esa.snap.worldwind.preferences.WorldWindPreferences.LOCAL_DEM, null);
        final String dir = preferences.get(org.esa.snap.worldwind.preferences.WorldWindPreferences.LOCAL_DEM_DIR, null);
        final java.util.function.Consumer<String> original = LocalDemTerrain.noTerrainMessage;
        final java.util.concurrent.BlockingQueue<String> messages = new java.util.concurrent.LinkedBlockingQueue<>();
        LocalDemTerrain.noTerrainMessage = messages::add;
        try {
            preferences.putBoolean(org.esa.snap.worldwind.preferences.WorldWindPreferences.LOCAL_DEM, true);
            preferences.put(org.esa.snap.worldwind.preferences.WorldWindPreferences.LOCAL_DEM_DIR, folder.getPath());
            final Earth globe = new Earth();
            javax.swing.SwingUtilities.invokeAndWait(() -> LocalDemTerrain.attach(globe,
                    org.mockito.Mockito.mock(gov.nasa.worldwind.WorldWindow.class)));

            final String message = messages.poll(30, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(message, message != null && message.contains(folder.getPath()));
            // the globe keeps its own elevations
            assertFalse(globe.getElevationModel() instanceof LocalDemTerrain.LocalFirstElevationModel);
        } finally {
            LocalDemTerrain.noTerrainMessage = original;
            restore(preferences, org.esa.snap.worldwind.preferences.WorldWindPreferences.LOCAL_DEM, enabled);
            restore(preferences, org.esa.snap.worldwind.preferences.WorldWindPreferences.LOCAL_DEM_DIR, dir);
            javax.swing.SwingUtilities.invokeAndWait(LocalDemTerrain::reload);
            deleteAll(folder);
        }
    }

    private static void restore(final java.util.prefs.Preferences preferences, final String key, final String value) {
        if (value == null) {
            preferences.remove(key);
        } else {
            preferences.put(key, value);
        }
    }
}
