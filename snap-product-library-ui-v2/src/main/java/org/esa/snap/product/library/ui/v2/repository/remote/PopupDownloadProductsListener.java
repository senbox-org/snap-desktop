package org.esa.snap.product.library.ui.v2.repository.remote;

import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductRunnable;
import org.esa.snap.remote.products.repository.RepositoryProduct;

/**
 * The listener interface for receiving events about a downloading product.
 *
 * Created by jcoravu on 11/2/2020.
 */
public interface PopupDownloadProductsListener {

    public void onUpdateProductDownloadProgress(RepositoryProduct repositoryProduct);

    public void onStopDownloadingProduct(DownloadProductRunnable downloadProductRunnable);
}
