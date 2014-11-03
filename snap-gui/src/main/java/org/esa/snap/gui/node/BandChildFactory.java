/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.node;

import java.beans.IntrospectionException;
import java.util.List;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.esa.snap.core.Band;
import org.esa.snap.core.Product;

public class BandChildFactory extends ChildFactory<Band> {

    private final Product product;

    public BandChildFactory(Product product) {
        this.product = product;
    }

    @Override
    protected boolean createKeys(List<Band> list) {
        list.addAll(product.getBands());
        return true;
    }

    @Override
    protected Node createNodeForKey(Band key) {
        BandNode node = null;
        try {
            node = new BandNode(key);
        } catch (IntrospectionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return node;
    }
}
