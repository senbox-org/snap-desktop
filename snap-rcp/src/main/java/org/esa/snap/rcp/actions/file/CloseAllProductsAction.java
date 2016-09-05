/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Action which closes all opened products.
 *
 * @author Norman
 */
@ActionID(category = "File", id = "CloseAllProductsAction")
@ActionRegistration(displayName = "#CTL_CloseAllProductsActionName", lazy = false)

@ActionReferences({
        @ActionReference(path = "Menu/File", position = 25),
        @ActionReference(path = "Context/Product/Product", position = 70)
})
@NbBundle.Messages({"CTL_CloseAllProductsActionName=Close All Products"})
public final class CloseAllProductsAction extends AbstractAction {

    public CloseAllProductsAction() {
        super(Bundle.CTL_CloseAllProductsActionName());
        ProductManager productManager = SnapApp.getDefault().getProductManager();
        productManager.addListener(new CloseAllProductListener());
        setEnabled(false);
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
        Set<Product> collect = Stream.of(SnapApp.getDefault().getProductManager().getProducts()).collect(Collectors.toSet());
        return CloseProductAction.closeProducts(collect);
    }

    private class CloseAllProductListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            updateEnableState();
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            updateEnableState();
        }

        private void updateEnableState() {
            setEnabled(SnapApp.getDefault().getProductManager().getProductCount() > 0);
        }
    }

}
