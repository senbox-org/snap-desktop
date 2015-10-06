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

package org.esa.snap.rcp.colormanip;

import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.ImageUtilities;

import javax.swing.AbstractButton;

class ImageInfoEditorSupport {

    final AbstractButton autoStretch95Button;
    final AbstractButton autoStretch100Button;
    final AbstractButton zoomInVButton;
    final AbstractButton zoomOutVButton;
    final AbstractButton zoomInHButton;
    final AbstractButton zoomOutHButton;
    final AbstractButton showExtraInfoButton;

    protected ImageInfoEditorSupport(final ImageInfoEditor2 imageInfoEditor) {

        final ColorManipulationForm form = imageInfoEditor.getParentForm();

        autoStretch95Button = createButton("org/esa/snap/rcp/icons/Auto95Percent24.gif");
        autoStretch95Button.setName("AutoStretch95Button");
        autoStretch95Button.setToolTipText("Auto-adjust to 95% of all pixels");
        autoStretch95Button.addActionListener(form.wrapWithAutoApplyActionListener(e -> imageInfoEditor.compute95Percent()));

        autoStretch100Button = createButton("org/esa/snap/rcp/icons/Auto100Percent24.gif");
        autoStretch100Button.setName("AutoStretch100Button");
        autoStretch100Button.setToolTipText("Auto-adjust to 100% of all pixels");
        autoStretch100Button.addActionListener(form.wrapWithAutoApplyActionListener(e -> imageInfoEditor.compute100Percent()));

        zoomInVButton = createButton("org/esa/snap/rcp/icons/ZoomIn24V.gif");
        zoomInVButton.setName("zoomInVButton");
        zoomInVButton.setToolTipText("Stretch histogram vertically");
        zoomInVButton.addActionListener(e -> imageInfoEditor.computeZoomInVertical());

        zoomOutVButton = createButton("org/esa/snap/rcp/icons/ZoomOut24V.gif");
        zoomOutVButton.setName("zoomOutVButton");
        zoomOutVButton.setToolTipText("Shrink histogram vertically");
        zoomOutVButton.addActionListener(e -> imageInfoEditor.computeZoomOutVertical());

        zoomInHButton = createButton("org/esa/snap/rcp/icons/ZoomIn24H.gif");
        zoomInHButton.setName("zoomInHButton");
        zoomInHButton.setToolTipText("Stretch histogram horizontally");
        zoomInHButton.addActionListener(e -> imageInfoEditor.computeZoomInToSliderLimits());

        zoomOutHButton = createButton("org/esa/snap/rcp/icons/ZoomOut24H.gif");
        zoomOutHButton.setName("zoomOutHButton");
        zoomOutHButton.setToolTipText("Shrink histogram horizontally");
        zoomOutHButton.addActionListener(e -> imageInfoEditor.computeZoomOutToFullHistogramm());

        showExtraInfoButton = createToggleButton("org/esa/snap/rcp/icons/Information24.gif");
        showExtraInfoButton.setName("ShowExtraInfoButton");
        showExtraInfoButton.setToolTipText("Show extra information");
        showExtraInfoButton.setSelected(imageInfoEditor.getShowExtraInfo());
        showExtraInfoButton.addActionListener(e -> imageInfoEditor.setShowExtraInfo(showExtraInfoButton.isSelected()));
    }

    public static AbstractButton createToggleButton(String s) {
        return ToolButtonFactory.createButton(ImageUtilities.loadImageIcon(s, false), true);
    }

    public static AbstractButton createButton(String s) {
        return ToolButtonFactory.createButton(ImageUtilities.loadImageIcon(s, false), false);
    }
}
