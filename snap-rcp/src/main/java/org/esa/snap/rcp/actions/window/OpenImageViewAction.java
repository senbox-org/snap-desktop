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
package org.esa.snap.rcp.actions.window;

import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.Debug;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This action opens an image view of the currently selected raster.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
@ActionID(category = "View", id = "OpenImageViewAction")
@ActionRegistration(
        displayName = "#CTL_OpenImageViewActionName",
        iconBase = "org/esa/snap/rcp/icons/RsBandAsSwath.gif"
)
@ActionReferences({
        @ActionReference(path = "Menu/Window", position = 100),
        @ActionReference(path = "Context/Product/RasterDataNode", position = 100),
})
@NbBundle.Messages("CTL_OpenImageViewActionName=Open Image Window")
public class OpenImageViewAction extends AbstractAction {

    RasterDataNode raster;

    public OpenImageViewAction(RasterDataNode rasterDataNode) {
        this.raster = rasterDataNode;
        putValue(Action.NAME, Bundle.CTL_OpenImageViewActionName());
        setActivateIfExists(true);
    }

    public static OpenImageViewAction create(RasterDataNode rasterDataNode, boolean activateIfExists) {
        OpenImageViewAction action = new OpenImageViewAction(rasterDataNode);
        action.setActivateIfExists(activateIfExists);
        return action;
    }

    public static void showImageView(RasterDataNode rasterDataNode) {
        create(rasterDataNode, true).execute();
    }

    public static void openImageView(RasterDataNode rasterDataNode) {
        create(rasterDataNode, false).execute();
    }

    public boolean getActivateIfExists() {
        Object value = getValue("activateIfExists");
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    public void setActivateIfExists(boolean value) {
        putValue("activateIfExists", value);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    public void execute() {
        if (getActivateIfExists()) {
            ProductSceneViewTopComponent tc = getProductSceneViewTopComponent(raster);
            if (tc != null) {
                tc.requestSelected();
            } else {
                openProductSceneView();
            }
        } else {
            openProductSceneView();
        }
    }

    private void openProductSceneView() {
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
                    // get the preferences: SnapApp.getInstance().getPreferences()
                    // add the view (as listener) to it
                    openDocumentWindow(view);
                } catch (Exception e) {
                    snapApp.handleError(MessageFormat.format("Failed to open image view.\n\n{0}", e.getMessage()), e);
                }
            }
        };
        worker.execute();
    }

    private ProductSceneViewTopComponent openDocumentWindow(final ProductSceneView view) {

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
                                                   SnapApp.getDefault().getPreferencesPropertyMap(),
                                                   SubProgressMonitor.create(pm, 1));
            }
            sceneImage.initVectorDataCollectionLayer();
            sceneImage.initMaskCollectionLayer();
            return sceneImage;
        } finally {
            pm.done();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ProductSceneView helper methods

    public static ProductSceneViewTopComponent getProductSceneViewTopComponent(RasterDataNode raster) {
        return WindowUtilities.getOpened(ProductSceneViewTopComponent.class)
                .filter(topComponent -> topComponent.getView().getNumRasters() == 1 && raster == topComponent.getView().getRaster())
                .findFirst()
                .orElse(null);
    }

    public static ProductSceneView getProductSceneView(RasterDataNode raster) {
        ProductSceneViewTopComponent component = getProductSceneViewTopComponent(raster);
        return component != null ? component.getView() : null;
    }

    public static void updateProductSceneViewImage(final ProductSceneView view) {
        SwingUtilities.invokeLater(view::updateImage);
    }

    public static void updateProductSceneViewImages(final RasterDataNode[] rasters) {
        updateProductSceneViewImages(rasters, ProductSceneViewImageUpdater.DEFAULT);
    }

    public static void updateProductSceneViewImages(final RasterDataNode[] rasters, ProductSceneViewImageUpdater updateMethod) {
        List<ProductSceneView> views = WindowUtilities.getOpened(ProductSceneViewTopComponent.class).map(ProductSceneViewTopComponent::getView).collect(Collectors.toList());
        for (ProductSceneView view : views) {
            boolean updateView = false;
            for (int j = 0; j < rasters.length && !updateView; j++) {
                final RasterDataNode raster = rasters[j];
                for (int k = 0; k < view.getNumRasters() && !updateView; k++) {
                    if (view.getRaster(k) == raster) {
                        updateView = true;
                    }
                }
            }
            if (updateView) {
                SwingUtilities.invokeLater(() -> updateMethod.updateView(view));
            }
        }
    }

    /**
     * A method used to update a <code>ProductSceneView</code>.
     */
    public interface ProductSceneViewImageUpdater {

        ProductSceneViewImageUpdater DEFAULT = ProductSceneView::updateImage;

        void updateView(ProductSceneView view);
    }

}
