/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.window;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.openide.util.ImageUtilities;

import java.awt.BorderLayout;

/**
 * A document window which displays images
 * @author Norman
 */
public class ProductSceneViewWindow extends DocumentWindow<ProductNode> {

    private final ProductSceneView view;
    private final ProductNodeListenerAdapter nodeRenameHandler;

    public ProductSceneViewWindow(ProductSceneView view) {
        super(view.getRaster());
        this.view = view;
        this.nodeRenameHandler = new NodeRenameHandler();
        initComponents();
        setName(view.getRaster().getName());
        setDisplayName(getUniqueEditorTitle(view.getRaster().getName()));
        setToolTipText(view.getRaster().getProduct().getName() + " - " + view.getRaster().getName());
        setIcon(ImageUtilities.loadImage("org/esa/snap/gui/icons/RsBandAsSwath16.gif"));
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
        getContent().add(view);
    }

    @Override
    protected void componentDeactivated() {
        getContent().remove(view);
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
