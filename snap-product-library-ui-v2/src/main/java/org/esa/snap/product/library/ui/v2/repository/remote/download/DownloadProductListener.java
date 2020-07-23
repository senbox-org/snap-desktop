package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.remote.products.repository.RepositoryProduct;

/**
 * The listener interface for receiving events when downloading a remote product.
 *
 * Created by jcoravu on 11/2/2020.
 */
public interface DownloadProductListener {

    public void onFinishDownloadingProduct(DownloadProductRunnable downloadProductRunnable, DownloadProgressStatus downloadProgressStatus, SaveProductData saveProductData, boolean hasProductsToDownload);

    public void onUpdateProductDownloadProgress(RepositoryProduct repositoryProduct);

    public void onRefreshProduct(RepositoryProduct repositoryProduct);
}
