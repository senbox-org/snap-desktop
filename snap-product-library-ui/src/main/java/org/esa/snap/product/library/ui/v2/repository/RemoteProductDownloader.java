package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 13/9/2019.
 */
public class RemoteProductDownloader {

    private final ProductRepositoryDownloader productRepositoryDownloader;
    private final Path localRepositoryFolderPath;
    private final RepositoryProduct productToDownload;

    public RemoteProductDownloader(RepositoryProduct productToDownload, ProductRepositoryDownloader productRepositoryDownloader, Path localRepositoryFolderPath) {
        this.productToDownload = productToDownload;
        this.productRepositoryDownloader = productRepositoryDownloader;
        this.localRepositoryFolderPath = localRepositoryFolderPath;
    }

    public Path download(ProgressListener progressListener) throws IOException {
        Path productFolderPath = this.productRepositoryDownloader.download(this.productToDownload, this.localRepositoryFolderPath, progressListener);
        return productFolderPath;
    }

    public void cancel() {
        this.productRepositoryDownloader.cancel();
    }

    public Path getLocalRepositoryFolderPath() {
        return localRepositoryFolderPath;
    }

    public RepositoryProduct getProductToDownload() {
        return productToDownload;
    }
}
