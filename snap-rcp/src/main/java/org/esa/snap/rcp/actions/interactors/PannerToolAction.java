/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.actions.interactors;

import com.bc.ceres.swing.figure.interactions.PanInteractor;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Interactors",
        id = "org.esa.snap.rcp.action.interactors.PannerToolAction"
)
@ActionRegistration(
        displayName = "#CTL_PannerToolActionText",
        lazy = false
)
@ActionReference(
        path = "Toolbars/" + PackageDefaults.PANNER_TOOL_TOOLBAR_NAME,
        position = PackageDefaults.PANNER_TOOL_TOOLBAR_POSITION
)
@Messages({
                  "CTL_PannerToolActionText=" + PackageDefaults.PANNER_TOOL_NAME,
                  "CTL_PannerToolActionDescription=Panning tool"
          })
public class PannerToolAction extends ToolAction {
    @SuppressWarnings("UnusedDeclaration")
    public PannerToolAction() {
        this(null);
    }

    public PannerToolAction(Lookup lookup) {
        super(lookup, new PanInteractor());
        putValue(NAME, Bundle.CTL_PannerToolActionText());
        putValue(SELECTED_KEY, true);   //Set Image Panning Tool as default
        putValue(SHORT_DESCRIPTION, Bundle.CTL_PannerToolActionDescription());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.PANNER_TOOL_ICON, false));
    }

    @Override
    public HelpCtx getHelpCtx() {
        // TODO: Make sure help page is available for ID
        return new HelpCtx("panTool");
    }
}