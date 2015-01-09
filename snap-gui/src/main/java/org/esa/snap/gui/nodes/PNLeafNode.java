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
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.snap.gui.actions.file.OpenImageViewAction;
import org.openide.awt.Actions;
import org.openide.awt.UndoRedo;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.List;

/**
 * A node that represents some {@link org.esa.beam.framework.datamodel.ProductNode} (=PN).
 *
 * @author Norman
 */
class PNLeafNode<T extends ProductNode> extends BeanNode<T> implements UndoRedo.Provider {

    private final UndoRedo undoRedo;

    public PNLeafNode(T productNode, UndoRedo undoRedo) throws IntrospectionException {
        super(productNode, Children.LEAF, Lookups.singleton(productNode));
        this.undoRedo = undoRedo;
        setDisplayName(productNode.getDisplayName());
        setShortDescription(productNode.getDescription());
    }

    public T getProductNode() {
        return getBean();
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
    }

    /**
     * A node that represents a {@link org.esa.beam.framework.datamodel.MetadataElement} (=ME).
     *
     * @author Norman
     */
    static class ME extends PNLeafNode<MetadataElement> {

        public ME(MetadataElement element, UndoRedo undoRedo) throws IntrospectionException {
            super(element, undoRedo);
            setIconBaseWithExtension("org/esa/snap/gui/icons/RsMetaData16.gif");
        }
    }

    /**
     * A node that represents an {@link org.esa.beam.framework.datamodel.IndexCoding} (=IC).
     *
     * @author Norman
     */
    static class IC extends PNLeafNode<IndexCoding> {

        public IC(IndexCoding indexCoding, UndoRedo undoRedo) throws IntrospectionException {
            super(indexCoding, undoRedo);
            // todo
            //setIconBaseWithExtension("org/esa/snap/gui/icons/xxx.gif");
        }
    }

    /**
     * A node that represents a {@link org.esa.beam.framework.datamodel.FlagCoding} (=FC).
     *
     * @author Norman
     */
    static class FC extends PNLeafNode<FlagCoding> {

        public FC(FlagCoding flagCoding, UndoRedo undoRedo) throws IntrospectionException {
            super(flagCoding, undoRedo);
            // todo
            //setIconBaseWithExtension("org/esa/snap/gui/icons/xxx.gif");
        }
    }

    /**
     * A node that represents a {@link org.esa.beam.framework.datamodel.VectorDataNode} (=VDN).
     *
     * @author Norman
     */
    static class VDN extends PNLeafNode<VectorDataNode> {

        public VDN(VectorDataNode vectorDataNode, UndoRedo undoRedo) throws IntrospectionException {
            super(vectorDataNode, undoRedo);
            // todo
            //setIconBaseWithExtension("org/esa/snap/gui/icons/xxx.gif");
        }
    }

    /**
     * A node that represents a {@link org.esa.beam.framework.datamodel.TiePointGrid} (=TPG).
     *
     * @author Norman
     */
    static class TPG extends PNLeafNode<TiePointGrid> {

        public TPG(TiePointGrid tiePointGrid, UndoRedo undoRedo) throws IntrospectionException {
            super(tiePointGrid, undoRedo);
            // todo
            //setIconBaseWithExtension("org/esa/snap/gui/icons/xxx.gif");
        }

        @Override
        public Action[] getActions(boolean context) {
            List<? extends Action> actions = Utilities.actionsForPath("Context/Product/TPGrid");
            return actions.toArray(new Action[actions.size()]);

        }

        @Override
        public Action getPreferredAction() {
            return Actions.forID("File", "org.esa.snap.gui.actions.file.OpenImageViewAction");
        }

    }

    /**
     * A node that represents a {@link org.esa.beam.framework.datamodel.Mask} (=M).
     *
     * @author Norman
     */
    static class M extends PNLeafNode<Mask> {

        public M(Mask mask, UndoRedo undoRedo) throws IntrospectionException {
            super(mask, undoRedo);
            // todo
            //setIconBaseWithExtension("org/esa/snap/gui/icons/xxx.gif");
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[]{new OpenImageViewAction(this.getBean())};
        }

        @Override
        public Action getPreferredAction() {
            return new OpenImageViewAction(this.getBean());
        }

    }

    /**
     * A node that represents a {@link org.esa.beam.framework.datamodel.Band} (=B).
     *
     * @author Norman
     */
    static class B extends PNLeafNode<Band> {

        public B(Band band, UndoRedo undoRedo) throws IntrospectionException {
            super(band, undoRedo);
            setIconBaseWithExtension("org/esa/snap/gui/icons/RsBandAsSwath16.gif");
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            fireNodeDestroyed();
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
        public Action[] getActions(boolean context) {
            List<? extends Action> actions = Utilities.actionsForPath("Context/Product/Band");
            return actions.toArray(new Action[actions.size()]);
        }

        @Override
        public Action getPreferredAction() {
            return Actions.forID("File", "org.esa.snap.gui.actions.file.OpenImageViewAction");
        }
    }
}
