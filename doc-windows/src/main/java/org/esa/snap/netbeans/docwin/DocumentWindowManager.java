package org.esa.snap.netbeans.docwin;

import org.openide.modules.OnStart;
import org.openide.util.Lookup;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Manages global opening, closing, and selection of {@link DocumentWindow}s.
 *
 * @author Norman Fomferra
 * @since 2.0
 */
public class DocumentWindowManager implements WindowContainer<DocumentWindow> {
    private static DocumentWindowManager defaultInstance;
    private final Map<Listener, Set<Class<?>>> listeners;
    private final Set<DocumentWindow> openDocumentWindows;
    private DocumentWindow selectedDocumentWindow;

    /**
     * Gets the {@code DocumentWindowManager}'s default implementation. It does this by
     * <pre>
     * DocumentWindowManager instance = Lookup.getDefault().lookup(DocumentWindowManager.class);
     * return (instance != null) ? instance : getDefaultInstance();
     * </pre>
     *
     * @return the default implementation
     */
    public static DocumentWindowManager getDefault() {
        DocumentWindowManager instance = Lookup.getDefault().lookup(DocumentWindowManager.class);
        return (instance != null) ? instance : getDefaultInstance();
    }

    private static DocumentWindowManager getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new DocumentWindowManager();
        }
        return defaultInstance;
    }

    /**
     * Constructor.
     */
    protected DocumentWindowManager() {
        listeners = new LinkedHashMap<>();
        openDocumentWindows = new LinkedHashSet<>();
        captureCurrentState();
        WindowManager.getDefault().getRegistry().addPropertyChangeListener(new RegistryPropertyChangeDelegate());
    }

    private void captureCurrentState() {
        TopComponent.Registry registry = WindowManager.getDefault().getRegistry();
        Set<TopComponent> topComponents = registry.getOpened();
        topComponents.stream()
                .filter(topComponent -> topComponent instanceof DocumentWindow)
                .forEach(topComponent -> {
                    DocumentWindow documentWindow = (DocumentWindow) topComponent;
                    openDocumentWindows.add(documentWindow);
                    if (registry.getActivated() == topComponent) {
                        selectedDocumentWindow = documentWindow;
                    }
                });
    }

    /**
     * Gets the currently selected document windows.
     *
     * @return the currently selected document windows. May be {@code null}.
     */
    @Override
    public DocumentWindow getSelectedWindow() {
        return selectedDocumentWindow;
    }

    /**
     * Gets all opened document windows.
     *
     * @return All opened document windows. List may be empty.
     */
    @Override
    public List<DocumentWindow> getOpenedWindows() {
        return new ArrayList<>(openDocumentWindows);
    }

    /**
     * Opens a document window.
     * <p>
     * Document windows are initially opened in the NetBeans {@code "editor"} mode which
     * is the central panel of the main frame.
     *
     * @param documentWindow The document window to be opened.
     * @return {@code true} on success
     */
    public boolean openWindow(DocumentWindow documentWindow) {
        TopComponent topComponent = documentWindow.getTopComponent();
        WorkspaceTopComponent workspaceTopComponent = WorkspaceTopComponent.findShowingInstance();
        if (workspaceTopComponent != null) {
            workspaceTopComponent.addTopComponent(topComponent);
            return true;
        }
        Mode editor = WindowManager.getDefault().findMode("editor");
        if (editor.dockInto(topComponent)) {
            topComponent.open();
            return true;
        }
        return false;
    }

    /**
     * Closes a document window.
     *
     * @param documentWindow The document window to be closed.
     * @return {@code true} on success
     */
    public boolean closeWindow(DocumentWindow documentWindow) {
        Optional<WorkspaceTopComponent> anyWorkspace = WindowUtilities
                .getOpened(WorkspaceTopComponent.class)
                .filter(tc -> tc.getTopComponents().contains(documentWindow.getTopComponent())).findAny();
        if (anyWorkspace.isPresent()) {
            return anyWorkspace.get().removeTopComponent(documentWindow.getTopComponent());
        } else {
            return removeOpenedWindow(documentWindow);
        }
    }

    /**
     * Requests that the given document window is being selected.
     *
     * @param documentWindow The document window to be selected.
     */
    public void requestSelected(DocumentWindow documentWindow) {
        TopComponent topComponent = documentWindow.getTopComponent();
        List<WorkspaceTopComponent> showingWorkspaces = WorkspaceTopComponent.findShowingInstances();
        for (WorkspaceTopComponent showingWorkspace : showingWorkspaces) {
            if (showingWorkspace.getTopComponents().contains(topComponent)) {
                showingWorkspace.requestActiveTopComponent(topComponent);
                return;
            }
        }
        topComponent.requestActive();
    }

    /**
     * Gets all registered document window listeners.
     *
     * @return The array of listeners which may be empty.
     */
    public final Listener[] getListeners() {
        return listeners.keySet().toArray(new Listener[listeners.size()]);
    }

    /**
     * Gets the document window listeners registered for the given document (base) type.
     *
     * @param documentType The document base type.
     * @return The array of listeners which may be empty.
     */
    public final <T> Listener<T>[] getListeners(Class<T> documentType) {
        ArrayList<Listener<T>> listeners = new ArrayList<>();
        for (Map.Entry<Listener, Set<Class<?>>> entry : this.listeners.entrySet()) {
            Listener listener = entry.getKey();
            Set<Class<?>> types = entry.getValue();
            for (Class<?> type : types) {
                if (type.isAssignableFrom(documentType)) {
                    //noinspection unchecked
                    listeners.add(listener);
                    break;
                }
            }
        }
        //noinspection unchecked
        return listeners.toArray(new Listener[listeners.size()]);
    }

    /**
     * Adds a document window listener for any document type.
     *
     * @param listener The listener.
     */
    public final void addListener(Listener listener) {
        //noinspection unchecked
        addListener(Object.class, listener);
    }

    /**
     * Adds a document window listener for the given document (base) type.
     *
     * @param documentType The document base type.
     * @param listener     The listener.
     */
    public final <D> void addListener(Class<D> documentType, Listener<D> listener) {
        Set<Class<?>> types = listeners.get(listener);
        if (types == null) {
            types = new HashSet<>();
            listeners.put(listener, types);
        }
        types.add(documentType);
    }

    /**
     * Removes the document window listener registered for any document type.
     *
     * @param listener The listener.
     */
    public final void removeListener(Listener listener) {
        //noinspection unchecked
        removeListener(Object.class, listener);
    }

    /**
     * Removes the document window listener registered for the given document type.
     *
     * @param listener The listener.
     */
    public final <D> void removeListener(Class<D> type, Listener<D> listener) {
        Set<Class<?>> types = listeners.get(listener);
        if (types != null) {
            types.remove(type);
            if (types.isEmpty()) {
                listeners.remove(listener);
            }
        }
    }

    void addOpenedWindow(DocumentWindow documentWindow) {
        if (openDocumentWindows.add(documentWindow)) {
            fireWindowEvent(Event.Type.WINDOW_OPENED, documentWindow);
        }
    }


    boolean removeOpenedWindow(DocumentWindow documentWindow) {
        if (openDocumentWindows.remove(documentWindow)) {
            if (getSelectedWindow() == documentWindow) {
                setSelectedWindow(null);
            }
            boolean isClosed = documentWindow.getTopComponent().close();
            if (isClosed) {
                fireWindowEvent(Event.Type.WINDOW_CLOSED, documentWindow);
            }
            return isClosed;
        }
        return false;
    }

    void setSelectedWindow(DocumentWindow newValue) {
        DocumentWindow oldValue = this.selectedDocumentWindow;
        if (oldValue != newValue) {
            this.selectedDocumentWindow = newValue;
            if (oldValue != null) {
                oldValue.componentDeselected();
                fireWindowEvent(Event.Type.WINDOW_DESELECTED, oldValue);
            }
            if (newValue != null) {
                newValue.componentSelected();
                fireWindowEvent(Event.Type.WINDOW_SELECTED, newValue);
            }
        }
    }

    private void fireWindowEvent(Event.Type eventType, DocumentWindow documentWindow) {
        Class<?> documentType = documentWindow.getDocument().getClass();
        Listener[] listeners = getListeners(documentType);
        if (listeners.length > 0) {
            //noinspection unchecked
            Event event = new Event(eventType, documentWindow);
            for (Listener listener : listeners) {
                switch (eventType) {
                    case WINDOW_OPENED:
                        //noinspection unchecked
                        listener.windowOpened(event);
                        break;
                    case WINDOW_CLOSED:
                        //noinspection unchecked
                        listener.windowClosed(event);
                        break;
                    case WINDOW_SELECTED:
                        //noinspection unchecked
                        listener.windowSelected(event);
                        break;
                    case WINDOW_DESELECTED:
                        //noinspection unchecked
                        listener.windowDeselected(event);
                        break;
                }
            }
        }
    }

    /**
     * An {@code Event} that adds support for
     * {@code DocumentWindow} objects as the event source.
     */
    public static final class Event<D> extends EventObject {
        public enum Type {
            WINDOW_OPENED,
            WINDOW_CLOSED,
            WINDOW_SELECTED,
            WINDOW_DESELECTED
        }

        private final DocumentWindow<D> documentWindow;
        private final Type type;

        public Event(Type type, DocumentWindow<D> documentWindow) {
            super(documentWindow);
            this.type = type;
            this.documentWindow = documentWindow;
        }

        public Type getType() {
            return type;
        }

        public DocumentWindow<D> getDocumentWindow() {
            return documentWindow;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "type=" + type +
                    ", documentWindow=" + documentWindow +
                    '}';
        }
    }

    /**
     * The listener interface for receiving document window events.
     */
    public interface Listener<D> extends EventListener {
        /**
         * Invoked when a document window has been opened.
         */
        void windowOpened(Event<D> e);

        /**
         * Invoked when a document window has been closed.
         */
        void windowClosed(Event<D> e);

        /**
         * Invoked when a document window has been selected.
         */
        void windowSelected(Event<D> e);

        /**
         * Invoked when a document window has been de-selected.
         */
        void windowDeselected(Event<D> e);
    }

    /**
     * This class is not API. It is public as an implementation detail.
     * <p>
     * Makes sure DocumentWindowManager can start listening to window events from the beginning.
     */
    @SuppressWarnings("unused")
    @OnStart
    public static class Starter implements Runnable {
        @Override
        public void run() {
            getDefault();
        }
    }

    private class RegistryPropertyChangeDelegate implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (TopComponent.Registry.PROP_ACTIVATED.equals(evt.getPropertyName())) {
                Object newValue = evt.getNewValue();
                if (newValue instanceof DocumentWindow) {
                    setSelectedWindow((DocumentWindow) newValue);
                }
            } else if (TopComponent.Registry.PROP_TC_OPENED.equals(evt.getPropertyName())) {
                Object newValue = evt.getNewValue();
                if (newValue instanceof DocumentWindow) {
                    addOpenedWindow((DocumentWindow) newValue);
                }
            } else if (TopComponent.Registry.PROP_TC_CLOSED.equals(evt.getPropertyName())) {
                Object newValue = evt.getNewValue();
                if (newValue instanceof DocumentWindow) {
                    removeOpenedWindow((DocumentWindow) newValue);
                }
            }
        }
    }
}
