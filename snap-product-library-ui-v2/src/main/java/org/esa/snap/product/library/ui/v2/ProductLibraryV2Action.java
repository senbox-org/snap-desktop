package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * The action class containing information about the option from the popup menu.
 *
 * Created by jcoravu on 21/7/2020.
 */
public abstract class ProductLibraryV2Action extends JMenuItem implements ActionListener {

    protected ProductLibraryToolViewV2 productLibraryToolView;

    public ProductLibraryV2Action(String text) {
        super(text);

        addActionListener(this);
    }

    public final void setProductLibraryToolView(ProductLibraryToolViewV2 productLibraryToolView) {
        if (productLibraryToolView == null) {
            throw new NullPointerException("The product library tool view is null.");
        }
        this.productLibraryToolView = productLibraryToolView;
    }

    public boolean canAddItemToPopupMenu(AbstractProductsRepositoryPanel visibleProductsRepositoryPanel, RepositoryProduct[] selectedProducts) {
        return true;
    }
}
