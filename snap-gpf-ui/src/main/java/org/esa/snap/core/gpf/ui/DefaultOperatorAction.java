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

package org.esa.snap.core.gpf.ui;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * <p><b>WARNING:</b> This class belongs to a preliminary API and may change in future releases.<p>
 * <p>An action which creates a default dialog for an operator given by the
 * action property action property {@code operatorName}.</p>
 * <p>Optionally the dialog title can be set via the {@code dialogTitle} property and
 * the ID of the help page can be given using the {@code helpId} property. If not given the
 * name of the operator will be used instead. Also optional the
 * file name suffix for the target product can be given via the {@code targetProductNameSuffix} property.</p>
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 */
public class DefaultOperatorAction extends AbstractSnapAction {

    private static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList("displayName", "operatorName", "dialogTitle", "helpId", "targetProductNameSuffix"));

    private ModelessDialog dialog;

    public static DefaultOperatorAction create(Map<String, Object> properties) {
        DefaultOperatorAction action = new DefaultOperatorAction();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (KNOWN_KEYS.contains(entry.getKey())) {
                action.putValue(entry.getKey(), entry.getValue());
            }
        }
        return action;
    }

    public String getOperatorName() {
        Object value = getValue("operatorName");
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public void setOperatorName(String operatorName) {
        putValue("operatorName", operatorName);
    }

    public String getDialogTitle() {
        Object value = getValue("dialogTitle");
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public void setDialogTitle(String dialogTitle) {
        putValue("dialogTitle", dialogTitle);
    }

    public String getTargetProductNameSuffix() {
        Object value = getValue("targetProductNameSuffix");
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public void setTargetProductNameSuffix(String targetProductNameSuffix) {
        putValue("targetProductNameSuffix", targetProductNameSuffix);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = createOperatorDialog();
        }
        dialog.show();
    }

    protected ModelessDialog createOperatorDialog() {
        DefaultSingleTargetProductDialog productDialog = new DefaultSingleTargetProductDialog(getOperatorName(), getAppContext(),
                                                                                              getDialogTitle(), getHelpId());
        if (getTargetProductNameSuffix() != null) {
            productDialog.setTargetProductNameSuffix(getTargetProductNameSuffix());
        }
        return productDialog;
    }
}
