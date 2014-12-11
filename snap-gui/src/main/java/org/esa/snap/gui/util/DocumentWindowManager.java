package org.esa.snap.gui.util;

import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventListener;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Norman Fomferra
 * @since 2.0
 */
public class DocumentWindowManager {
    private static DocumentWindowManager defaultInstance;
    private final Map<Listener, PropertyChangeListener> listenerMap;
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
        listenerMap = new LinkedHashMap<>();
        openDocumentWindows = new LinkedHashSet<>();
        captureCurrentState();
        addListener(new DocumentWindowTracker());
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

    public final Listener[] getListeners() {
        Set<Listener> listeners = listenerMap.keySet();
        return listeners.toArray(new Listener[listeners.size()]);
    }

    public final void addListener(Listener listener) {
        RegistryPropertyChangeDelegate pcl = new RegistryPropertyChangeDelegate(listener);
        listenerMap.put(listener, pcl);
        WindowManager.getDefault().getRegistry().addPropertyChangeListener(pcl);
    }

    public final void removeListener(Listener listener) {
        PropertyChangeListener pcl = listenerMap.remove(listener);
        WindowManager.getDefault().getRegistry().removePropertyChangeListener(pcl);
    }

    public DocumentWindow getSelectedDocumentWindow() {
        return selectedDocumentWindow;
    }

    public Set<DocumentWindow> getOpenDocumentWindows() {
        return new LinkedHashSet<>(openDocumentWindows);
    }

    private void setSelectedDocumentWindow(DocumentWindow newValue) {
        DocumentWindow oldValue = this.selectedDocumentWindow;
        if (oldValue != newValue) {
            this.selectedDocumentWindow = newValue;
            if (oldValue != null) {
                oldValue.componentDeselected();
            }
            if (newValue != null) {
                newValue.componentSelected();
            }
        }
    }

    public void requestSelected(DocumentTopComponent topComponent) {
        // todo: find open WorkspaceTopComponents, look if they contain the topComponent.
        // If so, activate WorkspaceTopComponents and make sure the topComponent's internal frame is selected.
    }

    /**
     * An <code>Event</code> that adds support for
     * <code>DocumentWindow</code> objects as the event source.
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
        public void windowOpened(Event e);

        /**
         * Invoked when a document window has been closed.
         */
        public void windowClosed(Event e);

        /**
         * Invoked when a document window is activated.
         */
        public void windowActivated(Event e);

        /**
         * Invoked when a document window is de-activated.
         */
        public void windowDeactivated(Event e);

    }

    private static class RegistryPropertyChangeDelegate implements PropertyChangeListener {
        private final Listener delegatee;

        public RegistryPropertyChangeDelegate(Listener delegatee) {
            this.delegatee = delegatee;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("activated".equals(evt.getPropertyName())) {
                Object oldValue = evt.getOldValue();
                if (oldValue instanceof DocumentWindow) {
                    delegatee.windowDeactivated(new Event((DocumentWindow) evt.getOldValue()));
                }
                Object newValue = evt.getNewValue();
                if (newValue instanceof DocumentWindow) {
                    delegatee.windowActivated(new Event((DocumentWindow) newValue));
                }
            } else if ("tcOpen".equals(evt.getPropertyName())) {
                Object newValue = evt.getNewValue();
                if (newValue instanceof DocumentWindow) {
                    delegatee.windowOpened(new Event((DocumentWindow) newValue));
                }
            } else if ("tcClose".equals(evt.getPropertyName())) {
                Object newValue = evt.getNewValue();
                if (newValue instanceof DocumentWindow) {
                    delegatee.windowClosed(new Event((DocumentWindow) evt.getNewValue()));
                }
            }
        }
    }

    private class DocumentWindowTracker implements Listener {
        @Override
        public void windowOpened(Event e) {
            DocumentWindow documentWindow = e.getDocumentWindow();
            if (documentWindow != null) {
                openDocumentWindows.add(documentWindow);
            }
        }

        @Override
        public void windowClosed(Event e) {
            DocumentWindow documentWindow = e.getDocumentWindow();
            if (documentWindow != null) {
                openDocumentWindows.remove(documentWindow);
                if (documentWindow == selectedDocumentWindow) {
                    setSelectedDocumentWindow(null);
                }
            }
        }

        @Override
        public void windowActivated(Event e) {
            setSelectedDocumentWindow(e.getDocumentWindow());
        }

        @Override
        public void windowDeactivated(Event e) {

        }
    }
}
