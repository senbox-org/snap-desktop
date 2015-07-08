/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

/**
 * Action which closes all opened products.
 *
 * @author Norman
 */
@ActionID(category = "File", id = "CloseAllProductsAction")
@ActionRegistration(displayName = "#CTL_CloseAllProductsActionName")

@ActionReferences({
        @ActionReference(path = "Menu/File", position = 25)
//        @ActionReference(path = "Context/Product/Product", position = 70)
})
@NbBundle.Messages({"CTL_CloseAllProductsActionName=Close All Products"})
public final class CloseAllProductsAction extends AbstractAction implements LookupListener, ContextAwareAction {

    private final Lookup lkp;

    public CloseAllProductsAction() {
        this(Utilities.actionsGlobalContext());
    }

    public CloseAllProductsAction(Lookup lkp) {
        super(Bundle.CTL_CloseAllProductsActionName());
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnabled(false);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new CloseAllProductsAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        ProductNode productNode = lkp.lookup(ProductNode.class);
        setEnabled(productNode != null);
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
