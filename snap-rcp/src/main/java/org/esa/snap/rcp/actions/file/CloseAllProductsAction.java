/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

/**
 * Action which closes all opened products.
 *
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "CloseAllProductsAction"
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
        execute();
    }

    /**
     * Executes the action command.
     *
     * @return {@code Boolean.TRUE} on success, {@code Boolean.FALSE} on failure, or {@code null} on cancellation.
     */
    public Boolean execute() {
        List<Product> products = Arrays.asList(SnapApp.getDefault().getProductManager().getProducts());
        return new CloseProductAction(products).execute();
    }
}
