/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.snap.gui.SnapApp;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Node;
import org.openide.util.Utilities;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A node that represents a {@link org.esa.beam.framework.datamodel.Product} (=P).
 * Every {@code PNode} holds a dedicated undo/redo context.
 *
 * @author Norman
 */
class PNode extends PNNode<Product> {

    private final PContent group;
    // todo - clean me up: this is an experimental property
    boolean flattenRasterDataGroups;

    public PNode(Product product) {
        this(product, new PContent());
    }

    private PNode(Product product, PContent group) {
        super(product, group);
        this.group = group;
        group.node = this;
        setDisplayName(product.getName());
        setShortDescription(product.getDescription());
        setIconBaseWithExtension("org/esa/snap/gui/icons/RsProduct16.gif");
    }

    public boolean getFlattenRasterDataGroups() {
        return flattenRasterDataGroups;
    }

    public void setFlattenRasterDataGroups(boolean flattenRasterDataGroups) {
        this.flattenRasterDataGroups = flattenRasterDataGroups;
        group.refresh();
    }

    public Product getProduct() {
        return getProductNode();
    }

    @Override
    public UndoRedo getUndoRedo() {
        return SnapApp.getDefault().getUndoManager(getProduct());
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public void destroy() throws IOException {

    }

    @Override
    public Action[] getActions(boolean context) {
        List<? extends Action> actions = Utilities.actionsForPath("Context/Product/Product");
        ArrayList<Action> actions1 = new ArrayList<>(actions);
        // todo - clean me up: this is experimental code
        AbstractAction abstractAction = new AbstractAction("Toggle Grouping") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFlattenRasterDataGroups(!getFlattenRasterDataGroups());
            }
        };
        actions1.add(abstractAction);
        return actions1.toArray(new Action[actions1.size()]);
    }

    @Override
    public Action getPreferredAction() {
        //Define the action that will be invoked
        //when the user double-clicks on the node:
        return super.getPreferredAction();
    }

    /*
    @Override
    public NewType[] getNewTypes() {
        return new NewType[] {
                new NewType() {
                    @Override
                    public String getName() {
                        return "Calculated Band";
                    }

                    @Override
                    public void create() throws IOException {
                    }
                },
                new NewType() {
                    @Override
                    public String getName() {
                        return "Filtered Band";
                    }

                    @Override
                    public void create() throws IOException {
                    }
                }
        };
    }
    */

    /**
     * A child factory for nodes below a {@link PNode} that holds a {@link org.esa.beam.framework.datamodel.Product}.
     *
     * @author Norman
     */
    static class PContent extends PNGroupBase<Object> {

        PNode node;

        @Override
        protected boolean createKeys(List<Object> list) {
            Product product = node.getProduct();
            list.add(new PNGGroup.ME(product.getMetadataRoot().getElementGroup()));
            if (product.getIndexCodingGroup().getNodeCount() > 0) {
                list.add(new PNGGroup.IC(product.getIndexCodingGroup()));
            }
            if (product.getFlagCodingGroup().getNodeCount() > 0) {
                list.add(new PNGGroup.FC(product.getFlagCodingGroup()));
            }
            if (product.getVectorDataGroup().getNodeCount() > 0) {
                list.add(new PNGGroup.VDN(product.getVectorDataGroup()));
            }

            if (!node.getFlattenRasterDataGroups()) {
                if (product.getTiePointGridGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.TPG(product.getTiePointGridGroup()));
                }
                if (product.getBandGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.B(product.getBandGroup()));
                }
                if (product.getMaskGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.M(product.getMaskGroup()));
                }
            } else {
                list.addAll(Arrays.asList(product.getTiePointGridGroup().toArray()));
                list.addAll(Arrays.asList(product.getBandGroup().toArray()));
                list.addAll(Arrays.asList(product.getMaskGroup().toArray()));
            }

            return true;
        }

        @Override
        protected Node createNodeForKey(Object key) {
            if (key instanceof ProductNode) {
                return PNNode.create((ProductNode) key);
            } else {
                return new PNGroupNode((PNGGroup) key);
            }
        }
    }
}
