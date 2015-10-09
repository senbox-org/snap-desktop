/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.rcp.actions;

import org.esa.snap.core.gpf.ui.DefaultOperatorAction;
import org.esa.snap.graphbuilder.rcp.dialogs.SingleOperatorDialog;
import org.esa.snap.graphbuilder.rcp.utils.IconUtils;
import org.esa.snap.ui.ModelessDialog;

import javax.swing.ImageIcon;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>An action which creates a default dialog for an operator given by the
 * action property action property {@code operatorName}.</p>
 * <p>Optionally the dialog title can be set via the {@code dialogTitle} property and
 * the ID of the help page can be given using the {@code helpId} property. If not given the
 * name of the operator will be used instead. Also optional the
 * file name suffix for the target product can be given via the {@code targetProductNameSuffix} property.</p>
 */
public class OperatorAction extends DefaultOperatorAction {
    protected static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList("displayName", "operatorName",
                                                                                "dialogTitle", "targetProductNameSuffix",
                                                                                "helpId", "icon"));

    private ModelessDialog dialog;

    public static OperatorAction create(Map<String, Object> properties) {
        OperatorAction action = new OperatorAction();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (KNOWN_KEYS.contains(entry.getKey())) {
                action.putValue(entry.getKey(), entry.getValue());
            }
        }
        return action;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        ModelessDialog dialog = createOperatorDialog();
        dialog.show();
    }

    public String getPropertyString(final String key) {
        Object value = getValue(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public String getIcon() {
        return getPropertyString("icon");
    }

    protected ModelessDialog createOperatorDialog() {
        setHelpId(getPropertyString("helpId"));

        final SingleOperatorDialog productDialog = new SingleOperatorDialog(getOperatorName(), getAppContext(),
                                                                            getDialogTitle(), getHelpId());
        if (getTargetProductNameSuffix() != null) {
            productDialog.setTargetProductNameSuffix(getTargetProductNameSuffix());
        }
        addIcon(productDialog);
        return productDialog;
    }

    protected void addIcon(final ModelessDialog dlg) {
        String iconName = getIcon();
        if (iconName == null) {
            //setIcon(dlg, IconUtils.esaPlanetIcon);
        } else if (iconName.equals("esaIcon")) {
            setIcon(dlg, IconUtils.esaPlanetIcon);
        } else if (iconName.equals("rstbIcon")) {
            setIcon(dlg, IconUtils.rstbIcon);
        } else if (iconName.equals("geoAusIcon")) {
            setIcon(dlg, IconUtils.geoAusIcon);
        } else {
            final ImageIcon icon = IconUtils.LoadIcon(iconName);
            if (icon != null)
                setIcon(dlg, icon);
        }
    }

    private static void setIcon(final ModelessDialog dlg, final ImageIcon ico) {
        if (ico == null) return;
        dlg.getJDialog().setIconImage(ico.getImage());
    }
}
