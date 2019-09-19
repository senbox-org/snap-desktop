package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RemoteRepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.remote.products.repository.ItemRenderer;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public abstract class AbstractProductsRepositoryPanel extends JPanel {

    protected final WorldWindowPanelWrapper worlWindPanel;
    protected final ComponentDimension componentDimension;
    protected List<AbstractParameterComponent<?>> parameterComponents;

    protected AbstractProductsRepositoryPanel(WorldWindowPanelWrapper worlWindPanel, ComponentDimension componentDimension, LayoutManager layoutManager) {
        super(layoutManager);

        this.worlWindPanel = worlWindPanel;
        this.componentDimension = componentDimension;
    }

    public abstract String getName();

    protected abstract void addParameterComponents();

    public JButton getTopBarButton() {
        return null;
    }

    public void refreshParameterComponents() {
        removeAll();
        addParameterComponents();
        revalidate();
        repaint();
    }

    public abstract JPopupMenu buildProductListPopupMenu();

    public abstract AbstractProgressTimerRunnable<?> buildThreadToSearchProducts(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                                              RemoteRepositoryProductListPanel repositoryProductListPanel);

    public int computeLeftPanelMaximumLabelWidth() {
        int maximumLabelWidth = 0;
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            int labelWidth = parameterComponent.getLabel().getPreferredSize().width;
            if (maximumLabelWidth < labelWidth) {
                maximumLabelWidth = labelWidth;
            }
        }
        return maximumLabelWidth;
    }

    protected final Map<String, Object> getParameterValues() {
        Map<String, Object> result = new HashMap<>();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            Object value = parameterComponent.getParameterValue();
            if (value == null) {
                if (parameterComponent.isRequired()) {
                    String message = "The value of the '" + parameterComponent.getLabel().getText()+"' parameter is required.";
                    showErrorMessageDialog(message, "Required parameter");
                    parameterComponent.getComponent().requestFocus();
                    return null;
                }
            } else {
                result.put(parameterComponent.getParameterName(), value);
            }
        }
        return result;
    }

    protected final void showErrorMessageDialog(String message, String title) {
        JOptionPane.showMessageDialog(getParent(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    protected final void refreshLabelWidths() {
        int maximumLabelWidth = computeLeftPanelMaximumLabelWidth();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            RemoteProductsRepositoryPanel.setLabelSize(parameterComponent.getLabel(), maximumLabelWidth);
        }
    }

    protected final void addAreaParameterComponent(QueryFilter areaOfInterestParameter) {
        SelectionAreaParameterComponent selectionAreaParameterComponent = new SelectionAreaParameterComponent(this.worlWindPanel, areaOfInterestParameter.getName(), areaOfInterestParameter.getLabel(), areaOfInterestParameter.isRequired());
        this.parameterComponents.add(selectionAreaParameterComponent);
        selectionAreaParameterComponent.getLabel().setVerticalAlignment(JLabel.TOP);
        JPanel centerPanel = new JPanel(new BorderLayout(this.componentDimension.getGapBetweenColumns(), 0));
        centerPanel.add(selectionAreaParameterComponent.getLabel(), BorderLayout.WEST);
        centerPanel.add(selectionAreaParameterComponent.getComponent(), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }
}
