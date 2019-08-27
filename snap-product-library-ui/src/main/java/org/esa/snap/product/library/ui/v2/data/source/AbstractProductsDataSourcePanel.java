package org.esa.snap.product.library.ui.v2.data.source;

import org.esa.snap.product.library.v2.DataSourceProductDownloader;
import org.esa.snap.product.library.v2.DataSourceResultsDownloader;

import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.nio.file.Path;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public abstract class AbstractProductsDataSourcePanel extends JPanel {

    protected AbstractProductsDataSourcePanel(LayoutManager layoutManager) {
        super(layoutManager);
    }

    public abstract String getName();

    public String getSelectedMission() {
        return null;
    }

    public void refreshMissionParameters() {
    }

    public DataSourceResultsDownloader buildResultsDownloader() {
        return null;
    }

    public DataSourceProductDownloader buidProductDownloader(String mission) {
        return null;
    }

    public Map<String, Object> getParameterValues() {
        return null;
    }

    public int computeLeftPanelMaximumLabelWidth() {
        return 70;
    }
}
