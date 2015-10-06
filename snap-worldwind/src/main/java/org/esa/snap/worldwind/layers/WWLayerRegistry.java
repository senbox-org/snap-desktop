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
import org.esa.snap.core.util.SystemUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A <code>WWLayerRegistry</code> provides access to WorldWind Layers as described by their WWLayerDescriptor.
 */
public class WWLayerRegistry {

    private static WWLayerRegistry instance = null;
    private final Map<String, WWLayerDescriptor> wwLayerDescriptors = new HashMap<>();

    public WWLayerRegistry() {
        registerWWLayers();
    }

    public static WWLayerRegistry getInstance() {
        if (instance == null) {
            instance = new WWLayerRegistry();
        }
        return instance;
    }

    public WWLayerDescriptor[] getWWLayerDescriptors() {
        return wwLayerDescriptors.values().toArray(new WWLayerDescriptor[wwLayerDescriptors.values().size()]);
    }

    private void registerWWLayers() {
        final FileObject fileObj = FileUtil.getConfigFile("WorldWindLayers");
        if (fileObj == null) {
            SystemUtils.LOG.warning("No World Wind layers found.");
            return;
        }
        final FileObject[] files = fileObj.getChildren();
        final List<FileObject> orderedFiles = FileUtil.getOrder(Arrays.asList(files), true);
        for (FileObject file : orderedFiles) {
            WWLayerDescriptor WWLayerDescriptor = null;
            try {
                WWLayerDescriptor = createWWLayerDescriptor(file);
            } catch (Exception e) {
                SystemUtils.LOG.severe(String.format("Failed to create WWLayerDescriptor from layer.xml path '%s'", file.getPath()));
            }
            if (WWLayerDescriptor != null) {
                final WWLayerDescriptor existingDescriptor = wwLayerDescriptors.get(WWLayerDescriptor.getId());
                if (existingDescriptor != null) {
                    SystemUtils.LOG.warning(String.format("WWLayer [%s] has been redeclared!\n",
                                                       WWLayerDescriptor.getId()));
                }

                wwLayerDescriptors.put(WWLayerDescriptor.getId(), WWLayerDescriptor);

                SystemUtils.LOG.fine(String.format("New WWLayer added from layer.xml path '%s': %s",
                                                   file.getPath(), WWLayerDescriptor.getId()));
            }
        }
    }

    public static WWLayerDescriptor createWWLayerDescriptor(final FileObject fileObject) {
        final String id = fileObject.getName();
        final String showInWorldMapToolView = (String) fileObject.getAttribute("showInWorldMapToolView");
        final String showIn3DToolView = (String) fileObject.getAttribute("showIn3DToolView");
        final Class<? extends WWLayer> WWLayerClass = getClassAttribute(fileObject, "WWLayerClass", WWLayer.class, false);

        Assert.argument(showInWorldMapToolView != null &&
                                (showInWorldMapToolView.equalsIgnoreCase("true") || showInWorldMapToolView.equalsIgnoreCase("false")),
                        "Missing attribute 'showInWorldMapToolView'");
        Assert.argument(showIn3DToolView != null &&
                                (showIn3DToolView.equalsIgnoreCase("true") || showIn3DToolView.equalsIgnoreCase("false")),
                        "Missing attribute 'showIn3DToolView'");
        Assert.argument(WWLayerClass != null, "Attribute 'class' must be provided");

        return new DefaultWWLayerDescriptor(id, Boolean.parseBoolean(showInWorldMapToolView),
                                            Boolean.parseBoolean(showIn3DToolView), WWLayerClass);
    }

    public static <T> Class<T> getClassAttribute(final FileObject fileObject,
                                                 final String attributeName,
                                                 final Class<T> expectedType,
                                                 final boolean required) {
        final String className = (String) fileObject.getAttribute(attributeName);
        if (className == null || className.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException(String.format("Missing attribute '%s' of type %s",
                                                                 attributeName, expectedType.getName()));
            }
            return null;
        }

        final Collection<? extends ModuleInfo> modules = Lookup.getDefault().lookupAll(ModuleInfo.class);
        for (ModuleInfo module : modules) {
            if (module.isEnabled()) {
                try {
                    final Class<?> implClass = module.getClassLoader().loadClass(className);
                    if (expectedType.isAssignableFrom(implClass)) {
                        //noinspection unchecked
                        return (Class<T>) implClass;
                    } else {
                        throw new IllegalArgumentException(String.format("Value %s of attribute '%s' must be a %s",
                                                                         implClass.getName(),
                                                                         attributeName,
                                                                         expectedType.getName()));
                    }
                } catch (ClassNotFoundException e) {
                    // it's ok, continue
                }
            }
        }
        return null;
    }
}
