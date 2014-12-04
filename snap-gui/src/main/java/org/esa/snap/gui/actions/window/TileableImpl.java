package org.esa.snap.gui.actions.window;

import org.esa.snap.gui.util.Tileable;
import org.esa.snap.gui.util.WindowUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import java.util.List;

/**
 * Default Tileable implementation which arranges all global editor windows in tiles.
 *
 * @author Norman Fomferra
 */
@NbBundle.Messages({
        "MSG_TileableImplNothingToDo=Nothing to do.",
        "MSG_TileableImplNotComplete=Not all windows could be arranged."
})
class TileableImpl implements Tileable {

    @Override
    public boolean canTile() {
        return WindowUtilities.countOpenEditorWindows() >= 2;
    }

    @Override
    public void tileEvenly() {
        tile(editorWindows -> {
            int windowCount = editorWindows.size();
            int[] result = ModeUtilities.getBestSubdivisionIntoSquares(windowCount, 16, 16);
            int rowCount = result[0];
            int colCount = result[1];
            int windowIndex = 0;
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                for (int colIndex = 0; colIndex < colCount; colIndex++) {
                    if (windowIndex < windowCount) {
                        TopComponent editorWindow = editorWindows.get(windowIndex);
                        if (ModeUtilities.openInEditorMode(rowIndex, colIndex, editorWindow)) {
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
            int colCount = Math.min(windowCount, 16);
            int windowIndex = 0;
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                if (windowIndex < windowCount) {
                    TopComponent editorWindow = editorWindows.get(windowIndex);
                    if (ModeUtilities.openInEditorMode(0, colIndex, editorWindow)) {
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
            int rowCount = Math.min(windowCount, 16);
            int windowIndex = 0;
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                if (windowIndex < windowCount) {
                    TopComponent editorWindow = editorWindows.get(windowIndex);
                    if (ModeUtilities.openInEditorMode(rowIndex, 0, editorWindow)) {
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
                if (ModeUtilities.openInEditorMode("editor", editorWindow)) {
                    windowIndex++;
                }
            }
            return windowIndex;
        });
    }

    private void tile(Tiler tiler) {
        TopComponent selectedWindow = TopComponent.getRegistry().getActivated();

        List<TopComponent> editorWindows = WindowUtilities.findOpenEditorWindows();
        System.out.println("TileableImpl: " + editorWindows.size() + " window(s) to tile");
        if (editorWindows.size() < 2) {
            NotifyDescriptor.Message message = new NotifyDescriptor.Message(Bundle.MSG_TileableImplNothingToDo());
            DialogDisplayer.getDefault().notify(message);
            return;
        }

        int windowCount = editorWindows.size();
        int tiledCount = tiler.tile(editorWindows);

        // Re-activate previously activated window, if any.
        if (selectedWindow != null) {
            selectedWindow.requestActive();
        }

        if (tiledCount < windowCount) {
            NotifyDescriptor.Message message = new NotifyDescriptor.Message(Bundle.MSG_TileableImplNotComplete());
            DialogDisplayer.getDefault().notify(message);
        }
    }

    private interface Tiler {
        int tile(List<TopComponent> editorWindows);
    }
}
