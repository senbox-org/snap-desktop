/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.netbeans.tile;

import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.min;

/**
 * Collections of utility methods used for editor tiling.
 *
 * @author Norman Fomferra
 * @since 1.0
 */
public class TileUtilities {
    public static final String EDITOR_MODE_NAME_FORMAT = "editor_r%dc%d";
    public static final int MAX_TILE_ROW_COUNT = 16;
    public static final int MAX_TILE_COLUMN_COUNT = 16;

    /**
     * Opens a top component in a mode of kind "editor" at the given row and column.
     *
     * @param topComponent The top component to open.
     * @param rowIndex     The row index.
     * @param colIndex     The column index.
     * @return {@code true} on success.
     */
    public static boolean openInEditorMode(TopComponent topComponent, int rowIndex, int colIndex) {
        String modeName = String.format(EDITOR_MODE_NAME_FORMAT, rowIndex, colIndex);
        return openInMode(topComponent, modeName);
    }

    /**
     * Opens a top component in the given mode.
     *
     * @param topComponent The top component to open.
     * @param modeName     The mode's name.
     * @return {@code true} on success.
     */
    public static boolean openInMode(TopComponent topComponent, String modeName) {
        Mode mode = WindowManager.getDefault().findMode(modeName);
        if (mode != null) {
            if (!Arrays.asList(mode.getTopComponents()).contains(topComponent)) {
                if (mode.dockInto(topComponent)) {
                    topComponent.open();
                    return true;
                }
            } else {
                topComponent.open();
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the currently opened top components in modes of kind "editor".
     *
     * @return The number of currently opened top components in modes of kind "editor"
     */
    public static int countOpenEditorWindows() {
        int count = 0;
        WindowManager wm = WindowManager.getDefault();
        Set<TopComponent> opened = wm.getRegistry().getOpened();
        for (TopComponent openedWindow : opened) {
            if (wm.isEditorTopComponent(openedWindow)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Finds all opened top components in modes of kind "editor" ordered by display name.
     *
     * @return The list of opened top components.
     */
    public static List<TopComponent> findOpenEditorWindows() {
        return findOpenEditorWindows((win1, win2) -> {
            String name1 = win1.getDisplayName();
            String name2 = win2.getDisplayName();
            return (name1 != null ? name1 : "").compareTo(name2 != null ? name2 : "");
        });
    }

    /**
     * Finds all opened top components in modes of kind "editor" using the given comparator.
     *
     * @return The list of opened top components.
     */
    public static List<TopComponent> findOpenEditorWindows(Comparator<TopComponent> comparator) {
        ArrayList<TopComponent> editorWindows = new ArrayList<>();
        Set<TopComponent> openedWindows = WindowManager.getDefault().getRegistry().getOpened();
        editorWindows.addAll(openedWindows
                                     .stream()
                                     .filter(topComponent -> WindowManager.getDefault().isEditorTopComponent(topComponent))
                                     .collect(Collectors.toList()));
        if (comparator != null) {
            editorWindows.sort(comparator);
        }
        return editorWindows;
    }

    /**
     * Finds the best matching matrix of equal-area squares
     * given a fixed number of window areas.
     *
     * @param windowCount Number of window areas.
     * @return Matrix size, where width=#columns, and height=#rows.
     */
    public static Dimension computeMatrixSizeForEqualAreaTiling(int windowCount) {
        double minDeltaValue = Double.POSITIVE_INFINITY;
        int bestRowCount = -1;
        int bestColCount = -1;
        for (int rowCount = 1; rowCount <= min(windowCount, MAX_TILE_ROW_COUNT); rowCount++) {
            for (int colCount = 1; colCount <= min(windowCount, MAX_TILE_COLUMN_COUNT); colCount++) {
                if (colCount * rowCount >= windowCount && colCount * rowCount <= 2 * windowCount) {
                    double deltaRatio = Math.abs(1.0 - rowCount / (double) colCount);
                    double deltaCount = Math.abs(1.0 - (colCount * rowCount) / ((double) windowCount));
                    double deltaValue = deltaRatio + deltaCount;
                    if (deltaValue < minDeltaValue) {
                        minDeltaValue = deltaValue;
                        bestRowCount = rowCount;
                        bestColCount = colCount;
                        if (deltaValue == 0.0) {
                            break;
                        }
                    }
                }
            }
        }
        return new Dimension(bestColCount, bestRowCount);
    }
}
