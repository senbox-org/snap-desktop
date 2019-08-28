package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.v2.RepositoryProduct;

import javax.swing.AbstractListModel;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class ProductListModel extends AbstractListModel<RepositoryProduct> {

    private Map<RepositoryProduct, Short> downloadingProductsProgressValue;
    private Map<RepositoryProduct, ImageIcon> quickLookImages;
    private List<RepositoryProduct> items;

    public ProductListModel() {
        super();

        clear();
    }

    @Override
    public int getSize() {
        return this.items.size();
    }

    @Override
    public RepositoryProduct getElementAt(int index) {
        return this.items.get(index);
    }

    public void addProducts(List<RepositoryProduct> products) {
        int oldSize = this.items.size();
        this.items.addAll(products);
        fireIntervalAdded(this, oldSize, this.items.size());
    }

    public void clearProducts() {
        int oldSize = this.items.size();
        clear();
        fireIntervalRemoved(this, 0, oldSize);
    }

    public List<RepositoryProduct> getProducts() {
        return new ArrayList<>(this.items);
    }

    public ImageIcon getProductQuickLookImage(RepositoryProduct product) {
        return this.quickLookImages.get(product);
    }

    public void setProductQuickLookImage(RepositoryProduct product, Image quickLookImage) {
        ImageIcon image = (quickLookImage == null) ? ProductListCellRenderer.EMPTY_ICON : new ImageIcon(quickLookImage);
        this.quickLookImages.put(product, image);
        fireContentsChanged(this, 0, this.items.size());
    }

    public void setProductDownloadPercent(RepositoryProduct product, short percent) {
        this.downloadingProductsProgressValue.put(product, percent);
        fireContentsChanged(this, 0, this.items.size());
    }

    public Short getProductDownloadPercent(RepositoryProduct product) {
        return this.downloadingProductsProgressValue.get(product);
    }

    public void sortProducts(Comparator<RepositoryProduct> comparator) {
        if (this.items.size() > 1) {
            Collections.sort(this.items, comparator);
            fireContentsChanged(this, 0, this.items.size());
        }
    }

    private void clear() {
        this.items = new ArrayList<>();
        this.quickLookImages = new HashMap<RepositoryProduct, ImageIcon>();
        this.downloadingProductsProgressValue = new HashMap<RepositoryProduct, Short>();
    }
}

