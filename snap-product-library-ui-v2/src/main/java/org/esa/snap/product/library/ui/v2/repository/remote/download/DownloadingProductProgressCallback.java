package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.remote.products.repository.RepositoryProduct;

/**
 * The callback interface to get the downloading progress of a remote repository product.
 *
 * Created by jcoravu on 17/2/2020.
 */
public interface DownloadingProductProgressCallback {

    public DownloadProgressStatus getDownloadingProductsProgressValue(RepositoryProduct repositoryProduct);
}
