package org.esa.snap.ui.util;

import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.GeoCoding;

import java.util.ArrayList;

public class GeoCodingUtil {

    public static String[] getTiePointGridsFromGeoCoding(GeoCoding geoCoding) {

        ArrayList<String> geoCodingTiePointGridNames = new ArrayList<>();

        if (geoCoding instanceof ComponentGeoCoding) {

            ComponentGeoCoding componentGeoCoding = (ComponentGeoCoding) geoCoding;
            GeoRaster geoRaster = componentGeoCoding.getGeoRaster();
            boolean usesTiePointsInX = geoRaster.getSubsamplingX() > 1;
            boolean usesTiePointsInY = geoRaster.getSubsamplingY() > 1;

            if (usesTiePointsInX) {
                String lonName = geoRaster.getLonVariableName();
                geoCodingTiePointGridNames.add(lonName);
            }

            if (usesTiePointsInY) {
                String latName = geoRaster.getLatVariableName();
                geoCodingTiePointGridNames.add(latName);
            }
        }
        return geoCodingTiePointGridNames.toArray(new String[0]);
    }
}
