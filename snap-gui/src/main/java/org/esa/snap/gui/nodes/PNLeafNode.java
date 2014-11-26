/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.ProductNode;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.util.lookup.Lookups;

import java.beans.IntrospectionException;

/**
 * A node that represents some {@link org.esa.beam.framework.datamodel.ProductNode} (=PN).
 *
 * @author Norman
 */
public class PNLeafNode<T extends ProductNode> extends BeanNode<T> {

    public PNLeafNode(T productNode) throws IntrospectionException {
        super(productNode, Children.LEAF, Lookups.singleton(productNode));
        setDisplayName(productNode.getDisplayName());
        setShortDescription(productNode.getDescription());
    }
}
