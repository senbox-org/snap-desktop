/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.ui;

import com.bc.ceres.swing.selection.SelectionContext;

import javax.swing.JPanel;

/**
 * The base class for application view panes. It provides support for a context menu and command handling.
 *
 * @see PopupMenuFactory
 */
public abstract class BasicView extends JPanel implements PopupMenuFactory, Disposable {

    /**
     * Creates a new <code>BasicView</code> with a double buffer and a flow layout.
     */
    public BasicView() {
    }

    /**
     * Releases all of the resources used by this view, its subcomponents, and all of its owned children.
     */
    public void dispose() {
    }

    /**
     * Gets the current selection context, if any.
     *
     * @return The current selection context, or {@code null} if none exists.
     * @since BEAM 4.7
     */
    public SelectionContext getSelectionContext() {
        return null;
    }
}
