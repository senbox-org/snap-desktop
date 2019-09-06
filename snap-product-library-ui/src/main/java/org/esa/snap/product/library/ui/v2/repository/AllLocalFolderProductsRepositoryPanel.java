package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RemoteRepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.local.LoadProductListTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressPanel;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.product.library.v2.AllLocalFolderProductsRepository;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.awt.GridBagLayout;
import java.util.List;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalFolderProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public AllLocalFolderProductsRepositoryPanel(ComponentDimension componentDimension, WorldWindowPanelWrapper worlWindPanel) {
        super(worlWindPanel, componentDimension, new GridBagLayout());

        this.allLocalFolderProductsRepository = new AllLocalFolderProductsRepository();
    }

    @Override
    public String getName() {
        return "All Local Folders";
    }

    @Override
    protected void addParameterComponents() {
        addParameterComponents(this.allLocalFolderProductsRepository.getParameters(), 0, 0);
    }

    @Override
    public AbstractProgressTimerRunnable<List<RepositoryProduct>> buildThreadToSearchProducts(ProgressPanel progressPanel, int threadId,
                                                       ThreadListener threadListener, RemoteRepositoryProductListPanel repositoryProductListPanel) {

        return new LoadProductListTimerRunnable(progressPanel, threadId, threadListener, repositoryProductListPanel);
    }
}
