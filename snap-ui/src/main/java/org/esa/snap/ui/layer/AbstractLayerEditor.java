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

package org.esa.snap.ui.layer;

import com.bc.ceres.glayer.Layer;

import javax.swing.JComponent;

/**
 * Base class for layer editors.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public abstract class AbstractLayerEditor implements LayerEditor {

    private Layer currentLayer;

    protected AbstractLayerEditor() {
    }

    @Override
    public final JComponent createControl(Layer layer) {
        this.currentLayer = layer;
        return createControl();
    }

    @Override
    public void handleEditorAttached() {
    }

    @Override
    public void handleEditorDetached() {
    }

    @Override
    public void handleLayerContentChanged() {
    }

    /**
     * Creates the editor control for this editor.
     *
     * @return The editor control.
     */
    protected abstract JComponent createControl();

    /**
     * @return The current layer.
     */
    protected Layer getCurrentLayer() {
        return currentLayer;
    }
}
