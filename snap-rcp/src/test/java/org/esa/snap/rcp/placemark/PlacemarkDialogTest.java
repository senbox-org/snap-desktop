package org.esa.snap.rcp.placemark;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.awt.HeadlessException;

import static org.junit.Assert.*;

public class PlacemarkDialogTest {

    @Test
    public void test() {
        try {
            PlacemarkDialog pinDialog = new PlacemarkDialog(null, new Product("x", "y", 10, 10), PinDescriptor.getInstance(), false);

            pinDialog.setDescription("descrip");
            assertEquals("descrip", pinDialog.getDescription());

            pinDialog.setLat(3.6f);
            assertEquals(3.6f, pinDialog.getLat(), 1e-15);

            pinDialog.setLon(5.7f);
            assertEquals(5.7f, pinDialog.getLon(), 1e-15);

            GeoPos geoPos = pinDialog.getGeoPos();
            assertNotNull(geoPos);
            assertEquals(3.6f, geoPos.lat, 1e-6f);
            assertEquals(5.7f, geoPos.lon, 1e-6f);

            pinDialog.setName("name");
            assertEquals("name", pinDialog.getName());

            pinDialog.setLabel("label");
            assertEquals("label", pinDialog.getLabel());

            pinDialog.setPixelX(2.3F);
            assertEquals(2.3F, pinDialog.getPixelX(), 1e-6F);

            pinDialog.setPixelY(14.1F);
            assertEquals(14.1F, pinDialog.getPixelY(), 1e-6F);

            PixelPos pixelPos = pinDialog.getPixelPos();
            assertNotNull(pixelPos);
            assertEquals(2.3F, pixelPos.x, 1e-6F);
            assertEquals(14.1F, pixelPos.y, 1e-6F);

        } catch (HeadlessException e) {
            System.out.println("A " + PlacemarkDialogTest.class + " test has not been performed: HeadlessException");
        }
    }
} 
