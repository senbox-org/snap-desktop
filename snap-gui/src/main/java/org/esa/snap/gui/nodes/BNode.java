/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Band;
import org.esa.snap.gui.actions.file.OpenImageViewAction;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.awt.Actions;
import org.openide.awt.UndoRedo;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;

import javax.swing.Action;
import java.awt.datatransfer.Transferable;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A node that represents a {@link org.esa.beam.framework.datamodel.Band} (=B).
 *
 * @author Norman
 */
public class BNode extends PNLeafNode<Band> {

    public BNode(Band band, UndoRedo undoRedo) throws IntrospectionException {
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
