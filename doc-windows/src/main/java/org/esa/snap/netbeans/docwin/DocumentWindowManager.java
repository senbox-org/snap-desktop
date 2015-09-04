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
    private final Map<Listener, Set<Predicate>> listeners;
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

    /**
     * Constructor.
     */
    protected DocumentWindowManager() {
        listeners = new LinkedHashMap<>();
        openDocumentWindows = new LinkedHashSet<>();
        captureCurrentState();
        WindowManager.getDefault().getRegistry().addPropertyChangeListener(new RegistryPropertyChangeDelegate());
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
     * Adds a document window listener for any document type.
     *
     * @param listener The listener.
     */
    public final void addListener(Listener listener) {
        //noinspection unchecked
        addListener(Predicate.any(), listener);
    }

    /**
     * Adds a document window listener for the given document (base) type.
     *
     * @param predicate The window selector.
     * @param listener  The listener.
     */
    public final <D, V> void addListener(Predicate<D, V> predicate, Listener<D, V> listener) {
        Set<Predicate> predicates = listeners.get(listener);
        if (predicates == null) {
            predicates = new HashSet<>();
            listeners.put(listener, predicates);
        }
        predicates.add(predicate);
    }

    /**
     * Removes the document window listener registered for any document type.
     *
     * @param listener The listener.
     */
    public final void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Removes the document window listener registered for the given window selector.
     *
     * @param listener The listener.
     */
    public final <D, V> void removeListener(Predicate<D, V> predicate, Listener<D, V> listener) {
        Set<Predicate> predicates = listeners.get(listener);
        if (predicates != null) {
            predicates.remove(predicate);
            if (predicates.isEmpty()) {
                listeners.remove(listener);
            }
        }
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
     * Gets the document window listeners registered for the given window selector.
     *
     * @param documentWindow The document window.
     * @return The array of listeners which may be empty.
     */
    final Listener[] getListeners(DocumentWindow documentWindow) {
        ArrayList<Listener> listeners = new ArrayList<>();
        for (Map.Entry<Listener, Set<Predicate>> entry : this.listeners.entrySet()) {
            Listener listener = entry.getKey();
            Set<Predicate> predicates = entry.getValue();
            for (Predicate predicate : predicates) {
                if (isPredicateApplicable(predicate, documentWindow) && predicate.test(documentWindow)) {
                    listeners.add(listener);
                    break;
                }
            }
        }
        return listeners.toArray(new Listener[listeners.size()]);
    }

    private boolean isPredicateApplicable(Predicate predicate, DocumentWindow documentWindow) {
        Object document = documentWindow.getDocument();
        Object view = documentWindow.getView();
        Class<?> actualDocType = document != null ? document.getClass() : Object.class;
        Class<?> actualViewType = view != null ? view.getClass() : Object.class;
        Class<?> requestedDocType = predicate.getDocType();
        Class<?> requestedViewType = predicate.getViewType();
        return requestedDocType.isAssignableFrom(actualDocType)
                && requestedViewType.isAssignableFrom(actualViewType);
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
        Listener[] listeners = getListeners(documentWindow);
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

    private static DocumentWindowManager getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new DocumentWindowManager();
        }
        return defaultInstance;
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
     * A predicate is used to select document windows.
     *
     * @param <D> The document type.
     * @param <V> The view type.
     */
    public interface Predicate<D, V> {

        /**
         * @return the requested document type.
         */
        Class<D> getDocType();

        /**
         * @return the requested view type.
         */
        Class<V> getViewType();

        /**
         * Tests if this predicate applies to the given document window.
         * <p>
         * The method will only be called if it has already been ensured that the {@link #getDocType() requested document type}
         * is assignable from the type of the
         * window's {@link DocumentWindow#getDocument document}
         * and that the {@link #getViewType() requested view type} is assignable from the type of the window's
         * {@link DocumentWindow#getView view}.
         *
         * @param window The document window
         * @return {@code true} if this predicate applies to the given document window.
         */
        boolean test(DocumentWindow window);

        /**
         * @return A predicate that applies to any document window.
         */
        static Predicate<Object, Object> any() {
            return DefaultPredicate.ANY;
        }

        /**
         * @return A predicate that applies to document windows with the given {@code docType}.
         */
        static <D> Predicate<D, Object> doc(Class<D> docType) {
            return new DefaultPredicate<>(docType, Object.class);
        }

        /**
         * @return A predicate that applies to document windows with the given {@code viewType}.
         */
        static <V> Predicate<Object, V> view(Class<V> viewType) {
            return new DefaultPredicate<>(Object.class, viewType);
        }

        /**
         * @return A predicate that applies to document windows with the given {@code docType} and {@code viewType}.
         */
        static <D, V> Predicate<D, V> docView(Class<D> docType, Class<V> viewType) {
            return new DefaultPredicate<>(docType, viewType);
        }
    }

    /**
     * Default {@link Predicate} implementation.
     *
     * @param <D> The document type.
     * @param <V> The view type.
     */
    public static class DefaultPredicate<D, V> implements Predicate<D, V> {

        private static final Predicate<Object, Object> ANY = new DefaultPredicate<>(Object.class, Object.class);

        private final Class<D> docType;
        private final Class<V> viewType;

        public DefaultPredicate(Class<D> docType, Class<V> viewType) {
            this.docType = docType;
            this.viewType = viewType;
        }

        @Override
        public Class<D> getDocType() {
            return docType;
        }

        @Override
        public Class<V> getViewType() {
            return viewType;
        }

        /**
         * Always returns {@code true}. Override if you want a more detailed test.
         *
         * @param window The document window
         * @return Always {@code true}.
         */
        @Override
        public boolean test(DocumentWindow window) {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DefaultPredicate<?, ?> that = (DefaultPredicate<?, ?>) o;
            return getDocType().equals(that.getDocType()) && getViewType().equals(that.getViewType());
        }

        @Override
        public int hashCode() {
            int result = getDocType().hashCode();
            result = 31 * result + getViewType().hashCode();
            return result;
        }
    }

    /**
     * An {@code Event} that adds support for
     * {@code DocumentWindow} objects as the event source.
     */
    public static final class Event<D, V> extends EventObject {
        /**
         * The type of event.
         */
        public enum Type {
            WINDOW_OPENED,
            WINDOW_CLOSED,
            WINDOW_SELECTED,
            WINDOW_DESELECTED
        }

        private final DocumentWindow<D, V> documentWindow;
        private final Type type;

        Event(Type type, DocumentWindow<D, V> documentWindow) {
            super(documentWindow);
            this.type = type;
            this.documentWindow = documentWindow;
        }

        /**
         * @return The type of event.
         */
        public Type getType() {
            return type;
        }

        /**
         * @return The document window.
         */
        public DocumentWindow<D, V> getWindow() {
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
    public interface Listener<D, V> extends EventListener {
        /**
         * Invoked when a document window has been opened.
         */
        default void windowOpened(Event<D, V> e) {
        }

        /**
         * Invoked when a document window has been closed.
         */
        default void windowClosed(Event<D, V> e) {
        }

        /**
         * Invoked when a document window has been selected.
         */
        default void windowSelected(Event<D, V> e) {
        }

        /**
         * Invoked when a document window has been de-selected.
         */
        default void windowDeselected(Event<D, V> e) {
        }
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
