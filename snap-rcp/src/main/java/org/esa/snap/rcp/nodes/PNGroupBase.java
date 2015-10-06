/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import org.esa.snap.core.datamodel.ProductNode;
import org.openide.nodes.ChildFactory;

import java.util.ArrayList;

/**
 * A group object serves as a key for {@link PNGroupNode}s and is a child factory for nodes
 * representing {@link ProductNode}s.
 *
 * @author Norman
 */
abstract class PNGroupBase<T> extends ChildFactory.Detachable<T> {

    void refresh() {
        refresh(true);
    }

    boolean isDirectChild(ProductNode productNode) {
        ArrayList list = new ArrayList<>();
        //noinspection unchecked
        createKeys(list);
        return list.contains(productNode);
    }

    boolean shallReactToPropertyChange(String propertyName) {
        return false;
    }
}
