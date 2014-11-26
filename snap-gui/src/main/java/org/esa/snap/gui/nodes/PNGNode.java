/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.lookup.Lookups;

/**
 * A {@link ProductNodeGroup} (PNG) node.
 *
 * @author Norman
 */
public class PNGNode<T extends ProductNode> extends AbstractNode {

    private final ProductNodeGroup<T> group;

    public PNGNode(ProductNodeGroup<T> group, PNChildFactory childFactory) {
        super(Children.create(childFactory, true), Lookups.singleton(group));
        this.group = group;
        setDisplayName(group.getName());
        setShortDescription(group.getDescription());
    }

    public ProductNodeGroup<T> getGroup() {
        return group;
    }
}
