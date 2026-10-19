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

import gov.nasa.worldwind.WorldWind;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * The WorldWind preferences of SNAP.
 */
public final class WorldWindPreferences {

    public static final String WMS_SERVERS = "snap.worldwind.wmsServers";
    public static final String LAST_DATA_DIR = "snap.worldwind.lastDataDir";
    public static final String OFFLINE = "snap.worldwind.offline";
    public static final String CACHE_DIR = "snap.worldwind.cacheDir";
    public static final String LOCAL_DEM = "snap.worldwind.localDem";
    public static final String LOCAL_DEM_DIR = "snap.worldwind.localDemDir";

    /**
     * System property that makes WorldWind keep its tile cache in {@code <value>/WorldWindData}; read once, when
     * WorldWind creates its file store.
     */
    static final String WORLDWIND_STORE_PROPERTY = "gov.nasa.worldwind.platform.alluser.store";
    /**
     * Name of the folder WorldWind creates in the cache location.
     */
    public static final String WORLDWIND_DATA_FOLDER = "WorldWindData";

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

    /**
     * Stores the WMS servers; the default list is not stored, so that later changes of the default reach the user.
     */
    public static void setWmsServers(final List<String> servers) {
        if (servers.equals(parseLines(DEFAULT_WMS_SERVERS))) {
            preferences().remove(WMS_SERVERS);
        } else {
            preferences().put(WMS_SERVERS, String.join("\n", servers));
        }
    }

    /**
     * Stores the DEM folder; SNAP's DEM folder is not stored, so that it follows a moved auxdata folder.
     */
    public static void setLocalDemDir(final String dir) {
        final String trimmed = dir == null ? "" : dir.trim();
        if (trimmed.isEmpty() || new File(trimmed).equals(defaultLocalDemDir())) {
            preferences().remove(LOCAL_DEM_DIR);
        } else {
            preferences().put(LOCAL_DEM_DIR, trimmed);
        }
    }

    static File defaultLocalDemDir() {
        return SystemUtils.getAuxDataPath().resolve("dem").toFile();
    }

    /**
     * True if the tile cache folder can be used: empty (WorldWind's default), or a folder that exists and is writable,
     * or can be created in a writable folder.
     */
    public static boolean isUsableCacheDir(final String dir) {
        if (dir == null || dir.trim().isEmpty()) {
            return true;
        }
        File folder = new File(dir.trim()).getAbsoluteFile();
        if (folder.exists()) {
            return folder.isDirectory() && folder.canWrite();
        }
        while (folder != null && !folder.exists()) {
            folder = folder.getParentFile();
        }
        return folder != null && folder.isDirectory() && folder.canWrite();
    }

    public static boolean isOffline() {
        return preferences().getBoolean(OFFLINE, false);
    }

    /**
     * The folder chosen for WorldWind's tile cache, or null for WorldWind's default location.
     */
    public static File getCacheDir() {
        final String dir = preferences().get(CACHE_DIR, "").trim();
        return dir.isEmpty() ? null : new File(dir);
    }

    public static boolean isLocalDemEnabled() {
        return preferences().getBoolean(LOCAL_DEM, false);
    }

    /**
     * The folder searched for DEM GeoTIFFs; by default SNAP's DEM folder, where SNAP keeps the tiles it downloads.
     */
    public static File getLocalDemDir() {
        final String dir = preferences().get(LOCAL_DEM_DIR, "").trim();
        return dir.isEmpty() ? defaultLocalDemDir() : new File(dir);
    }

    /**
     * Applies the cache location and the offline mode. Called once at start-up, before WorldWind creates its file
     * store; a later change of the cache location needs a restart.
     */
    public static void applyAtStartup() {
        final File cacheDir = getCacheDir();
        if (cacheDir != null && System.getProperty(WORLDWIND_STORE_PROPERTY) == null) {
            System.setProperty(WORLDWIND_STORE_PROPERTY, cacheDir.getAbsolutePath());
        }
        if (isOffline()) {
            WorldWind.setOfflineMode(true);
        }
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
