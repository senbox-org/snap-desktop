/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.file;

import org.esa.snap.gui.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Action which closes all opened products.
 *
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.actions.file.CloseAllProductsAction"
)
@ActionRegistration(
        displayName = "#CTL_CloseAllProductsActionName"
)
@ActionReference(path = "Menu/File", position = 40)
@NbBundle.Messages({
        "CTL_CloseAllProductsActionName=Close All Products"
})
public final class CloseAllProductsAction extends AbstractAction {

    public CloseAllProductsAction() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CloseProductAction.closeProducts(SnapApp.getInstance().getProductManager().getProducts());
    }
}
