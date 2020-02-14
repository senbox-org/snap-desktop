package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.input.AbstractParameterComponent;
import org.esa.snap.product.library.ui.v2.repository.input.SelectionAreaParameterComponent;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductResults;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.thread.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldMapPanelWrapper;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public abstract class AbstractProductsRepositoryPanel extends JPanel {

    private static final String INPUT_PARAMETER_COMPONENTS_CHANGED = "parameterComponentsChanged";

    private final WorldMapPanelWrapper worlWindPanel;

    protected final ComponentDimension componentDimension;
    protected List<AbstractParameterComponent<?>> parameterComponents;

    private OutputProductResults outputProductResults;

    protected AbstractProductsRepositoryPanel(WorldMapPanelWrapper worlWindPanel, ComponentDimension componentDimension, LayoutManager layoutManager) {
        super(layoutManager);

        this.worlWindPanel = worlWindPanel;
        this.componentDimension = componentDimension;

        resetOutputProducts();
    }

    public abstract String getName();

    protected abstract void addInputParameterComponentsToPanel();

    public abstract JPopupMenu buildProductListPopupMenu(RepositoryProduct[] selectedProducts, OutputProductListModel productListModel);

    public abstract boolean refreshInputParameterComponentValues();

    public abstract void resetInputParameterValues();

    public abstract AbstractRepositoryProductPanel buildProductProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                                                            ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon);

    public abstract AbstractProgressTimerRunnable<?> buildSearchProductListThread(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                                  RemoteRepositoriesSemaphore remoteRepositoriesSemaphore, RepositoryOutputProductListPanel repositoryProductListPanel);

    public JButton[] getTopBarButton() {
        return null;
    }

    public final void resetOutputProducts() {
        this.outputProductResults = new OutputProductResults();
    }

    public final OutputProductResults getOutputProductResults() {
        return outputProductResults;
    }

    public final void addInputParameterComponents() {
        removeAll();
        addInputParameterComponentsToPanel();
        refreshLabelWidths();
        revalidate();
        repaint();
        firePropertyChange(INPUT_PARAMETER_COMPONENTS_CHANGED, null, null);
    }

    public void addInputParameterComponentsChangedListener(PropertyChangeListener changeListener) {
        addPropertyChangeListener(INPUT_PARAMETER_COMPONENTS_CHANGED, changeListener);
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

    public void clearInputParameterComponentValues() {
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            parameterComponent.clearParameterValue();
        }
    }

    protected final Map<String, Object> getParameterValues() {
        Map<String, Object> result = new HashMap<>();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent<?> parameterComponent = this.parameterComponents.get(i);
            Object value = parameterComponent.getParameterValue();
            if (value == null) {
                String errorMessage = parameterComponent.getRequiredErrorDialogMessage();
                if (errorMessage != null) {
                    showErrorMessageDialog(errorMessage, "Required parameter");
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

    protected final void showInformationMessageDialog(String message, String title) {
        JOptionPane.showMessageDialog(getParent(), message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    protected final void refreshLabelWidths() {
        int maximumLabelWidth = computeLeftPanelMaximumLabelWidth();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            JLabel label = this.parameterComponents.get(i).getLabel();
            Dimension labelSize = label.getPreferredSize();
            labelSize.width = maximumLabelWidth;
            label.setPreferredSize(labelSize);
            label.setMinimumSize(labelSize);
        }
    }

    protected final void addAreaParameterComponent(RepositoryQueryParameter areaOfInterestParameter) {
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
