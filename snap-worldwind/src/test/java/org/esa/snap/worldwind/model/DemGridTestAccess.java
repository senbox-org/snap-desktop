package org.esa.snap.worldwind.model;

/**
 * Creates grids for tests outside the package.
 */
public final class DemGridTestAccess {

    private DemGridTestAccess() {
    }

    public static DemGrid create(double minLat, double maxLat, double minLon, double maxLon, int width, int height,
                                 float[] heights) {
        return new DemGrid(minLat, maxLat, minLon, maxLon, width, height, heights);
    }
}
