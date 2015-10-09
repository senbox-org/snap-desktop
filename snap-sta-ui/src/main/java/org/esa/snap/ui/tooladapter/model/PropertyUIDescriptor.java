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
package org.esa.snap.ui.tooladapter.model;

import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolParameterDescriptor;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.HashMap;

/**
 * @author Ramona Manda
 */
public class PropertyUIDescriptor {

    private AbstractButton delButton;
    private AbstractButton editButton;

    private HashMap<String, PropertyMemberUIWrapper> UIcomponentsMap;

    public static PropertyUIDescriptor buildUIMinimalDescriptor(ToolParameterDescriptor parameter, String property, ToolAdapterOperatorDescriptor operator, BindingContext context, ActionListener deleteActionListener, ActionListener editActionListener, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        PropertyUIDescriptor descriptor = new PropertyUIDescriptor();

        AbstractButton delButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/DeleteShapeTool16.gif"),
                false);
        delButton.addActionListener(deleteActionListener);
        descriptor.setDelButton(delButton);
        AbstractButton editButton = new JButton("...");
        editButton.addActionListener(editActionListener);
        descriptor.setEditButton(editButton);

        HashMap<String, PropertyMemberUIWrapper> UIcomponentsMap = new HashMap<>();
        UIcomponentsMap.put(property, PropertyMemberUIWrapperFactory.buildPropertyWrapper(property, parameter, operator, context, callback));
        descriptor.setUIcomponentsMap(UIcomponentsMap);

        return descriptor;
    }

    public static PropertyUIDescriptor buildUIDescriptor(ToolParameterDescriptor prop, String[] columnsMembers, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context, ActionListener deleteActionListener, ActionListener editActionListener, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        PropertyUIDescriptor descriptor = new PropertyUIDescriptor();

        AbstractButton delButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/DeleteShapeTool16.gif"),
                false);
        delButton.addActionListener(deleteActionListener);
        descriptor.setDelButton(delButton);
        AbstractButton editButton = new JButton("...");
        editButton.addActionListener(editActionListener);
        descriptor.setEditButton(editButton);

        HashMap<String, PropertyMemberUIWrapper> UIcomponentsMap = new HashMap<>();
        for (String col : columnsMembers) {
            if (!col.equals("del")) {
                UIcomponentsMap.put(col, PropertyMemberUIWrapperFactory.buildPropertyWrapper(col, prop, opDescriptor, context, callback));
            }
        }
        descriptor.setUIcomponentsMap(UIcomponentsMap);

        return descriptor;
    }

    public void fillDescriptor(String[] columnsMembers) {
        for (String col : columnsMembers) {
            if (!col.equals("del")) {
                //UIcomponentsMap.put(col, PropertyMemberUIWrapperFactory.buildPropertyWrapper(col, prop, context, callback));
            }
        }
    }

    public void setAttributeEditCallback(String attributeName, PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        UIcomponentsMap.get(attributeName).setCallback(callback);
    }

    public void setEditCallback(PropertyMemberUIWrapper.CallBackAfterEdit callback) {
        while (UIcomponentsMap.keySet().iterator().hasNext()) {
            UIcomponentsMap.get(UIcomponentsMap.keySet().iterator().next()).setCallback(callback);
        }
    }

    public AbstractButton getDelButton() {
        return delButton;
    }

    public AbstractButton getEditButton() {
        return editButton;
    }

    public HashMap<String, PropertyMemberUIWrapper> getUIcomponentsMap() {
        return UIcomponentsMap;
    }

    public void setDelButton(AbstractButton delButton) {
        this.delButton = delButton;
    }

    public void setEditButton(AbstractButton editButton) {
        this.editButton = editButton;
    }

    public void setUIcomponentsMap(HashMap<String, PropertyMemberUIWrapper> UIcomponentsMap) {
        this.UIcomponentsMap = UIcomponentsMap;
    }
}
