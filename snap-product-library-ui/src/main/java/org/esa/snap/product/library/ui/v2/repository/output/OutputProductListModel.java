package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.product.library.ui.v2.repository.local.LocalProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
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
public class OutputProductListModel {

    private final OutputProductResultsCallback outputProductResultsCallback;

    private List<RepositoryProduct> products;

    public OutputProductListModel(OutputProductResultsCallback outputProductResultsCallback) {
        super();

        this.outputProductResultsCallback = outputProductResultsCallback;
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

    public ImageIcon getProductQuickLookImage(RepositoryProduct repositoryProduct) {
        return this.outputProductResultsCallback.getOutputProductResults().getProductQuickLookImage(repositoryProduct);
    }

    public void updateProductQuickLookImage(RepositoryProduct repositoryProduct) {
        int index = findProductIndex(repositoryProduct);
        if (index >= 0) {
            fireIntervalChanged(index, index);
        }
    }

    public List<RepositoryProduct> findProductsWithoutQuickLookImage() {
        List<RepositoryProduct> result = new ArrayList<>();
        for (int i=0; i<this.products.size(); i++) {
            RepositoryProduct existingProduct = this.products.get(i);
            if (existingProduct.getQuickLookImage() == null && existingProduct.getDownloadQuickLookImageURL() != null) {
                result.add(existingProduct);
            }
        }
        return result;
    }

    public void setProducts(List<RepositoryProduct> products) {
        if (products.size() > 0) {
            int oldProductCount = this.products.size();
            int newProductCount = products.size();

            this.products = new ArrayList<>(products);
            if (this.products.size() > 1) {
                Collections.sort(this.products, this.outputProductResultsCallback.getProductsComparator());
            }

            if (oldProductCount < newProductCount) {
                if (oldProductCount > 0) {
                    fireIntervalChanged(0, oldProductCount - 1);
                }
                fireIntervalAdded(oldProductCount, newProductCount-1);
            } else if (oldProductCount > newProductCount) {
                fireIntervalRemoved(newProductCount, oldProductCount-1);
                fireIntervalChanged(0, newProductCount - 1);
            } else {
                // the same count
                fireIntervalChanged(0, newProductCount - 1);
            }
        } else {
            clear();
        }
    }

    public void sortProducts() {
        if (this.products.size() > 1) {
            Collections.sort(this.products, this.outputProductResultsCallback.getProductsComparator());
            int startIndex = 0;
            int endIndex = this.products.size() - 1;
            fireIntervalChanged(startIndex, endIndex);
        }
    }

    public void removePendingDownloadProducts() {
        this.outputProductResultsCallback.getOutputProductResults().removePendingDownloadProducts();
        if (this.products.size() > 0) {
            fireIntervalChanged(0, this.products.size()-1);
        }
    }

    public Map<RepositoryProduct, Path> addPendingOpenDownloadedProducts(RepositoryProduct[] pendingOpenDownloadedProducts) {
        Map<RepositoryProduct, Path> productsToOpen = new HashMap<>();
        if (pendingOpenDownloadedProducts.length > 0) {
            int startIndex = pendingOpenDownloadedProducts.length - 1;
            int endIndex = 0;
            for (int i=0; i<pendingOpenDownloadedProducts.length; i++) {
                DownloadProgressStatus progressPercent = getDownloadingProductsProgressValue().get(pendingOpenDownloadedProducts[i]);
                if (progressPercent != null && progressPercent.canOpen()) {
                    progressPercent.setStatus(DownloadProgressStatus.PENDING_OPEN);
                    productsToOpen.put(pendingOpenDownloadedProducts[i], progressPercent.getDownloadedPath());
                    int index = findProductIndex(pendingOpenDownloadedProducts[i]);
                    if (index >= 0) {
                        if (startIndex > index) {
                            startIndex = index;
                        }
                        if (endIndex < index) {
                            endIndex = index;
                        }
                    }
                }
            }
            if (productsToOpen.size() > 0) {
                fireIntervalChanged(startIndex, endIndex);
            }
        }
        return productsToOpen;
    }

    public List<RepositoryProduct> addPendingDownloadProducts(RepositoryProduct[] pendingDownloadProducts) {
        List<RepositoryProduct> productsToDownload = new ArrayList<>(pendingDownloadProducts.length);
        if (pendingDownloadProducts.length > 0) {
            int startIndex = pendingDownloadProducts.length - 1;
            int endIndex = 0;
            for (int i=0; i<pendingDownloadProducts.length; i++) {
                DownloadProgressStatus progressPercent = getDownloadingProductsProgressValue().get(pendingDownloadProducts[i]);
                if (progressPercent == null || progressPercent.isStoppedDownload()) {
                    productsToDownload.add(pendingDownloadProducts[i]);
                    int index = findProductIndex(pendingDownloadProducts[i]);
                    if (index >= 0) {
                        if (startIndex > index) {
                            startIndex = index;
                        }
                        if (endIndex < index) {
                            endIndex = index;
                        }
                        getDownloadingProductsProgressValue().put(pendingDownloadProducts[i], new DownloadProgressStatus());
                    }
                }
            }
            if (productsToDownload.size() > 0) {
                fireIntervalChanged(startIndex, endIndex);
            }
        }
        return productsToDownload;
    }

    private Map<RepositoryProduct, LocalProgressStatus> getLocalProductsMap() {
        return this.outputProductResultsCallback.getOutputProductResults().getLocalProductsMap();
    }

    private Map<RepositoryProduct, DownloadProgressStatus> getDownloadingProductsProgressValue() {
        return this.outputProductResultsCallback.getOutputProductResults().getDownloadingProductsProgressValue();
    }

    public LocalProgressStatus getOpeningProductStatus(RepositoryProduct repositoryProduct) {
        return getLocalProductsMap().get(repositoryProduct);
    }

    public List<RepositoryProduct> addPendingOpenProducts(RepositoryProduct[] pendingOpenProducts) {
        return addPendingLocalProgressProducts(pendingOpenProducts, LocalProgressStatus.PENDING_OPEN);
    }

    public List<RepositoryProduct> addPendingDeleteProducts(RepositoryProduct[] pendingDeleteProducts) {
        return addPendingLocalProgressProducts(pendingDeleteProducts, LocalProgressStatus.PENDING_DELETE);
    }

    public void setOpenDownloadedProductStatus(RepositoryProduct repositoryProduct, byte openStatus) {
        DownloadProgressStatus progressPercent = getDownloadingProductsProgressValue().get(repositoryProduct);
        if (progressPercent != null) {
            progressPercent.setStatus(openStatus);
            int index = findProductIndex(repositoryProduct);
            if (index >= 0) {
                fireIntervalChanged(index, index);
            }
        }
    }

    public void setLocalProductStatus(RepositoryProduct repositoryProduct, byte localStatus) {
        LocalProgressStatus openProgressStatus = getLocalProductsMap().get(repositoryProduct);
        if (openProgressStatus != null) {
            openProgressStatus.setStatus(localStatus);
            int index = findProductIndex(repositoryProduct);
            if (index >= 0) {
                if (localStatus == LocalProgressStatus.DELETED) {
                    this.products.remove(index);
                    fireIntervalRemoved(index, index);
                } else {
                    fireIntervalChanged(index, index);
                }
            }
        }
    }

    public void setProductDownloadStatus(RepositoryProduct repositoryProduct, byte status) {
        DownloadProgressStatus progressPercent = getDownloadingProductsProgressValue().get(repositoryProduct);
        if (progressPercent != null) {
            progressPercent.setStatus(status);
            int index = findProductIndex(repositoryProduct);
            if (index >= 0) {
                fireIntervalChanged(index, index);
            }
        }
    }

    public void setProductDownloadPercent(RepositoryProduct repositoryProduct, short progressPercent, Path downloadedPath) {
        DownloadProgressStatus progressPercentItem = getDownloadingProductsProgressValue().get(repositoryProduct);
        if (progressPercentItem != null) {
            progressPercentItem.setValue(progressPercent);
            progressPercentItem.setDownloadedPath(downloadedPath);
            int index = findProductIndex(repositoryProduct);
            if (index >= 0) {
                fireIntervalChanged(index, index);
            }
        }
    }

    public DownloadProgressStatus getProductDownloadPercent(RepositoryProduct repositoryProduct) {
        return getDownloadingProductsProgressValue().get(repositoryProduct);
    }

    public void clear() {
        int endIndex = this.products.size() - 1;
        this.products = new ArrayList<>();
        if (endIndex >= 0) {
            fireIntervalRemoved(0, endIndex);
        }
    }

    private List<RepositoryProduct> addPendingLocalProgressProducts(RepositoryProduct[] pendingLocalProducts, byte status) {
        List<RepositoryProduct> productsToProcess = new ArrayList<>(pendingLocalProducts.length);
        if (pendingLocalProducts.length > 0) {
            int startIndex = pendingLocalProducts.length - 1;
            int endIndex = 0;
            for (int i=0; i<pendingLocalProducts.length; i++) {
                LocalProgressStatus openProgressStatus = getLocalProductsMap().get(pendingLocalProducts[i]);
                if (openProgressStatus == null || openProgressStatus.isFailOpened() || openProgressStatus.isFailOpenedBecauseNoProductReader() || openProgressStatus.isFailDeleted()) {
                    productsToProcess.add(pendingLocalProducts[i]);
                    int index = findProductIndex(pendingLocalProducts[i]);
                    if (index >= 0) {
                        if (startIndex > index) {
                            startIndex = index;
                        }
                        if (endIndex < index) {
                            endIndex = index;
                        }
                        getLocalProductsMap().put(pendingLocalProducts[i], new LocalProgressStatus(status));
                    }
                }
            }
            if (productsToProcess.size() > 0) {
                fireIntervalChanged(startIndex, endIndex);
            }
        }
        return productsToProcess;
    }

    private int findProductIndex(RepositoryProduct repositoryProductToFind) {
        for (int i=0; i<this.products.size(); i++) {
            if (this.products.get(i) == repositoryProductToFind) {
                return i;
            }
        }
        return -1;
    }
}

