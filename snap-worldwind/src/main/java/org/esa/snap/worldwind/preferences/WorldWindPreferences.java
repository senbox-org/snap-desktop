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
package org.esa.snap.worldwind.preferences;

import org.esa.snap.runtime.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * The WorldWind preferences of SNAP.
 */
public final class WorldWindPreferences {

    public static final String WMS_SERVERS = "snap.worldwind.wmsServers";
    public static final String LAST_DATA_DIR = "snap.worldwind.lastDataDir";

    /**
     * WMS servers offered by the "Add WMS Layer" dialog, checked 2026-10-19.
     */
    static final String DEFAULT_WMS_SERVERS = String.join("\n",
            "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi",
            "https://tiles.maps.eox.at/wms");

    private WorldWindPreferences() {
    }

    static Preferences preferences() {
        return Config.instance().preferences();
    }

    public static List<String> getWmsServers() {
        return parseLines(preferences().get(WMS_SERVERS, DEFAULT_WMS_SERVERS));
    }

    public static void setWmsServers(final List<String> servers) {
        preferences().put(WMS_SERVERS, String.join("\n", servers));
    }

    /**
     * Non-empty, trimmed lines of a text, without duplicates.
     */
    static List<String> parseLines(final String text) {
        final List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        for (String line : text.split("\\R")) {
            final String trimmed = line.trim();
            if (!trimmed.isEmpty() && !lines.contains(trimmed)) {
                lines.add(trimmed);
            }
        }
        return lines;
    }
}
