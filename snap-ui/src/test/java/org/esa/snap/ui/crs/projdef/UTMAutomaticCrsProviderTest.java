package org.esa.snap.ui.crs.projdef;

import org.esa.snap.core.datamodel.GeoPos;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
public class UTMAutomaticCrsProviderTest {
    @Test
    public void getCRS() throws Exception {
        CoordinateReferenceSystem crs;
        UTMAutomaticCrsProvider autoUtmCrsProvider = new UTMAutomaticCrsProvider(DefaultGeodeticDatum.WGS84);

        // The string is different to the one in the UI, because we use DefaultGeodeticDatum.WGS84, but in the UI we
        // use the datum from the database.
        crs = autoUtmCrsProvider.getCRS(new GeoPos(31.0, 33.0), null, DefaultGeodeticDatum.WGS84);
        assertEquals("UTM Zone 36 / WGS84", crs.getName().getCode());

        crs = autoUtmCrsProvider.getCRS(new GeoPos(-31.0, 33.0), null, DefaultGeodeticDatum.WGS84);
        assertEquals("UTM Zone 36, South / WGS84", crs.getName().getCode());

        crs = autoUtmCrsProvider.getCRS(new GeoPos(54.0, 10.0), null, DefaultGeodeticDatum.WGS84);
        assertEquals("UTM Zone 32 / WGS84", crs.getName().getCode());

        crs = autoUtmCrsProvider.getCRS(new GeoPos(36.0, -100.0), null, DefaultGeodeticDatum.WGS84);
        assertEquals("UTM Zone 14 / WGS84", crs.getName().getCode());
    }

}