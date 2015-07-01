/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.netbeans.tile;

import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

/**
 * The abstract base class for actions that can arrange editor windows in tiles.
 * Uses either a {@link Tileable} looked-up from the global
 * or, if there is no such, uses the default from {@link Tileable#getDefault()}.
 * The action's enablement is based on the return value of {@link Tileable#canTile()}.
 *
 * @author Norman Fomferra
 * @since 1.0
 */
public abstract class TileAction extends AbstractAction implements
                                                        ContextAwareAction, LookupListener, PropertyChangeListener, Presenter.Menu {

    private Lookup.Result<Tileable> tileableResult;
    private final Tileable defaultTileable;

    protected TileAction(Lookup actionContext) {
        defaultTileable = Tileable.getDefault();
        tileableResult = actionContext.lookupResult(Tileable.class);
        tileableResult.addLookupListener(WeakListeners.create(LookupListener.class, this, tileableResult));
        TopComponent.Registry registry = WindowManager.getDefault().getRegistry();
        registry.addPropertyChangeListener(WeakListeners.propertyChange(this, registry));
        updateState();
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        updateState();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        boolean windowCountChanged = TopComponent.Registry.PROP_TC_CLOSED.equals(propertyName)
                || TopComponent.Registry.PROP_TC_OPENED.equals(propertyName);
        if (windowCountChanged) {
            updateState();
        }
    }

    protected Tileable getTileable() {
        Collection<? extends Tileable> tileables = tileableResult.allInstances();
        if (!tileables.isEmpty()) {
            return tileables.iterator().next();
        } else {
            return defaultTileable;
        }
    }

    private void updateState() {
        setEnabled(getTileable().canTile());
    }

    @Override
    public JMenuItem getMenuPresenter() {
        final JMenuItem jMenuItem = new JMenuItem(this);
        jMenuItem.setIcon(null);
        return jMenuItem;
    }

}
