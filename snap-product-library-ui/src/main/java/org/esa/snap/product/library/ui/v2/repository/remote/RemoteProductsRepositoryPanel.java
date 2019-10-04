package org.esa.snap.product.library.ui.v2.repository.remote;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.MissionParameterListener;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.ParametersPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
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
    private ActionListener downloadProductListener;
    private final JComboBox<Credentials> userAccountsComboBox;

    public RemoteProductsRepositoryPanel(RemoteProductsRepositoryProvider productsRepositoryProvider, ComponentDimension componentDimension,
                                         MissionParameterListener missionParameterListener, WorldWindowPanelWrapper worlWindPanel) {

        super(worlWindPanel, componentDimension, new BorderLayout(0, componentDimension.getGapBetweenRows()));

        this.productsRepositoryProvider = productsRepositoryProvider;
        this.missionParameterListener = missionParameterListener;

        if (this.productsRepositoryProvider.requiresAuthentication()) {
            this.userAccountsComboBox = buildComboBox(componentDimension);
            int cellItemHeight = this.userAccountsComboBox.getPreferredSize().height;
            LabelListCellRenderer<Credentials> renderer = new LabelListCellRenderer<Credentials>(cellItemHeight) {
                @Override
                protected String getItemDisplayText(Credentials value) {
                    return (value == null) ? " " : value.getUserPrincipal().getName();
                }
            };
            this.userAccountsComboBox.setRenderer(renderer);
        } else {
            this.userAccountsComboBox = null;
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

        if (this.userAccountsComboBox != null) {
            this.userAccountsComboBox.setEnabled(enabled);
        }
        this.missionsComboBox.setEnabled(enabled);
        for (int i=0; i<this.parameterComponents.size(); i++) {
            JComponent component = this.parameterComponents.get(i).getComponent();
            component.setEnabled(enabled);
        }
    }

    @Override
    public AbstractProgressTimerRunnable<?> buildThreadToSearchProducts(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                        RepositoryProductListPanel productResultsPanel) {

        Credentials selectedCredentials = null;
        boolean canContinue = true;
        if (this.userAccountsComboBox != null) {
            // the repository provider requires authentication
            selectedCredentials = (Credentials) this.userAccountsComboBox.getSelectedItem();
            if (selectedCredentials == null) {
                String message = "Select the account used to download the data.";
                showErrorMessageDialog(message, "Required credentials");
                this.userAccountsComboBox.requestFocus();
                canContinue = false;
            }
        }
        if (canContinue) {
            Map<String, Object> parameterValues = getParameterValues();
            if (parameterValues != null) {
                String selectedMission = getSelectedMission();
                return new DownloadProductListTimerRunnable(progressPanel, threadId, selectedCredentials, this.productsRepositoryProvider, threadListener,
                                                              productResultsPanel, getName(), selectedMission, parameterValues);
            }
        }
        return null;
    }

    @Override
    public JPopupMenu buildProductListPopupMenu(RepositoryProduct[] selectedProducts) {
        JMenuItem downloadMenuItem = new JMenuItem("Download");
        downloadMenuItem.addActionListener(this.downloadProductListener);
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(downloadMenuItem);
        return popupMenu;
    }

    @Override
    public RepositoryProductPanel buildProductProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                                           ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon) {

        return new RemoteRepositoryProductPanel(repositoryProductPanelBackground, componentDimension, expandImageIcon, collapseImageIcon);
    }

    @Override
    protected void addParameterComponents() {
        ParametersPanel panel = new ParametersPanel();
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();

        int rowIndex = 0;
        if (this.userAccountsComboBox != null) {
            GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
            panel.add(new JLabel("Account"), c);
            c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
            panel.add(this.userAccountsComboBox, c);
            rowIndex++;
            gapBetweenRows = this.componentDimension.getGapBetweenRows();
        }

        GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        panel.add(new JLabel("Mission"), c);
        c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        panel.add(this.missionsComboBox, c);
        rowIndex++;

        Class<?> areaOfInterestClass = Rectangle2D.class;
        Class<?>[] classesToIgnore = new Class<?>[] {areaOfInterestClass};
        String selectedMission = (String) this.missionsComboBox.getSelectedItem();
        List<QueryFilter> parameters = this.productsRepositoryProvider.getMissionParameters(selectedMission);
        this.parameterComponents = panel.addParameterComponents(parameters, rowIndex, gapBetweenRows, this.componentDimension, classesToIgnore);

        QueryFilter areaOfInterestParameter = null;
        for (int i=0; i<parameters.size(); i++) {
            QueryFilter param = parameters.get(i);
            if (param.getType() == areaOfInterestClass) {
                areaOfInterestParameter = param;
            }
        }

        add(panel, BorderLayout.NORTH);

        if (areaOfInterestParameter != null) {
            addAreaParameterComponent(areaOfInterestParameter);
        }

        refreshLabelWidths();
    }

    public void setDownloadProductListener(ActionListener downloadProductListener) {
        this.downloadProductListener = downloadProductListener;
    }

    public RemoteProductsRepositoryProvider getProductsRepositoryProvider() {
        return productsRepositoryProvider;
    }

    public void setUserAccounts(List<Credentials> repositoryCredentials) {
        if (this.userAccountsComboBox != null && repositoryCredentials.size() > 0) {
            this.userAccountsComboBox.removeAllItems();
            for (int i = 0; i < repositoryCredentials.size(); i++) {
                this.userAccountsComboBox.addItem(repositoryCredentials.get(i));
            }
            //this.userAccountsComboBox.setSelectedItem(null);
        }
    }

    private String getSelectedMission() {
        return (String) this.missionsComboBox.getSelectedItem();
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
        int cellItemHeight = comboBox.getPreferredSize().height;
        LabelListCellRenderer<String> renderer = new LabelListCellRenderer<String>(cellItemHeight) {
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

