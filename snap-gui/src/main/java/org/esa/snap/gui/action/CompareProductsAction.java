/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.esa.snap.core.Product;

/**
 *
 * @author Norman
 */
@ActionID(
        category = "Tools",
        id = "org.snap.gui.CompareProductsAction"
)
@ActionRegistration(
        displayName = "Compare Products",
        lazy = false
)
@ActionReference(path = "Menu/Tools", position = 0)
public class CompareProductsAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private final Lookup.Result<Product> context;

    public CompareProductsAction() {
        this(Utilities.actionsGlobalContext());
    }

    public CompareProductsAction(Lookup lookup) {
        super("Compare Products");
        context = lookup.lookupResult(Product.class);
        context.addLookupListener(WeakListeners.create(LookupListener.class, this, context));
        setEnabled(false);
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new CompareProductsAction(lkp);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String m = "<html>";
        for (Product product : context.allInstances()) {
            //Optionally, check if the property is set to the value you're interested in
            //prior to doing something with the Object.
            m += "<b>Name</b>: " + product.getName() + "<br/>";
        }
        JOptionPane.showMessageDialog(null, m);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        super.setEnabled(context.allInstances().size() >= 2);
    }
}
