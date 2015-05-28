/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.ui.tooladapter.actions;

import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.OperatorSpiRegistry;
import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterOpSpi;
import org.esa.snap.rcp.SnapDialogs;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.OnStart;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for creating menu entries for tool adapter operators.
 * The inner runnable class should be invoked when the IDE starts, and will
 * register the available adapters as menu actions.
 *
 * @author Cosmin Cara
 */

public class ToolAdapterActionRegistrar {

    private static final String DEFAULT_MENU_PATH = "Menu/Tools/External Tools";

    private static final Map<String, ToolAdapterOperatorDescriptor> actionMap = new HashMap<>();

    static {
        try {
        FileObject defaultMenu = FileUtil.getConfigFile(DEFAULT_MENU_PATH);
            if (defaultMenu == null) {
                defaultMenu = FileUtil.getConfigFile("Menu").createFolder(DEFAULT_MENU_PATH.replace("Menu/", ""));
            }
            defaultMenu.setAttribute("position", 9999);
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns the map of menu items (actions) and operator descriptors.
     *
     * @return
     */
    public static Map<String, ToolAdapterOperatorDescriptor> getActionMap() {
        return actionMap;
    }

    public static String getDefaultMenuLocation() {
        return DEFAULT_MENU_PATH;
    }

    /**
     * Creates a menu entry in the default menu location (Tools > External Tools) for the given adapter operator.
     *
     * @param operator  The operator descriptor
     */
    public static void registerOperatorMenu(ToolAdapterOperatorDescriptor operator) {
        String menuGroup = operator.getMenuLocation();
        if (menuGroup == null) {
            operator.setMenuLocation(DEFAULT_MENU_PATH);
        }
         registerOperatorMenu(operator, true);
    }

    /**
     * Creates a menu entry in the given menu location for the given adapter operator.
     *
     * @param operator  The operator descriptor
     * @param hasChanged      Flag that indicates if the descriptor has changed (true) or is new (false)
     */
    public static void registerOperatorMenu(ToolAdapterOperatorDescriptor operator, boolean hasChanged) {
        String menuLocation = operator.getMenuLocation();
        if (menuLocation == null) {
            menuLocation = getDefaultMenuLocation();
            operator.setMenuLocation(menuLocation);
        }
        FileObject menuFolder = FileUtil.getConfigFile(menuLocation);
        try {
            if (menuFolder == null) {
                FileObject root = FileUtil.getConfigFile("Menu");
                menuFolder = root.createFolder(menuLocation.replace("Menu/", ""));
                menuFolder.setAttribute("position", 9999);
            }
            /*FileObject groupItem = null;
            if (groupName != null) {
                groupItem = menuFolder.getFileObject(groupName);
                if (groupItem == null) {
                    groupItem = menuFolder.createFolder(groupName);
                    groupItem.setAttribute("position", 1001);
                }
            } else {
                groupItem = menuFolder;
            }*/
            String candidateMenuKey = operator.getAlias();
            /*FileObject newItem = groupItem.getFileObject(candidateMenuKey, "instance");
            if (newItem == null) {
                newItem = groupItem.createData(candidateMenuKey, "instance");
            }*/
            FileObject newItem = menuFolder.getFileObject(candidateMenuKey, "instance");
            if (newItem == null) {
                newItem = menuFolder.createData(candidateMenuKey, "instance");
            }
            ExecuteToolAdapterAction action = new ExecuteToolAdapterAction(candidateMenuKey);
            newItem.setAttribute("instanceCreate", action);
            newItem.setAttribute("instanceClass", action.getClass().getName());
            if (actionMap.containsKey(candidateMenuKey)) {
                actionMap.remove(candidateMenuKey);
            }
            actionMap.put(candidateMenuKey, operator);
        } catch (IOException e) {
            SnapDialogs.showError("Error:" + e.getMessage());
        }
    }

    public static void removeOperatorMenu(ToolAdapterOperatorDescriptor operator) {
        if (!operator.isSystem()) {
            FileObject menuFolder = FileUtil.getConfigFile(operator.getMenuLocation());
            try {
                /*FileObject groupItem = groupName != null ? menuFolder.getFileObject(groupName) : menuFolder;
                if (groupItem != null) {*/
                if (menuFolder != null) {
                    String operatorAlias = operator.getAlias();
                    FileObject newItem = menuFolder.getFileObject(operatorAlias, "instance");
                    if (newItem != null) {
                        newItem.delete();
                    }
                    if (actionMap.containsKey(operatorAlias)) {
                        actionMap.remove(operatorAlias);
                    }
                }
            } catch (IOException e) {
                SnapDialogs.showError("Error:" + e.getMessage());
            }
        }
    }

    /**
     * Startup class that performs menu initialization to be invoked by NetBeans.
     */
    @OnStart
    public static class StartOp implements Runnable {
        @Override
        public void run() {
            OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
            if (spiRegistry != null) {
                Collection<OperatorSpi> operatorSpis = spiRegistry.getOperatorSpis();
                if (operatorSpis != null) {
                    if (operatorSpis.size() == 0) {
                        operatorSpis.addAll(ToolAdapterIO.searchAndRegisterAdapters());
                    }
                    operatorSpis.stream().filter(spi -> spi instanceof ToolAdapterOpSpi).forEach(spi -> {
                        ToolAdapterOperatorDescriptor operatorDescriptor = (ToolAdapterOperatorDescriptor) spi.getOperatorDescriptor();
                        registerOperatorMenu(operatorDescriptor, false);
                    });
                }
            }
        }
    }
}
