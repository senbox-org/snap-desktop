package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.RemoteRepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.RemoteMission;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/9/2019.
 */
public class LoadProductListTimerRunnable extends AbstractProgressTimerRunnable<List<RepositoryProduct>> {

    private final ThreadListener threadListener;
    private final RemoteRepositoryProductListPanel repositoryProductListPanel;
    private final RemoteMission mission;
    private final Map<String, Object> parameterValues;

    public LoadProductListTimerRunnable(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                        RemoteMission mission, Map<String, Object> parameterValues, RemoteRepositoryProductListPanel repositoryProductListPanel) {

        super(progressPanel, threadId, 500);

        this.threadListener = threadListener;
        this.repositoryProductListPanel = repositoryProductListPanel;
        this.mission = mission;
        this.parameterValues = parameterValues;
    }

    @Override
    protected List<RepositoryProduct> execute() throws Exception {
        return ProductLibraryDAL.loadProductList(this.mission, this.parameterValues);
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the product list from the database.";
    }

    @Override
    protected void onStopExecuting() {
        this.threadListener.onStopExecuting(null);
    }

    @Override
    protected void onSuccessfullyFinish(List<RepositoryProduct> results) {
        this.repositoryProductListPanel.setProducts(results);
        if (results.size() == 0) {
            onShowInformationMessageDialog(this.repositoryProductListPanel, "No product available according to the filter values.", "Information");
        }
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog(this.repositoryProductListPanel, "Failed to read the product list from the database.", "Error");
    }
}
