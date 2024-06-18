/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import com.bc.ceres.core.Assert;
import eu.esa.snap.core.datamodel.group.BandGroup;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.util.List;


/**
 * A group that gets its nodes from a {@link ProductNodeGroup} (=PNG).
 *
 * @author Norman
 */
@NbBundle.Messages({
        "LBL_MetadataGroupName=Metadata",
        "LBL_FlagCodingGroupName=Flag Codings",
        "LBL_IndexCodingGroupName=Index Codings",
        "LBL_VectorDataGroupName=Vector Data",
        "LBL_QuicklookGroupName=Quicklooks",
})
abstract class PNGGroup<T extends ProductNode> extends PNGroup<T> {

    private final String displayName;
    private final ProductNodeGroup<T> group;

    protected PNGGroup(String displayName, ProductNodeGroup<T> group) {
        Assert.notNull(group, "group");
        this.displayName = displayName;
        this.group = group;
    }

    @Override
    public Product getProduct() {
        return group.getProduct();
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
    protected abstract Node createNodeForKey(T key);

    public static class B extends PNGGroup<Band> {

        private final Product product;
        private final ProductNodeGroup<Band> group;

        public B(String displayName, ProductNodeGroup<Band> group, Product product) {
            super(displayName, group);
            this.product = product;
            this.group = group;
        }

        @Override
        protected PNNode createNodeForKey(Band key) {
            return new PNNode.B(key);
        }

        @Override
        void refresh() {
            refreshGroup();
            super.refresh();
        }

        private void refreshGroup() {
            final ProductNodeGroup<Band> productBandGroup = product.getBandGroup();
            if (group != productBandGroup) {
                final BandGroup autoGrouping = product.getAutoGrouping();
                if (autoGrouping != null) {
                    final int groupIndex = autoGrouping.indexOf(group.getDisplayName());
                    group.removeAll();
                    for (int i = 0; i < productBandGroup.getNodeCount(); i++) {
                        final Band band = productBandGroup.get(i);
                        if (autoGrouping.indexOf(band.getName()) == groupIndex) {
                            group.add(band);
                        }
                    }
                }
            }
        }

        @Override
        public Product getProduct() {
            return product;
        }
    }

    public static class TPG extends PNGGroup<TiePointGrid> {

        private final Product product;
        private final ProductNodeGroup<TiePointGrid> group;

        public TPG(String displayName, ProductNodeGroup<TiePointGrid> group, Product product) {
            super(displayName, group);
            this.product = product;
            this.group = group;
        }

        @Override
        protected PNNode createNodeForKey(TiePointGrid key) {
            return new PNNode.TPG(key);
        }

        @Override
        void refresh() {
            refreshGroup();
            super.refresh();
        }

        private void refreshGroup() {
            final ProductNodeGroup<TiePointGrid> productTiePointGridGroup = product.getTiePointGridGroup();
            if (group != productTiePointGridGroup) {
                final BandGroup autoGrouping = product.getAutoGrouping();
                if (autoGrouping != null) {
                    final int groupIndex = autoGrouping.indexOf(group.getDisplayName());
                    group.removeAll();
                    for (int i = 0; i < productTiePointGridGroup.getNodeCount(); i++) {
                        final TiePointGrid tiePointGrid = productTiePointGridGroup.get(i);
                        if (autoGrouping.indexOf(tiePointGrid.getName()) == groupIndex) {
                            group.add(tiePointGrid);
                        }
                    }
                }
            }
        }

        @Override
        public Product getProduct() {
            return product;
        }
    }

    public static class VDN extends PNGGroup<VectorDataNode> {

        public VDN(ProductNodeGroup<VectorDataNode> group) {
            super(Bundle.LBL_VectorDataGroupName(), group);
        }

        @Override
        protected PNNode createNodeForKey(VectorDataNode key) {
            return new PNNode.VDN(key);
        }
    }

    public static class M extends PNGGroup<Mask> {

        private final Product product;
        private final ProductNodeGroup<Mask> group;

        public M(String displayName, ProductNodeGroup<Mask> group, Product product) {
            super(displayName, group);
            this.product = product;
            this.group = group;
        }

        @Override
        protected PNNode createNodeForKey(Mask key) {
            return new PNNode.M(key);
        }

        @Override
        void refresh() {
            refreshGroup();
            super.refresh();
        }

        private void refreshGroup() {
            final ProductNodeGroup<Mask> productMaskGroup = product.getMaskGroup();
            if (group != productMaskGroup) {
                final BandGroup autoGrouping = product.getAutoGrouping();
                if (autoGrouping != null) {
                    final int groupIndex = autoGrouping.indexOf(group.getDisplayName());
                    group.removeAll();
                    for (int i = 0; i < productMaskGroup.getNodeCount(); i++) {
                        final Mask mask = productMaskGroup.get(i);
                        if (autoGrouping.indexOf(mask.getName()) == groupIndex) {
                            group.add(mask);
                        }
                    }
                }
            }
        }

        @Override
        public Product getProduct() {
            return product;
        }
    }

    public static class FC extends PNGGroup<FlagCoding> {

        public FC(ProductNodeGroup<FlagCoding> group) {
            super(Bundle.LBL_FlagCodingGroupName(), group);
        }

        @Override
        protected PNNode createNodeForKey(FlagCoding key) {
            return new PNNode.FC(key);
        }
    }

    public static class IC extends PNGGroup<IndexCoding> {

        public IC(ProductNodeGroup<IndexCoding> group) {
            super(Bundle.LBL_IndexCodingGroupName(), group);
        }

        @Override
        protected PNNode createNodeForKey(IndexCoding key) {
            return new PNNode.IC(key);
        }
    }

    public static class ME extends PNGGroup<MetadataElement> {

        public ME(ProductNodeGroup<MetadataElement> group) {
            super(Bundle.LBL_MetadataGroupName(), group);
        }

        @Override
        protected PNNode createNodeForKey(MetadataElement key) {
            return new PNNode.ME(key);
        }
    }

    public static class QL extends PNGGroup<Quicklook> {

        public QL(ProductNodeGroup<Quicklook> group) {
            super(Bundle.LBL_QuicklookGroupName(), group);
        }

        @Override
        protected PNNode createNodeForKey(Quicklook key) {
            return new PNNode.QL(key);
        }
    }
}
