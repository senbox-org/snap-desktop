/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.ui.mosaic;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Mosaicing action.
 * Enablement: always enabled
 *
 * @author Norman Fomferra
 * @author Daniel Knowles
 * @author Bing Yang
 */
//Apr2019 - Knowles/Yang - Added access to this tool in the "Raster" toolbar including enablement, tooltips and related icon.


@ActionID(category = "Operators", id = "org.esa.snap.core.gpf.ui.mosaic.MosaicAction")
@ActionRegistration(displayName = "#CTL_MosaicAction_Name", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/Raster/Geometric Operations", position = 10000),
        @ActionReference(path = "Toolbars/Raster", position = 50)
})
@NbBundle.Messages({
        "CTL_MosaicAction_Name=Mosaicking",
        "CTL_MosaicAction_ShortDescription=Creates a mosaic aggregation of multiple files"
})
public final class MosaicAction extends AbstractSnapAction implements Presenter.Menu, Presenter.Toolbar {

    private ModelessDialog dialog;

    private static final String ICONS_DIRECTORY = "org/esa/snap/core/gpf/docs/gpf/icons/";
    private static final String TOOL_ICON_LARGE = ICONS_DIRECTORY + "Mosaic24.png";
    private static final String TOOL_ICON_SMALL = ICONS_DIRECTORY + "Mosaic16.png";


    public MosaicAction() {
        putValue(NAME, Bundle.CTL_MosaicAction_Name()+"...");
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(TOOL_ICON_LARGE, false));
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(TOOL_ICON_SMALL, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_MosaicAction_ShortDescription());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new MosaicDialog(Bundle.CTL_MosaicAction_Name(),
                    "mosaicAction", getAppContext());
        }
        dialog.show();
    }

    @Override
    public boolean isEnabled() {
        return GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Mosaic") != null;
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
