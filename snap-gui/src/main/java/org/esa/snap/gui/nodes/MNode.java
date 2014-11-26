/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.snap.gui.actions.file.OpenImageViewAction;

import javax.swing.Action;
import java.beans.IntrospectionException;

/**
 * A node that represents a {@link org.esa.beam.framework.datamodel.Mask} (=M).
 *
 * @author Norman
 */
public class MNode extends PNLeafNode<Mask> {

    public MNode(Mask mask) throws IntrospectionException {
        super(mask);
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
