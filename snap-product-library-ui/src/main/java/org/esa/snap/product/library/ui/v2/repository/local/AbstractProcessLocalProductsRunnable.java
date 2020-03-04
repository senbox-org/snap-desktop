package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.PairRunnable;

import javax.swing.*;
import java.util.List;

/**
 * Created by jcoravu on 27/9/2019.
 */
public abstract class AbstractProcessLocalProductsRunnable extends AbstractProgressTimerRunnable<Void> {

    protected final List<RepositoryProduct> productsToProcess;
    protected final RepositoryOutputProductListPanel repositoryProductListPanel;

    protected AbstractProcessLocalProductsRunnable(ProgressBarHelper progressPanel, int threadId, RepositoryOutputProductListPanel repositoryProductListPanel,
                                                   List<RepositoryProduct> productsToProcess) {

        super(progressPanel, threadId, 500);

        this.repositoryProductListPanel = repositoryProductListPanel;
        this.productsToProcess = productsToProcess;
    }

    protected final void updateProductProgressStatusLater(RepositoryProduct repositoryProduct, byte localStatus) {
        UpdateLocalProgressStatusRunnable runnable = new UpdateLocalProgressStatusRunnable(repositoryProduct, localStatus, this.repositoryProductListPanel);
        SwingUtilities.invokeLater(runnable);
    }

    private static class UpdateLocalProgressStatusRunnable implements Runnable {

        private final RepositoryProduct repositoryProduct;
        private final byte localStatus;
        private final RepositoryOutputProductListPanel repositoryProductListPanel;

        private UpdateLocalProgressStatusRunnable(RepositoryProduct repositoryProduct, byte localStatus, RepositoryOutputProductListPanel repositoryProductListPanel) {
            this.repositoryProduct = repositoryProduct;
            this.localStatus = localStatus;
            this.repositoryProductListPanel = repositoryProductListPanel;
        }

        @Override
        public void run() {
            OutputProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.setLocalProductStatus(this.repositoryProduct, this.localStatus);
        }
    }
}
