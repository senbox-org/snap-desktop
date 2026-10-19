package org.esa.snap.worldwind;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.LocalElevationModel;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import org.esa.snap.worldwind.model.DemGrid;
import org.esa.snap.worldwind.model.DemGridTestAccess;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    @Test
    public void testAddAndRemoveFromGlobe() {
        final Earth globe = new Earth();
        globe.setElevationModel(new ZeroElevationModel());
        final LocalElevationModel model = new LocalElevationModel();

        LocalDemTerrain.addTo(globe, model);
        assertTrue(globe.getElevationModel() instanceof CompoundElevationModel);
        final CompoundElevationModel compound = (CompoundElevationModel) globe.getElevationModel();
        assertTrue(compound.containsElevationModel(model));
        assertEquals("the globe's own elevations are kept", 2, compound.getElevationModels().size());

        LocalDemTerrain.addTo(globe, model);
        assertEquals("added once", 2, compound.getElevationModels().size());

        LocalDemTerrain.removeFrom(globe, model);
        assertFalse(compound.containsElevationModel(model));
    }
}
