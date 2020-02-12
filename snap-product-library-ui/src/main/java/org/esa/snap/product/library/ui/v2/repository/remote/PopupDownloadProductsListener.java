package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;

/**
 * Created by jcoravu on 11/2/2020.
 */
public interface PopupDownloadProductsListener {

    public void onUpdateProductDownloadProgress(RepositoryProduct repositoryProduct, DownloadProgressStatus progressProgressStatus);

    public void onStopDownloadingProduct(DownloadProductRunnable downloadProductRunnable);
}
