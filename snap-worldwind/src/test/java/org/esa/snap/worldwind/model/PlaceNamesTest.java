package org.esa.snap.worldwind.model;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaceNamesTest {

    private static List<PlaceNames.Place> read(final String text) throws IOException {
        return PlaceNames.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static final String PLACES = "# comment\n" +
            "Big\t10.0\t20.0\t1.7\t1\t1000000\n" +
            "Small\t11.0\t21.0\t6.0\t0\t5000\n" +
            "Fiji\t-18.0\t179.5\t3.0\t1\t80000\n" +
            "Samoa\t-14.0\t-171.0\t3.0\t1\t40000\n";

    @Test
    public void testRead() throws IOException {
        final List<PlaceNames.Place> places = read(PLACES);
        assertEquals(4, places.size());
        final PlaceNames.Place big = places.get(0);
        assertEquals("Big", big.name);
        assertEquals(10.0, big.lat, 0.0);
        assertEquals(20.0, big.lon, 0.0);
        assertTrue(big.capital);
        assertEquals(1000000L, big.population);
        assertFalse(places.get(1).capital);
    }

    @Test(expected = IOException.class)
    public void testReadInvalid() throws IOException {
        read("Broken\tnot-a-number\t1\t1\t0\t0\n");
    }

    @Test
    public void testMaxAltitudeHalvesPerZoomLevel() {
        assertEquals(PlaceNames.ZOOM_ZERO_ALTITUDE, PlaceNames.maxAltitude(0), 1e-6);
        assertEquals(PlaceNames.maxAltitude(3) / 2, PlaceNames.maxAltitude(4), 1e-6);
    }

    @Test
    public void testSelectByAltitude() throws IOException {
        final List<PlaceNames.Place> places = read(PLACES);
        // high above: only the place with the lowest zoom level
        List<PlaceNames.Place> selected = PlaceNames.select(places, 2.0e7, -90, 90, -180, 180);
        assertEquals(1, selected.size());
        assertEquals("Big", selected.get(0).name);
        // close to the ground: all, in the given order
        selected = PlaceNames.select(places, 1000, -90, 90, -180, 180);
        assertEquals(4, selected.size());
        assertEquals("Big", selected.get(0).name);
    }

    @Test
    public void testSelectByBox() throws IOException {
        final List<PlaceNames.Place> places = read(PLACES);
        final List<PlaceNames.Place> selected = PlaceNames.select(places, 1000, 10.5, 12, 0, 30);
        assertEquals(1, selected.size());
        assertEquals("Small", selected.get(0).name);
    }

    @Test
    public void testSelectAcrossAntimeridian() throws IOException {
        final List<PlaceNames.Place> places = read(PLACES);
        final List<PlaceNames.Place> selected = PlaceNames.select(places, 1000, -30, 0, 170, -165);
        assertEquals(2, selected.size());
        assertEquals("Fiji", selected.get(0).name);
        assertEquals("Samoa", selected.get(1).name);
    }

    @Test
    public void testBundledPlaces() throws IOException {
        final List<PlaceNames.Place> places = PlaceNames.getBundledPlaces();
        assertEquals(1251, places.size());
        // most important first
        assertTrue(places.get(0).minZoom <= places.get(places.size() - 1).minZoom);
        boolean hasCapital = false;
        for (PlaceNames.Place place : places) {
            assertTrue(place.name, place.lat >= -90 && place.lat <= 90 && place.lon >= -180 && place.lon <= 180);
            hasCapital |= place.capital;
        }
        assertTrue(hasCapital);
        // UTF-8 names survive
        boolean found = false;
        for (PlaceNames.Place place : places) {
            found |= place.name.equals("São Paulo");
        }
        assertTrue(found);
    }
}
