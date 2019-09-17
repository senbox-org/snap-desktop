package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RemoteRepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
    protected List<AbstractParameterComponent> parameterComponents;

    protected AbstractProductsRepositoryPanel(WorldWindowPanelWrapper worlWindPanel, ComponentDimension componentDimension, LayoutManager layoutManager) {
        super(layoutManager);

        this.worlWindPanel = worlWindPanel;
        this.componentDimension = componentDimension;
    }

    public abstract String getName();

    protected abstract void addParameterComponents();

    public String getSelectedMission() {
        return null;
    }

    public void refreshParameterComponents() {
        removeAll();
        addParameterComponents();
        revalidate();
        repaint();
    }

    public abstract JPopupMenu buildProductListPopupMenu();

    public AbstractProgressTimerRunnable<List<RepositoryProduct>> buildThreadToSearchProducts(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                                              RemoteRepositoryProductListPanel repositoryProductListPanel) {
        return null;
    }

    public AbstractRunnable<?> buildThreadToDisplayQuickLookImages(List<RepositoryProduct> productList, ThreadListener threadListener, RemoteRepositoryProductListPanel productResultsPanel) {
        return null;
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

    protected final void addParameterComponents(List<QueryFilter> parameters, int startRowIndex, int startGapBetweenRows) {
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();
        int textFieldPreferredHeight = this.componentDimension.getTextFieldPreferredHeight();

        this.parameterComponents = new ArrayList<>();

        int rowIndex = startRowIndex;
        QueryFilter rectangleParameter = null;
        for (int i=0; i<parameters.size(); i++) {
            QueryFilter param = parameters.get(i);
            AbstractParameterComponent parameterComponent = null;
            if (param.getType() == String.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : (String)param.getDefaultValue();
                if (param.getValueSet() == null) {
                    parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight);
                } else {
                    String[] defaultValues = (String[])param.getValueSet();
                    String[] values = new String[defaultValues.length + 1];
                    System.arraycopy(defaultValues, 0, values, 1, defaultValues.length);
                    parameterComponent = new StringComboBoxParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), values, this.componentDimension);
                }
            } else if (param.getType() == Double.class || param.getType() == Integer.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight);
            } else if (param.getType() == Date.class) {
                parameterComponent = new DateParameterComponent(param.getName(), param.getLabel(), param.isRequired(), textFieldPreferredHeight);
            } else if (param.getType() == Rectangle.Double.class) {
                rectangleParameter = param;
            } else if (param.getType() == String[].class) {
                //TODO Jean implement a specific parameter
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight);
            } else {
                throw new IllegalArgumentException("Unknown parameter: name: '"+param.getName()+"', type: '"+param.getType()+"', label: '" + param.getLabel()+"'.");
            }
            if (parameterComponent != null) {
                this.parameterComponents.add(parameterComponent);
                int topMargin = (rowIndex == startRowIndex) ? startGapBetweenRows : gapBetweenRows;
                GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, topMargin, 0);
                add(parameterComponent.getLabel(), c);
                c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, topMargin, gapBetweenColumns);
                add(parameterComponent.getComponent(), c);
                rowIndex++;
            }
        }

        if (rectangleParameter == null) {
            GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, 0);
            add(Box.createVerticalGlue(), c); // add an empty label
        } else {
            this.worlWindPanel.clearSelectedArea();

            SelectionAreaParameterComponent selectionAreaParameterComponent = new SelectionAreaParameterComponent(this.worlWindPanel, rectangleParameter.getName(), rectangleParameter.getLabel(), rectangleParameter.isRequired());
            this.parameterComponents.add(selectionAreaParameterComponent);
            int topMargin = (rowIndex == startRowIndex) ? startGapBetweenRows : gapBetweenRows;

            GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, topMargin, 0);
            add(selectionAreaParameterComponent.getLabel(), c);
            c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, topMargin, gapBetweenColumns);
            add(selectionAreaParameterComponent.getComponent(), c);
            rowIndex++;
        }
    }
}
