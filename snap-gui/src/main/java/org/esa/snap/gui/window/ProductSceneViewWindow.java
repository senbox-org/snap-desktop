/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.window;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;

import java.awt.BorderLayout;
import java.awt.Container;

/**
 * @author Norman
 */
@TopComponent.Description(
        preferredID = "ProductNodeTopComponent",
        persistenceType = TopComponent.PERSISTENCE_NEVER)
@TopComponent.Registration(
        mode = "editor",
        openAtStartup = false)
public class ProductSceneViewWindow extends DocumentWindow<ProductNode> {

    private final Container view;
    private final ProductNodeListenerAdapter nodeRenameHandler;

    public ProductSceneViewWindow(ProductNode productNode, Container view) {
        super(productNode);
        this.view = view;
        this.nodeRenameHandler = new NodeRenameHandler();
        initComponents();
        setName(productNode.getName());
        setDisplayName(productNode.getName());
        setToolTipText(productNode.getProduct().getName() + " - " + productNode.getName());
        setActivatedNodes(new Node[] {});
    }

    public Container getView() {
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
