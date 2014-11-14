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
package org.esa.snap.gui.action;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.window.WorkspaceTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.SwingWorker;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.List;


@ActionID(
        category = "View",
        id = "org.snap.gui.action.ShowImageViewAction"
)
@ActionRegistration(
        displayName = "Show Image View"
)
@ActionReference(path = "Menu/View", position = 149)

/**
 * This action opens an image view of the currently selected raster.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ShowImageViewAction extends AbstractAction {

    RasterDataNode band;

    public ShowImageViewAction(RasterDataNode band) {
        this.band = band;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SnapApp.getInstance().setStatusBarMessage("Opening image view...");

        UIUtils.setRootFrameWaitCursor(SnapApp.getInstance().getMainFrame());

        String progressMonitorTitle = MessageFormat.format("{0} - Creating image for ''{1}''",
                                                           SnapApp.getInstance().getInstanceName(),
                                                           band.getName());

        SwingWorker worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(SnapApp.getInstance().getMainFrame(),
                                                                                       progressMonitorTitle) {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {
                try {
                    return createProductSceneImage(band, pm);
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
                    ProductSceneView view = new ProductSceneView(get());
                    openInternalFrame(view);
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

    public WorkspaceTopComponent.Editor<ProductSceneView> openInternalFrame(final ProductSceneView view) {
        return openInternalFrame(view, true);
    }

    public WorkspaceTopComponent.Editor<ProductSceneView> openInternalFrame(final ProductSceneView view, boolean configureByPreferences) {
        final RasterDataNode selectedProductNode = view.getRaster();
        //view.setCommandUIFactory(snapApp.getCommandUIFactory());
        if (configureByPreferences) {
            view.setLayerProperties(SnapApp.getInstance().getCompatiblePreferences());
        }

        final Product product = selectedProductNode.getProduct();

        final String title = getUniqueEditorTitle(selectedProductNode);
        final Icon icon = null; // UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
        //final JInternalFrame internalFrame = SnapApp.getInstance().createInternalFrame(title, icon, view, getHelpId(), true);
        WorkspaceTopComponent.Editor<ProductSceneView> editor = WorkspaceTopComponent.getInstance().addComponent(title, view);

        final ProductNodeListenerAdapter pnl = new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                if (event.getSourceNode() == selectedProductNode &&
                    event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                    editor.setTitle(getUniqueEditorTitle(selectedProductNode));
                }
            }
        };
        product.addProductNodeListener(pnl);

        WorkspaceTopComponent.EditorAdapter<ProductSceneView> el = new WorkspaceTopComponent.EditorAdapter<ProductSceneView>() {
            @Override
            public void editorClosed(WorkspaceTopComponent.Editor<ProductSceneView> editor) {
                product.removeProductNodeListener(pnl);
            }
        };
        editor.addListener(el);
        return editor;
    }

    private String getUniqueEditorTitle(final RasterDataNode raster) {
        return WorkspaceTopComponent.getInstance().getUniqueEditorTitle(raster.getDisplayName());
    }

    protected ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProgressMonitor pm) {
        Debug.assertNotNull(raster);
        Debug.assertNotNull(pm);

        try {
            pm.beginTask("Creating image...", 1);
            List<WorkspaceTopComponent.Editor<ProductSceneView>> imageEditors = WorkspaceTopComponent.getInstance().findImageEditors(raster);
            ProductSceneImage sceneImage;
            if (!imageEditors.isEmpty()) {
                ProductSceneView view = imageEditors.get(0).getComponent();
                sceneImage = new ProductSceneImage(raster, view);
            } else {
                sceneImage = new ProductSceneImage(raster,
                                                   null, //SnapApp.getInstance().getPreferences(),
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
