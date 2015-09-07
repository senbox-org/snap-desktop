/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.netbeans.docwin;

import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;


/**
 * A {@code TopComponent} designed to serve as an editor for a "document" object.
 * In contrast to a "normal" {@code TopComponent}, a {@code DocumentTopComponent}
 * retains a selected state even after component deactivation. It can also be undocked/floated
 * into a {@link WorkspaceTopComponent} and thereby undergo the usual {@code TopComponent} lifecyle as
 * indicated by the various notification methods given by the interface {@link NotifiableComponent}:
 * <ol>
 * <li>{@link #componentOpened()}/{@link #componentClosed()} method will be called if the corresponding internal frame is activated/deactivated;</li>
 * <li>{@link #componentActivated()}/{@link #componentDeactivated()} method will be called if the corresponding internal frame is activated/deactivated;</li>
 * <li>{@link #componentShowing()}/{@link #componentHidden()} method will be called if the corresponding internal frame is iconified/deiconified.</li>
 * </ol>
 * <p>
 * Document windows keep a constant reference to the document object which it exposes through the window's lookup.
 * Overrides may use the {@link #getDynamicContent() content} to alter the objects in the exposed lookup,
 * however, the document object will always remain in it.
 *
 * @param <D> The document type.
 * @param <V> The view type.
 * @author Norman Fomferra
 * @since 1.0
 */
public abstract class DocumentTopComponent<D, V> extends TopComponent
        implements DocumentWindow<D, V>, NotifiableComponent {
    private static final Logger LOG = Logger.getLogger(DocumentTopComponent.class.getName());

    private final D document;
    private final InstanceContent dynamicContent;

    public DocumentTopComponent(D document) {
        if (document == null) {
            throw new NullPointerException("document");
        }
        this.document = document;
        this.dynamicContent = new InstanceContent();
        associateLookup(new ProxyLookup(Lookups.fixed(document), new AbstractLookup(dynamicContent)));
    }

    protected InstanceContent getDynamicContent() {
        return dynamicContent;
    }

    @Override
    public final D getDocument() {
        return document;
    }

    @Override
    public final TopComponent getTopComponent() {
        return this;
    }

    @Override
    public final boolean isSelected() {
        return DocumentWindowManager.getDefault().getSelectedWindow() == this;
    }

    @Override
    public void requestSelected() {
        if (isOpened()) {
            requestActive();
        } else {
            requestVisible();
            DocumentWindowManager.getDefault().requestSelected(this);
        }
    }

    @Override
    public Action[] getActions() {
        Action[] baseActions = super.getActions();
        Action[] extraActions = WorkspaceTopComponent.getExtraActions(this);
        if (extraActions.length > 0) {
            ArrayList<Action> actions = new ArrayList<>(Arrays.asList(baseActions));
            if (!actions.isEmpty()) {
                actions.add(null);
            }
            actions.addAll(Arrays.asList(extraActions));
            return actions.toArray(new Action[actions.size()]);
        }
        return baseActions;
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }

    /**
     * Called when this component opened for the first time or after being closed.
     */
    @Override
    public void componentOpened() {
    }

    /**
     * Called when this component was closed.
     */
    @Override
    public void componentClosed() {
    }

    /**
     * Called when this component is about to be shown.
     */
    @Override
    public void componentShowing() {
    }

    /**
     * Called when this component was hidden.
     */
    @Override
    public void componentHidden() {
    }

    /**
     * Called when this component is activated.
     */
    @Override
    public void componentActivated() {
    }

    /**
     * Called when this component is deactivated.
     */
    @Override
    public void componentDeactivated() {
    }

    /**
     * Called when this component was selected.
     * <p>
     * Default implementation simply calls {@link #updateSelectedState}.
     */
    @Override
    public void componentSelected() {
        updateSelectedState();
    }

    /**
     * Called when this component was deselected.
     * <p>
     * Default implementation simply calls {@link #updateSelectedState}.
     */
    @Override
    public void componentDeselected() {
        updateSelectedState();
    }

    /**
     * Called when the document is about to be closed.
     * The default implementation makes an attempt to close this window by
     * calling {@code DocumentWindowManager.getDefault().closeWindow(this)}.
     */
    @Override
    public void documentClosing() {
        DocumentWindowManager.getDefault().closeWindow(this);
    }

    /**
     * Does anything that indicates the selected state of this component. The default implementation
     * simply calls {@link #repaint()}.
     */
    protected void updateSelectedState() {
        repaint();
    }
}
