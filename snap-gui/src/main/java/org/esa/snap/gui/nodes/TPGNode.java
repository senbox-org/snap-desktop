/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.snap.gui.actions.file.OpenImageViewAction;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.awt.Actions;
import org.openide.awt.UndoRedo;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;

import javax.swing.Action;
import java.beans.IntrospectionException;
import java.util.ArrayList;

/**
 * A node that represents a {@link org.esa.beam.framework.datamodel.TiePointGrid} (=TPG).
 *
 * @author Norman
 */
public class TPGNode extends PNLeafNode<TiePointGrid> {

    public TPGNode(TiePointGrid tiePointGrid, UndoRedo undoRedo) throws IntrospectionException {
        super(tiePointGrid, undoRedo);
        // todo
        //setIconBaseWithExtension("org/esa/snap/gui/icons/xxx.gif");
    }

    @Override
    public Action[] getActions(boolean context) {
        ArrayList<Action> actions = new ArrayList<>(Utilities.actionsForPath("Context/Product/TPGrid"));
        return actions.toArray(new Action[actions.size()]);

    }

    @Override
    public Action getPreferredAction() {
        return Actions.forID("File", "org.esa.snap.gui.actions.file.OpenImageViewAction");
    }

}
