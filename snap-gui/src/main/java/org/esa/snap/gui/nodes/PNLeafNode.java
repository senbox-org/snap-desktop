/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.ProductNode;
import org.openide.awt.UndoRedo;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.util.lookup.Lookups;

import java.beans.IntrospectionException;

/**
 * A node that represents some {@link org.esa.beam.framework.datamodel.ProductNode} (=PN).
 *
 * @author Norman
 */
public class PNLeafNode<T extends ProductNode> extends BeanNode<T> implements UndoRedo.Provider {

    private final T productNode;
    private final UndoRedo undoRedo;

    public PNLeafNode(T productNode, UndoRedo undoRedo) throws IntrospectionException {
        super(productNode, Children.LEAF, Lookups.singleton(productNode));
        this.productNode = productNode;
        this.undoRedo = undoRedo;
        setDisplayName(productNode.getDisplayName());
        setShortDescription(productNode.getDescription());
    }

    public T getProductNode() {
        return productNode;
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
    }
}
