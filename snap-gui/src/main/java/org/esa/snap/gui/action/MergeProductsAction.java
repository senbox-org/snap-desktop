/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.esa.snap.core.Product;

@ActionID(
        category = "Edit",
        id = "org.snap.gui.MergeProductsAction"
)
@ActionRegistration(
        displayName = "#CTL_MergeProductsAction"
)
@ActionReference(path = "Menu/Tools", position = 150, separatorAfter = 175)
@Messages("CTL_MergeProductsAction=Merge Products")
public final class MergeProductsAction implements ActionListener {

    private final List<Product> context;

    public MergeProductsAction(List<Product> context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        for (Product product : context) {
            // TODO use product
        }
    }
}
