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

import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Interactors", id = "org.esa.snap.rcp.action.interactors.RangeFinderAction" )
@ActionRegistration(displayName = "#CTL_RangeFinderActionText", lazy = false )
@ActionReference(
        path = "Toolbars/" + PackageDefaults.RANGE_FINDER_TOOLBAR_NAME,
        position = PackageDefaults.RANGE_FINDER_TOOLBAR_POSITION )
@Messages({
        "CTL_RangeFinderActionText=" + PackageDefaults.RANGE_FINDER_NAME,
        "CTL_RangeFinderActionDescription=Determines the distance between two points"
})
public class RangeFinderAction extends ToolAction {

    @SuppressWarnings("UnusedDeclaration")
    public RangeFinderAction() {
        this(null);
    }

    public RangeFinderAction(Lookup lookup) {
        super(lookup, new RangeFinderInteractor());
        putValue(NAME, Bundle.CTL_RangeFinderActionText());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_RangeFinderActionDescription());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/" + PackageDefaults.RANGE_FINDER_ICON, false));
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("rangeFinder");
    }

}