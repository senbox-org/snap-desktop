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
package org.esa.snap.netbeans.tile;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

import javax.swing.Action;
import java.awt.event.ActionEvent;

/**
 * The "Tile Evenly" action.
 *
 * @author Norman Fomferra
 * @since 1.0
 */
@ActionID(category = "Window", id = "org.esa.snap.netbeans.tile.TileEvenlyAction" )
@ActionReferences({
        @ActionReference(path = "Menu/Window", position = 220),
        @ActionReference(path = "Toolbars/Window Arrangement", position = 20)
})
@ActionRegistration(displayName = "#CTL_TileEvenlyActionName", lazy = false )
@NbBundle.Messages("CTL_TileEvenlyActionName=Tile Evenly")
public class TileEvenlyAction extends TileAction {

    @SuppressWarnings("UnusedDeclaration")
    public TileEvenlyAction() {
        this(Utilities.actionsGlobalContext());
    }

    public TileEvenlyAction(Lookup actionContext) {
        super(actionContext);
        putValue(NAME, Bundle.CTL_TileEvenlyActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/netbeans/tile/icons/TileEvenly20.png", false));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getTileable().tileEvenly();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new TileEvenlyAction(actionContext);
    }
}
