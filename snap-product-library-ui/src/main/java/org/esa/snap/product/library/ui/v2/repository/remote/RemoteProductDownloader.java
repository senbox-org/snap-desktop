package org.esa.snap.product.library.ui.v2.repository.remote;

import org.apache.http.auth.Credentials;
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
    private final Credentials credentials;

    public RemoteProductDownloader(RepositoryProduct productToDownload, ProductRepositoryDownloader productRepositoryDownloader,
                                   Path localRepositoryFolderPath, Credentials credentials) {

        this.productToDownload = productToDownload;
        this.productRepositoryDownloader = productRepositoryDownloader;
        this.localRepositoryFolderPath = localRepositoryFolderPath;
        this.credentials = credentials;
    }

    public Path download(ProgressListener progressListener) throws IOException, InterruptedException {
        return this.productRepositoryDownloader.download(this.productToDownload, this.credentials, this.localRepositoryFolderPath, progressListener);
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

    public String getRepositoryId() {
        return this.productRepositoryDownloader.getRepositoryId();
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
