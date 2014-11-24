/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.window;

import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * <p>
 * Document windows keep a constant reference to the document object which it exposes through the window's lookup.
 * Overrides may use the {@link #getDynamicContent() content} to alter the objects in the exposed lookup,
 * however, the document object will always remain in it.
 * <p>
 *
 * @author Norman
 */
public class DocumentWindow<T> extends TopComponent {

    private final T document;
    private final InstanceContent dynamicContent;

    public DocumentWindow(T document) {
        this.document = document;
        this.dynamicContent = new InstanceContent();
        associateLookup(new ProxyLookup(Lookups.fixed(document), new AbstractLookup(dynamicContent)));
    }

    public T getDocument() {
        return document;
    }

    protected InstanceContent getDynamicContent() {
        return dynamicContent;
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
    public List<Mode> availableModes(List<Mode> modes) {
        return Arrays.asList(WindowManager.getDefault().findMode("editor"));
    }

    @Override
    protected void componentOpened() {
    }

    @Override
    protected void componentClosed() {
    }

    @Override
    protected void componentShowing() {
    }

    @Override
    protected void componentHidden() {
    }

    @Override
    protected void componentActivated() {
    }

    @Override
    protected void componentDeactivated() {
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }

    /**
     * Gets a unique window title.
     *
     * @param titleBase The title base.
     * @return A unique window title.
     */
    public static String getUniqueEditorTitle(String titleBase) {

        List<String> titles = WorkspaceTopComponent.visitOpenWindows(TopComponent::getDisplayName);

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

}
