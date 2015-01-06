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
package org.esa.snap.gui.actions.file;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.nodes.PNodeFactory;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.gui.windows.ProductSceneViewTopComponent;
import org.openide.awt.*;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;

/**
 * This action opens an image view of the currently selected raster.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.actions.file.OpenImageViewAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenImageViewActionName",
        iconBase = "org/esa/snap/gui/icons/RsBandAsSwath16.gif"
)
@ActionReferences({
        //@ActionReference(path = "Menu/File", position = 149),
        @ActionReference(path = "Context/Product/Band", position = 100),
        @ActionReference(path = "Context/Product/TPGrid", position = 100)
})
@NbBundle.Messages("CTL_OpenImageViewActionName=Open in Image View")
public class OpenImageViewAction extends AbstractAction {

    RasterDataNode raster;

    public OpenImageViewAction(RasterDataNode rasterDataNode) {
        this.raster = rasterDataNode;
        putValue(Action.NAME, Bundle.CTL_OpenImageViewActionName());
        putValue(Action.LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/gui/icons/RsBandAsSwath24.gif", false));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SnapApp.getInstance().openProductSceneView(raster);
    }

}
