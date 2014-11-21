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
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.window.ProductSceneViewWindow;
import org.esa.snap.gui.window.WorkspaceTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.windows.TopComponent;

import javax.swing.AbstractAction;
import javax.swing.SwingWorker;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


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

    public ProductSceneViewWindow openDocumentWindow(final ProductSceneView view) {
        return openDocumentWindow(view, true);
    }

    public ProductSceneViewWindow openDocumentWindow(final ProductSceneView view, boolean configureByPreferences) {
        final RasterDataNode selectedProductNode = view.getRaster();
        //view.setCommandUIFactory(snapApp.getCommandUIFactory());
        if (configureByPreferences) {
            view.setLayerProperties(SnapApp.getInstance().getCompatiblePreferences());
        }
        //Product product = selectedProductNode.getProduct();
        //String title = getUniqueEditorTitle(selectedProductNode);
        //Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
        //JInternalFrame internalFrame = SnapApp.getInstance().createInternalFrame(title, icon, view, getHelpId(), true);
        ProductSceneViewWindow productSceneViewWindow = new ProductSceneViewWindow(selectedProductNode, view);
        productSceneViewWindow.setDisplayName(getUniqueEditorTitle(selectedProductNode.getDisplayName()));

        WorkspaceTopComponent.getDefault().addWindow(productSceneViewWindow);
        return productSceneViewWindow;
    }

    protected ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProgressMonitor pm) {
        Debug.assertNotNull(raster);
        Debug.assertNotNull(pm);

        try {
            pm.beginTask("Creating image...", 1);

            ProductSceneView view = getProductSceneView(raster);
            ProductSceneImage sceneImage;
            if (view != null) {
                sceneImage = new ProductSceneImage(raster, view);
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


    public interface WindowVisitor<T> {
        T visit(TopComponent topComponent);
    }

    public static <T> List<T> visitOpenWindows(WindowVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        T element;
        Set<TopComponent> topComponents = TopComponent.getRegistry().getOpened();
        for (TopComponent topComponent : topComponents) {
            element = visitor.visit(topComponent);
            if (element != null) {
                result.add(element);
            }
            if (topComponent instanceof WorkspaceTopComponent) {
                WorkspaceTopComponent workspaceTopComponent = (WorkspaceTopComponent) topComponent;
                List<TopComponent> containedWindows = workspaceTopComponent.getContainedTopComponents();
                for (TopComponent containedWindow : containedWindows) {
                    element = visitor.visit(containedWindow);
                    if (element != null) {
                        result.add(element);
                    }
                }
            }
        }
        return result;
    }

    public static String getUniqueEditorTitle(String titleBase) {

        List<String> titles = visitOpenWindows(TopComponent::getDisplayName);

        if (titles.isEmpty()) {
            return titleBase;
        }

        if (!titles.contains(titleBase)) {
            return titleBase;
        }

        for (int i = 2; ;i++)  {
            final String title = String.format("%s (%d)", titleBase, i);
            if (!titles.contains(title)) {
                return title;
            }
        }
    }

    public ProductSceneView getProductSceneView(RasterDataNode raster) {
        List<ProductSceneView> list = visitOpenWindows(topComponent -> {
            if (topComponent instanceof ProductSceneViewWindow) {
                ProductSceneViewWindow window = (ProductSceneViewWindow) topComponent;
                if (window.getDocument() == raster) {
                    Container container = window.getView();
                    if (container instanceof ProductSceneView) {
                        return (ProductSceneView) container;
                    }
                }
            }
            return null;
        });
        return list.isEmpty() ? null : list.get(0);
    }

}
