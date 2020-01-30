package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.product.library.ui.v2.repository.local.LocalProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

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
    private final Map<RepositoryProduct, DownloadProgressStatus> downloadingProductsProgressValue;

    public OutputProductResults() {
        this.scaledQuickLookImages = new HashMap<>();
        this.downloadingProductsProgressValue = new HashMap<>();
        this.localProductsMap = new HashMap<>();
    }

    public Map<RepositoryProduct, LocalProgressStatus> getLocalProductsMap() {
        return localProductsMap;
    }

    public Map<RepositoryProduct, DownloadProgressStatus> getDownloadingProductsProgressValue() {
        return downloadingProductsProgressValue;
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

    public void removePendingDownloadProducts() {
        java.util.List<RepositoryProduct> keysToRemove = new ArrayList<>(getDownloadingProductsProgressValue().size());
        Iterator<Map.Entry<RepositoryProduct, DownloadProgressStatus>> it = getDownloadingProductsProgressValue().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<RepositoryProduct, DownloadProgressStatus> entry = it.next();
            DownloadProgressStatus progressPercent = entry.getValue();
            if (progressPercent.isPendingDownload()) {
                keysToRemove.add(entry.getKey());
            } else if (progressPercent.isDownloading()) {
                if (progressPercent.getValue() < 100) {
                    progressPercent.setStatus(DownloadProgressStatus.STOP_DOWNLOADING);
                }
            }
        }
        for (int i=0; i<keysToRemove.size(); i++) {
            getDownloadingProductsProgressValue().remove(keysToRemove.get(i));
        }
    }
}
