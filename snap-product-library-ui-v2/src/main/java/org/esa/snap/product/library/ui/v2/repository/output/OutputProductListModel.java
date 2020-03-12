package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.product.library.ui.v2.repository.local.LocalProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadingProductProgressCallback;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.nio.file.Path;
import java.util.*;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class OutputProductListModel {

    private final OutputProductResultsCallback outputProductResultsCallback;

    private List<RepositoryProduct> products;
    private DownloadingProductProgressCallback downloadingProductProgressCallback;

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

    public void setDownloadingProductProgressCallback(DownloadingProductProgressCallback downloadingProductProgressCallback) {
        this.downloadingProductProgressCallback = downloadingProductProgressCallback;
    }

    public OutputProductResults getOutputProductResults() {
        return this.outputProductResultsCallback.getOutputProductResults();
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

    public Map<RepositoryProduct, Path> addPendingOpenDownloadedProducts(RepositoryProduct[] pendingOpenDownloadedProducts) {
        Map<RepositoryProduct, Path> productsToOpen = new HashMap<>();
        if (pendingOpenDownloadedProducts.length > 0) {
            int startIndex = pendingOpenDownloadedProducts.length - 1;
            int endIndex = 0;
            for (int i=0; i<pendingOpenDownloadedProducts.length; i++) {
                DownloadProgressStatus progressPercent = getOutputProductResults().getDownloadedProductProgress(pendingOpenDownloadedProducts[i]);
                if (progressPercent != null && progressPercent.canOpen()) {
                    if (progressPercent.getDownloadedPath() == null) {
                        throw new NullPointerException("The downloaded path is null for product '" + pendingOpenDownloadedProducts[i].getName()+"'.");
                    }
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

    private Map<RepositoryProduct, LocalProgressStatus> getLocalProductsMap() {
        return this.outputProductResultsCallback.getOutputProductResults().getLocalProductsMap();
    }

    public List<RepositoryProduct> addPendingOpenLocalProducts(RepositoryProduct[] pendingOpenProducts) {
        return addPendingLocalProgressProducts(pendingOpenProducts, LocalProgressStatus.PENDING_OPEN);
    }

    public List<RepositoryProduct> addPendingCopyLocalProducts(RepositoryProduct[] pendingCopyProducts) {
        return addPendingLocalProgressProducts(pendingCopyProducts, LocalProgressStatus.PENDING_COPY);
    }

    public List<RepositoryProduct> addPendingMoveLocalProducts(RepositoryProduct[] pendingMoveProducts) {
        return addPendingLocalProgressProducts(pendingMoveProducts, LocalProgressStatus.PENDING_MOVE);
    }

    public List<RepositoryProduct> addPendingDeleteLocalProducts(RepositoryProduct[] pendingDeleteProducts) {
        return addPendingLocalProgressProducts(pendingDeleteProducts, LocalProgressStatus.PENDING_DELETE);
    }

    public void setOpenDownloadedProductStatus(RepositoryProduct repositoryProduct, byte openStatus) {
        DownloadProgressStatus progressPercent = getOutputProductResults().getDownloadedProductProgress(repositoryProduct);
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

    public void refreshProducts() {
        if (this.products.size() > 0) {
            fireIntervalChanged(0, this.products.size() - 1);
        }
    }

    public void refreshProductDownloadPercent(RepositoryProduct repositoryProduct) {
        int index = findProductIndex(repositoryProduct);
        if (index >= 0) {
            fireIntervalChanged(index, index); // the downloading product is visible in the current page
        }
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
//            for (int i=0; i<pendingLocalProducts.length; i++) {
//                LocalProgressStatus openProgressStatus = getLocalProductsMap().get(pendingLocalProducts[i]);
//                if (openProgressStatus == null || openProgressStatus.isFailOpened() || openProgressStatus.isFailOpenedBecauseNoProductReader() || openProgressStatus.isFailDeleted()) {
//                    productsToProcess.add(pendingLocalProducts[i]);
//                    int index = findProductIndex(pendingLocalProducts[i]);
//                    if (index >= 0) {
//                        if (startIndex > index) {
//                            startIndex = index;
//                        }
//                        if (endIndex < index) {
//                            endIndex = index;
//                        }
//                        getLocalProductsMap().put(pendingLocalProducts[i], new LocalProgressStatus(status));
//                    }
//                }
//            }
            for (int i=0; i<pendingLocalProducts.length; i++) {
                LocalProgressStatus openProgressStatus = getLocalProductsMap().get(pendingLocalProducts[i]);
                if (openProgressStatus == null ) {
                    getLocalProductsMap().put(pendingLocalProducts[i], new LocalProgressStatus(status));
                } else {
                    openProgressStatus.setStatus(status);
                }
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

