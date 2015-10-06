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

package org.esa.snap.rcp.statistics;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.VectorDataLayer;
import org.esa.snap.ui.product.VectorDataLayerFilterFactory;
import org.openide.util.HelpCtx;
import org.openide.windows.TopComponent;

import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D;

/**
 * The window containing all statistics.
 *
 * @author Marco Peters
 * @author Tonio Fincke
 */
public abstract class AbstractStatisticsTopComponent extends TopComponent implements HelpCtx.Provider {

    private PagePanel pagePanel;
    private Product product;

    private final PagePanelLL pagePanelLL;
    private final SelectionChangeListener pagePanelSCL;
    private final PagePanelProductHandler pagePanelProductListener;
    private final PagePanelProductSceneViewHandler pagePanelProductSceneViewListener;
    private final PagePanelProductRemovedListener pagePanelProductRemovedListener;

    protected AbstractStatisticsTopComponent() {
        pagePanelProductListener = new PagePanelProductHandler();
        pagePanelProductSceneViewListener = new PagePanelProductSceneViewHandler();
        pagePanelProductRemovedListener = new PagePanelProductRemovedListener();
        pagePanelLL = new PagePanelLL();
        pagePanelSCL = new PagePanelSCL();
        initComponent();
    }

    public void initComponent() {
        setLayout(new BorderLayout());
        pagePanel = createPagePanel();
        pagePanel.initComponents();
        setCurrentSelection();
        add(pagePanel, BorderLayout.CENTER);
    }

    protected abstract PagePanel createPagePanel();
    public abstract HelpCtx getHelpCtx();


    @Override
    public void componentShowing() {
        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(pagePanelProductRemovedListener);
        snapApp.getSelectionSupport(ProductNode.class).addHandler(pagePanelProductListener);
        snapApp.getSelectionSupport(ProductSceneView.class).addHandler(pagePanelProductSceneViewListener);
        final ProductSceneView productSceneView = snapApp.getSelectedProductSceneView();
        addViewListener(productSceneView);
        setCurrentSelection();
        transferProductNodeListener(null, product);
    }

    @Override
    public void componentHidden() {
        transferProductNodeListener(product, null);
        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().removeListener(pagePanelProductRemovedListener);
        snapApp.getSelectionSupport(ProductNode.class).removeHandler(pagePanelProductListener);
        snapApp.getSelectionSupport(ProductSceneView.class).removeHandler(pagePanelProductSceneViewListener);
        removeViewListener(snapApp.getSelectedProductSceneView());
    }

    private void addViewListener(ProductSceneView view) {
        if (view != null) {
            view.getRootLayer().addListener(pagePanelLL);
            view.getFigureEditor().addSelectionChangeListener(pagePanelSCL);
        }
    }

    private void removeViewListener(ProductSceneView view) {
        if (view != null) {
            view.getRootLayer().removeListener(pagePanelLL);
            view.getFigureEditor().removeSelectionChangeListener(pagePanelSCL);
        }
    }

    private void transferProductNodeListener(Product oldProduct, Product newProduct) {
        if (oldProduct != newProduct) {
            if (oldProduct != null) {
                oldProduct.removeProductNodeListener(pagePanel);
            }
            if (newProduct != null) {
                newProduct.addProductNodeListener(pagePanel);
            }
        }
    }

    private void updateTitle() {
        setDisplayName(pagePanel.getTitle());
    }

    void setCurrentSelection() {

        Product product = null;
        RasterDataNode raster = null;
        VectorDataNode vectorDataNode = null;

        final ProductNode selectedNode = SnapApp.getDefault().getSelectedProductNode();
        if (selectedNode != null && selectedNode.getProduct() != null) {
            product = selectedNode.getProduct();
        }
        if (selectedNode instanceof RasterDataNode) {
            raster = (RasterDataNode) selectedNode;
        } else if (selectedNode instanceof VectorDataNode) {
            vectorDataNode = (VectorDataNode) selectedNode;
        }

        selectionChanged(product, raster, vectorDataNode);
    }


    private void selectionChanged(final Product product, final RasterDataNode raster, final VectorDataNode vectorDataNode) {
        this.product = product;

        runInEDT(new Runnable() {
            @Override
            public void run() {
                pagePanel.selectionChanged(product, raster, vectorDataNode);
                updateTitle();
            }
        });
    }

    private void runInEDT(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private class PagePanelProductHandler implements SelectionSupport.Handler<ProductNode> {

        @Override
        public void selectionChange(ProductNode oldValue, ProductNode newValue) {
            if (newValue != null) {
                RasterDataNode raster = null;
                if (newValue instanceof RasterDataNode) {
                    raster = (RasterDataNode) newValue;
                }
                VectorDataNode vector = null;
                if (newValue instanceof VectorDataNode) {
                    vector = (VectorDataNode) newValue;
                    final ProductSceneView sceneView = SnapApp.getDefault().getSelectedProductSceneView();
                    if (sceneView != null) {
                        raster = sceneView.getRaster();
                    }
                }
                Product product = newValue.getProduct();
                if (product != null) {
                    selectionChanged(product, raster, vector);
                }
            }
        }
    }

    private class PagePanelProductRemovedListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            //do nothing
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            selectionChanged(null, null, null);
        }

    }

    private class PagePanelProductSceneViewHandler implements SelectionSupport.Handler<ProductSceneView> {

        @Override
        public void selectionChange(ProductSceneView oldValue, ProductSceneView newValue) {
            if (oldValue != null) {
                removeViewListener(oldValue);
            }
            if (newValue != null) {
                addViewListener(newValue);
                VectorDataNode vectorDataNode = getVectorDataNode(newValue);
                selectionChanged(newValue.getRaster().getProduct(), newValue.getRaster(), vectorDataNode);
            }
        }

        private VectorDataNode getVectorDataNode(ProductSceneView view) {
            final Layer rootLayer = view.getRootLayer();
            final Layer layer = LayerUtils.getChildLayer(rootLayer, LayerUtils.SearchMode.DEEP,
                                                         VectorDataLayerFilterFactory.createGeometryFilter());
            VectorDataNode vectorDataNode = null;
            if (layer instanceof VectorDataLayer) {
                VectorDataLayer vdl = (VectorDataLayer) layer;
                vectorDataNode = vdl.getVectorDataNode();
            }
            return vectorDataNode;
        }
    }

    private class PagePanelLL extends AbstractLayerListener {

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            pagePanel.handleLayerContentChanged();
        }
    }

    private class PagePanelSCL implements SelectionChangeListener {

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            pagePanel.handleLayerContentChanged();
        }

        @Override
        public void selectionContextChanged(SelectionChangeEvent event) {
            pagePanel.handleLayerContentChanged();
        }
    }
}
