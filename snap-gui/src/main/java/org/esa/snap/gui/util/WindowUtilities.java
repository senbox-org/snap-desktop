package org.esa.snap.gui.util;

import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Various NetBeans window system utilities.
 *
 * @author Norman Fomferra
 * @since 2.0
 */
public class WindowUtilities {
    static final int MAX_ROW_COUNT = 16;
    static final int MAX_COL_COUNT = 16;
    static final String EDITOR_MODE_NAME_FORMAT = "editor_r%dc%d";

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

    public static WorkspaceTopComponent findShowingWorkspace() {
        TopComponent activated = WindowManager.getDefault().getRegistry().getActivated();
        if (activated instanceof WorkspaceTopComponent) {
            return (WorkspaceTopComponent) activated;
        }
        List<WorkspaceTopComponent> showingWorkspaces = findShowingWorkspaces();
        if (!showingWorkspaces.isEmpty()) {
            return showingWorkspaces.get(0);
        }
        return null;
    }

    public static List<WorkspaceTopComponent> findShowingWorkspaces() {
        return collectOpen(WorkspaceTopComponent.class, new Collector<WorkspaceTopComponent, WorkspaceTopComponent>() {
                @Override
                public void collect(WorkspaceTopComponent topComponent, List<WorkspaceTopComponent> list) {
                    if (topComponent.isShowing()) {
                        list.add(topComponent);
                    }
                }
            });
    }

    /**
     * Gets a unique window title.
     *
     * @param titleBase  The title base.
     * @param windowType The window type.
     * @return A unique window title.
     */
    public static String getUniqueTitle(String titleBase, Class<? extends TopComponent> windowType) {
        List<String> titles = collectOpen(windowType, (topComponent, list) -> list.add(topComponent.getDisplayName()));

        if (titles.isEmpty()) {
            return titleBase;
        }

        if (!titles.contains(titleBase)) {
            return titleBase;
        }

        for (int i = 2; ; i++) {
            final String title = String.format("%s (%d)", titleBase, i);
            if (!titles.contains(title)) {
                return title;
            }
        }
    }

    public static <W extends TopComponent> List<W> findOpen(Class<W> windowType) {
        return collectOpen(windowType, new Converter<W, W>() {
            @Override
            protected W convert(W topComponent) {
                return topComponent;
            }
        });
    }

    public static <W extends TopComponent, L> List<L> collectOpen(Collector<W, L> collector) {
        return collectOpen(null, collector);
    }

    public static <W extends TopComponent, L> List<L> collectOpen(Class<W> windowType, Collector<W, L> collector) {
        List<L> result = new ArrayList<>();
        visitMany(TopComponent.getRegistry().getOpened(), windowType, collector, result);
        return result;
    }

    private static <W extends TopComponent, L> void visitMany(Collection<TopComponent> topComponents,
                                                              Class<W> type,
                                                              Collector<W, L> collector,
                                                              List<L> result) {
        for (TopComponent topComponent : topComponents) {
            visitOne(topComponent, type, collector, result);
            if (topComponent instanceof WorkspaceTopComponent) {
                WorkspaceTopComponent workspaceTopComponent = (WorkspaceTopComponent) topComponent;
                List<TopComponent> containedWindows = workspaceTopComponent.getTopComponents();
                visitMany(containedWindows, type, collector, result);
            }
        }
    }

    private static <W extends TopComponent, L> void visitOne(TopComponent topComponent,
                                                             Class<W> type,
                                                             Collector<W, L> collector,
                                                             List<L> result) {
        if (type.isAssignableFrom(topComponent.getClass())) {
            collector.collect((W) topComponent, result);
        }
    }

    public static int[] getBestSubdivisionIntoSquares(int windowCount, int maxRowCount, int maxColCount) {
        double bestDeltaValue = Double.POSITIVE_INFINITY;
        int bestRowCount = -1;
        int bestColCount = -1;
        for (int rowCount = 1; rowCount <= Math.max(windowCount, maxRowCount); rowCount++) {
            for (int colCount = 1; colCount <= Math.max(windowCount, maxColCount); colCount++) {
                if (colCount * rowCount >= windowCount && colCount * rowCount <= 2 * windowCount) {
                    double deltaRatio = Math.abs(1.0 - rowCount / (double) colCount);
                    double deltaCount = Math.abs(1.0 - (colCount * rowCount) / ((double) windowCount));
                    double deltaValue = deltaRatio + deltaCount;
                    if (deltaValue < bestDeltaValue) {
                        bestDeltaValue = deltaValue;
                        bestRowCount = rowCount;
                        bestColCount = colCount;
                    }
                }
            }
        }
        return new int[]{bestRowCount, bestColCount};
    }

    public interface Collector<W extends TopComponent, L> {
        void collect(W topComponent, List<L> list);
    }

    public static abstract class Converter<W extends TopComponent, L> implements Collector<W, L> {
        @Override
        public void collect(W topComponent, List<L> list) {
            list.add(convert(topComponent));
        }

        protected abstract L convert(W topComponent);
    }
}
