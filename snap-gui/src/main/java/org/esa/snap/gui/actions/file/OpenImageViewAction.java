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

import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.SnapDialogs;
import org.esa.snap.gui.windows.ProductSceneViewTopComponent;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingWorker;
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
        openProductSceneView();
    }

    public void openProductSceneView() {
        SnapApp snapApp = SnapApp.getDefault();
        snapApp.setStatusBarMessage("Opening image view...");

        UIUtils.setRootFrameWaitCursor(snapApp.getMainFrame());

        String progressMonitorTitle = MessageFormat.format("Creating image for ''{0}''", raster.getName());

        ProductSceneView existingView = getProductSceneView(raster);
        SwingWorker worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(snapApp.getMainFrame(), progressMonitorTitle) {

            @Override
            protected ProductSceneImage doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
                try {
                    return createProductSceneImage(raster, existingView, pm);
                } finally {
                    if (pm.isCanceled()) {
                        raster.unloadRasterData();
                    }
                }
            }

            @Override
            public void done() {

                UIUtils.setRootFrameDefaultCursor(snapApp.getMainFrame());
                snapApp.setStatusBarMessage("");
                try {
                    ProductSceneImage sceneImage = get();
                    UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(sceneImage.getProduct());
                    ProductSceneView view = new ProductSceneView(sceneImage, undoManager);
                    openDocumentWindow(view);
                } catch (Exception e) {
                    snapApp.handleError(MessageFormat.format("Failed to open image view.\n\n{0}", e.getMessage()), e);
                }
            }
        };
        worker.execute();
    }

    private ProductSceneView getProductSceneView(RasterDataNode raster) {
        return WindowUtilities.getOpened(ProductSceneViewTopComponent.class)
                .filter(topComponent -> raster == topComponent.getView().getRaster())
                .map(ProductSceneViewTopComponent::getView)
                .findFirst()
                .orElse(null);
    }

    private ProductSceneViewTopComponent openDocumentWindow(final ProductSceneView view) {
        return openDocumentWindow(view, true);
    }

    private ProductSceneViewTopComponent openDocumentWindow(final ProductSceneView view, boolean configureByPreferences) {
        if (configureByPreferences) {
            view.setLayerProperties(SnapApp.getDefault().getCompatiblePreferences());
        }

        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(view.getProduct());
        ProductSceneViewTopComponent productSceneViewWindow = new ProductSceneViewTopComponent(view, undoManager);

        DocumentWindowManager.getDefault().openWindow(productSceneViewWindow);
        productSceneViewWindow.requestSelected();

        return productSceneViewWindow;
    }

    private ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProductSceneView existingView, com.bc.ceres.core.ProgressMonitor pm) {
        Debug.assertNotNull(raster);
        Debug.assertNotNull(pm);

        try {
            pm.beginTask("Creating image...", 1);

            ProductSceneImage sceneImage;
            if (existingView != null) {
                sceneImage = new ProductSceneImage(raster, existingView);
            } else {
                sceneImage = new ProductSceneImage(raster,
                                                   SnapApp.getDefault().getCompatiblePreferences(),
                                                   SubProgressMonitor.create(pm, 1));
            }
            sceneImage.initVectorDataCollectionLayer();
            sceneImage.initMaskCollectionLayer();
            return sceneImage;
        } finally {
            pm.done();
        }

    }


}
