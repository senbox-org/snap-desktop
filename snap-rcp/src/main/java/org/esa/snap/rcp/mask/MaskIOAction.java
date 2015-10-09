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

package org.esa.snap.rcp.mask;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.openide.windows.TopComponent;

import javax.swing.JOptionPane;
import java.io.File;

abstract class MaskIOAction extends MaskAction {
    private final TopComponent maskTopComponent;

    public MaskIOAction(TopComponent maskTopComponent, MaskForm maskForm, String iconPath, String buttonName,
                        String description
    ) {
        super(maskForm, iconPath, buttonName, description);
        this.maskTopComponent = maskTopComponent;
    }

    void showErrorDialog(final String message) {
        SnapDialogs.showMessage(maskTopComponent.getDisplayName() + " - Error", message, JOptionPane.ERROR_MESSAGE, null);
    }

    void setDirectory(final File directory) {
        SnapApp.getDefault().getPreferences().put("mask.io.dir", directory.getPath());
    }

    File getDirectory() {
        File directory = SystemUtils.getUserHomeDir();
        return new File(SnapApp.getDefault().getPreferences().get("mask.io.dir", directory.getPath()));
    }
}
