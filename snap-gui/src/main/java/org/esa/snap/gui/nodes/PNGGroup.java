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
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.Exceptions;

import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.util.List;


/**
 * A group that represents a {@link org.esa.beam.framework.datamodel.ProductNodeGroup} (=PNG).
 *
 * @author Norman
 */
abstract class PNGGroup<T extends ProductNode> extends Group<T> implements NodeListener {

    private final String displayName;
    private final ProductNodeGroup<T> group;
    private final UndoRedo undoRedo;
    private ProductNodeListenerAdapter nodeListener;

    protected PNGGroup(String displayName, ProductNodeGroup<T> group, UndoRedo undoRedo) {
        this.displayName = displayName;
        this.group = group;
        this.undoRedo = undoRedo;
        nodeListener = new PNGProductNodeListenerAdapter();
    }

    @Override
    protected void addNotify() {
        group.getProduct().addProductNodeListener(nodeListener);
    }

    @Override
    protected void removeNotify() {
        group.getProduct().removeProductNodeListener(nodeListener);
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
    }

    @Override
    public String getDisplayName() {
        return displayName;
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
            Node node = createPNLeafNode(key);
            node.addNodeListener(this);
            return node;
        } catch (IntrospectionException e) {
            Exceptions.printStackTrace(e);
            return null;
        }
    }

    protected abstract PNLeafNode createPNLeafNode(T key) throws IntrospectionException;

    @Override
    public void childrenAdded(NodeMemberEvent ev) {
    }

    @Override
    public void childrenRemoved(NodeMemberEvent ev) {
    }

    @Override
    public void childrenReordered(NodeReorderEvent ev) {
    }

    @Override
    public void nodeDestroyed(NodeEvent ev) {
        PNLeafNode node = (PNLeafNode) ev.getNode();
        group.remove((T) node.getProductNode());
        refresh(true);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
    }

    public static class B extends PNGGroup<Band> {

        public B(ProductNodeGroup<Band> group, UndoRedo undoRedo) {
            super("Bands", group, undoRedo);
        }

        @Override
        protected PNLeafNode createPNLeafNode(Band key) throws IntrospectionException {
            return new BNode(key, getUndoRedo());
        }

    }

    public static class TPG extends PNGGroup<TiePointGrid> {

        public TPG(ProductNodeGroup<TiePointGrid> group, UndoRedo undoRedo) {
            super("Tie-Point Grids", group, undoRedo);
        }

        @Override
        protected PNLeafNode createPNLeafNode(TiePointGrid key) throws IntrospectionException {
            return new TPGNode(key, getUndoRedo());
        }
    }

    public static class VDN extends PNGGroup<VectorDataNode> {

        public VDN(ProductNodeGroup<VectorDataNode> group, UndoRedo undoRedo) {
            super("Vector Data", group, undoRedo);
        }

        @Override
        protected PNLeafNode createPNLeafNode(VectorDataNode key) throws IntrospectionException {
            return new VDNNode(key, getUndoRedo());
        }
    }

    public static class M extends PNGGroup<Mask> {

        public M(ProductNodeGroup<Mask> group, UndoRedo undoRedo) {
            super("Masks", group, undoRedo);
        }

        @Override
        protected PNLeafNode createPNLeafNode(Mask key) throws IntrospectionException {
            return new MNode(key, getUndoRedo());
        }
    }

    public static class FC extends PNGGroup<FlagCoding> {

        public FC(ProductNodeGroup<FlagCoding> group, UndoRedo undoRedo) {
            super("Flag Codings", group, undoRedo);
        }

        @Override
        protected PNLeafNode createPNLeafNode(FlagCoding key) throws IntrospectionException {
            return new FCNode(key, getUndoRedo());
        }
    }

    public static class IC extends PNGGroup<IndexCoding> {

        public IC(ProductNodeGroup<IndexCoding> group, UndoRedo undoRedo) {
            super("Index Codings", group, undoRedo);
        }

        @Override
        protected PNLeafNode createPNLeafNode(IndexCoding key) throws IntrospectionException {
            return new ICNode(key, getUndoRedo());
        }
    }

    private class PNGProductNodeListenerAdapter extends ProductNodeListenerAdapter {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getGroup() == group) {
                refresh(true);
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getGroup() == group) {
                refresh(true);
            }
        }
    }
}
