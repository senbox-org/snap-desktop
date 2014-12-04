/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.tools;

import org.esa.beam.framework.datamodel.Product;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 *  * Experimental!
 */
@ActionID(
        category = "Tools",
        id = "org.snap.gui.MergeProductsAction"
)
@ActionRegistration(
        displayName = "#CTL_MergeProductsAction"
)
@ActionReference(path = "Menu/Tools", position = 200, separatorAfter = 250)
@Messages("CTL_MergeProductsAction=Merge Products")
public final class MergeProductsAction implements ActionListener {

    private final List<Product> context;

    public MergeProductsAction(List<Product> context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        for (Product product : context) {
            System.out.println("product = " + product.getName());
        }
    }
}
