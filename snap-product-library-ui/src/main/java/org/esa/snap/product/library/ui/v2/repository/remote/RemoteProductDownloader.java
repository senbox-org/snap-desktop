package org.esa.snap.product.library.ui.v2.repository.remote;

import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 13/9/2019.
 */
public class RemoteProductDownloader {

    private final RemoteProductsRepositoryProvider remoteProductsRepositoryProvider;
    private final Path localRepositoryFolderPath;
    private final RepositoryProduct productToDownload;
    private final Credentials credentials;

    public RemoteProductDownloader(RemoteProductsRepositoryProvider remoteProductsRepositoryProvider, RepositoryProduct productToDownload,
                                   Path localRepositoryFolderPath, Credentials credentials) {

        this.remoteProductsRepositoryProvider = remoteProductsRepositoryProvider;
        this.productToDownload = productToDownload;
        this.localRepositoryFolderPath = localRepositoryFolderPath;
        this.credentials = credentials;
    }

    public Path download(ProgressListener progressListener) throws Exception {
        return this.remoteProductsRepositoryProvider.downloadProduct(this.productToDownload, this.credentials, this.localRepositoryFolderPath, progressListener);
    }

    public void cancel() {
        this.remoteProductsRepositoryProvider.cancelDownloadProduct(this.productToDownload);
    }

    public Path getLocalRepositoryFolderPath() {
        return localRepositoryFolderPath;
    }

    public RepositoryProduct getProductToDownload() {
        return productToDownload;
    }

    public String getRepositoryName() {
        return this.remoteProductsRepositoryProvider.getRepositoryName();
    }

    public Credentials getCredentials() {
        return this.credentials;
    }
}
