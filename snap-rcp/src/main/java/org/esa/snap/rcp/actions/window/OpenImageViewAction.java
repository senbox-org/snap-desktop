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
import eu.esa.snap.netbeans.docwin.DocumentWindowManager;
import eu.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.PreferencesPropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.*;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;
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
        iconBase = "org/esa/snap/rcp/icons/RsBandAsSwath.gif")
@ActionReferences({
        @ActionReference(path = "Menu/Window", position = 100),
        @ActionReference(path = "Context/Product/RasterDataNode", position = 100),})
@NbBundle.Messages("CTL_OpenImageViewActionName=Open Image Window")
public class OpenImageViewAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private RasterDataNode rasterDataNode;
    private Lookup lookup;

    public OpenImageViewAction() {
        this(Utilities.actionsGlobalContext());
    }


    public OpenImageViewAction(Lookup lookup) {
        putValue(Action.NAME, Bundle.CTL_OpenImageViewActionName());
        this.lookup = lookup;
        Lookup.Result<RasterDataNode> rasterDataNodeResult = lookup.lookupResult(RasterDataNode.class);
        rasterDataNodeResult.addLookupListener(WeakListeners.create(LookupListener.class, this, rasterDataNodeResult));
        setEnabledState();
        setActionName();
    }

    public OpenImageViewAction(RasterDataNode rasterDataNode) {
        putValue(Action.NAME, Bundle.CTL_OpenImageViewActionName());
        this.rasterDataNode = rasterDataNode;
    }

    public static OpenImageViewAction create(RasterDataNode rasterDataNode) {
        return new OpenImageViewAction(rasterDataNode);
    }

    public static void openImageView(RasterDataNode rasterDataNode) {
        new OpenImageViewAction().openRasterDataNode(rasterDataNode);
    }

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

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ProductSceneView helper methods

    public void execute() {
        Collection<? extends RasterDataNode> selectedRasterD = getSelectedRasterDataNodes();
        if (Objects.nonNull(selectedRasterD)) {
            for (RasterDataNode rasterDataNode : selectedRasterD) {
                openRasterDataNode(rasterDataNode);
            }
        } else if (Objects.nonNull(rasterDataNode)) {
            openRasterDataNode(rasterDataNode);
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new OpenImageViewAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnabledState();
        setActionName();
    }

    private void openRasterDataNode(RasterDataNode rasterDataNode) {
        ProductSceneViewTopComponent tc = getProductSceneViewTopComponent(rasterDataNode);
        if (tc != null) {
            tc.requestSelected();
        } else {
            openProductSceneView(rasterDataNode);
        }
    }

    private Collection<? extends RasterDataNode> getSelectedRasterDataNodes() {
        return lookup.lookupAll(RasterDataNode.class);
    }

    private void setActionName() {
        Collection<? extends RasterDataNode> selectedRasterDataNode = getSelectedRasterDataNodes();
        int size = selectedRasterDataNode.size();
        if (size > 1) {
            this.putValue(Action.NAME, String.format("Open %d Image Window", size));
        } else {
            this.putValue(Action.NAME, Bundle.CTL_OpenImageViewActionName());
        }

    }

    private void setEnabledState() {
        if (Objects.nonNull(lookup)) {
            setEnabled(lookup.lookup(RasterDataNode.class) != null);
        }
    }

    private void openProductSceneView(RasterDataNode rasterDataNode) {
        SnapApp snapApp = SnapApp.getDefault();
        snapApp.setStatusBarMessage("Opening image view...");

        UIUtils.setRootFrameWaitCursor(snapApp.getMainFrame());

        String progressMonitorTitle = MessageFormat.format("Creating image for ''{0}''", rasterDataNode.getName());

        ProductSceneView existingView = getProductSceneView(rasterDataNode);
        SwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(snapApp.getMainFrame(), progressMonitorTitle) {

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

            @Override
            protected ProductSceneImage doInBackground(com.bc.ceres.core.ProgressMonitor pm) {
                try {
                    return createProductSceneImage(rasterDataNode, existingView, pm);
                } finally {
                    if (pm.isCanceled()) {
                        rasterDataNode.unloadRasterData();
                    }
                }
            }
        };
        worker.execute();
    }

    private void openDocumentWindow(final ProductSceneView view) {

        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(view.getProduct());
        ProductSceneViewTopComponent productSceneViewWindow = new ProductSceneViewTopComponent(view, undoManager);

        DocumentWindowManager.getDefault().openWindow(productSceneViewWindow);
        productSceneViewWindow.requestSelected();
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
                final Preferences preferences = SnapApp.getDefault().getPreferences();
                final PreferencesPropertyMap configuration = new PreferencesPropertyMap(preferences);
                sceneImage = new ProductSceneImage(raster,
                        configuration,
                        SubProgressMonitor.create(pm, 1));
            }
            sceneImage.initVectorDataCollectionLayer();
            sceneImage.initMaskCollectionLayer();
            return sceneImage;
        } finally {
            pm.done();
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
