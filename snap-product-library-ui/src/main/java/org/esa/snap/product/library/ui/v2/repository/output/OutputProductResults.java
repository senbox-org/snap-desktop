package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.product.library.ui.v2.repository.local.LocalProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 30/1/2020.
 */
public class OutputProductResults {

    public static final ImageIcon EMPTY_ICON;
    static {
        BufferedImage emptyImage = new BufferedImage(75, 75, BufferedImage.TYPE_INT_ARGB);
        EMPTY_ICON = new ImageIcon(emptyImage);
    }

    private final Map<RepositoryProduct, ImageIcon> scaledQuickLookImages;
    private final Map<RepositoryProduct, LocalProgressStatus> localProductsMap;
    private final List<RepositoryProduct> availableProducts;
    private final Map<RepositoryProduct, DownloadProgressStatus> downloadedProductsProgress;

    private int currentPageNumber;

    public OutputProductResults() {
        this.scaledQuickLookImages = new HashMap<>();
        this.localProductsMap = new HashMap<>();
        this.downloadedProductsProgress = new HashMap<>();
        this.availableProducts = new ArrayList<>();

        this.currentPageNumber = 0;
    }

    public DownloadProgressStatus getDownloadedProductProgress(RepositoryProduct repositoryProduct) {
        return this.downloadedProductsProgress.get(repositoryProduct);
    }

    public void addDownloadedProductProgress(RepositoryProduct repositoryProduct, DownloadProgressStatus downloadProgressStatus) {
        this.downloadedProductsProgress.put(repositoryProduct, downloadProgressStatus);
    }

    public LocalProgressStatus getOpeningProductStatus(RepositoryProduct repositoryProduct) {
        return this.localProductsMap.get(repositoryProduct);
    }

    public Map<RepositoryProduct, LocalProgressStatus> getLocalProductsMap() {
        return localProductsMap;
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

    public void setCurrentPageNumber(int currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    public int getCurrentPageNumber() {
        return this.currentPageNumber;
    }

    public int getAvailableProductCount() {
        return this.availableProducts.size();
    }

    public void addProducts(List<RepositoryProduct> products) {
        this.availableProducts.addAll(products);
    }

    public RepositoryProduct getProductAt(int index) {
        return this.availableProducts.get(index);
    }
}
