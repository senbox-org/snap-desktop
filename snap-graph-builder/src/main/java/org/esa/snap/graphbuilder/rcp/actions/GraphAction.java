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

import org.esa.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog;
import org.esa.snap.ui.ModelessDialog;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

/**
 * <p>An action which creates a graph builder dialog for a graph given by the
 * action property action property {@code graphFile}.</p>
 * <p>Optionally the dialog title can be set via the {@code dialogTitle} property and
 * the ID of the help page can be given using the {@code helpId} property. If not given the
 * name of the operator will be used instead. Also optional the
 * file name suffix for the target product can be given via the {@code targetProductNameSuffix} property.</p>
 */
public class GraphAction extends OperatorAction {

    static {
        KNOWN_KEYS.addAll(Arrays.asList("graphFile", "enableEditing"));
    }

    public static GraphAction create(Map<String, Object> properties) {
        GraphAction action = new GraphAction();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (KNOWN_KEYS.contains(entry.getKey())) {
                action.putValue(entry.getKey(), entry.getValue());
            }
        }
        return action;
    }

    public String getGraphFileName() {
        return getPropertyString("graphFile");
    }

    public boolean isEditingEnabled() {
        final String enableEditingStr = getPropertyString("enableEditing");
        return enableEditingStr != null && enableEditingStr.equalsIgnoreCase("true");
    }

    @Override
    protected ModelessDialog createOperatorDialog() {
        setHelpId(getPropertyString("helpId"));

        final GraphBuilderDialog dialog = new GraphBuilderDialog(getAppContext(), getDialogTitle(), getHelpId(), isEditingEnabled());
        dialog.show();

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        final File graphFile = new File(graphPath, getGraphFileName());

        addIcon(dialog);
        dialog.LoadGraph(graphFile);
        return dialog;
    }
}
