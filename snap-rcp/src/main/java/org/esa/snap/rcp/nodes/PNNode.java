/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.nodes;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.FlagCoding;
import org.esa.snap.framework.datamodel.IndexCoding;
import org.esa.snap.framework.datamodel.Mask;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.datamodel.ProductNodeEvent;
import org.esa.snap.framework.datamodel.ProductNodeGroup;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.snap.framework.datamodel.VectorDataNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.OpenImageViewAction;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.Lookups;

import javax.swing.Action;
import java.awt.datatransfer.Transferable;
import java.io.IOException;

/**
 * A node that represents some {@link org.esa.snap.framework.datamodel.ProductNode} (=PN).
 *
 * @author Norman
 */
abstract class PNNode<T extends ProductNode> extends PNNodeBase {

    private final T productNode;
    private final PNNodeSupport nodeSupport;

    public PNNode(T productNode) {
        this(productNode, null);
    }

    public PNNode(T productNode, PNGroupBase childFactory) {
        super(childFactory, Lookups.singleton(productNode));
        this.productNode = productNode;
        setDisplayName(productNode.getName());
        setShortDescription(productNode.getDescription());
        nodeSupport = PNNodeSupport.create(this, childFactory);
    }

    public T getProductNode() {
        return productNode;
    }

    @Override
    public void nodeChanged(ProductNodeEvent event) {
        if (event.getSourceNode() == getProductNode()) {
            if (ProductNode.PROPERTY_NAME_NAME.equals(event.getPropertyName())) {
                setDisplayName(getProductNode().getName());
            }
            if (ProductNode.PROPERTY_NAME_DESCRIPTION.equals(event.getPropertyName())) {
                setShortDescription(getProductNode().getDescription());
            }
        }
        nodeSupport.nodeChanged(event);
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        nodeSupport.nodeDataChanged(event);
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
        nodeSupport.nodeAdded(event);
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
        nodeSupport.nodeRemoved(event);
    }

    @Override
    public PropertySet[] getPropertySets() {
        Sheet.Set set = new Sheet.Set();
        set.setDisplayName("Product Node Properties");
        set.put(new PropertySupport.ReadWrite<String>("name", String.class, "Name", "Name of the element") {
            @Override
            public String getValue() {
                return getProductNode().getName();
            }

            @Override
            public void setValue(String val) {
                getProductNode().setName(val);
                // todo - add undoable edit
            }
        });
        set.put(new PropertySupport.ReadWrite<String>("description", String.class, "Description", "Short description of the element") {
            @Override
            public String getValue() {
                return getProductNode().getDescription();
            }

            @Override
            public void setValue(String val) {
                getProductNode().setDescription(val);
                // todo - add undoable edit
            }
        });
        set.put(new PropertySupport.ReadOnly<Boolean>("modified", Boolean.class, "Modified", "Has the element been modified?") {
            @Override
            public Boolean getValue() {
                return getProductNode().isModified();
            }
        });
        return new PropertySet[]{
                set
        };
    }

    @Override
    public Action[] getActions(boolean context) {
        ProductNode productNode1 = getProductNode();
        return PNNodeSupport.getContextActions(productNode1);
    }

    public static Node create(ProductNode productNode) {
        if (productNode instanceof FlagCoding) {
            return new PNNode.FC((FlagCoding) productNode);
        } else if (productNode instanceof IndexCoding) {
            return new PNNode.IC((IndexCoding) productNode);
        } else if (productNode instanceof MetadataElement) {
            return new PNNode.ME((MetadataElement) productNode);
        } else if (productNode instanceof VectorDataNode) {
            return new PNNode.VDN((VectorDataNode) productNode);
        } else if (productNode instanceof TiePointGrid) {
            return new PNNode.TPG((TiePointGrid) productNode);
        } else if (productNode instanceof Mask) {
            return new PNNode.M((Mask) productNode);
        } else if (productNode instanceof Band) {
            return new PNNode.B((Band) productNode);
        }
        throw new IllegalStateException("unhandled product node type: " + productNode.getClass() + " named '" + productNode.getName() + "'");
    }

    static <T extends ProductNode> void deleteProductNode(Product product, ProductNodeGroup<T> group, T productNode) {
        // todo - close all document windows / layers that refer to productNode (nf/mp - 14.01.2015)
        int index = group.indexOf(productNode);
        if (group.remove(productNode)) {
            UndoRedo.Manager manager = SnapApp.getDefault().getUndoManager(product);
            if (manager != null) {
                manager.addEdit(new UndoableProductNodeDeletion<>(group, productNode, index));
            }
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.MetadataElement} (=ME).
     *
     * @author Norman
     */
    static class ME extends PNNode<MetadataElement> {

        public ME(MetadataElement element) {
            super(element, element.getElementGroup() != null ? new PNGGroup.ME(element.getElementGroup()) : null);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsMetaData16.gif");
        }

        @Override
        public boolean canDestroy() {
            return getProductNode().getParentElement() != null;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getParentElement().getElementGroup(),
                              getProductNode());
        }
    }

    /**
     * A node that represents an {@link org.esa.snap.framework.datamodel.IndexCoding} (=IC).
     *
     * @author Norman
     */
    static class IC extends PNNode<IndexCoding> {

        public IC(IndexCoding indexCoding) {
            super(indexCoding);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandIndexes16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getIndexCodingGroup(),
                              getProductNode());
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.FlagCoding} (=FC).
     *
     * @author Norman
     */
    static class FC extends PNNode<FlagCoding> {

        public FC(FlagCoding flagCoding) {
            super(flagCoding);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandFlags16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getFlagCodingGroup(),
                              getProductNode());
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.VectorDataNode} (=VDN).
     *
     * @author Norman
     */
    static class VDN extends PNNode<VectorDataNode> {

        public VDN(VectorDataNode vectorDataNode) {
            super(vectorDataNode);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsVectorData16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getVectorDataGroup(),
                              getProductNode());
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.TiePointGrid} (=TPG).
     *
     * @author Norman
     */
    static class TPG extends PNNode<TiePointGrid> {

        public TPG(TiePointGrid tiePointGrid) {
            super(tiePointGrid);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandAsTiePoint16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getTiePointGridGroup(),
                              getProductNode());
        }

        @Override
        public Action getPreferredAction() {
            return new OpenImageViewAction(this.getProductNode());
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.Mask} (=M).
     *
     * @author Norman
     */
    static class M extends PNNode<Mask> {

        public M(Mask mask) {
            super(mask);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsMask16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getMaskGroup(),
                              getProductNode());
        }

        @Override
        public Action getPreferredAction() {
            return new OpenImageViewAction(this.getProductNode());
        }
    }

    /**
     * A node that represents a {@link org.esa.snap.framework.datamodel.Band} (=B).
     *
     * @author Norman
     */
    static class B extends PNNode<Band> {

        public B(Band band) {
            super(band);
            setIconBaseWithExtension("org/esa/snap/rcp/icons/RsBandAsSwath16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            deleteProductNode(getProductNode().getProduct(),
                              getProductNode().getProduct().getBandGroup(),
                              getProductNode());
        }

        @Override
        public Transferable clipboardCopy() throws IOException {
            return super.clipboardCopy();
        }

        @Override
        public boolean canCopy() {
            return true;
        }

        @Override
        public Transferable clipboardCut() throws IOException {
            return super.clipboardCut();
        }

        @Override
        public boolean canCut() {
            return true;
        }

        @Override
        public Action getPreferredAction() {
            return new OpenImageViewAction(this.getProductNode());
        }
    }
}
