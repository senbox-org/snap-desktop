package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;
import ro.cs.tao.eodata.EOProduct;

import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 9/8/2019.
 */
public class DownloadProductListTimerRunnable extends AbstractTimerRunnable<List<EOProduct>> {

    private final String sensor;
    private final Map<String, Object> parametersValues;

    public DownloadProductListTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, String sensor, Map<String, Object> parametersValues) {
        super(loadingIndicator, threadId, 500);

        this.sensor = sensor;
        this.parametersValues = parametersValues;
    }

    @Override
    protected List<EOProduct> execute() throws Exception {
        List<EOProduct> results = SciHubDownloader.downloadProductList("jcoravu", "jcoravu@yahoo.com", this.sensor, this.parametersValues);
        System.out.println(results);
        return results;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the product list.";
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        onDisplayLoadingIndicatorMessage("Searching...");
    }
}
