/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.openide.awt.UndoRedo;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.util.lookup.Lookups;

import javax.swing.Action;
import java.beans.IntrospectionException;

/**
 * A node that represents a {@link org.esa.beam.framework.datamodel.Product} (=P).
 * Every {@code PNode} holds a dedicated undo/redo context.
 *
 * @author Norman
 */
public class PNode extends BeanNode<Product> implements UndoRedo.Provider {

    private final UndoRedo.Manager undoRedo;

    public PNode(Product product, UndoRedo.Manager undoRedo) throws IntrospectionException {
        super(product, Children.create(new PChildFactory(product, undoRedo), false), Lookups.fixed(product));
        this.undoRedo = undoRedo;
        setDisplayName(product.getName());
        setShortDescription(product.getDescription());
        setIconBaseWithExtension("org/esa/snap/gui/icons/RsProduct16.gif");
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
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
}
