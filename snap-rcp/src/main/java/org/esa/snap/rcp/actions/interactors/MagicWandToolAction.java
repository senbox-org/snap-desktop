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

import org.esa.snap.rcp.magicwand.MagicWandInteractor;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Interactors", id = "org.esa.snap.rcp.action.interactors.MagicWandToolAction")
@ActionRegistration(displayName = "#CTL_MagicWandToolActionText", lazy = false)
@ActionReference(
        path = "Toolbars/" + PackageDefaults.MAGIC_WAND_TOOLBAR_NAME,
        position = PackageDefaults.MAGIC_WAND_TOOLBAR_POSITION)
@Messages({
        "CTL_MagicWandToolActionText=" + PackageDefaults.MAGIC_WAND_NAME,
        "CTL_MagicWandToolActionDescription=Creates a ROI mask using a magic wand"
})
public class MagicWandToolAction extends ToolAction {
    @SuppressWarnings("UnusedDeclaration")
    public MagicWandToolAction() {
        this(null);
    }

    public MagicWandToolAction(Lookup lookup) {
        super(lookup, new MagicWandInteractor());
        putValue(NAME, Bundle.CTL_MagicWandToolActionText());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_MagicWandToolActionDescription());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.MAGIC_WAND_ICON, false));
    }

    @Override
    public HelpCtx getHelpCtx() {
        // TODO: Make sure help page is available for ID
        return new HelpCtx("magicWandTool");
    }

}