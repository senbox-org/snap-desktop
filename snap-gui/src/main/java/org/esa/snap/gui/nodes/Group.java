/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.ProductNode;
import org.openide.nodes.ChildFactory;

/**
 * A group object serves as a key for {@link GroupNode}s and is a child factory for nodes
 * representing {@link ProductNode}s.
 *
 * @author Norman
 */
abstract class Group<T extends ProductNode> extends ChildFactory<T> {

    public abstract String getDisplayName();
}
