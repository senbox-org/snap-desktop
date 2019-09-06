package org.esa.snap.product.library.ui.v2.repository;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.DownloadProductListTimerRunnable;
import org.esa.snap.product.library.ui.v2.DownloadProductTimerRunnable;
import org.esa.snap.product.library.ui.v2.DownloadQuickLookImagesRunnable;
import org.esa.snap.product.library.ui.v2.MissionParameterListener;
import org.esa.snap.product.library.ui.v2.LoginDialog;
import org.esa.snap.product.library.ui.v2.RemoteRepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.RemoteRepositoryCredentials;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressPanel;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class RemoteRepositoryParametersPanel extends AbstractProductsRepositoryPanel {

    private static final Logger logger = Logger.getLogger(RemoteRepositoryParametersPanel.class.getName());

    private final MissionParameterListener missionParameterListener;
    private final JLabel missionsLabel;
    private final JComboBox<String> missionsComboBox;
    private final RemoteProductsRepositoryProvider productsRepositoryProvider;
    private final ActionListener downloadRemoteProductListener;

    private Credentials credentials;

    public RemoteRepositoryParametersPanel(RemoteProductsRepositoryProvider productsRepositoryProvider, ComponentDimension componentDimension,
                                           ActionListener downloadRemoteProductListener, MissionParameterListener missionParameterListener,
                                           WorldWindowPanelWrapper worlWindPanel) {

        super(worlWindPanel, componentDimension, new GridBagLayout());

        this.productsRepositoryProvider = productsRepositoryProvider;
        this.missionParameterListener = missionParameterListener;
        this.downloadRemoteProductListener = downloadRemoteProductListener;

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

        this.parameterComponents = Collections.emptyList();
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
                                                                                              RemoteRepositoryProductListPanel productResultsPanel) {

        DownloadProductListTimerRunnable thread = null;
        Map<String, Object> parameterValues = getParameterValues();
        if (parameterValues != null) {
            if (this.credentials == null) {
                readUserCredentials();
            }
            if (this.credentials != null) {
                String selectedMission = getSelectedMission();
                thread = new DownloadProductListTimerRunnable(progressPanel, threadId, this.credentials, this.productsRepositoryProvider, threadListener,
                                                              this, productResultsPanel, getName(), selectedMission, parameterValues);
            }
        }
        return thread;
    }

    @Override
    public AbstractRunnable<?> buildThreadToDisplayQuickLookImages(List<RepositoryProduct> productList, ThreadListener threadListener,
                                                                   RemoteRepositoryProductListPanel productResultsPanel) {

        if (this.credentials == null) {
            throw new NullPointerException("The credentials are null.");
        } else {
            return new DownloadQuickLookImagesRunnable(productList, this.credentials, threadListener, this, this.productsRepositoryProvider, productResultsPanel);
        }
    }

    @Override
    public AbstractProgressTimerRunnable<?> buildThreadToDownloadProduct(ProgressPanel progressPanel, int threadId, ThreadListener threadListener,
                                                                         RepositoryProduct selectedProduct, Path targetFolderPath,
                                                                         RemoteRepositoryProductListPanel productResultsPanel) {

        ProductRepositoryDownloader productRepositoryDownloader = this.productsRepositoryProvider.buidProductDownloader(selectedProduct.getMission());
        return new DownloadProductTimerRunnable(progressPanel, threadId, getName(), threadListener, productRepositoryDownloader, selectedProduct, targetFolderPath, productResultsPanel, this);
    }

    @Override
    public int computeLeftPanelMaximumLabelWidth() {
        int maximumLabelWidth = super.computeLeftPanelMaximumLabelWidth();
        return Math.max(this.missionsLabel.getPreferredSize().width, maximumLabelWidth);
    }

    @Override
    public JPopupMenu buildProductListPopupMenu() {
        JMenuItem downloadSelectedMenuItem = new JMenuItem("Download");
        downloadSelectedMenuItem.addActionListener(this.downloadRemoteProductListener);
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(downloadSelectedMenuItem);
        return popupMenu;
    }

    @Override
    protected void addParameterComponents() {
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        add(this.missionsLabel, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        add(this.missionsComboBox, c);

        String selectedMission = (String) this.missionsComboBox.getSelectedItem();
        List<QueryFilter> missionParameters = this.productsRepositoryProvider.getMissionParameters(selectedMission);
        addParameterComponents(missionParameters, 1, gapBetweenRows);
    }

    private void showErrorMessageDialog(String message, String title) {
        JOptionPane.showMessageDialog(getParent(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    private Map<String, Object> getParameterValues() {
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

    private void newSelectedMission() {
        this.missionParameterListener.newSelectedMission(getSelectedMission(), RemoteRepositoryParametersPanel.this);
    }

    private void readUserCredentials() {
        RemoteRepositoryCredentials remoteRepositoryCredentials = RemoteRepositoryCredentials.getInstance();
        try {
            this.credentials = remoteRepositoryCredentials.read(this.productsRepositoryProvider.getRepositoryId());
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to read the credentials from the application preferences.", exception);
        }
        if (this.credentials == null) {
            LoginDialog loginDialog = new LoginDialog(SnapApp.getDefault().getMainFrame(), "User credentials");
            loginDialog.show();
            if (loginDialog.areCredentialsEntered()) {
                this.credentials = new UsernamePasswordCredentials(loginDialog.getUsername(), loginDialog.getPassword());
                try {
                    remoteRepositoryCredentials.save(this.productsRepositoryProvider.getRepositoryId(), this.credentials);
                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Failed to save the credentials into the application preferences.", exception);
                }
            }
        }
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

