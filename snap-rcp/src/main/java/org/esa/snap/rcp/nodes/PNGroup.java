/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;

/**
 * A group object serves as a key for {@link PNGroupNode}s and is a child factory for nodes
 * representing {@link ProductNode}s.
 *
 * @author Norman
 */
abstract class PNGroup<T> extends PNGroupBase<T> {

    public abstract Product getProduct();

    public abstract String getDisplayName();
}
