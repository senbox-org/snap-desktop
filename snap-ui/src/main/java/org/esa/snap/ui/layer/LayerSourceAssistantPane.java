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

import com.bc.ceres.glayer.LayerContext;
import org.esa.snap.ui.assistant.AssistantPane;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.util.Utilities;

import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

/**
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
public class LayerSourceAssistantPane extends AssistantPane implements LayerSourcePageContext {

    private final Map<String, Object> properties;
    private LayerSource layerSource;

    public LayerSourceAssistantPane(Window parent, String title) {
        super(parent, title);
        properties = new HashMap<String, Object>();
    }

    private ProductSceneView getSelectedProductSceneView() {
        return Utilities.actionsGlobalContext().lookup(ProductSceneView.class);
    }

    @Override
    public LayerContext getLayerContext() {
        return getSelectedProductSceneView().getSceneImage();
    }

    @Override
    public void setLayerSource(LayerSource layerSource) {
        this.layerSource = layerSource;
    }

    @Override
    public LayerSource getLayerSource() {
        return layerSource;
    }

    @Override
    public Object getPropertyValue(String key) {
        return properties.get(key);
    }

    @Override
    public void setPropertyValue(String key, Object value) {
        properties.put(key, value);
    }
}
