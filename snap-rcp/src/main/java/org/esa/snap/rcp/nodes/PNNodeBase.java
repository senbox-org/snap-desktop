/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.openide.awt.UndoRedo;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Lookup;

/**
 * A node that represents some {@link ProductNode} (=PN).
 *
 * @author Norman
 */
abstract class PNNodeBase extends AbstractNode implements ProductNodeListener, UndoRedo.Provider {

    protected PNNodeBase() {
        this(null, null);
    }

    protected PNNodeBase(PNGroupBase childFactory) {
        this(childFactory, null);
    }

    protected PNNodeBase(PNGroupBase childFactory, Lookup lookup) {
        super(childFactory != null ? Children.create(childFactory, false) : Children.LEAF, lookup);
    }

    @Override
    public UndoRedo getUndoRedo() {
        return PNNodeSupport.getUndoRedo(getParentNode());
    }

    public boolean isDirectChild(ProductNode productNode) {
        return PNNodeSupport.isDirectChild(this.getChildren(), productNode);
    }
}
