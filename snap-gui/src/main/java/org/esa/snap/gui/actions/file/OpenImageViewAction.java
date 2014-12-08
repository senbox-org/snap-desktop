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
import org.esa.snap.gui.util.WindowUtilities;
import org.esa.snap.gui.windows.ProductSceneViewTopComponent;
import org.openide.awt.*;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.List;

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

    RasterDataNode band;

    public OpenImageViewAction(RasterDataNode band) {
        this.band = band;
        putValue(Action.NAME, Bundle.CTL_OpenImageViewActionName());
        putValue(Action.LARGE_ICON_KEY, ImageUtilities.loadImageIcon("org/esa/snap/gui/icons/RsBandAsSwath24.gif", false));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SnapApp.getInstance().setStatusBarMessage("Opening image view...");

        UIUtils.setRootFrameWaitCursor(SnapApp.getInstance().getMainFrame());

        String progressMonitorTitle = MessageFormat.format("{0} - Creating image for ''{1}''",
                                                           SnapApp.getInstance().getInstanceName(),
                                                           band.getName());

        ProductSceneView existingView = getProductSceneView(band);
        SwingWorker worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(SnapApp.getInstance().getMainFrame(),
                                                                                       progressMonitorTitle) {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {
                try {
                    return createProductSceneImage(band, existingView, pm);
                } finally {
                    if (pm.isCanceled()) {
                        band.unloadRasterData();
                    }
                }
            }

            @Override
            public void done() {
                UIUtils.setRootFrameDefaultCursor(SnapApp.getInstance().getMainFrame());
                SnapApp.getInstance().setStatusBarMessage("");
                try {
                    ProductSceneImage sceneImage = get();
                    UndoRedo.Manager undoManager = PNodeFactory.getInstance().getUndoManager(sceneImage.getProduct());
                    ProductSceneView view = new ProductSceneView(sceneImage, undoManager);
                    openDocumentWindow(view);
                } catch (OutOfMemoryError ignored) {
                    SnapApp.getInstance().showOutOfMemoryErrorDialog("Failed to open image view.");
                } catch (Exception e) {
                    SnapApp.getInstance().handleError(
                            MessageFormat.format("Failed to open image view.\n\n{0}", e.getMessage()), e);
                }
            }
        };
        worker.execute();
    }

    public ProductSceneViewTopComponent openDocumentWindow(final ProductSceneView view) {
        return openDocumentWindow(view, true);
    }

    public ProductSceneViewTopComponent openDocumentWindow(final ProductSceneView view, boolean configureByPreferences) {
        if (configureByPreferences) {
            view.setLayerProperties(SnapApp.getInstance().getCompatiblePreferences());
        }

        UndoRedo.Manager undoManager = PNodeFactory.getInstance().getUndoManager(view.getProduct());
        ProductSceneViewTopComponent productSceneViewWindow = new ProductSceneViewTopComponent(view, undoManager);

        WindowUtilities.openDocumentWindow(productSceneViewWindow);
        return productSceneViewWindow;
    }

    protected ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProductSceneView existingView, ProgressMonitor pm) {
        Debug.assertNotNull(raster);
        Debug.assertNotNull(pm);

        try {
            pm.beginTask("Creating image...", 1);

            ProductSceneImage sceneImage;
            if (existingView != null) {
                sceneImage = new ProductSceneImage(raster, existingView);
            } else {
                sceneImage = new ProductSceneImage(raster,
                                                   SnapApp.getInstance().getCompatiblePreferences(),
                                                   SubProgressMonitor.create(pm, 1));
            }
            sceneImage.initVectorDataCollectionLayer();
            sceneImage.initMaskCollectionLayer();
            return sceneImage;
        } finally {
            pm.done();
        }

    }

    public ProductSceneView getProductSceneView(RasterDataNode raster) {
        List<ProductSceneView> list = WindowUtilities.collectOpen(ProductSceneViewTopComponent.class, new WindowUtilities.Collector<ProductSceneViewTopComponent, ProductSceneView>() {
            @Override
            public void collect(ProductSceneViewTopComponent topComponent, List<ProductSceneView> list) {
                if (raster == topComponent.getView().getRaster()) {
                    list.add(topComponent.getView());
                }
            }
        });
        return list.isEmpty() ? null : list.get(0);
    }
}
