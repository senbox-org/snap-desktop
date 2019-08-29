package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.QueryProductResultsPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressPanel;
import org.esa.snap.product.library.v2.RepositoryProduct;
import org.esa.snap.product.library.v2.repository.ProductRepositoryDownloader;
import org.esa.snap.product.library.v2.repository.ProductsRepositoryProvider;

import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public abstract class AbstractProductsRepositoryPanel extends JPanel {

    protected AbstractProductsRepositoryPanel(LayoutManager layoutManager) {
        super(layoutManager);
    }

    public abstract String getName();

    public String getSelectedMission() {
        return null;
    }

    public void refreshMissionParameters() {
    }

    public ProductsRepositoryProvider buildProductListDownloader() {
        return null;
    }

    public ProductRepositoryDownloader buidProductDownloader(String mission) {
        return null;
    }

    public Map<String, Object> getParameterValues() {
        return null;
    }

    public int computeLeftPanelMaximumLabelWidth() {
        return 70;
    }

    public AbstractProgressTimerRunnable<List<RepositoryProduct>> buildThreadToSearchProducts(ProgressPanel progressPanel, int threadId, ThreadListener threadListener,
                                                                                              QueryProductResultsPanel productResultsPanel) {
        return null;
    }

    public AbstractRunnable<?> buildThreadToDisplayQuickLookImages(List<RepositoryProduct> productList, ThreadListener threadListener, QueryProductResultsPanel productResultsPanel) {
        return null;
    }
}
