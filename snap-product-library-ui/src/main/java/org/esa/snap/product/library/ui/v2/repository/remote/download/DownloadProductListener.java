package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.nio.file.Path;

/**
 * Created by jcoravu on 11/2/2020.
 */
public interface DownloadProductListener {

    public void onFinishDownloadingProduct(DownloadProductRunnable downloadProductRunnable, SaveDownloadedProductData saveProductData, boolean hasProductsToDownload);

    public void onUpdateProductDownloadPercent(RepositoryProduct repositoryProduct, short progressPercent, Path downloadedPath);

    public void onUpdateProductDownloadStatus(RepositoryProduct repositoryProduct, byte status);
}
