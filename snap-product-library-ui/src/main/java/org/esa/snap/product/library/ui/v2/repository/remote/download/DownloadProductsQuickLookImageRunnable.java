package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.remote.products.repository.HTTPServerException;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 29/1/2020.
 */
public class DownloadProductsQuickLookImageRunnable extends AbstractBackgroundDownloadRunnable {

    private static final Logger logger = Logger.getLogger(DownloadProductsQuickLookImageRunnable.class.getName());

    private final RemoteProductsRepositoryProvider productsRepositoryProvider;
    private final Credentials credentials;
    private final RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;
    private final RepositoryOutputProductListPanel repositoryProductListPanel;
    private final List<RepositoryProduct> productsWithoutQuickLookImage;

    public DownloadProductsQuickLookImageRunnable(List<RepositoryProduct> productsWithoutQuickLookImage, RemoteProductsRepositoryProvider productsRepositoryProvider,
                                                  Credentials credentials, RemoteRepositoriesSemaphore remoteRepositoriesSemaphore,
                                                  RepositoryOutputProductListPanel repositoryProductListPanel) {

        this.productsWithoutQuickLookImage = productsWithoutQuickLookImage;
        this.productsRepositoryProvider = productsRepositoryProvider;
        this.credentials = credentials;
        this.remoteRepositoriesSemaphore = remoteRepositoriesSemaphore;
        this.repositoryProductListPanel = repositoryProductListPanel;
    }

    @Override
    public void run() {
        try {
            startRunning();

            if (!isRunning()) {
                return; // nothing to return
            }

            this.remoteRepositoriesSemaphore.acquirePermission(this.productsRepositoryProvider.getRepositoryName(), this.credentials);
            try {
                if (!isRunning()) {
                    return; // nothing to return
                }

                int maximumInternalServerErrorCount = 3;
                int internalServerErrorCount = 0;
                for (int i = 0; i < this.productsWithoutQuickLookImage.size() && internalServerErrorCount < maximumInternalServerErrorCount; i++) {
                    if (!isRunning()) {
                        return; // nothing to return
                    }

                    RepositoryProduct repositoryProduct = this.productsWithoutQuickLookImage.get(i);
                    BufferedImage quickLookImage = null;
                    if (repositoryProduct.getDownloadQuickLookImageURL() != null) {
                        try {
                            quickLookImage = this.productsRepositoryProvider.downloadProductQuickLookImage(this.credentials, repositoryProduct.getDownloadQuickLookImageURL(), this);
                        } catch (java.lang.InterruptedException exception) {
                            logger.log(Level.WARNING, "Stop downloading the product quick look image from url '" + repositoryProduct.getDownloadQuickLookImageURL() + "'.");
                            return; // nothing to return
                        } catch (HTTPServerException exception) {
                            logger.log(Level.SEVERE, "Failed to download the product quick look image from url '" + repositoryProduct.getDownloadQuickLookImageURL() + "'.", exception);
                            if (exception.getStatusCodeResponse() >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
                                internalServerErrorCount++;
                            }
                        } catch (java.lang.Exception exception) {
                            logger.log(Level.SEVERE, "Failed to download the product quick look image from url '" + repositoryProduct.getDownloadQuickLookImageURL() + "'.", exception);
                        }
                    }
                    setProductQuickLookImageLater(repositoryProduct, quickLookImage);
                }
            } finally {
                this.remoteRepositoriesSemaphore.releasePermission(this.productsRepositoryProvider.getRepositoryName(), this.credentials);
            }
        } catch (Exception exception) {
        } finally {
            finishRunning();
        }
    }

    protected void finishRunning() {
    }

    private void setProductQuickLookImageLater(RepositoryProduct product, BufferedImage quickLookImage) {
        Runnable runnable = new DownloadRemoteProductsHelper.PairRunnable<RepositoryProduct, BufferedImage>(product, quickLookImage) {
            @Override
            public void run() {
                repositoryProductListPanel.setProductQuickLookImage(this.first, this.second);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
