package org.esa.snap.netbeans.docwin;

import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Various NetBeans window system utilities.
 *
 * @author Norman Fomferra
 * @since 1.0
 */
public class WindowUtilities {


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
