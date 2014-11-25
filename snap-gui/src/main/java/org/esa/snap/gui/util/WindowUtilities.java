package org.esa.snap.gui.util;

import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Various window utilities.
 *
 * @author Norman Fomferra
 */
public class WindowUtilities {

    public static boolean openDocumentWindow(TopComponent documentWindow) {
        WorkspaceTopComponent workspaceTopComponent = getShowingWorkspace();
        if (workspaceTopComponent != null) {
            workspaceTopComponent.addTopComponent(documentWindow);
            return true;
        }
        Mode editor = WindowManager.getDefault().findMode("editor");
        if (editor.dockInto(documentWindow)) {
            documentWindow.open();
            documentWindow.requestActive();
            return true;
        }
        return false;
    }

    public static WorkspaceTopComponent getShowingWorkspace() {
        TopComponent activated = WindowManager.getDefault().getRegistry().getActivated();
        if (activated instanceof WorkspaceTopComponent) {
            return (WorkspaceTopComponent) activated;
        }
        List<WorkspaceTopComponent> showingWorkspaces = visitOpen(topComponent -> topComponent instanceof WorkspaceTopComponent && topComponent.isShowing() ? (WorkspaceTopComponent) topComponent : null);
        if (!showingWorkspaces.isEmpty()) {
            return showingWorkspaces.get(0);
        }
        return null;
    }

    /**
     * Gets a unique window title.
     *
     * @param titleBase  The title base.
     * @param windowType The window type.
     * @return A unique window title.
     */
    public static String getUniqueTitle(String titleBase, Class<? extends TopComponent> windowType) {
        List<String> titles = visitOpen(TopComponent::getDisplayName, windowType);

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

    public static <T extends TopComponent> List<T> findOpen(Class<T> windowType) {
        return visitOpen(topComponent -> (T) topComponent, windowType);
    }

    public static <T> List<T> visitOpen(Visitor<T> visitor) {
        return visitOpen(visitor, TopComponent.class);
    }

    public static <T> List<T> visitOpen(Visitor<T> visitor, Class<? extends TopComponent> windowType) {
        List<T> result = new ArrayList<>();
        visitMany(TopComponent.getRegistry().getOpened(), windowType, visitor, result);
        return result;
    }

    private static <T> void visitMany(Collection<TopComponent> topComponents,
                                      Class<? extends TopComponent> type,
                                      Visitor<T> visitor,
                                      List<T> result) {
        for (TopComponent topComponent : topComponents) {
            visitOne(topComponent, type, visitor, result);
            if (topComponent instanceof WorkspaceTopComponent) {
                WorkspaceTopComponent workspaceTopComponent = (WorkspaceTopComponent) topComponent;
                List<TopComponent> containedWindows = workspaceTopComponent.getTopComponents();
                visitMany(containedWindows, type, visitor, result);
            }
        }
    }

    private static <T> void visitOne(TopComponent topComponent,
                                     Class<? extends TopComponent> type,
                                     Visitor<T> visitor,
                                     List<T> result) {
        if (type.isAssignableFrom(topComponent.getClass())) {
            T element = visitor.visit(topComponent);
            if (element != null) {
                result.add(element);
            }
        }
    }

    public interface Visitor<T> {
        T visit(TopComponent topComponent);
    }
}
