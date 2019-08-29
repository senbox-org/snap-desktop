package org.esa.snap.product.library.ui.v2.repository;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.CustomSplitPane;
import org.esa.snap.product.library.ui.v2.DownloadProductListTimerRunnable;
import org.esa.snap.product.library.ui.v2.DownloadQuickLookImagesRunnable;
import org.esa.snap.product.library.ui.v2.IMissionParameterListener;
import org.esa.snap.product.library.ui.v2.LoginDialog;
import org.esa.snap.product.library.ui.v2.QueryProductResultsPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressPanel;
import org.esa.snap.product.library.v2.RemoteRepositoryCredentials;
import org.esa.snap.product.library.v2.RepositoryProduct;
import org.esa.snap.product.library.v2.parameters.QueryFilter;
import org.esa.snap.product.library.v2.repository.ProductRepositoryDownloader;
import org.esa.snap.product.library.v2.repository.ProductsRepositoryProvider;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class RemoteProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private static final Logger logger = Logger.getLogger(RemoteProductsRepositoryPanel.class.getName());

    private final ComponentDimension componentDimension;
    private final IMissionParameterListener missionParameterListener;
    private final JLabel missionsLabel;
    private final JComboBox<String> missionsComboBox;
    private final ProductsRepositoryProvider productsRepositoryProvider;

    private List<AbstractParameterComponent> parameterComponents;
    private Credentials credentials;

    public RemoteProductsRepositoryPanel(ProductsRepositoryProvider productsRepositoryProvider, ComponentDimension componentDimension,
                                         IMissionParameterListener missionParameterListener) {

        super(new BorderLayout(componentDimension.getGapBetweenColumns(), componentDimension.getGapBetweenRows()));

        this.productsRepositoryProvider = productsRepositoryProvider;
        this.componentDimension = componentDimension;
        this.missionParameterListener = missionParameterListener;

        this.missionsLabel = new JLabel("Mission");

        String[] availableMissions = this.productsRepositoryProvider.getAvailableMissions();
        if (availableMissions.length > 0) {
            String valueToSelect = availableMissions[0];
            this.missionsComboBox = buildComboBox(availableMissions, valueToSelect, this.componentDimension);
            this.missionsComboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                        newSelectedMission();
                    }
                }
            });
        } else {
            throw new IllegalStateException("At least one supported mission must be defined.");
        }

        addParameters();
    }

    @Override
    public String getName() {
        return this.productsRepositoryProvider.getRepositoryName();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        this.missionsComboBox.setEnabled(enabled);
        for (int i=0; i<this.parameterComponents.size(); i++) {
            JComponent component = this.parameterComponents.get(i).getComponent();
            component.setEnabled(enabled);
        }
    }

    @Override
    public String getSelectedMission() {
        return (String) this.missionsComboBox.getSelectedItem();
    }

    @Override
    public AbstractProgressTimerRunnable<List<RepositoryProduct>> buildThreadToSearchProducts(ProgressPanel progressPanel, int threadId, ThreadListener threadListener,
                                                                                              QueryProductResultsPanel productResultsPanel) {

        DownloadProductListTimerRunnable thread = null;
        Map<String, Object> parameterValues = getParameterValues();
        if (parameterValues != null) {
            Credentials credentials = getUserCredentials();
            if (credentials != null) {
                String selectedMission = getSelectedMission();
                thread = new DownloadProductListTimerRunnable(progressPanel, threadId, credentials, this.productsRepositoryProvider, threadListener,
                                                              this, productResultsPanel, getName(), selectedMission, parameterValues);
            }
        }
        return thread;
    }

    @Override
    public AbstractRunnable<?> buildThreadToDisplayQuickLookImages(List<RepositoryProduct> productList, ThreadListener threadListener, QueryProductResultsPanel productResultsPanel) {
        if (this.credentials == null) {
            throw new NullPointerException("The credentials are null.");
        } else {
            return new DownloadQuickLookImagesRunnable(productList, this.credentials, threadListener, this, this.productsRepositoryProvider, productResultsPanel);
        }
    }

    @Override
    public Map<String, Object> getParameterValues() {
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

    @Override
    public ProductsRepositoryProvider buildProductListDownloader() {
        return this.productsRepositoryProvider;
    }

    @Override
    public ProductRepositoryDownloader buidProductDownloader(String mission) {
        return this.productsRepositoryProvider.buidProductDownloader(mission);
    }

    @Override
    public void refreshMissionParameters() {
        removeAll();
        addParameters();
        revalidate();
        repaint();
    }

    @Override
    public int computeLeftPanelMaximumLabelWidth() {
        int maximumLabelWidth = this.missionsLabel.getPreferredSize().width;
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            int labelWidth = parameterComponent.getLabel().getPreferredSize().width;
            if (maximumLabelWidth < labelWidth) {
                maximumLabelWidth = labelWidth;
            }
        }
        return maximumLabelWidth;
    }

    private void showErrorMessageDialog(String message, String title) {
        JOptionPane.showMessageDialog(getParent(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void newSelectedMission() {
        this.missionParameterListener.newSelectedMission(getSelectedMission(), RemoteProductsRepositoryPanel.this);
    }

    private Credentials getUserCredentials() {
        if (this.credentials == null) {
            RemoteRepositoryCredentials remoteRepositoryCredentials = RemoteRepositoryCredentials.getInstance();
            this.credentials = remoteRepositoryCredentials.read(this.productsRepositoryProvider.getRepositoryId());
            if (this.credentials == null) {
                LoginDialog loginDialog = new LoginDialog(SnapApp.getDefault().getMainFrame(), "User credentials");
                loginDialog.show();
                if (loginDialog.areCredentialsEntered()) {
                    try {
                        this.credentials = remoteRepositoryCredentials.save(this.productsRepositoryProvider.getRepositoryId(), loginDialog.getUsername(), loginDialog.getPassword());
                    } catch (BackingStoreException exception) {
                        logger.log(Level.SEVERE, "Failed to save the credentials into the application preferences.", exception);
                        this.credentials = new UsernamePasswordCredentials(loginDialog.getUsername(), loginDialog.getPassword());
                    }
                }
            }
        }
        return this.credentials;
    }

    private void addParameters() {
        JComponent panel = new JPanel(new GridBagLayout());
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();
        int textFieldPreferredHeight = this.componentDimension.getTextFieldPreferredHeight();

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        panel.add(this.missionsLabel, c);

        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        panel.add(this.missionsComboBox, c);

        this.parameterComponents = new ArrayList<>();

        String selectedMission = (String) this.missionsComboBox.getSelectedItem();
        int rowIndex = 1;
        QueryFilter rectangleParameter = null;
        List<QueryFilter> sensorParameters = this.productsRepositoryProvider.getMissionParameters(selectedMission);
        for (int i=0; i<sensorParameters.size(); i++) {
            QueryFilter param = sensorParameters.get(i);
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
                c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
                panel.add(parameterComponent.getLabel(), c);
                c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
                panel.add(parameterComponent.getComponent(), c);
                rowIndex++;
            }
        }

        c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, 0);
        panel.add(Box.createVerticalGlue(), c); // add an empty label

        if (rectangleParameter != null) {
            SelectionAreaParameterComponent selectionAreaParameterComponent = new SelectionAreaParameterComponent(rectangleParameter.getName(), rectangleParameter.getLabel(), rectangleParameter.isRequired());
            this.parameterComponents.add(selectionAreaParameterComponent);
            JPanel worldPanel = selectionAreaParameterComponent.getComponent();
            worldPanel.setBackground(Color.WHITE);
            worldPanel.setOpaque(true);
            worldPanel.setBorder(new EtchedBorder());

            JPanel areaOfInterestPanel = new JPanel(new GridBagLayout());
            c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, 0, 0);
            areaOfInterestPanel.add(selectionAreaParameterComponent.getLabel(), c);
            c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
            areaOfInterestPanel.add(worldPanel, c);

            CustomSplitPane verticalSplitPane = new CustomSplitPane(JSplitPane.VERTICAL_SPLIT, 1, 2);
            verticalSplitPane.setTopComponent(panel);
            verticalSplitPane.setBottomComponent(areaOfInterestPanel);

            panel = verticalSplitPane;
        }

        // set the same label with
        int maximumLabelWidth = computeLeftPanelMaximumLabelWidth();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            RemoteProductsRepositoryPanel.setLabelSize(parameterComponent.getLabel(), maximumLabelWidth);
        }

        add(panel, BorderLayout.CENTER);
    }

    public static void setLabelSize(JLabel label, int maximumLabelWidth) {
        Dimension labelSize = label.getPreferredSize();
        labelSize.width = maximumLabelWidth;
        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);
    }

    public static JComboBox<String> buildComboBox(String[] values, String valueToSelect, ComponentDimension componentDimension) {
        JComboBox<String> comboBox = new JComboBox<String>(values) {
            @Override
            public Color getBackground() {
                return Color.WHITE;
            }
        };
        Dimension comboBoxSize = comboBox.getPreferredSize();
        comboBoxSize.height = componentDimension.getTextFieldPreferredHeight();
        comboBox.setPreferredSize(comboBoxSize);
        comboBox.setMinimumSize(comboBoxSize);
        LabelListCellRenderer<String> renderer = new LabelListCellRenderer<String>(componentDimension.getListItemMargins()) {
            @Override
            protected String getItemDisplayText(String value) {
                return (value == null) ? " " : value;
            }
        };
        comboBox.setRenderer(renderer);
        comboBox.setMaximumRowCount(5);
        if (valueToSelect != null) {
            for (int i=0; i<values.length; i++) {
                if (valueToSelect.equals(values[i])) {
                    comboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        return comboBox;
    }
}

