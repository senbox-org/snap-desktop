package org.esa.snap.product.library.ui.v2;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.IProgressPanel;
import org.esa.snap.product.library.v2.IProductsDownloaderListener;
import org.esa.snap.product.library.v2.IThread;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 9/8/2019.
 */
public class DownloadProductListTimerRunnable extends AbstractProgressTimerRunnable<List<ProductLibraryItem>> {

    private final String mission;
    private final Map<String, Object> parametersValues;
    private final String dataSourceName;
    private final JComponent parentComponent;
    private final Credentials credentials;
    private final QueryProductResultsPanel productResultsPanel;

    public DownloadProductListTimerRunnable(IProgressPanel progressPanel, int threadId, Credentials credentials,
                                            JComponent parentComponent, QueryProductResultsPanel productResultsPanel,
                                            String dataSourceName, String mission, Map<String, Object> parametersValues) {

        super(progressPanel, threadId, 500);

        this.mission = mission;
        this.parametersValues = parametersValues;
        this.dataSourceName = dataSourceName;
        this.credentials = credentials;
        this.parentComponent = parentComponent;
        this.productResultsPanel = productResultsPanel;
    }

    @Override
    protected List<ProductLibraryItem> execute() throws Exception {
        IProductsDownloaderListener downloaderListener = new IProductsDownloaderListener() {
            @Override
            public void notifyProductCount(long totalProductCount) {
                if (isRunning()) {
                    notifyProductCountLater(totalProductCount);
                }
            }

            @Override
            public void notifyPageProducts(int pageNumber, List<ProductLibraryItem> pageResults, long totalProductCount, int retrievedProductCount) {
                if (isRunning()) {
                    notifyPageProductsLater(pageResults, totalProductCount, retrievedProductCount);
                }
            }
        };
        return SciHubDownloader.downloadProductList(this.credentials, this.mission, this.parametersValues, downloaderListener, this, 1);
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to retrieve the product list from '" + this.dataSourceName + "'.";
    }

    @Override
    protected boolean onTimerWakeUp() {
        boolean progressPanelVisible = super.onTimerWakeUp();
        if (progressPanelVisible) {
            this.productResultsPanel.startSearchingProductList(this.dataSourceName);
        }
        return progressPanelVisible;
    }

    @Override
    protected void onSuccessfullyFinish(List<ProductLibraryItem> results) {
        this.productResultsPanel.finishDownloadingProductList();
        if (results.size() > 0) {
            // do nothing
        } else {
            onShowInformationDialog("No product available according to the filter values.", "Information");
        }
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorDialog("Failed to retrieve the product list from " + this.dataSourceName + ".", "Error");
    }

    protected final Credentials getCredentials() {
        return credentials;
    }

    private void notifyProductCountLater(long totalProductCount) {
        GenericRunnable<Long> runnable = new GenericRunnable<Long>(totalProductCount) {
            @Override
            protected void execute(Long totalProductCountValue) {
                if (isCurrentProgressPanelThread()) {
                    productResultsPanel.startDownloadingProductList(totalProductCountValue.longValue(), dataSourceName);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void notifyPageProductsLater(List<ProductLibraryItem> pageResults, long totalProductCount, int retrievedProductCount) {
        Runnable runnable = new ProductPageResultsRunnable(pageResults, totalProductCount, retrievedProductCount) {
            @Override
            protected void execute(List<ProductLibraryItem> pageResultsValue, long totalProductCountValue, int retrievedProductCountValue) {
                if (isCurrentProgressPanelThread()) {
                    productResultsPanel.addProducts(pageResultsValue, totalProductCountValue, retrievedProductCountValue, dataSourceName);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void onShowErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void onShowInformationDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private static abstract class ProductPageResultsRunnable implements Runnable {

        private final List<ProductLibraryItem> pageResults;
        private final long totalProductCount;
        private final int retrievedProductCount;

        public ProductPageResultsRunnable(List<ProductLibraryItem> pageResults, long totalProductCount, int retrievedProductCount) {
            this.pageResults = pageResults;
            this.totalProductCount = totalProductCount;
            this.retrievedProductCount = retrievedProductCount;
        }

        protected abstract void execute(List<ProductLibraryItem> pageResults, long totalProductCount, int retrievedProductCount);

        @Override
        public void run() {
            execute(this.pageResults, this.totalProductCount, this.retrievedProductCount);
        }
    }
}
