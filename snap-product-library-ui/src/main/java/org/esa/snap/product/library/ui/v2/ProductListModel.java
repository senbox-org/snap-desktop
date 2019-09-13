package org.esa.snap.product.library.ui.v2;

import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.AbstractListModel;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class ProductListModel extends AbstractListModel<RepositoryProduct> {

    private Map<RepositoryProduct, ProgressPercent> downloadingProductsProgressValue;
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

    public void setProducts(List<RepositoryProduct> products) {
        this.items = new ArrayList<>(products);
        this.downloadingProductsProgressValue = new HashMap<RepositoryProduct, ProgressPercent>();
        fireIntervalAdded(this, 0, this.items.size());
    }

    public void clearProducts() {
        int oldSize = this.items.size();
        clear();
        fireIntervalRemoved(this, 0, oldSize);
    }

    public List<RepositoryProduct> getProducts() {
        return new ArrayList<>(this.items);
    }

    public void removePendingDownloadProducts() {
        List<RepositoryProduct> keysToRemove = new ArrayList<>(this.downloadingProductsProgressValue.size());
        Iterator<Map.Entry<RepositoryProduct, ProgressPercent>> it = this.downloadingProductsProgressValue.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<RepositoryProduct, ProgressPercent> entry = it.next();
            ProgressPercent progressPercent = entry.getValue();
            if (progressPercent.isPendingDownload()) {
                keysToRemove.add(entry.getKey());
            } else if (progressPercent.isDownloading()) {
                if (progressPercent.getValue() < 100) {
                    progressPercent.setStopDownloading();
                }
            }
        }
        for (int i=0; i<keysToRemove.size(); i++) {
            this.downloadingProductsProgressValue.remove(keysToRemove.get(i));
        }
        fireContentsChanged(this, 0, this.items.size());
    }

    public void addPendingDownloadProducts(RepositoryProduct[] pendingProducts) {
        for (int i=0; i<pendingProducts.length; i++) {
            ProgressPercent progressPercent = new ProgressPercent();
            this.downloadingProductsProgressValue.put(pendingProducts[i], new ProgressPercent());
        }
        fireContentsChanged(this, 0, this.items.size());
    }

    public void setProductDownloadPercent(RepositoryProduct product, short progressPercent) {
        if (progressPercent >=0 && progressPercent <= 100) {
            ProgressPercent progressPercentItem = this.downloadingProductsProgressValue.get(product);
            if (progressPercentItem == null) {
                throw new IllegalArgumentException("The product '"+product.getName()+"' to update the progress percent does not exist.");
            } else {
                progressPercentItem.setValue(progressPercent);
                fireContentsChanged(this, 0, this.items.size());
            }
        } else {
            throw new IllegalArgumentException("The progress percent value " + progressPercent + " is out of bounds.");
        }
    }

    public ProgressPercent getProductDownloadPercent(RepositoryProduct product) {
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
        this.downloadingProductsProgressValue = new HashMap<RepositoryProduct, ProgressPercent>();
    }
}

