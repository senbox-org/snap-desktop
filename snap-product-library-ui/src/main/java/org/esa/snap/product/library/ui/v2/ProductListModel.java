package org.esa.snap.product.library.ui.v2;

import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class ProductListModel {

    public static final ImageIcon EMPTY_ICON;
    static {
        BufferedImage emptyImage = new BufferedImage(75, 75, BufferedImage.TYPE_INT_ARGB);
        EMPTY_ICON = new ImageIcon(emptyImage);
    }

    private final Map<String, Map<String, String>> visibleAttributesPerMission;

    private Map<RepositoryProduct, ProgressPercent> downloadingProductsProgressValue;
    private Map<RepositoryProduct, ImageIcon> scaledQuickLookImages;
    private List<RepositoryProduct> products;

    public ProductListModel() {
        super();

        this.visibleAttributesPerMission = new HashMap<>();
        this.downloadingProductsProgressValue = new HashMap<>();
        this.scaledQuickLookImages = new HashMap<>();
        this.products = new ArrayList<>();
    }

    protected void fireIntervalAdded(int startIndex, int endIndex) {
    }

    protected void fireIntervalRemoved(int startIndex, int endIndex) {
    }

    protected void fireIntervalChanged(int startIndex, int endIndex) {
    }

    public int getProductCount() {
        return this.products.size();
    }

    public RepositoryProduct getProductAt(int index) {
        return this.products.get(index);
    }

    public Map<String, String> getMissionVisibleAttributes(String mission) {
        Map<String, String> visibleAttributes = this.visibleAttributesPerMission.get(mission);
        if (visibleAttributes == null) {
            Set<RemoteProductsRepositoryProvider> remoteProductsRepositoryProviders = ProductLibraryToolViewV2.getRemoteProductsRepositoryProviders();
            for (RemoteProductsRepositoryProvider repositoryProvider : remoteProductsRepositoryProviders) {
                String[] availableMissions = repositoryProvider.getAvailableMissions();
                for (int i=0; i<availableMissions.length; i++) {
                    if (availableMissions[i].equalsIgnoreCase(mission)) {
                        visibleAttributes = repositoryProvider.getDisplayedAttributes();
                        this.visibleAttributesPerMission.put(mission, visibleAttributes);
                        break;
                    }
                }
            }
        }
        return visibleAttributes;
    }

    public void setProductQuickLookImage(RepositoryProduct repositoryProduct, BufferedImage quickLookImage) {
        for (int i=0; i<this.products.size(); i++) {
            RepositoryProduct existingProduct = this.products.get(i);
            if (existingProduct == repositoryProduct) {
                existingProduct.setQuickLookImage(quickLookImage);
                fireIntervalChanged(i, i);
                return;
            }
        }
        throw new IllegalArgumentException("The repository product '"+repositoryProduct.getName()+"' does not exist into the list.");
    }

    public void addProducts(List<RepositoryProduct> products, Comparator<RepositoryProduct> comparator) {
        if (products.size() > 0) {
            int startIndex = this.products.size();
            this.products.addAll(products);
            if (this.products.size() > 1) {
                Collections.sort(this.products, comparator);
            }
            int endIndex = this.products.size() - 1;
            fireIntervalAdded(startIndex, endIndex);
        }
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
        if (this.products.size() > 0) {
            fireIntervalChanged(0, this.products.size()-1);
        }
    }

    public List<RepositoryProduct> addPendingDownloadProducts(RepositoryProduct[] pendingProducts) {
        List<RepositoryProduct> productsToDownload = new ArrayList<>(pendingProducts.length);
        if (pendingProducts.length > 0) {
            int startIndex = pendingProducts.length - 1;
            int endIndex = 0;
            for (int i=0; i<pendingProducts.length; i++) {
                ProgressPercent progressPercent = this.downloadingProductsProgressValue.get(pendingProducts[i]);
                if (progressPercent == null || progressPercent.isStoppedDownload()) {
                    productsToDownload.add(pendingProducts[i]);
                    int index = findProductIndex(pendingProducts[i]);
                    if (startIndex > index) {
                        startIndex = index;
                    }
                    if (endIndex < index) {
                        endIndex = index;
                    }
                    this.downloadingProductsProgressValue.put(pendingProducts[i], new ProgressPercent());
                }
            }
            if (productsToDownload.size() > 0) {
                fireIntervalChanged(startIndex, endIndex);
            }
        }
        return productsToDownload;
    }

    private int findProductIndex(RepositoryProduct repositoryProductToFind) {
        for (int i=0; i<this.products.size(); i++) {
            if (this.products.get(i) == repositoryProductToFind) {
                return i;
            }
        }
        throw new IllegalArgumentException("The repository product '"+repositoryProductToFind.getName()+"' does not exist into the list.");
    }

    public void setStopDownloadingProduct(RepositoryProduct repositoryProduct) {
        ProgressPercent progressPercent = this.downloadingProductsProgressValue.get(repositoryProduct);
        if (progressPercent != null) {
            progressPercent.setStopDownloading();
            int index = findProductIndex(repositoryProduct);
            fireIntervalChanged(index, index);
        }
    }

    public void setFailedDownloadingProduct(RepositoryProduct repositoryProduct) {
        ProgressPercent progressPercent = this.downloadingProductsProgressValue.get(repositoryProduct);
        if (progressPercent != null) {
            progressPercent.setFailedDownloading();
            int index = findProductIndex(repositoryProduct);
            fireIntervalChanged(index, index);
        }
    }

    public void setProductDownloadPercent(RepositoryProduct repositoryProduct, short progressPercent) {
        ProgressPercent progressPercentItem = this.downloadingProductsProgressValue.get(repositoryProduct);
        if (progressPercentItem != null) {
            progressPercentItem.setValue(progressPercent);
            int index = findProductIndex(repositoryProduct);
            fireIntervalChanged(index, index);
        }
    }

    public ProgressPercent getProductDownloadPercent(RepositoryProduct repositoryProduct) {
        return this.downloadingProductsProgressValue.get(repositoryProduct);
    }

    public ImageIcon getProductQuickLookImage(RepositoryProduct repositoryProduct) {
        ImageIcon imageIcon = this.scaledQuickLookImages.get(repositoryProduct);
        if (imageIcon == null) {
            if (repositoryProduct.getQuickLookImage() == null) {
                imageIcon = EMPTY_ICON;
            } else {
                Image scaledQuickLookImage = repositoryProduct.getQuickLookImage().getScaledInstance(EMPTY_ICON.getIconWidth(), EMPTY_ICON.getIconHeight(), BufferedImage.SCALE_FAST);
                imageIcon = new ImageIcon(scaledQuickLookImage);
                this.scaledQuickLookImages.put(repositoryProduct, imageIcon);
            }
        }
        return imageIcon;
    }

    public void clear() {
        int endIndex = this.products.size() - 1;
        this.downloadingProductsProgressValue = new HashMap<>();
        this.scaledQuickLookImages = new HashMap<>();
        this.products = new ArrayList<>();
        if (endIndex >= 0) {
            fireIntervalRemoved(0, endIndex);
        }
    }

    public void sortProducts(Comparator<RepositoryProduct> comparator) {
        if (this.products.size() > 1) {
            Collections.sort(this.products, comparator);
            int startIndex = 0;
            int endIndex = this.products.size() - 1;
            fireIntervalChanged(startIndex, endIndex);
        }
    }
}

