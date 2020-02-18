package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.nio.file.Path;

/**
 * Created by jcoravu on 11/2/2020.
 */
public interface DownloadProductListener {

    public void onFinishDownloadingProduct(DownloadProductRunnable downloadProductRunnable, DownloadProgressStatus downloadProgressStatus, SaveDownloadedProductData saveProductData, boolean hasProductsToDownload);

    public void onUpdateProductDownloadProgress(RepositoryProduct repositoryProduct);

    public void onCancelDownloadingProduct(DownloadProductRunnable downloadProductRunnable, DownloadProgressStatus downloadProgressStatus);
}
