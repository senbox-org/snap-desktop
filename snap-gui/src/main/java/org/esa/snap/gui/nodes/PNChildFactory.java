/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

import java.beans.IntrospectionException;
import java.util.List;


/**
 * A {@link org.esa.beam.framework.datamodel.ProductNodeGroup} (PNG) child factory.
 * @param <T> The product node type.
 */
public abstract class PNChildFactory<T extends ProductNode> extends ChildFactory<T> {

    final ProductNodeGroup<T> group;

    private PNChildFactory(ProductNodeGroup<T> group) {
        this.group = group;
    }

    public ProductNodeGroup<T> getGroup() {
        return group;
    }

    @Override
    protected boolean createKeys(List<T> list) {
        int nodeCount = group.getNodeCount();
        for (int i = 0; i < nodeCount; i++) {
            list.add(group.get(i));
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(T key) {
        try {
            return createPNNode(key);
        } catch (IntrospectionException e) {
            Exceptions.printStackTrace(e);
            return null;
        }
    }

    protected abstract Node createPNNode(T key) throws IntrospectionException;

    public static class B extends PNChildFactory<Band> {
        public B(ProductNodeGroup<Band> group) {
            super(group);
        }

        @Override
        protected Node createPNNode(Band key) throws IntrospectionException {
            return new BNode(key);
        }
    }

    public static class TPG extends PNChildFactory<TiePointGrid> {
        public TPG(ProductNodeGroup<TiePointGrid> group) {
            super(group);
        }

        @Override
        protected Node createPNNode(TiePointGrid key) throws IntrospectionException {
            return new TPGNode(key);
        }
    }

    public static class VDN extends PNChildFactory<VectorDataNode> {
        public VDN(ProductNodeGroup<VectorDataNode> group) {
            super(group);
        }

        @Override
        protected Node createPNNode(VectorDataNode key) throws IntrospectionException {
            return new VDNNode(key);
        }
    }

    public static class M extends PNChildFactory<Mask> {
        public M(ProductNodeGroup<Mask> group) {
            super(group);
        }

        @Override
        protected Node createPNNode(Mask key) throws IntrospectionException {
            return new MNode(key);
        }
    }

    public static class FC extends PNChildFactory<FlagCoding> {
        public FC(ProductNodeGroup<FlagCoding> group) {
            super(group);
        }

        @Override
        protected Node createPNNode(FlagCoding key) throws IntrospectionException {
            return new FCNode(key);
        }
    }

    public static class IC extends PNChildFactory<IndexCoding> {
        public IC(ProductNodeGroup<IndexCoding> group) {
            super(group);
        }

        @Override
        protected Node createPNNode(IndexCoding key) throws IntrospectionException {
            return new ICNode(key);
        }
    }
}
