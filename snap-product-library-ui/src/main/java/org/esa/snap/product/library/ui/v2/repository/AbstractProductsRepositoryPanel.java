package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.v2.repository.ProductRepositoryDownloader;
import org.esa.snap.product.library.v2.repository.ProductListRepositoryDownloader;

import javax.swing.JPanel;
import java.awt.LayoutManager;
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

    public ProductListRepositoryDownloader buildResultsDownloader() {
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
}
