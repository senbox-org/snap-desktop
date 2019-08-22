package org.esa.snap.product.library.ui.v2;

import javax.swing.JPanel;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public abstract class AbstractProductsDataSource extends JPanel {

    protected AbstractProductsDataSource() {
    }

    public abstract String getName();

    public String getSelectedMission() {
        return null;
    }

    public void refreshMissionParameters() {

    }

    public Map<String, Object> getParameterValues() {
        return null;
    }

    public int computeLeftPanelMaximumLabelWidth() {
        return 70;
    }
}
