/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model;


import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**

 */

public class AOIManager {

    public static final String LAST_AOI_PATH = "snap.aoi.last_aoi_path";
    private static final String LAST_INPUT_PATH = "snap.aoi.last_input_path";
    private static final String LAST_OUTPUT_PATH = "snap.aoi.last_output_path";
    private static final String LAST_GRAPH_PATH = "snap.aoi.last_graph_path";

    private static final String AOI_FILE = "snap.aoi.AOIFile_";
    private static final String AOI_BATCH_TARGET = "snap.aoi.AOITarget_";

    private static final Preferences pref = SnapApp.getDefault().getPreferences();

    private final List<AOI> aoiList = new ArrayList<>(5);

    public AOI[] getAOIList() {
        return aoiList.toArray(new AOI[aoiList.size()]);
    }

    public AOI createAOI(final File aoiFile) {
        final AOI aoi = new AOI(aoiFile);
        aoiList.add(aoi);
        return aoi;
    }

    public void removeAOI(final AOI aoi) {
        aoiList.remove(aoi);
    }

    public File getNewAOIFile() {
        return new File(getAOIFolder(), "aoi_" + (aoiList.size() + 1));
    }

    public AOI getAOIAt(final int index) {
        return aoiList.get(index);
    }

    public static File getAOIFolder() {
        final File aoiFolder = new File(SystemUtils.getAuxDataPath().toFile(), File.separator + "aoi");
        if (!aoiFolder.exists())
            aoiFolder.mkdirs();
        return aoiFolder;
    }

    static String getLastInputPath() {
        return pref.get(LAST_INPUT_PATH, "");
    }

    public static void setLastInputPath(final String path) {
        pref.put(LAST_INPUT_PATH, path);
    }

    static String getLastOutputPath() {
        return pref.get(LAST_OUTPUT_PATH, "");
    }

    public static void setLastOutputPath(final String path) {
        pref.put(LAST_OUTPUT_PATH, path);
    }

    public static void addBaseDir(final File baseDir) {
        pref.put(AOI_FILE + baseDir.getAbsolutePath(), baseDir.getAbsolutePath());
    }

    public static void setBatchProcessResult(final AOI aoi, final File inputFile, final File targetFile) {
        pref.put(AOI_BATCH_TARGET + inputFile.getAbsolutePath() + '_' + aoi.getProcessingGraph(),
                 targetFile.getAbsolutePath());
    }

    public static boolean hasBeenBatchedProcessed(final AOI aoi, final File inputFile) {
        final String targetFileStr = pref.get(AOI_BATCH_TARGET + inputFile.getAbsolutePath()
                                                      + '_' + aoi.getProcessingGraph(), null);
        if (targetFileStr != null) {
            final File targetFile = new File(targetFileStr);
            return targetFile.exists();
        }
        return false;
    }

    public static void removeBaseDir(final File baseDir) {
        pref.remove(AOI_FILE + baseDir.getAbsolutePath());
    }

    public static File[] getBaseDirs() {
        final ArrayList<File> dirList = new ArrayList<>();
        try {
            for (String key : pref.keys()) {
                if (key.startsWith(AOI_FILE)) {
                    final String path = pref.get(key, null);
                    if (path != null) {
                        final File file = new File(path);
                        if (file.exists()) {
                            dirList.add(file);
                        }
                    }
                }
            }
        } catch (Exception e) {
            SnapApp.getDefault().handleError("AOIConfig unable to reload base folders", e);
        }
        return dirList.toArray(new File[dirList.size()]);
    }
}
