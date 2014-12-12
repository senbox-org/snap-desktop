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
package org.esa.snap.gui.actions.window;

import org.esa.snap.gui.util.Tileable;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

/**
 * The abstract base class for actions that tile windows.
 *
 * @author Norman Fomferra
 */
public abstract class TileAction extends AbstractAction implements ContextAwareAction, LookupListener, PropertyChangeListener {

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

    protected Tileable getTileable() {
        Collection<? extends Tileable> tileables = tileableResult.allInstances();
        if (!tileables.isEmpty()) {
            return tileables.toArray(new Tileable[tileables.size()])[0];
        } else {
            return defaultTileable;
        }
    }

    private void updateState() {
        setEnabled(getTileable().canTile());
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
}
