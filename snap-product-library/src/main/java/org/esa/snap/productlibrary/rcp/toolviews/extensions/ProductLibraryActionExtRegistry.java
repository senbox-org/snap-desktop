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
package org.esa.snap.productlibrary.rcp.toolviews.extensions;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIRegistry;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import java.util.*;

/**
 * A <code>ProductLibraryActionExtRegistry</code> provides access to action extensions as described by the ProductLibraryActionExtDescriptor.
 */
public class ProductLibraryActionExtRegistry {

    private static ProductLibraryActionExtRegistry instance = null;
    private final Map<String, ProductLibraryActionExtDescriptor> actionExtDescriptors = new HashMap<>();

    private static Comparator<ProductLibraryActionExtDescriptor> descriptorComparator
            = (a, b) -> a.getPosition() - b.getPosition();

    private ProductLibraryActionExtRegistry() {
        registerActions();
    }

    public static ProductLibraryActionExtRegistry getInstance() {
        if (instance == null) {
            instance = new ProductLibraryActionExtRegistry();
        }
        return instance;
    }

    public ProductLibraryActionExtDescriptor[] getDescriptors() {
        final List<ProductLibraryActionExtDescriptor> values = new ArrayList<>(actionExtDescriptors.values());
        values.sort(descriptorComparator);
        return values.toArray(new ProductLibraryActionExtDescriptor[values.size()]);
    }

    private void registerActions() {
        final FileObject fileObj = FileUtil.getConfigFile("ProductLibraryActions");
        if (fileObj == null) {
            SystemUtils.LOG.warning("No ProductLibrary Action found.");
            return;
        }
        final FileObject[] files = fileObj.getChildren();
        final List<FileObject> orderedFiles = FileUtil.getOrder(Arrays.asList(files), true);
        for (FileObject file : orderedFiles) {
            ProductLibraryActionExtDescriptor actionExtDescriptor = null;
            try {
                actionExtDescriptor = createDescriptor(file);
            } catch (Exception e) {
                SystemUtils.LOG.severe(String.format("Failed to create ProductLibrary action from layer.xml path '%s'", file.getPath()));
            }
            if (actionExtDescriptor != null) {
                if(!actionExtDescriptor.isSeperator()) {
                    final ProductLibraryActionExtDescriptor existingDescriptor = actionExtDescriptors.get(actionExtDescriptor.getId());
                    if (existingDescriptor != null) {
                        SystemUtils.LOG.warning(String.format("ProductLibrary action [%s] has been redeclared!\n",
                                actionExtDescriptor.getId()));
                    }
                }

                actionExtDescriptors.put(actionExtDescriptor.getId(), actionExtDescriptor);
                SystemUtils.LOG.fine(String.format("New ProductLibrary action added from layer.xml path '%s': %s",
                        file.getPath(), actionExtDescriptor.getId()));
            }
        }
    }

    private static ProductLibraryActionExtDescriptor createDescriptor(FileObject fileObject) {
        final String id = fileObject.getName();
        final Integer position = (Integer) fileObject.getAttribute("position");

        Assert.argument(position != null, "Attribute 'position' must be provided");

        if(id.equals("Separator")) {
            return new ProductLibraryActionExtDescriptor(id, null, position);
        }

        final Class<? extends ProductLibraryActionExt> actionExtClass =
                OperatorUIRegistry.getClassAttribute(fileObject, "actionExtClass", ProductLibraryActionExt.class, false);

        Assert.argument(actionExtClass != null, "Attribute 'actionExtClass' must be provided");

        return new ProductLibraryActionExtDescriptor(id, actionExtClass, position);
    }
}
