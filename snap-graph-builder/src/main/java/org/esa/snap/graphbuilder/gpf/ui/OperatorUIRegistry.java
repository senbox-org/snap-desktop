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
package org.esa.snap.graphbuilder.gpf.ui;

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
 * An <code>OperatorUIRegistry</code> provides access to operator user interfaces as described by their OperatorUIDescriptor.
 */
public class OperatorUIRegistry {

    private static OperatorUIRegistry instance = null;
    private final Map<String, OperatorUIDescriptor> operatorUIDescriptors = new HashMap<>();

    public OperatorUIRegistry() {
        registerOperatorUIs();
    }

    public static OperatorUIRegistry getInstance() {
        if(instance == null) {
            instance = new OperatorUIRegistry();
        }
        return instance;
    }

    public OperatorUIDescriptor[] getOperatorUIDescriptors() {
        return operatorUIDescriptors.values().toArray(new OperatorUIDescriptor[operatorUIDescriptors.values().size()]);
    }

    public OperatorUIDescriptor getOperatorUIDescriptor(final String operatorName) {
        return operatorUIDescriptors.get(operatorName);
    }

    private void registerOperatorUIs() {
        FileObject fileObj = FileUtil.getConfigFile("OperatorUIs");
        if(fileObj == null) {
            SystemUtils.LOG.warning("No operatorUIs found.");
            return;
        }
        final FileObject[] files = fileObj.getChildren();
        final List<FileObject> orderedFiles = FileUtil.getOrder(Arrays.asList(files), true);
        for (FileObject file : orderedFiles) {
            OperatorUIDescriptor operatorUIDescriptor = null;
            try {
                operatorUIDescriptor = createOperatorUIDescriptor(file);
            } catch (Exception e) {
                SystemUtils.LOG.severe(String.format("Failed to create operatorUI from layer.xml path '%s'", file.getPath()));
            }
            if (operatorUIDescriptor != null) {
                // must have only one operatorUI per operator
                final OperatorUIDescriptor existingDescriptor = operatorUIDescriptors.get(operatorUIDescriptor.getOperatorName());
                if (existingDescriptor != null) {
                    SystemUtils.LOG.info(String.format("OperatorUI [%s] has been redeclared for [%s]!\n",
                                                       operatorUIDescriptor.getId(), operatorUIDescriptor.getOperatorName()));
                }

                operatorUIDescriptors.put(operatorUIDescriptor.getOperatorName(), operatorUIDescriptor);
                SystemUtils.LOG.info(String.format("New operatorUI added from layer.xml path '%s': %s",
                                                   file.getPath(), operatorUIDescriptor.getOperatorName()));
            }
        }
    }

    public static OperatorUIDescriptor createOperatorUIDescriptor(FileObject fileObject) {
        final String id = fileObject.getName();

        final String operatorName = (String) fileObject.getAttribute("operatorName");
        Assert.argument(operatorName != null && !operatorName.isEmpty(), "Missing attribute 'operatorName'");

        final Class<? extends OperatorUI> operatorUIClass = getClassAttribute(fileObject, "operatorUIClass", OperatorUI.class, false);

        Boolean disableFromGraphBuilder = false;
        try {
            final String disableFromGraphBuilderStr = (String) fileObject.getAttribute("disableFromGraphBuilder");
            if (disableFromGraphBuilderStr != null) {
                disableFromGraphBuilder = Boolean.parseBoolean(disableFromGraphBuilderStr);
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("OperatorUIRegistry: Unable to parse disableFromGraphBuilder "+e.toString());
            //continue
        }

        return new DefaultOperatorUIDescriptor(id, operatorName, operatorUIClass, disableFromGraphBuilder);
    }

    public static OperatorUI CreateOperatorUI(final String operatorName) {

        final OperatorUIRegistry reg = OperatorUIRegistry.getInstance();
        if (reg != null) {
            OperatorUIDescriptor desc = reg.getOperatorUIDescriptor(operatorName);
            if (desc != null) {
                return desc.createOperatorUI();
            }
            desc = OperatorUIRegistry.getInstance().getOperatorUIDescriptor("DefaultUI");
            if (desc != null) {
                return desc.createOperatorUI();
            }
        }
        return new DefaultUI();
    }

    public static boolean showInGraphBuilder(final String operatorName) {
        final OperatorUIRegistry reg = OperatorUIRegistry.getInstance();
        if (reg != null) {
            OperatorUIDescriptor desc = reg.getOperatorUIDescriptor(operatorName);
            if (desc != null) {
                if(desc.disableFromGraphBuilder()) {
                    SystemUtils.LOG.warning(operatorName + " disabled from GraphBuilder");
                }
                return !desc.disableFromGraphBuilder();
            }
        }
        return true;
    }

    public static <T> Class<T> getClassAttribute(FileObject fileObject,
                                                 String attributeName,
                                                 Class<T> expectedType,
                                                 boolean required) {
        String className = (String) fileObject.getAttribute(attributeName);
        if (className == null || className.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException(String.format("Missing attribute '%s' of type %s",
                                                                 attributeName, expectedType.getName()));
            }
            return null;
        }

        Collection<? extends ModuleInfo> modules = Lookup.getDefault().lookupAll(ModuleInfo.class);
        for (ModuleInfo module : modules) {
            if (module.isEnabled()) {
                try {
                    Class<?> implClass = module.getClassLoader().loadClass(className);
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
