/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.util;

import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

// See https://blogs.oracle.com/geertjan/entry/opening_a_topcomponent_per_node
// See https://blogs.oracle.com/geertjan/entry/loosely_coupled_open_action
// See https://blogs.oracle.com/geertjan/entry/loosely_coupled_open_action_part

/**
 * A {@code TopComponent} designed to serve as an editor for a "document" object.
 * When added to a {@link WorkspaceTopComponent} its
 * <ol>
 * <li>{@link #componentOpened()}/{@link #componentClosed()} method will be called if the corresponding internal frame is activated/deactivated;</li>
 * <li>{@link #componentActivated()}/{@link #componentDeactivated()} method will be called if the corresponding internal frame is activated/deactivated;</li>
 * <li>{@link #componentShowing()}/{@link #componentHidden()} method will be called if the corresponding internal frame is iconified/deiconified.</li>
 * </ol>
 * <p/>
 * Document windows keep a constant reference to the document object which it exposes through the window's lookup.
 * Overrides may use the {@link #getDynamicContent() content} to alter the objects in the exposed lookup,
 * however, the document object will always remain in it.
 * <p/>
 *
 * @author Norman Fomferra
 */
public class DocumentTopComponent<T> extends TopComponent implements DocumentWindow, NotifiableComponent {
    private static final Logger LOG = Logger.getLogger(DocumentTopComponent.class.getName());

    private final T document;
    private final InstanceContent dynamicContent;

    public DocumentTopComponent(T document) {
        this.document = document;
        this.dynamicContent = new InstanceContent();
        associateLookup(new ProxyLookup(Lookups.fixed(document), new AbstractLookup(dynamicContent)));
    }

    protected InstanceContent getDynamicContent() {
        return dynamicContent;
    }

    @Override
    public final T getDocument() {
        return document;
    }

    @Override
    public final TopComponent getTopComponent() {
        return this;
    }

    @Override
    public final boolean isSelected() {
        return DocumentWindowManager.getDefault().getSelectedDocumentWindow() == this;
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
     *
     * Default implementation simply calls {@link #updateSelectedState}.
     */
    @Override
    public void componentSelected() {
        updateSelectedState();
    }

    /**
     * Called when this component was deselected.
     *
     * Default implementation simply calls {@link #updateSelectedState}.
     */
    @Override
    public void componentDeselected() {
        updateSelectedState();
    }

    /**
     * Does anything that indicates the selected state of this component. The default implementation
     * simply calls {@link #repaint()}.
     */
    protected void updateSelectedState() {
        repaint();
    }
}
