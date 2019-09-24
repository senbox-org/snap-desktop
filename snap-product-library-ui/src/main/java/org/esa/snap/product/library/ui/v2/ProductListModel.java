package org.esa.snap.product.library.ui.v2;

import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.util.ArrayList;
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

    private final Map<String, Map<String, String>> visibleAttributesPerMission;

    private Map<RepositoryProduct, ProgressPercent> downloadingProductsProgressValue;
    private Set<RepositoryProductPanel> selectedProducts;

    public ProductListModel() {
        super();

        this.visibleAttributesPerMission = new HashMap<>();
        clear();
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

    public List<RepositoryProduct> removePendingDownloadProducts() {
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
        return keysToRemove;
    }

    public void addPendingDownloadProducts(RepositoryProduct[] pendingProducts) {
        for (int i=0; i<pendingProducts.length; i++) {
            this.downloadingProductsProgressValue.put(pendingProducts[i], new ProgressPercent());
        }
    }

    public void setProductDownloadPercent(RepositoryProduct product, short progressPercent) {
        if (progressPercent >=0 && progressPercent <= 100) {
            ProgressPercent progressPercentItem = this.downloadingProductsProgressValue.get(product);
            if (progressPercentItem == null) {
                // the product does not exist into the list
            } else {
                progressPercentItem.setValue(progressPercent);
            }
        } else {
            throw new IllegalArgumentException("The progress percent value " + progressPercent + " is out of bounds.");
        }
    }

    public ProgressPercent getProductDownloadPercent(RepositoryProduct product) {
        return this.downloadingProductsProgressValue.get(product);
    }

    public void clear() {
        this.downloadingProductsProgressValue = new HashMap<RepositoryProduct, ProgressPercent>();
        this.selectedProducts = new HashSet<RepositoryProductPanel>();
    }
}

