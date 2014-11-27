/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.snap.gui.actions.file.OpenImageViewAction;
import org.openide.awt.UndoRedo;

import javax.swing.Action;
import java.beans.IntrospectionException;

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
        return new Action[]{new OpenImageViewAction(this.getBean())};
    }

    @Override
    public Action getPreferredAction() {
        return new OpenImageViewAction(this.getBean());
    }

}
