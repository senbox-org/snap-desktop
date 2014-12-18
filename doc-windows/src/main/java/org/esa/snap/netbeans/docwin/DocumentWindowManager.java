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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Manages global opening, closing, and selection of {@link DocumentWindow}s.
 *
 * @author Norman Fomferra
 * @since 2.0
 */
public class DocumentWindowManager implements WindowContainer<DocumentWindow> {
    private static DocumentWindowManager defaultInstance;
    private final List<Listener> listeners;
    private final Set<DocumentWindow> openDocumentWindows;
    private DocumentWindow selectedDocumentWindow;

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

    protected DocumentWindowManager() {
        listeners = new LinkedList<>();
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

    @Override
    public DocumentWindow getSelectedWindow() {
        return selectedDocumentWindow;
    }

    @Override
    public List<DocumentWindow> getOpenedWindows() {
        return new ArrayList<>(openDocumentWindows);
    }

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

    public boolean closeWindow(DocumentWindow documentWindow) {
        Optional<WorkspaceTopComponent> anyWorkspace = WindowUtilities
                .getOpened(WorkspaceTopComponent.class)
                .filter(tc -> tc.getTopComponents().contains(documentWindow.getTopComponent())).findAny();
        if (anyWorkspace.isPresent()) {
            return anyWorkspace.get().removeTopComponent(documentWindow.getTopComponent());
        } else {
            return documentWindow.getTopComponent().close();
        }
    }

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

    public final Listener[] getListeners() {
        return listeners.toArray(new Listener[listeners.size()]);
    }

    public final void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public final void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    void addOpenedWindow(DocumentWindow documentWindow) {
        if (openDocumentWindows.add(documentWindow)) {
            Event event = new Event(documentWindow);
            Listener[] listeners = getListeners();
            for (Listener listener : listeners) {
                listener.windowOpened(event);
            }
        }
    }

    void removeOpenedWindow(DocumentWindow documentWindow) {
        if (openDocumentWindows.remove(documentWindow)) {
            if (getSelectedWindow() == documentWindow) {
                setSelectedWindow(null);
            }
            Event event = new Event(documentWindow);
            Listener[] listeners = getListeners();
            for (Listener listener : listeners) {
                listener.windowClosed(event);
            }
        }
    }

    void setSelectedWindow(DocumentWindow newValue) {
        DocumentWindow oldValue = this.selectedDocumentWindow;
        if (oldValue != newValue) {
            this.selectedDocumentWindow = newValue;
            if (oldValue != null) {
                oldValue.componentDeselected();
                Event event = new Event(oldValue);
                Listener[] listeners = getListeners();
                for (Listener listener : listeners) {
                    listener.windowDeselected(event);
                }
            }
            if (newValue != null) {
                newValue.componentSelected();
                Event event = new Event(newValue);
                Listener[] listeners = getListeners();
                for (Listener listener : listeners) {
                    listener.windowSelected(event);
                }
            }
        }
    }

    /**
     * An {@code Event} that adds support for
     * {@code DocumentWindow} objects as the event source.
     */
    public static final class Event extends EventObject {
        private final DocumentWindow documentWindow;

        public Event(DocumentWindow documentWindow) {
            super(documentWindow);
            this.documentWindow = documentWindow;
        }

        public DocumentWindow getDocumentWindow() {
            return documentWindow;
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
         * Invoked when a document window has been opened.
         */
        void windowOpened(Event e);

        /**
         * Invoked when a document window has been closed.
         */
        void windowClosed(Event e);

        /**
         * Invoked when a document window has been selected.
         */
        void windowSelected(Event e);

        /**
         * Invoked when a document window has been de-selected.
         */
        void windowDeselected(Event e);
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


    /**
     * Makes sure DocumentWindowManager can start listening to window events from the beginning.
     */
    @OnStart
    public static class Starter implements Runnable {
        @Override
        public void run() {
            getDefault();
        }
    }
}
