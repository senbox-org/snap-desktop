/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.windows;

import com.bc.ceres.swing.undo.support.DefaultUndoContext;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.gui.util.DocumentTopComponent;
import org.esa.snap.gui.util.WindowUtilities;
import org.openide.awt.UndoRedo;
import org.openide.util.ImageUtilities;

import java.awt.BorderLayout;

/**
 * A document window which displays images
 * @author Norman
 */
public class ProductSceneViewTopComponent extends DocumentTopComponent<ProductNode> implements UndoRedo.Provider {

    private final ProductSceneView view;
    private UndoRedo undoRedo;
    private final ProductNodeListenerAdapter nodeRenameHandler;

    public ProductSceneViewTopComponent(ProductSceneView view, UndoRedo undoRedo) {
        super(view.getRaster());
        this.view = view;
        this.undoRedo = undoRedo;
        this.nodeRenameHandler = new NodeRenameHandler();
        initComponents();
        setName(view.getRaster().getName());
        setDisplayName(WindowUtilities.getUniqueTitle(view.getRaster().getName(), ProductSceneViewTopComponent.class));
        setToolTipText(view.getRaster().getProduct().getName() + " - " + view.getRaster().getName());
        setIcon(ImageUtilities.loadImage("org/esa/snap/gui/icons/RsBandAsSwath16.gif"));
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
    }

    public ProductSceneView getView() {
        return view;
    }

    @Override
    protected void componentOpened() {
        getDocument().getProduct().addProductNodeListener(nodeRenameHandler);
    }

    @Override
    protected void componentClosed() {
        getDocument().getProduct().removeProductNodeListener(nodeRenameHandler);
    }

    @Override
    protected void componentActivated() {
        getDynamicContent().add(view);
    }

    @Override
    protected void componentDeactivated() {
        getDynamicContent().remove(view);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(view, BorderLayout.CENTER);
    }

    private class NodeRenameHandler extends ProductNodeListenerAdapter {
        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            if (event.getSourceNode() == getDocument() &&
                    event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                setDisplayName(getDocument().getName());
            }
        }
    }
}
