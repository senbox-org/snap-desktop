/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;

import java.util.List;


/**
 * A group that gets its nodes from a {@link org.esa.beam.framework.datamodel.ProductNodeGroup} (=PNG).
 *
 * @author Norman
 */
abstract class PNGGroup<T extends ProductNode> extends PNGroup<T> {

    private final String displayName;
    private final ProductNodeGroup<T> group;

    protected PNGGroup(String displayName, ProductNodeGroup<T> group) {
        Assert.notNull(group, "group");
        this.displayName = displayName;
        this.group = group;

    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    boolean isDirectChild(ProductNode productNode) {
        int nodeCount = group.getNodeCount();
        for (int i = 0; i < nodeCount; i++) {
            if (group.get(i) == productNode) {
                return true;
            }
        }
        return false;
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
    protected abstract PNNode createNodeForKey(T key);


    public static class B extends PNGGroup<Band> {

        public B(ProductNodeGroup<Band> group) {
            super("Bands", group);
        }

        @Override
        protected PNNode createNodeForKey(Band key) {
            return new PNNode.B(key);
        }
    }

    public static class TPG extends PNGGroup<TiePointGrid> {

        public TPG(ProductNodeGroup<TiePointGrid> group) {
            super("Tie-Point Grids", group);
        }

        @Override
        protected PNNode createNodeForKey(TiePointGrid key) {
            return new PNNode.TPG(key);
        }
    }

    public static class VDN extends PNGGroup<VectorDataNode> {

        public VDN(ProductNodeGroup<VectorDataNode> group) {
            super("Vector Data", group);
        }

        @Override
        protected PNNode createNodeForKey(VectorDataNode key) {
            return new PNNode.VDN(key);
        }
    }

    public static class M extends PNGGroup<Mask> {

        public M(ProductNodeGroup<Mask> group) {
            super("Masks", group);
        }

        @Override
        protected PNNode createNodeForKey(Mask key) {
            return new PNNode.M(key);
        }
    }

    public static class FC extends PNGGroup<FlagCoding> {

        public FC(ProductNodeGroup<FlagCoding> group) {
            super("Flag Codings", group);
        }

        @Override
        protected PNNode createNodeForKey(FlagCoding key) {
            return new PNNode.FC(key);
        }
    }

    public static class IC extends PNGGroup<IndexCoding> {

        public IC(ProductNodeGroup<IndexCoding> group) {
            super("Index Codings", group);
        }

        @Override
        protected PNNode createNodeForKey(IndexCoding key) {
            return new PNNode.IC(key);
        }
    }

    public static class ME extends PNGGroup<MetadataElement> {

        public ME(ProductNodeGroup<MetadataElement> group) {
            super("Metadata", group);
        }

        @Override
        protected PNNode createNodeForKey(MetadataElement key) {
            return new PNNode.ME(key);
        }
    }
}
