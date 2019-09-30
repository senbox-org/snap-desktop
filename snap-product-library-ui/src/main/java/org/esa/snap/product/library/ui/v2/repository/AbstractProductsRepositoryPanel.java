package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public abstract class AbstractProductsRepositoryPanel extends JPanel {

    private final WorldWindowPanelWrapper worlWindPanel;

    protected final ComponentDimension componentDimension;
    protected List<AbstractParameterComponent<?>> parameterComponents;

    protected AbstractProductsRepositoryPanel(WorldWindowPanelWrapper worlWindPanel, ComponentDimension componentDimension, LayoutManager layoutManager) {
        super(layoutManager);

        this.worlWindPanel = worlWindPanel;
        this.componentDimension = componentDimension;
    }

    public abstract String getName();

    protected abstract void addParameterComponents();

    public abstract JPopupMenu buildProductListPopupMenu(RepositoryProduct[] selectedProducts);

    public abstract RepositoryProductPanel buildProductProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                                                    ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon);

    public abstract AbstractProgressTimerRunnable<?> buildThreadToSearchProducts(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                                              RepositoryProductListPanel repositoryProductListPanel);

    public JButton getTopBarButton() {
        return null;
    }

    public void refreshParameterComponents() {
        removeAll();
        addParameterComponents();
        revalidate();
        repaint();
    }

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
        this.worlWindPanel.clearSelectedArea();
        SelectionAreaParameterComponent selectionAreaParameterComponent = new SelectionAreaParameterComponent(this.worlWindPanel, areaOfInterestParameter.getName(), areaOfInterestParameter.getLabel(), areaOfInterestParameter.isRequired());
        this.parameterComponents.add(selectionAreaParameterComponent);

        JLabel label = selectionAreaParameterComponent.getLabel();
        label.setVerticalAlignment(JLabel.TOP);
        int difference = this.componentDimension.getTextFieldPreferredHeight() - label.getPreferredSize().height;
        label.setBorder(new EmptyBorder((difference/2), 0, 0 , 0));
        JPanel centerPanel = new JPanel(new BorderLayout(this.componentDimension.getGapBetweenColumns(), 0));
        centerPanel.add(label, BorderLayout.WEST);
        centerPanel.add(selectionAreaParameterComponent.getComponent(), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }
}
