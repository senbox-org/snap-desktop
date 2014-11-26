/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Band;
import org.esa.snap.gui.actions.file.OpenImageViewAction;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;

import javax.swing.Action;
import java.beans.IntrospectionException;

/**
 * @author Norman
 */
public class BNode extends PNLeafNode<Band> {

    public BNode(Band band) throws IntrospectionException {
        super(band);
        setIconBaseWithExtension("org/esa/snap/gui/icons/RsBandAsSwath16.gif");
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
