package org.esa.snap.ui.util;

import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.GeoCoding;

public class GeoCodingUtil {

    public static boolean geoCodingUsesTiePointGrid(GeoCoding geoCoding, String productNodeName) {
        if (geoCoding instanceof ComponentGeoCoding) {

            ComponentGeoCoding componentGeoCoding = (ComponentGeoCoding) geoCoding;
            GeoRaster geoRaster = componentGeoCoding.getGeoRaster();
            boolean usesTiePointsInX = geoRaster.getSubsamplingX() > 1;
            boolean usesTiePointsInY = geoRaster.getSubsamplingY() > 1;

            String lonName = geoRaster.getLonVariableName();
            String latName = geoRaster.getLatVariableName();

            if ((usesTiePointsInX && productNodeName.equals(lonName)) ||
                    (usesTiePointsInY && productNodeName.equals(latName))) {
                return true;
            }
        }
        return false;
    }
}
