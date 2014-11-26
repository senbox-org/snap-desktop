/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

import java.util.List;


/**
 * A {@link org.esa.beam.framework.datamodel.ProductNodeGroup} (PNG) child factory.
 */
class GroupChildFactory extends ChildFactory<Group> {

    private final Product product;

    GroupChildFactory(Product product) {
        this.product = product;
    }

    @Override
    protected boolean createKeys(List<Group> list) {
        if (product.getIndexCodingGroup().getNodeCount() > 0)
            list.add(new Group("Index Codings", new PNChildFactory.IC(product.getIndexCodingGroup())));
        if (product.getFlagCodingGroup().getNodeCount() > 0)
            list.add(new Group("Flag Codings", new PNChildFactory.FC(product.getFlagCodingGroup())));
        if (product.getVectorDataGroup().getNodeCount() > 0)
            list.add(new Group("Vector Data", new PNChildFactory.VDN(product.getVectorDataGroup())));
        if (product.getTiePointGridGroup().getNodeCount() > 0)
            list.add(new Group("Tie-Point Grids", new PNChildFactory.TPG(product.getTiePointGridGroup())));
        if (product.getBandGroup().getNodeCount() > 0)
            list.add(new Group("Bands", new PNChildFactory.B(product.getBandGroup())));
        if (product.getMaskGroup().getNodeCount() > 0)
            list.add(new Group("Masks", new PNChildFactory.M(product.getMaskGroup())));
        return true;
    }

    @Override
    protected Node createNodeForKey(Group key) {
        return new GroupNode(key);
    }
}
