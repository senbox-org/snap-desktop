package org.esa.snap.gui.util;

import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EventListener;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Various window utilities.
 *
 * @author Norman Fomferra
 */
public class WindowUtilities {

    final static Map<Listener, PropertyChangeListener> listenerMap = new LinkedHashMap<>();

    public static Listener[] getListeners() {
        Set<Listener> listeners = listenerMap.keySet();
        return listeners.toArray(new Listener[listeners.size()]);
    }

    public static void addListener(Listener listener) {
        MyPropertyChangeListener pcl = new MyPropertyChangeListener(listener);
        listenerMap.put(listener, pcl);
        WindowManager.getDefault().getRegistry().addPropertyChangeListener(pcl);
    }

    public static void removeListener(Listener listener) {
        PropertyChangeListener pcl = listenerMap.remove(listener);
        WindowManager.getDefault().getRegistry().removePropertyChangeListener(pcl);
    }

    public static List<TopComponent> findOpenEditorWindows() {
        return findOpenEditorWindows((win1, win2) -> {
            String name1 = win1.getDisplayName();
            String name2 = win2.getDisplayName();
            return (name1 != null ? name1 : "").compareTo(name2 != null ? name2 : "");
        });
    }

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
        List<WorkspaceTopComponent> showingWorkspaces = collectOpen(WorkspaceTopComponent.class, new Collector<WorkspaceTopComponent, WorkspaceTopComponent>() {
            @Override
            public void collect(WorkspaceTopComponent topComponent, List<WorkspaceTopComponent> list) {
                if (topComponent.isShowing()) {
                    list.add(topComponent);
                }
            }
        });
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

    /**
     * An <code>Event</code> that adds support for
     * <code>TopComponent</code> objects as the event source.
     */
    public static class Event extends EventObject {
        public Event(TopComponent source) {
            super(source);
        }

        public TopComponent getTopComponent() {
            return getSource() instanceof TopComponent ? (TopComponent) getSource() : null;
        }
    }

    /**
     * The listener interface for receiving window events.
     * This class is functionally equivalent to the WindowListener class
     * in the AWT.
     *
     * @see java.awt.event.WindowListener
     */
    public interface Listener extends EventListener {
        /**
         * Invoked when a window has been opened.
         */
        public void windowOpened(Event e);

        /**
         * Invoked when an window has been closed.
         */
        public void windowClosed(Event e);

        /**
         * Invoked when an window is activated.
         */
        public void windowActivated(Event e);

        /**
         * Invoked when an window is de-activated.
         */
        public void windowDeactivated(Event e);

    }

    private static class MyPropertyChangeListener implements PropertyChangeListener {
        private final Listener listener;

        public MyPropertyChangeListener(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("activated".equals(evt.getPropertyName())) {
                Object oldValue = evt.getOldValue();
                if (oldValue instanceof TopComponent) {
                    listener.windowDeactivated(new Event((TopComponent) evt.getOldValue()));
                }
                Object newValue = evt.getNewValue();
                if (newValue instanceof TopComponent) {
                    listener.windowActivated(new Event((TopComponent) newValue));
                }
            } else if ("tcOpen".equals(evt.getPropertyName())) {
                Object newValue = evt.getNewValue();
                if (newValue instanceof TopComponent) {
                    listener.windowOpened(new Event((TopComponent) newValue));
                }
            } else if ("tcClose".equals(evt.getPropertyName())) {
                Object newValue = evt.getNewValue();
                if (newValue instanceof TopComponent) {
                    listener.windowClosed(new Event((TopComponent) evt.getNewValue()));
                }
            }
        }
    }
}
