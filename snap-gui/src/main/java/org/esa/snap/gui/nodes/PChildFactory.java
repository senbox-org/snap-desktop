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
 * A child factory for nodes that represent groups in a {@link org.esa.beam.framework.datamodel.Product}.
 *
 * @author Norman
 */
class PChildFactory extends ChildFactory<Group> {

    private final Product product;

    PChildFactory(Product product) {
        this.product = product;
    }

    @Override
    protected boolean createKeys(List<Group> list) {
        list.add(new MetadataGroup(product.getMetadataRoot()));
        if (product.getIndexCodingGroup().getNodeCount() > 0)
            list.add(new PNGGroup.IC(product.getIndexCodingGroup()));
        if (product.getFlagCodingGroup().getNodeCount() > 0)
            list.add(new PNGGroup.FC(product.getFlagCodingGroup()));
        if (product.getVectorDataGroup().getNodeCount() > 0)
            list.add(new PNGGroup.VDN(product.getVectorDataGroup()));
        if (product.getTiePointGridGroup().getNodeCount() > 0)
            list.add(new PNGGroup.TPG(product.getTiePointGridGroup()));
        if (product.getBandGroup().getNodeCount() > 0)
            list.add(new PNGGroup.B(product.getBandGroup()));
        if (product.getMaskGroup().getNodeCount() > 0)
            list.add(new PNGGroup.M(product.getMaskGroup()));
        return true;
    }

    @Override
    protected Node createNodeForKey(Group key) {
        return new GroupNode(key);
    }
}
