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

package org.esa.snap.collocation.visat;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Geographic collocation action.
 * Enablement: always enabled
 *
 * @author Ralf Quast
 * @author Marco Peters
 * @author Daniel Knowles
 * @author Bing Yang
 */
//Apr2019 - Knowles / Yang - Added access to this tool in the "Raster" toolbar including enablement, tooltips and related icon.


@ActionID(category = "Processors", id = "org.esa.snap.collocation.visat.CollocationAction")
@ActionRegistration(displayName = "#CTL_CollocationAction_Name", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/Raster/Geometric Operations", position = 10000),
        @ActionReference(path = "Toolbars/Raster", position = 30)
})
@NbBundle.Messages({
        "CTL_CollocationAction_Name=Collocation",
        "CTL_CollocationAction_ShortDescription=Creates a geographic collocation of two files"
})
public final class CollocationAction extends AbstractSnapAction implements Presenter.Menu, Presenter.Toolbar {

    private ModelessDialog dialog;

    private static final String ICONS_DIRECTORY = "org/esa/snap/collocation/docs/icons/";
    private static final String TOOL_ICON_LARGE = ICONS_DIRECTORY + "Collocate24.png";
    private static final String TOOL_ICON_SMALL = ICONS_DIRECTORY + "Collocate16.png";


    public CollocationAction() {
        putValue(NAME, Bundle.CTL_CollocationAction_Name()+"...");
        putValue(SHORT_DESCRIPTION, Bundle.CTL_CollocationAction_ShortDescription());
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(TOOL_ICON_LARGE, false));
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(TOOL_ICON_SMALL, false));
        setHelpId(CollocationDialog.HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new CollocationDialog(getAppContext());
        }
        dialog.show();
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(this);
        return menuItem;
    }
    @Override
    public Component getToolbarPresenter() {
        JButton button = new JButton(this);
        button.setText(null);
        return button;
    }

}
