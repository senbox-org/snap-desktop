package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.v2.IProductsDownloaderListener;
import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.GenericRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;
import ro.cs.tao.eodata.EOProduct;

import javax.swing.SwingUtilities;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 9/8/2019.
 */
public class DownloadProductListTimerRunnable extends AbstractTimerRunnable<List<EOProduct>> {

    private final String sensor;
    private final Map<String, Object> parametersValues;
    private final String dataSourceName;

    public DownloadProductListTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, String dataSourceName, String sensor, Map<String, Object> parametersValues) {
        super(loadingIndicator, threadId, 500);

        this.sensor = sensor;
        this.parametersValues = parametersValues;
        this.dataSourceName = dataSourceName;
    }

    @Override
    protected List<EOProduct> execute() throws Exception {
        IProductsDownloaderListener downloaderListener = new IProductsDownloaderListener() {
            @Override
            public void notifyProductCount(long productCount) {
                String loadingIndicatorMessage;
                if (productCount == 1) {
                    loadingIndicatorMessage = "Download " + productCount + " product from " + dataSourceName + "...";
                } else {
                    loadingIndicatorMessage = "Download " + productCount + " products from " + dataSourceName + "...";
                }
                updateLoadingIndicatorMessageLater(loadingIndicatorMessage);
            }

            @Override
            public void notifyPageProducts(int pageNumber, List<EOProduct> pageResults) {
                notifyPageProductsLater(pageResults);
            }
        };

        SciHubDownloader sciHubDownloader = new SciHubDownloader("jcoravu", "jcoravu@yahoo.com", this.sensor, this.parametersValues);
        List<EOProduct> results = sciHubDownloader.downloadProducts(downloaderListener);

        System.out.println(results);
        return results;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the product list.";
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        onDisplayLoadingIndicatorMessage("Searching products to " + this.dataSourceName+"...");
    }

    protected void onDownloadPageProducts(List<EOProduct> pageResults) {

    }

    private void notifyPageProductsLater(List<EOProduct> pageResults) {
        Runnable runnable = new GenericRunnable<List<EOProduct>>(pageResults) {
            @Override
            protected void execute(List<EOProduct> results) {
                onDownloadPageProducts(results);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
