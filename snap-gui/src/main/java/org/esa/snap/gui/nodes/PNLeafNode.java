/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.snap.gui.actions.file.OpenImageViewAction;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.util.lookup.Lookups;

import javax.swing.Action;
import java.beans.IntrospectionException;

/**
 * @author Norman
 */
public class PNLeafNode<T extends ProductNode> extends BeanNode<T> {

    private T productNode;

    public PNLeafNode(T productNode) throws IntrospectionException {
        super(productNode, Children.LEAF, Lookups.singleton(productNode));
        this.productNode = productNode;
        setDisplayName(productNode.getDisplayName());
        setShortDescription(productNode.getDescription());
    }

    public T getProductNode() {
        return productNode;
    }
}
