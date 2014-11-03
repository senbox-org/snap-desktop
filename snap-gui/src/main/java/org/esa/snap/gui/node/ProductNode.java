/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.node;

import java.beans.IntrospectionException;
import javax.swing.Action;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;
import org.esa.snap.core.Product;

/**
 *
 * @author Norman
 */
public class ProductNode extends BeanNode<Product> {

    @Messages("MSG_NUM_BANDS=#Bands: ")
    public ProductNode(Product product) throws IntrospectionException {
        super(product, Children.create(new BandChildFactory(product), true), Lookups.singleton(product));
        setDisplayName(product.getName());
        setShortDescription(Bundle.MSG_NUM_BANDS() + product.getBandCount());
        //setIconBaseWithExtension("org/fully/qualified/name/myicon.png");
    }

    @Override
    public Action[] getActions(boolean context) {
        //Define an array of actions here,
        //which will appear on the right-click popup menu:
        return super.getActions(context);
    }

    @Override
    public Action getPreferredAction() {
        //Define the action that will be invoked
        //when the user double-clicks on the node:
        return super.getPreferredAction();
    }
}
