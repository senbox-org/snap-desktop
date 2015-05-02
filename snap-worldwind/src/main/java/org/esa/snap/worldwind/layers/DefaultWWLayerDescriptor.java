/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.worldwind.layers;

import com.bc.ceres.core.Assert;

/**
 * Provides a standard implementation for {@link WWLayerDescriptor}.
 */
public class DefaultWWLayerDescriptor implements WWLayerDescriptor {

    private String id;
    private boolean showInWorldMapToolView;
    private boolean showIn3DToolView;
    private Class<? extends WWLayer> WWLayerClass;

    public DefaultWWLayerDescriptor(final String id,
                                    final boolean showInWorldMapToolView, final boolean showIn3DToolView,
                                    final Class<? extends WWLayer> WWLayerClass) {
        this.id = id;
        this.showInWorldMapToolView = showInWorldMapToolView;
        this.showIn3DToolView = showIn3DToolView;
        this.WWLayerClass = WWLayerClass;
    }

    public String getId() {
        return id;
    }

    public boolean showInWorldMapToolView() {
        return showInWorldMapToolView;
    }

    public boolean showIn3DToolView() {
        return showIn3DToolView;
    }

    public WWLayer createWWLayer() {
        Assert.state(WWLayerClass != null, "WWLayerClass != null");
        Object object;
        try {
            object = WWLayerClass.newInstance();
        } catch (Throwable e) {
            throw new IllegalStateException("WWLayerClass.newInstance()", e);
        }
        Assert.state(object instanceof WWLayer, "object instanceof WWLayer");
        return (WWLayer) object;
    }
}
