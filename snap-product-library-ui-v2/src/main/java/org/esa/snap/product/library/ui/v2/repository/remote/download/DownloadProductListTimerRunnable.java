package org.esa.snap.product.library.ui.v2.repository.remote.download;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.thread.ThreadListener;
import org.esa.snap.product.library.v2.preferences.RepositoriesCredentialsController;
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
 * The thread class to search the products on a remote repository.
 *
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
    private final Object lock = new Object();

    public DownloadProductListTimerRunnable(ProgressBarHelper progressPanel, int threadId, Credentials credentials,
                                            RemoteProductsRepositoryProvider productsRepositoryProvider, ThreadListener threadListener, RepositoryOutputProductListPanel repositoryProductListPanel,
                                            String remoteRepositoryName, String mission, Map<String, Object> parameterValues) {

        super(progressPanel, threadId, 500);

        this.mission = mission;
        this.productsRepositoryProvider = productsRepositoryProvider;
        this.parameterValues = parameterValues;
        this.remoteRepositoryName = remoteRepositoryName;
        this.credentials = credentials;
        this.threadListener = threadListener;
        this.repositoryProductListPanel = repositoryProductListPanel;
    }

    private boolean downloadsAllPages() {
        return RepositoriesCredentialsController.getInstance().downloadsAllPages();
    }

    private int getPageSize() {
        return RepositoriesCredentialsController.getInstance().getNrRecordsOnPage();
    }

    @Override
    protected Void execute() throws Exception {
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
                        if (!downloadsAllPages() && retrievedProductCount < totalProductCount) {
                            synchronized (lock) {
                                try {
                                    lock.wait(500);
                                    hideProgressPanelLater();
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            showProgressPanelLater();
                        }
                    }
                }
            };
            this.productsRepositoryProvider.downloadProductList(this.credentials, this.mission, getPageSize(), this.parameterValues, downloaderListener, this);
            if (this.parameterValues.containsKey("username") && this.parameterValues.containsKey("password")) {
                String username = (String) this.parameterValues.get("username");
                String password = (String) this.parameterValues.get("password");
                RepositoriesCredentialsController.getInstance().saveRepositoryCollectionCredential(this.remoteRepositoryName, this.mission, new UsernamePasswordCredentials(username, password));
            }
        } catch (java.lang.InterruptedException exception) {
            logger.log(Level.FINE, "Stop searching the product list on the '" + this.remoteRepositoryName+"' remote repository using the '" +this.mission+"' mission.");
            return null; // nothing to return
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

    public void downloadProductListNextPage() {
        if (!isFinished()) {
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to retrieve the product list from '" + this.remoteRepositoryName + "'.";
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog(this.repositoryProductListPanel, "Failed to retrieve the product list from " + this.remoteRepositoryName + ".\nReason: " + exception.getMessage(), "Error");
    }

    @Override
    protected void onFinishRunning() {
        this.threadListener.onStopExecuting(this);
    }

    @Override
    protected boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp(getSearchingProductListMessage() + "...");
    }

    private void updateProductListSizeLater(long totalProductCount) {
        repositoryProductListPanel.setCurrentFullResultsListCount(totalProductCount);
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
        repositoryProductListPanel.setDownloadProductListTimerRunnable(this);
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

    private void hideProgressPanelLater() {
        SwingUtilities.invokeLater(() -> super.onHideProgressPanelLater());
    }

    private void showProgressPanelLater() {
        SwingUtilities.invokeLater(() -> super.onTimerWakeUp("Fetching next page ..."));
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
        return getSearchingProductListMessage() + ": " + Long.toString(totalDownloaded) + " out of " + Long.toString(totalProducts);
    }

    private static String getSearchingProductListMessage() {
        return "Searching product list";
    }
}
