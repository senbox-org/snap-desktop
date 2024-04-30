package org.esa.snap.worldwind.productlibrary;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Polyline;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * The class stores the coordinate of a polyline.
 *
 * Created by jcoravu on 11/9/2019.
 */
public class CustomPolyline extends Polyline {

    private final Path2D.Double path;

    public CustomPolyline(Path2D.Double path) {
        this.path = path;

        List<Position> positions = new ArrayList<>();

        double[] coordinates = new double[2];

        PathIterator pathIterator = this.path.getPathIterator(null);

        pathIterator.currentSegment(coordinates);
        Position firstPosition = new Position(Angle.fromDegreesLatitude(coordinates[1]), Angle.fromDegreesLongitude(coordinates[0]), 0.0d);
        positions.add(firstPosition);
        pathIterator.next();

        while (!pathIterator.isDone()) {
            pathIterator.currentSegment(coordinates);
            Position position = new Position(Angle.fromDegreesLatitude(coordinates[1]), Angle.fromDegreesLongitude(coordinates[0]), 0.0d);
            positions.add(position);
            pathIterator.next();
        }

        setPositions(positions);
    }

    public Path2D.Double getPath() {
        return path;
    }
}
