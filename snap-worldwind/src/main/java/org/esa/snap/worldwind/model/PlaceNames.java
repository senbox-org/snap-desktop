/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.worldwind.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Offline place names: the Natural Earth 1:50m populated places bundled with the module, and the choice of the
 * places to label for an eye altitude.
 * <p>
 * Each place has the web-map zoom level from which Natural Earth recommends labelling it ({@code min_zoom}); it is
 * labelled below the eye altitude that shows about as much of the Earth as that zoom level.
 */
public final class PlaceNames {

    static final String RESOURCE = "/org/esa/snap/worldwind/naturalearth/ne_50m_populated_places.txt";

    /**
     * Eye altitude in metres at which places of zoom level 0 are labelled; halved for each further zoom level.
     */
    static final double ZOOM_ZERO_ALTITUDE = 1.0e8;

    public static final class Place {
        public final String name;
        public final double lat;
        public final double lon;
        public final double minZoom;
        public final boolean capital;
        public final long population;

        Place(String name, double lat, double lon, double minZoom, boolean capital, long population) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.minZoom = minZoom;
            this.capital = capital;
            this.population = population;
        }

        /**
         * The highest eye altitude in metres at which the place is labelled.
         */
        public double getMaxAltitude() {
            return maxAltitude(minZoom);
        }
    }

    private static List<Place> bundled;

    private PlaceNames() {
    }

    public static double maxAltitude(final double minZoom) {
        return ZOOM_ZERO_ALTITUDE / Math.pow(2.0, minZoom);
    }

    /**
     * The bundled places, most important first.
     */
    public static synchronized List<Place> getBundledPlaces() throws IOException {
        if (bundled == null) {
            try (InputStream in = PlaceNames.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    throw new IOException("Place names " + RESOURCE + " not found");
                }
                bundled = Collections.unmodifiableList(read(in));
            }
        }
        return bundled;
    }

    /**
     * Reads places from tab-separated lines: name, latitude, longitude, min_zoom, adm0cap (0/1), pop_max. Lines
     * starting with '#' are comments.
     */
    static List<Place> read(final InputStream in) throws IOException {
        final List<Place> places = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            final String[] fields = line.split("\t");
            if (fields.length < 6) {
                throw new IOException("Invalid place name line " + lineNumber + ": " + line);
            }
            try {
                places.add(new Place(fields[0], Double.parseDouble(fields[1]), Double.parseDouble(fields[2]),
                                     Double.parseDouble(fields[3]), "1".equals(fields[4]), Long.parseLong(fields[5])));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid place name line " + lineNumber + ": " + line, e);
            }
        }
        return places;
    }

    /**
     * The places to label at an eye altitude inside a lat/lon box, keeping the order of the given places. The box may
     * cross the antimeridian (minLon greater than maxLon).
     */
    public static List<Place> select(final List<Place> places, final double eyeAltitude,
                                     final double minLat, final double maxLat, final double minLon, final double maxLon) {
        final List<Place> selected = new ArrayList<>();
        final boolean crossesAntimeridian = minLon > maxLon;
        for (Place place : places) {
            if (eyeAltitude > place.getMaxAltitude() || place.lat < minLat || place.lat > maxLat) {
                continue;
            }
            final boolean inLon = crossesAntimeridian ? (place.lon >= minLon || place.lon <= maxLon)
                                                      : (place.lon >= minLon && place.lon <= maxLon);
            if (inLon) {
                selected.add(place);
            }
        }
        return selected;
    }
}
