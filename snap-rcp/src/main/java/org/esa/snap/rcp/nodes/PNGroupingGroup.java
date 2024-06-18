/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import com.bc.ceres.core.Assert;
import eu.esa.snap.core.datamodel.group.BandGrouping;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A group that gets its nodes from a {@link ProductNodeGroup} (=PNG)
 * and can have a {@link ProductNodeGroup} as child
 *
 * @author Tonio Fincke
 */
@NbBundle.Messages({
        "LBL_TiePointGroupName=Tie-Point Grids",
        "LBL_BandGroupName=Bands",
        "LBL_MaskGroupName=Masks",
})
abstract class PNGroupingGroup extends PNGroup<Object> {

    private final String displayName;
    private final ProductNodeGroup group;
    List<Node> nodes;
    Map<String, Boolean> nodesAreSelected;

    protected PNGroupingGroup(String displayName, ProductNodeGroup group) {
        Assert.notNull(group, "group");
        this.displayName = displayName;
        this.group = group;
        nodes = new ArrayList<>();
        nodesAreSelected = new HashMap<>();
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
        return group.contains(productNode);
    }

    @Override
    void refresh() {
        for (Node node : nodes) {
            nodesAreSelected.put(node.getDisplayName(), NodeExpansionManager.isNodeExpanded(node));
        }
        nodes.clear();
        super.refresh();
        for (Node node : nodes) {
            if (nodesAreSelected.containsKey(node.getDisplayName()) && nodesAreSelected.get(node.getDisplayName())) {
                NodeExpansionManager.expandNode(node);
            }
        }
        nodesAreSelected.clear();
    }

    @Override
    boolean shallReactToPropertyChange(String propertyName) {
        return propertyName.equals("autoGrouping");
    }

    /**
     * A group that represents a {@link ProductNodeGroup}
     * of {@link Band} members
     *
     * @author Tonio
     */
    static class B extends PNGroupingGroup {

        private final ProductNodeGroup<Band> group;

        protected B(ProductNodeGroup<Band> group) {
            super(Bundle.LBL_BandGroupName(), group);
            this.group = group;
        }

        @Override
        protected Node createNodeForKey(Object key) {
            if (key instanceof Band) {
                return new PNNode.B((Band) key);
            } else {
                final PNGroupNode pnGroupNode = new PNGroupNode((PNGroup) key);
                nodes.add(pnGroupNode);
                return pnGroupNode;
            }
        }

        @Override
        protected boolean createKeys(List<Object> list) {
            final BandGrouping autoGrouping = getProduct().getAutoGrouping();
            if (autoGrouping == null) {
                for (int i = 0; i < group.getNodeCount(); i++) {
                    list.add(group.get(i));
                }
                return true;
            }
            ProductNodeGroup<Band>[] autogroupingNodes = new ProductNodeGroup[autoGrouping.size() + 1];
            for (int i = 0; i < autoGrouping.size(); i++) {
                autogroupingNodes[i] = new ProductNodeGroup<>(autoGrouping.get(i)[0]);
            }
            autogroupingNodes[autoGrouping.size()] = new ProductNodeGroup<>(Bundle.LBL_BandGroupName());
            for (int i = 0; i < this.group.getNodeCount(); i++) {
                final Band band = this.group.get(i);
                final int index = autoGrouping.indexOf(band.getName());
                if (index != -1) {
                    autogroupingNodes[index].add(band);
                } else {
                    autogroupingNodes[autoGrouping.size()].add(band);
                }
            }
            for (int i = 0; i < autoGrouping.size(); i++) {
                if (autogroupingNodes[i].getNodeCount() > 0) {
                    list.add(new PNGGroup.B(autoGrouping.get(i)[0], autogroupingNodes[i], getProduct()));
                }
            }
            for (int i = 0; i < autogroupingNodes[autoGrouping.size()].getNodeCount(); i++) {
                list.add(autogroupingNodes[autoGrouping.size()].get(i));
            }
            return true;
        }

    }

    /**
     * A group that represents a {@link ProductNodeGroup}
     * of {@link TiePointGrid} members
     *
     * @author Tonio
     */
    static class TPG extends PNGroupingGroup {

        private final ProductNodeGroup<TiePointGrid> group;

        protected TPG(ProductNodeGroup<TiePointGrid> group) {
            super(Bundle.LBL_TiePointGroupName(), group);
            this.group = group;
        }

        @Override
        protected Node createNodeForKey(Object key) {
            if (key instanceof TiePointGrid) {
                return new PNNode.TPG((TiePointGrid) key);
            } else {
                final PNGroupNode pnGroupNode = new PNGroupNode((PNGroup) key);
                nodes.add(pnGroupNode);
                return pnGroupNode;
            }
        }

        @Override
        protected boolean createKeys(List<Object> list) {
            final BandGrouping autoGrouping = getProduct().getAutoGrouping();
            if (autoGrouping == null) {
                for (int i = 0; i < group.getNodeCount(); i++) {
                    list.add(group.get(i));
                }
                return true;
            }
            ProductNodeGroup<TiePointGrid>[] autogroupingNodes = new ProductNodeGroup[autoGrouping.size() + 1];
            for (int i = 0; i < autoGrouping.size(); i++) {
                autogroupingNodes[i] = new ProductNodeGroup<>(autoGrouping.get(i)[0]);
            }
            autogroupingNodes[autoGrouping.size()] = new ProductNodeGroup<>(Bundle.LBL_TiePointGroupName());
            for (int i = 0; i < this.group.getNodeCount(); i++) {
                final TiePointGrid tiePointGrid = this.group.get(i);
                final int index = autoGrouping.indexOf(tiePointGrid.getName());
                if (index != -1) {
                    autogroupingNodes[index].add(tiePointGrid);
                } else {
                    autogroupingNodes[autoGrouping.size()].add(tiePointGrid);
                }
            }
            for (int i = 0; i < autoGrouping.size(); i++) {
                if (autogroupingNodes[i].getNodeCount() > 0) {
                    list.add(new PNGGroup.TPG(autoGrouping.get(i)[0], autogroupingNodes[i], getProduct()));
                }
            }
            for (int i = 0; i < autogroupingNodes[autoGrouping.size()].getNodeCount(); i++) {
                list.add(autogroupingNodes[autoGrouping.size()].get(i));
            }
            return true;
        }

    }


    /**
     * A group that represents a {@link ProductNodeGroup}
     * of {@link Mask} members
     *
     * @author Tonio
     */
    static class M extends PNGroupingGroup {

        private final ProductNodeGroup<Mask> group;

        protected M(ProductNodeGroup<Mask> group) {
            super(Bundle.LBL_MaskGroupName(), group);
            this.group = group;
        }

        @Override
        protected Node createNodeForKey(Object key) {
            if (key instanceof Mask) {
                return new PNNode.M((Mask) key);
            } else {
                final PNGroupNode pnGroupNode = new PNGroupNode((PNGroup) key);
                nodes.add(pnGroupNode);
                return pnGroupNode;
            }
        }

        @Override
        protected boolean createKeys(List<Object> list) {
            final BandGrouping autoGrouping = getProduct().getAutoGrouping();
            if (autoGrouping == null) {
                for (int i = 0; i < group.getNodeCount(); i++) {
                    list.add(group.get(i));
                }
                return true;
            }
            ProductNodeGroup<Mask>[] autogroupingNodes = new ProductNodeGroup[autoGrouping.size() + 1];
            for (int i = 0; i < autoGrouping.size(); i++) {
                autogroupingNodes[i] = new ProductNodeGroup<>(autoGrouping.get(i)[0]);
            }
            autogroupingNodes[autoGrouping.size()] = new ProductNodeGroup<>(Bundle.LBL_MaskGroupName());
            for (int i = 0; i < this.group.getNodeCount(); i++) {
                final Mask mask = this.group.get(i);
                final int index = autoGrouping.indexOf(mask.getName());
                if (index != -1) {
                    autogroupingNodes[index].add(mask);
                } else {
                    autogroupingNodes[autoGrouping.size()].add(mask);
                }
            }
            for (int i = 0; i < autoGrouping.size(); i++) {
                if (autogroupingNodes[i].getNodeCount() > 0) {
                    list.add(new PNGGroup.M(autoGrouping.get(i)[0], autogroupingNodes[i], getProduct()));
                }
            }
            for (int i = 0; i < autogroupingNodes[autoGrouping.size()].getNodeCount(); i++) {
                list.add(autogroupingNodes[autoGrouping.size()].get(i));
            }
            return true;
        }

    }
}