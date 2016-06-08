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
package org.esa.snap.graphbuilder.rcp.wizards;

import org.esa.snap.graphbuilder.rcp.actions.OperatorAction;
import org.esa.snap.rcp.SnapApp;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
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
public class WizardAction extends OperatorAction {
    protected static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList("wizardPanelClass"));
    static {
        KNOWN_KEYS.addAll(OperatorAction.KNOWN_KEYS);
    }

    public String getWizardPanelClass() {
        return getPropertyString("wizardPanelClass");
    }

    public static OperatorAction create(Map<String, Object> properties) {
        WizardAction action = new WizardAction();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (KNOWN_KEYS.contains(entry.getKey())) {
                action.putValue(entry.getKey(), entry.getValue());
            }
        }
        return action;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            Class<?> wizardClass = getClass(getWizardPanelClass());
            final WizardPanel wizardPanel = (WizardPanel) wizardClass.newInstance();

            final WizardDialog dialog = new WizardDialog(SnapApp.getDefault().getMainFrame(), false,
                                                         getDialogTitle(), getHelpId(), wizardPanel);
            dialog.setVisible(true);
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to create wizard", e);
        }
    }

    private static Class<?> getClass(String className) {
        Collection<? extends ModuleInfo> modules = Lookup.getDefault().lookupAll(ModuleInfo.class);
        for (ModuleInfo module : modules) {
            if (module.isEnabled()) {
                try {
                    Class<?> implClass = module.getClassLoader().loadClass(className);
                    if (WizardPanel.class.isAssignableFrom(implClass)) {
                        //noinspection unchecked
                        return (Class<?>) implClass;
                    }
                } catch (ClassNotFoundException e) {
                    // it's ok, continue
                }
            }
        }
        return null;
    }
}
