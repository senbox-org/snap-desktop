package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.v2.ProductLibraryItem;

import javax.swing.AbstractListModel;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class ProductListModel extends AbstractListModel<ProductLibraryItem> {

    private Map<ProductLibraryItem, ImageIcon> quickLookImages;
    private List<ProductLibraryItem> items;

    public ProductListModel() {
        clearProducts();
    }

    @Override
    public int getSize() {
        return this.items.size();
    }

    @Override
    public ProductLibraryItem getElementAt(int index) {
        return this.items.get(index);
    }

    public void addProducts(List<ProductLibraryItem> products) {
        int oldSize = this.items.size();
        this.items.addAll(products);
        fireIntervalAdded(this, oldSize, this.items.size());
    }

    public ImageIcon getProductQuickLookImage(ProductLibraryItem product) {
        return this.quickLookImages.get(product);
    }

    public void setProductQuickLookImage(ProductLibraryItem product, Image quickLookImage) {
        ImageIcon image = (quickLookImage == null) ? ProductListCellRenderer.EMPTY_ICON : new ImageIcon(quickLookImage);
        this.quickLookImages.put(product, image);
        fireContentsChanged(this, 0, this.items.size());
    }

    public void clearProducts() {
        this.items = new ArrayList<>();
        this.quickLookImages = new HashMap<ProductLibraryItem, ImageIcon>();
    }
}
