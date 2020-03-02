package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.ProductLibraryToolViewV2;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 9/8/2019.
 */
public class DownloadProductListTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(DownloadProductListTimerRunnable.class.getName());

    private final String mission;
    private final Map<String, Object> parameterValues;
    private final String remoteRepositoryName;
    private final Credentials credentials;
    private final RepositoryOutputProductListPanel repositoryProductListPanel;
    private final RemoteProductsRepositoryProvider productsRepositoryProvider;
    private final ThreadListener threadListener;
    private final RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;

    public DownloadProductListTimerRunnable(ProgressBarHelper progressPanel, int threadId, Credentials credentials,
                                            RemoteProductsRepositoryProvider productsRepositoryProvider, ThreadListener threadListener,
                                            RemoteRepositoriesSemaphore remoteRepositoriesSemaphore, RepositoryOutputProductListPanel repositoryProductListPanel,
                                            String remoteRepositoryName, String mission, Map<String, Object> parameterValues) {

        super(progressPanel, threadId, 500);

        this.mission = mission;
        this.productsRepositoryProvider = productsRepositoryProvider;
        this.parameterValues = parameterValues;
        this.remoteRepositoryName = remoteRepositoryName;
        this.credentials = credentials;
        this.threadListener = threadListener;
        this.repositoryProductListPanel = repositoryProductListPanel;
        this.remoteRepositoriesSemaphore = remoteRepositoriesSemaphore;
    }

    @Override
    protected Void execute() throws Exception {
        this.remoteRepositoriesSemaphore.acquirePermission(this.productsRepositoryProvider.getRepositoryName(), this.credentials);
        try {
            if (isFinished()) {
                return null; // nothing to return
            }

            try {
                ProductListDownloaderListener downloaderListener = new ProductListDownloaderListener() {
                    @Override
                    public void notifyProductCount(long totalProductCount) {
                        if (!isFinished()) {
                            updateProductListSizeLater(totalProductCount);
                        }
                    }

                    @Override
                    public void notifyPageProducts(int pageNumber, List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount) {
                        if (!isFinished()) {
                            updatePageProductsLater(pageResults, totalProductCount, retrievedProductCount);
                        }
                    }
                };
                this.productsRepositoryProvider.downloadProductList(this.credentials, this.mission, this.parameterValues, downloaderListener, this);
            } catch (java.lang.InterruptedException exception) {
                logger.log(Level.FINE, "Stop searching the product list on the '" + this.remoteRepositoryName+"' remote repository using the '" +this.mission+"' mission.");
                return null; // nothing to return
            }
        } finally {
            this.remoteRepositoriesSemaphore.releasePermission(this.productsRepositoryProvider.getRepositoryName(), this.credentials);
        }
        return null; // nothing to return
    }

    @Override
    public void cancelRunning() {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Cancel searching the product list on the '" + this.remoteRepositoryName+"' remote repository using the '" +this.mission+"' mission.");
        }

        super.cancelRunning();
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to retrieve the product list from '" + this.remoteRepositoryName + "'.";
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog(this.repositoryProductListPanel, "Failed to retrieve the product list from " + this.remoteRepositoryName + ".", "Error");
    }

    @Override
    protected void onFinishRunning() {
        this.threadListener.onStopExecuting(this);
    }

    private void updateProductListSizeLater(long totalProductCount) {
        GenericRunnable<Long> runnable = new GenericRunnable<Long>(totalProductCount) {
            @Override
            protected void execute(Long totalProductCountValue) {
                if (isCurrentProgressPanelThread()) {
                    String text = buildProgressBarDownloadingText(0, totalProductCountValue.longValue());
                    onUpdateProgressBarText(text);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void updatePageProductsLater(List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount) {
        Runnable runnable = new ProductPageResultsRunnable(pageResults, totalProductCount, retrievedProductCount) {
            @Override
            protected void execute(List<RepositoryProduct> pageResultsValue, long totalProductCountValue, int retrievedProductCountValue) {
                if (isCurrentProgressPanelThread()) {
                    repositoryProductListPanel.addProducts(pageResultsValue);
                    String text = buildProgressBarDownloadingText(retrievedProductCountValue, totalProductCountValue);
                    onUpdateProgressBarText(text);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private static abstract class ProductPageResultsRunnable implements Runnable {

        private final List<RepositoryProduct> pageResults;
        private final long totalProductCount;
        private final int retrievedProductCount;

        public ProductPageResultsRunnable(List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount) {
            this.pageResults = pageResults;
            this.totalProductCount = totalProductCount;
            this.retrievedProductCount = retrievedProductCount;
        }

        protected abstract void execute(List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount);

        @Override
        public void run() {
            execute(this.pageResults, this.totalProductCount, this.retrievedProductCount);
        }
    }

    public static String buildProgressBarDownloadingText(long totalDownloaded, long totalProducts) {
        return ProductLibraryToolViewV2.getSearchingProductListMessage() + ": " + Long.toString(totalDownloaded) + " out of " + Long.toString(totalProducts);
    }
}
