package org.esa.snap.product.library.ui.v2.repository.remote;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.MissionParameterListener;
import org.esa.snap.product.library.ui.v2.RemoteRepositoryCredentials;
import org.esa.snap.product.library.ui.v2.RemoteRepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class RemoteProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private final MissionParameterListener missionParameterListener;
    private final JComboBox<String> missionsComboBox;
    private final RemoteProductsRepositoryProvider productsRepositoryProvider;
    private final ActionListener downloadRemoteProductListener;
    private final JComboBox<Credentials> userAccountsComboBox;

    public RemoteProductsRepositoryPanel(RemoteProductsRepositoryProvider productsRepositoryProvider, ComponentDimension componentDimension,
                                         ActionListener downloadRemoteProductListener, MissionParameterListener missionParameterListener,
                                         WorldWindowPanelWrapper worlWindPanel) {

        super(worlWindPanel, componentDimension, new GridBagLayout());

        this.productsRepositoryProvider = productsRepositoryProvider;
        this.missionParameterListener = missionParameterListener;
        this.downloadRemoteProductListener = downloadRemoteProductListener;

        this.userAccountsComboBox = buildComboBox(componentDimension);
        LabelListCellRenderer<Credentials> renderer = new LabelListCellRenderer<Credentials>(componentDimension.getListItemMargins()) {
            @Override
            protected String getItemDisplayText(Credentials value) {
                return (value == null) ? " " : value.getUserPrincipal().getName();
            }
        };
        this.userAccountsComboBox.setRenderer(renderer);
        List<Credentials> credentials = RemoteRepositoryCredentials.getInstance().getRepositoryCredentials(productsRepositoryProvider.getRepositoryId());
        for (int i=0; i<credentials.size(); i++) {
            this.userAccountsComboBox.addItem(credentials.get(i));
        }

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
    public AbstractProgressTimerRunnable<List<RepositoryProduct>> buildThreadToSearchProducts(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                                              RemoteRepositoryProductListPanel productResultsPanel) {

        DownloadProductListTimerRunnable thread = null;
        Credentials selectedCredentials = (Credentials) this.userAccountsComboBox.getSelectedItem();
        if (selectedCredentials == null) {
            String message = "Select the account used to download the data.";
            showErrorMessageDialog(message, "Required credentials");
            this.userAccountsComboBox.requestFocus();
        } else {
            Map<String, Object> parameterValues = getParameterValues();
            if (parameterValues != null) {
                String selectedMission = getSelectedMission();
                thread = new DownloadProductListTimerRunnable(progressPanel, threadId, selectedCredentials, this.productsRepositoryProvider, threadListener,
                                                              this, productResultsPanel, getName(), selectedMission, parameterValues);
            }
        }
        return thread;
    }

    @Override
    public AbstractRunnable<?> buildThreadToDisplayQuickLookImages(List<RepositoryProduct> productList, ThreadListener threadListener,
                                                                   RemoteRepositoryProductListPanel productResultsPanel) {

        Credentials selectedCredentials = (Credentials) this.userAccountsComboBox.getSelectedItem();
        if (selectedCredentials == null) {
            throw new NullPointerException("The credentials are null.");
        } else {
            return new DownloadQuickLookImagesRunnable(productList, selectedCredentials, threadListener, this, this.productsRepositoryProvider, productResultsPanel);
        }
    }

    @Override
    public JPopupMenu buildProductListPopupMenu() {
        JMenuItem downloadMenuItem = new JMenuItem("Download");
        downloadMenuItem.addActionListener(this.downloadRemoteProductListener);
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(downloadMenuItem);
        return popupMenu;
    }

    @Override
    protected void addParameterComponents() {
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        add(new JLabel("Account"), c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        add(this.userAccountsComboBox, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(new JLabel("Mission"), c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        add(this.missionsComboBox, c);

        String selectedMission = (String) this.missionsComboBox.getSelectedItem();
        List<QueryFilter> missionParameters = this.productsRepositoryProvider.getMissionParameters(selectedMission);
        addParameterComponents(missionParameters, 2, gapBetweenRows);
    }

    public RemoteProductsRepositoryProvider getProductsRepositoryProvider() {
        return productsRepositoryProvider;
    }

    private void newSelectedMission() {
        this.missionParameterListener.newSelectedMission(getSelectedMission(), RemoteProductsRepositoryPanel.this);
    }

    public static void setLabelSize(JLabel label, int maximumLabelWidth) {
        Dimension labelSize = label.getPreferredSize();
        labelSize.width = maximumLabelWidth;
        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);
    }

    public static <ItemType> JComboBox<ItemType> buildComboBox(ComponentDimension componentDimension) {
        JComboBox<ItemType> comboBox = new JComboBox<ItemType>() {
            @Override
            public Color getBackground() {
                return Color.WHITE;
            }
        };
        Dimension comboBoxSize = comboBox.getPreferredSize();
        comboBoxSize.height = componentDimension.getTextFieldPreferredHeight();
        comboBox.setPreferredSize(comboBoxSize);
        comboBox.setMinimumSize(comboBoxSize);
        comboBox.setMaximumRowCount(5);
        return comboBox;
    }

    public static JComboBox<String> buildComboBox(String[] values, String valueToSelect, ComponentDimension componentDimension) {
        JComboBox<String> comboBox = buildComboBox(componentDimension);
        LabelListCellRenderer<String> renderer = new LabelListCellRenderer<String>(componentDimension.getListItemMargins()) {
            @Override
            protected String getItemDisplayText(String value) {
                return (value == null) ? " " : value;
            }
        };
        comboBox.setRenderer(renderer);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                comboBox.addItem(values[i]);
            }
        }
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

