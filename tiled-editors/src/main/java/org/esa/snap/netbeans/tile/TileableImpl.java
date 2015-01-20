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

import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import java.awt.*;
import java.util.List;

/**
 * Default Tileable implementation which arranges all global editor windows in tiles.
 *
 * @author Norman Fomferra
 * @since 1.0
 */
@NbBundle.Messages({
        "MSG_TileableImplNothingToDo=Nothing to do.",
        "MSG_TileableImplNotComplete=Not all windows could be arranged."
})
class TileableImpl implements Tileable {

    @Override
    public boolean canTile() {
        return TileUtilities.countOpenEditorWindows() >= 2;
    }

    @Override
    public void tileEvenly() {
        tile(editorWindows -> {
            int windowCount = editorWindows.size();
            Dimension matrixSize = TileUtilities.computeMatrixSizeForEqualAreaTiling(windowCount);
            int windowIndex = 0;
            for (int rowIndex = 0; rowIndex < matrixSize.height; rowIndex++) {
                for (int colIndex = 0; colIndex < matrixSize.width; colIndex++) {
                    if (windowIndex < windowCount) {
                        TopComponent editorWindow = editorWindows.get(windowIndex);
                        if (TileUtilities.openInEditorMode(editorWindow, rowIndex, colIndex)) {
                            windowIndex++;
                        }
                    }
                }
            }
            return windowIndex;
        });
    }

    @Override
    public void tileHorizontally() {
        tile(editorWindows -> {
            int windowCount = editorWindows.size();
            int colCount = Math.min(windowCount, TileUtilities.MAX_TILE_COLUMN_COUNT);
            int windowIndex = 0;
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                if (windowIndex < windowCount) {
                    TopComponent editorWindow = editorWindows.get(windowIndex);
                    if (TileUtilities.openInEditorMode(editorWindow, 0, colIndex)) {
                        windowIndex++;
                    }
                }
            }
            return windowIndex;
        });
    }

    @Override
    public void tileVertically() {
        tile(editorWindows -> {
            int windowCount = editorWindows.size();
            int rowCount = Math.min(windowCount, TileUtilities.MAX_TILE_ROW_COUNT);
            int windowIndex = 0;
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                if (windowIndex < windowCount) {
                    TopComponent editorWindow = editorWindows.get(windowIndex);
                    if (TileUtilities.openInEditorMode(editorWindow, rowIndex, 0)) {
                        windowIndex++;
                    }
                }
            }
            return windowIndex;
        });
    }

    @Override
    public void tileSingle() {
        tile(editorWindows -> {
            int windowIndex = 0;
            for (TopComponent editorWindow : editorWindows) {
                if (TileUtilities.openInMode(editorWindow, "editor")) {
                    windowIndex++;
                }
            }
            return windowIndex;
        });
    }

    private void tile(Tiler tiler) {
        TopComponent selectedWindow = TopComponent.getRegistry().getActivated();

        List<TopComponent> editorWindows = TileUtilities.findOpenEditorWindows();
        if (editorWindows.size() < 2) {
            NotifyDescriptor.Message message = new NotifyDescriptor.Message(Bundle.MSG_TileableImplNothingToDo());
            DialogDisplayer.getDefault().notify(message);
            return;
        }

        int totalWindowCount = editorWindows.size();
        int tiledWindowCount = tiler.tile(editorWindows);

        // Re-activate previously activated window, if any.
        if (selectedWindow != null) {
            selectedWindow.requestActive();
        }

        if (tiledWindowCount < totalWindowCount) {
            NotifyDescriptor.Message message = new NotifyDescriptor.Message(Bundle.MSG_TileableImplNotComplete());
            DialogDisplayer.getDefault().notify(message);
        }
    }

    private interface Tiler {
        int tile(List<TopComponent> editorWindows);
    }
}
