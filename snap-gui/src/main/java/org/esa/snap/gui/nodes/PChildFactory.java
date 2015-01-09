/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.openide.awt.UndoRedo;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

import java.util.List;


/**
 * A child factory for nodes that represent groups in a {@link org.esa.beam.framework.datamodel.Product}.
 *
 * @author Norman
 */
class PChildFactory extends ChildFactory<Group> {

    private final Product product;
    private final UndoRedo undoRedo;

    PChildFactory(Product product, UndoRedo undoRedo) {
        this.product = product;
        this.undoRedo = undoRedo;
    }

    @Override
    protected boolean createKeys(List<Group> list) {
        list.add(new MetadataGroup(product.getMetadataRoot(), undoRedo));
        if (product.getIndexCodingGroup().getNodeCount() > 0)
            list.add(new PNGGroup.IC(product.getIndexCodingGroup(), undoRedo));
        if (product.getFlagCodingGroup().getNodeCount() > 0)
            list.add(new PNGGroup.FC(product.getFlagCodingGroup(), undoRedo));
        if (product.getVectorDataGroup().getNodeCount() > 0)
            list.add(new PNGGroup.VDN(product.getVectorDataGroup(), undoRedo));
        if (product.getTiePointGridGroup().getNodeCount() > 0)
            list.add(new PNGGroup.TPG(product.getTiePointGridGroup(), undoRedo));
        if (product.getBandGroup().getNodeCount() > 0)
            list.add(new PNGGroup.B(product.getBandGroup(), undoRedo));
        if (product.getMaskGroup().getNodeCount() > 0)
            list.add(new PNGGroup.M(product.getMaskGroup(), undoRedo));
        return true;
    }

    @Override
    protected Node createNodeForKey(Group key) {
        return new GroupNode(key);
    }
}
