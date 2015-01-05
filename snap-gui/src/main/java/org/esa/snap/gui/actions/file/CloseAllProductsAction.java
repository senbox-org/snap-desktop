/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.file;

import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.nodes.PNodeFactory;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
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
        List<Product> openedProducts = PNodeFactory.getInstance().getOpenedProducts();
        CloseProductAction.closeProducts(openedProducts.toArray(new Product[openedProducts.size()]));
    }

}
