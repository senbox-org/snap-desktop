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
        } finally {
            notATiff.delete();
            folder.delete();
        }
    }
}
