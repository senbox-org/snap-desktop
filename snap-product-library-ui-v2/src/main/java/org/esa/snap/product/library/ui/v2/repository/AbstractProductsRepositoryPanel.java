package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.ProductLibraryV2Action;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.repository.input.AbstractParameterComponent;
import org.esa.snap.product.library.ui.v2.repository.input.ParametersPanel;
import org.esa.snap.product.library.ui.v2.repository.input.SelectionAreaParameterComponent;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductResults;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.thread.ThreadListener;
import org.esa.snap.worldwind.productlibrary.WorldMapPanelWrapper;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.ui.loading.CustomSplitPane;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The panel containing the query parameters of a repository.
 *
 * Created by jcoravu on 5/8/2019.
 */
public abstract class AbstractProductsRepositoryPanel extends JPanel {

    private static final String INPUT_PARAMETER_COMPONENTS_CHANGED = "parameterComponentsChanged";

    private final WorldMapPanelWrapper worlWindPanel;

    protected final ComponentDimension componentDimension;
    protected List<AbstractParameterComponent<?>> parameterComponents;

    private OutputProductResults outputProductResults;
    private List<ProductLibraryV2Action> popupMenuActions;

    protected AbstractProductsRepositoryPanel(WorldMapPanelWrapper worlWindPanel, ComponentDimension componentDimension, LayoutManager layoutManager) {
        super(layoutManager);

        this.worlWindPanel = worlWindPanel;
        this.componentDimension = componentDimension;

        resetOutputProducts();
    }

    public abstract String getRepositoryName();

    protected abstract ParametersPanel getInputParameterComponentsPanel();

    protected abstract RepositoryQueryParameter getAreaOfInterestParameter();

    public abstract boolean refreshInputParameterComponentValues();

    public abstract void resetInputParameterValues();

    public abstract AbstractRepositoryProductPanel buildProductProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                                                            ComponentDimension componentDimension);

    public abstract AbstractProgressTimerRunnable<?> buildSearchProductListThread(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                                  RemoteRepositoriesSemaphore remoteRepositoriesSemaphore, RepositoryOutputProductListPanel repositoryProductListPanel);

    private void addInputParameterComponentsToPanel(){
        ParametersPanel parametersPanel = getInputParameterComponentsPanel();
        RepositoryQueryParameter areaOfInterestParameter = getAreaOfInterestParameter();
        int gapBetweenColumns = 5;
        int visibleDividerSize = gapBetweenColumns - 2;
        int dividerMargins = 0;
        float initialDividerLocationPercent = 0.5f;
        CustomSplitPane parametersSplitPane = new CustomSplitPane(JSplitPane.VERTICAL_SPLIT, visibleDividerSize, dividerMargins, initialDividerLocationPercent, SwingUtils.TRANSPARENT_COLOR);
        JScrollPane parametersScrollPanel = new JScrollPane(parametersPanel);
        parametersScrollPanel.setBorder(null);
        parametersScrollPanel.setMinimumSize(new Dimension(300, 200));
        parametersSplitPane.setTopComponent(parametersScrollPanel);
        if(areaOfInterestParameter != null) {
            JPanel areaParameterComponent = getAreaParameterComponent(areaOfInterestParameter);
            areaParameterComponent.setMinimumSize(new Dimension(300, 200));
            parametersSplitPane.setBottomComponent(areaParameterComponent);
        }
        add(parametersSplitPane, BorderLayout.CENTER);
    }

    public JButton[] getTopBarButton() {
        return null;
    }

    public final void resetOutputProducts() {
        this.outputProductResults = new OutputProductResults();
    }

    public final OutputProductResults getOutputProductResults() {
        return outputProductResults;
    }

    public void setPopupMenuActions(List<ProductLibraryV2Action> remoteActions) {
        this.popupMenuActions = remoteActions;
    }

    public final JPopupMenu buildProductListPopupMenu(RepositoryProduct[] selectedProducts, OutputProductListModel productListModel) {
        JPopupMenu popupMenu = new JPopupMenu();
        for (int i = 0; i<this.popupMenuActions.size(); i++) {
            ProductLibraryV2Action action = this.popupMenuActions.get(i);
            if (action.canAddItemToPopupMenu(this, selectedProducts)) {
                popupMenu.add(action);
            }
        }
        return popupMenu;
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
        Map<String, Object> parameterValues = new LinkedHashMap<>();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent<?> parameterComponent = this.parameterComponents.get(i);
            Boolean result = parameterComponent.hasValidValue();
            if (result == null) {
                // the value is not specified
                String errorMessage = parameterComponent.getRequiredValueErrorDialogMessage();
                if (errorMessage != null) {
                    // the value is required and show a message dialog
                    showErrorMessageDialog(errorMessage, "Required parameter value");
                    parameterComponent.getComponent().requestFocus();
                    return null; // no parameters to return
                }
            } else if (result.booleanValue()) {
                // the value is specified and it is valid
                Object value = parameterComponent.getParameterValue();
                if (value == null) {
                    throw new NullPointerException("The parameter value is null.");
                } else {
                    parameterValues.put(parameterComponent.getParameterName(), value);
                }
            } else {
                // the value is specified and it is invalid
                String errorMessage = parameterComponent.getInvalidValueErrorDialogMessage();
                if (errorMessage == null) {
                    throw new NullPointerException("The error message for invalid value is null.");
                } else {
                    // the value is required and show a message dialog
                    showErrorMessageDialog(errorMessage, "Invalid parameter value");
                    parameterComponent.getComponent().requestFocus();
                    return null; // no parameters to return
                }
            }
        }
        return parameterValues;
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

    protected final JPanel getAreaParameterComponent(RepositoryQueryParameter areaOfInterestParameter) {
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
        return centerPanel;
    }
}
