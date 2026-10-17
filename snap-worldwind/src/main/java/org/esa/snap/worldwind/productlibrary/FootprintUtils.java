package org.esa.snap.worldwind.productlibrary;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Helpers for product footprints given as lon/lat paths, which may cross the antimeridian (e.g. lon 178 to -178).
 * Such a path is stored with a longitude jump of more than 180 degrees between consecutive vertices; treated as a
 * planar shape it would span the whole map. These helpers work on an "unwrapped" copy whose negative longitudes are
 * shifted by +360, so that the footprint is contiguous in the range [0, 360].
 */
public class FootprintUtils {

    private FootprintUtils() {
    }

    /**
     * @return true if two consecutive vertices of the path are more than 180 degrees of longitude apart
     */
    public static boolean crossesAntimeridian(Path2D path) {
        final double[] coords = new double[6];
        boolean hasPrevious = false;
        double previousLon = 0;
        for (PathIterator it = path.getPathIterator(null); !it.isDone(); it.next()) {
            final int type = it.currentSegment(coords);
            if (type == PathIterator.SEG_CLOSE) {
                continue;
            }
            if (hasPrevious && type != PathIterator.SEG_MOVETO && Math.abs(coords[0] - previousLon) > 180.0d) {
                return true;
            }
            previousLon = coords[0];
            hasPrevious = true;
        }
        return false;
    }

    /**
     * @return the path itself if it does not cross the antimeridian, otherwise a copy with negative longitudes
     * shifted by +360
     */
    public static Path2D.Double unwrap(Path2D.Double path) {
        if (!crossesAntimeridian(path)) {
            return path;
        }
        final Path2D.Double unwrapped = new Path2D.Double();
        final double[] coords = new double[6];
        for (PathIterator it = path.getPathIterator(null); !it.isDone(); it.next()) {
            final int type = it.currentSegment(coords);
            final double lon = coords[0] < 0 ? coords[0] + 360.0d : coords[0];
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    unwrapped.moveTo(lon, coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    unwrapped.lineTo(lon, coords[1]);
                    break;
                case PathIterator.SEG_CLOSE:
                    unwrapped.closePath();
                    break;
                default:
                    // footprints only contain straight segments
                    unwrapped.lineTo(lon, coords[1]);
                    break;
            }
        }
        return unwrapped;
    }

    /**
     * Point-in-footprint test that also works for footprints crossing the antimeridian.
     */
    public static boolean contains(Path2D.Double path, double longitude, double latitude) {
        if (!crossesAntimeridian(path)) {
            return path.contains(longitude, latitude);
        }
        final Path2D.Double unwrapped = unwrap(path);
        return unwrapped.contains(longitude, latitude) || unwrapped.contains(longitude + 360.0d, latitude);
    }

    /**
     * @return the centre of the bounding box of the footprint as (lon, lat), with the longitude in [-180, 180]
     */
    public static Point2D.Double getCentre(Path2D.Double path) {
        final Rectangle2D bounds = unwrap(path).getBounds2D();
        double lon = bounds.getCenterX();
        if (lon > 180.0d) {
            lon -= 360.0d;
        }
        return new Point2D.Double(lon, bounds.getCenterY());
    }

    /**
     * @return the shapes to draw for the footprint on a flat [-180, 180] map: the path itself, or for a footprint
     * crossing the antimeridian, its unwrapped copy plus the same copy shifted by -360, so that both halves show
     */
    public static Path2D.Double[] getDrawablePaths(Path2D.Double path) {
        if (!crossesAntimeridian(path)) {
            return new Path2D.Double[]{path};
        }
        final Path2D.Double east = unwrap(path);
        final Path2D.Double west = (Path2D.Double) east.clone();
        west.transform(AffineTransform.getTranslateInstance(-360.0d, 0.0d));
        return new Path2D.Double[]{east, west};
    }
}
