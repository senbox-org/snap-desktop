package org.esa.snap.product.library.ui.v2.repository.remote;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.MissionParameterListener;
import org.esa.snap.product.library.ui.v2.ProductLibraryV2Action;
import org.esa.snap.product.library.ui.v2.repository.input.AbstractParameterComponent;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.AbstractRepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadingProductProgressCallback;
import org.esa.snap.product.library.ui.v2.thread.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.input.ParametersPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductListTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldMapPanelWrapper;
import org.esa.snap.product.library.v2.preferences.RepositoriesCredentialsController;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.CustomComboBox;
import org.esa.snap.ui.loading.ItemRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The panel containing the query parameters of a remote repository.
 *
 * Created by jcoravu on 5/8/2019.
 */
public class RemoteProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private final MissionParameterListener missionParameterListener;
    private final JComboBox<String> missionsComboBox;
    private final RemoteProductsRepositoryProvider productsRepositoryProvider;
    private final JComboBox<Credentials> userAccountsComboBox;

    private ItemListener missionItemListener;
    private RemoteInputParameterValues remoteInputParameterValues;
    private DownloadingProductProgressCallback downloadingProductProgressCallback;

    public RemoteProductsRepositoryPanel(RemoteProductsRepositoryProvider productsRepositoryProvider, ComponentDimension componentDimension,
                                         MissionParameterListener missionParameterListener, WorldMapPanelWrapper worlWindPanel) {

        super(worlWindPanel, componentDimension, new BorderLayout(0, componentDimension.getGapBetweenRows()));

        if (productsRepositoryProvider == null) {
            throw new NullPointerException("The remote products repository provider is null.");
        }

        this.productsRepositoryProvider = productsRepositoryProvider;
        this.missionParameterListener = missionParameterListener;

        if (this.productsRepositoryProvider.requiresAuthentication()) {
            ItemRenderer<Credentials> accountsItemRenderer = new ItemRenderer<Credentials>() {
                @Override
                public String getItemDisplayText(Credentials item) {
                    return (item == null) ? " " : item.getUserPrincipal().getName();
                }
            };
            this.userAccountsComboBox = new CustomComboBox(accountsItemRenderer, componentDimension.getTextFieldPreferredHeight(), false, componentDimension.getTextFieldBackgroundColor());
        } else {
            this.userAccountsComboBox = null;
        }

        String[] availableMissions = this.productsRepositoryProvider.getAvailableMissions();
        if (availableMissions.length > 0) {
            String valueToSelect = availableMissions[0];
            this.missionsComboBox = SwingUtils.buildComboBox(availableMissions, valueToSelect, this.componentDimension.getTextFieldPreferredHeight(), false);
            this.missionsComboBox.setBackground(this.componentDimension.getTextFieldBackgroundColor());
            this.missionItemListener = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                        newSelectedMission();
                    }
                }
            };
            this.missionsComboBox.addItemListener(this.missionItemListener);
        } else {
            throw new IllegalStateException("At least one supported mission must be defined for '"+ getRepositoryName()+"' remote repository.");
        }

        this.parameterComponents = Collections.emptyList();
    }

    @Override
    public String getRepositoryName() {
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
    public void resetInputParameterValues() {
        this.remoteInputParameterValues = null;
    }

    @Override
    public AbstractProgressTimerRunnable<?> buildSearchProductListThread(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                         RemoteRepositoriesSemaphore remoteRepositoriesSemaphore, RepositoryOutputProductListPanel productResultsPanel) {

        Credentials selectedCredentials = null;
        boolean canContinue = true;
        if (this.userAccountsComboBox != null) {
            // the repository provider requires authentication
            selectedCredentials = (Credentials) this.userAccountsComboBox.getSelectedItem();
            if (selectedCredentials == null) {
                // no credential account is selected
                if (this.userAccountsComboBox.getModel().getSize() > 0) {
                    String message = "Select the account used to search the product list on the remote repository.";
                    showErrorMessageDialog(message, "Required credentials");
                    this.userAccountsComboBox.requestFocus();
                } else {
                    StringBuilder message = new StringBuilder();
                    message.append("There is no account defined in the application.")
                            .append("\n\n")
                            .append("To add an account for the remote repository go to the 'Tools -> Options' menu and select the 'Product Library' tab.");
                    showInformationMessageDialog(message.toString(), "Add credentials");
                }
                canContinue = false;
            }
        }
        if (canContinue) {
            Map<String, Object> parameterValues = getParameterValues();
            if (parameterValues != null) {
                String selectedMission = getSelectedMission(); // the selected mission can not be null
                if (selectedMission == null) {
                    throw new NullPointerException("The remote mission is null");
                }
                this.remoteInputParameterValues = new RemoteInputParameterValues(parameterValues, selectedMission);
                return new DownloadProductListTimerRunnable(progressPanel, threadId, selectedCredentials, this.productsRepositoryProvider, threadListener, productResultsPanel, getRepositoryName(), selectedMission, parameterValues);
            }
        }
        return null;
    }

    @Override
    public AbstractRepositoryProductPanel buildProductProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                                                   ComponentDimension componentDimension) {

        return new RemoteRepositoryProductPanel(repositoryProductPanelBackground, this.downloadingProductProgressCallback, componentDimension);
    }

    @Override
    protected ParametersPanel getInputParameterComponentsPanel() {
        ParametersPanel panel = new ParametersPanel();
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();

        int rowIndex = 0;
        int gapBetweenRows = 0;
        if (this.userAccountsComboBox != null) {
            GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
            panel.add(new JLabel("Account"), c);
            c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
            panel.add(this.userAccountsComboBox, c);
            rowIndex++;
            gapBetweenRows = this.componentDimension.getGapBetweenRows(); // the gap between rows for next lines
        }

        GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        panel.add(new JLabel("Mission"), c);
        c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        panel.add(this.missionsComboBox, c);
        rowIndex++;

        gapBetweenRows = this.componentDimension.getGapBetweenRows(); // the gap between rows for next lines

        Class<?> areaOfInterestClass = Rectangle2D.class;
        Class<?>[] classesToIgnore = new Class<?>[] {areaOfInterestClass};
        String selectedMission = getSelectedMission();
        List<RepositoryQueryParameter> parameters = this.productsRepositoryProvider.getMissionParameters(selectedMission);
        Credentials savedCredentials = RepositoriesCredentialsController.getInstance().getRepositoryCollectionCredential(this.productsRepositoryProvider.getRepositoryName(), selectedMission);
        // filter the UI displayed parameters (those having a proper label)
        List<RepositoryQueryParameter> parametersForUI = parameters.stream().filter(p -> StringUtils.isNotNullAndNotEmpty(p.getLabel())).collect(Collectors.toList());
        this.parameterComponents = panel.addParameterComponents(parametersForUI, rowIndex, gapBetweenRows, this.componentDimension, classesToIgnore);

        if (savedCredentials != null) {
            for (AbstractParameterComponent<?> parameterComponent : this.parameterComponents) {
                if (parameterComponent.getParameterName().contentEquals("username")) {
                    parameterComponent.setParameterValue(savedCredentials.getUserPrincipal().getName());
                }
                if (parameterComponent.getParameterName().contentEquals("password")) {
                    parameterComponent.setParameterValue(savedCredentials.getPassword());
                }
            }
        }
        return panel;
    }

    @Override
    protected RepositoryQueryParameter getAreaOfInterestParameter(){
        Class<?> areaOfInterestClass = Rectangle2D.class;
        String selectedMission = getSelectedMission();
        List<RepositoryQueryParameter> parameters = this.productsRepositoryProvider.getMissionParameters(selectedMission);
        RepositoryQueryParameter areaOfInterestParameter = null;
        for (int i=0; i<parameters.size(); i++) {
            RepositoryQueryParameter param = parameters.get(i);
            if (param.getType() == areaOfInterestClass) {
                areaOfInterestParameter = param;
            }
        }
        return areaOfInterestParameter;
    }

    @Override
    public boolean refreshInputParameterComponentValues() {
        if (this.remoteInputParameterValues != null) {
            this.missionsComboBox.removeItemListener(this.missionItemListener);
            try {
                this.missionsComboBox.setSelectedItem(this.remoteInputParameterValues.getMissionName());
            } finally {
                this.missionsComboBox.addItemListener(this.missionItemListener);
            }
            for (int i=0; i<this.parameterComponents.size(); i++) {
                AbstractParameterComponent<?> inputParameterComponent = this.parameterComponents.get(i);
                Object parameterValue = this.remoteInputParameterValues.getParameterValue(inputParameterComponent.getParameterName());
                inputParameterComponent.setParameterValue(parameterValue);
            }
            return true;
        }
        return false;
    }

    public void updateInputParameterValues(String missionName, LocalDateTime startDate, LocalDateTime endDate, Rectangle2D.Double areaOfInterestToSelect) {
        this.missionsComboBox.removeItemListener(this.missionItemListener);
        try {
            this.missionsComboBox.setSelectedItem(missionName);
        } finally {
            this.missionsComboBox.addItemListener(this.missionItemListener);
        }
        for (AbstractParameterComponent<?> inputParameterComponent : this.parameterComponents) {
            switch (inputParameterComponent.getParameterName()) {
                case RepositoryQueryParameter.FOOTPRINT:
                    inputParameterComponent.setParameterValue(areaOfInterestToSelect);
                    break;
                case RepositoryQueryParameter.START_DATE:
                    inputParameterComponent.setParameterValue(startDate);
                    break;
                case RepositoryQueryParameter.END_DATE:
                    inputParameterComponent.setParameterValue(endDate);
                    break;
            }
        }
    }

    public void addDownloadedProductProgress(RepositoryProduct repositoryProduct, DownloadProgressStatus downloadProgressStatus) {
        getOutputProductResults().addDownloadedProductProgress(repositoryProduct, downloadProgressStatus);
    }

    public void setDownloadingProductProgressCallback(DownloadingProductProgressCallback downloadingProductProgressCallback) {
        this.downloadingProductProgressCallback = downloadingProductProgressCallback;
    }

    private Credentials getSearchCredentials() {
        String username = null;
        String password = null;
        for (AbstractParameterComponent<?> parameterComponent : this.parameterComponents) {
            if (parameterComponent.getParameterName().contentEquals("username")) {
                username = (String) parameterComponent.getParameterValue();
            }
            if (parameterComponent.getParameterName().contentEquals("password")) {
                password = (String) parameterComponent.getParameterValue();
            }
        }
        if (StringUtils.isNotNullAndNotEmpty(username) && StringUtils.isNotNullAndNotEmpty(password)) {
            return new UsernamePasswordCredentials(username, password);
        }
        return null;
    }

    public Credentials getSelectedAccount() {
        Credentials selectedCredentials = getSearchCredentials();
        if (selectedCredentials != null) {
            return selectedCredentials;
        }
        if (this.userAccountsComboBox != null) {
            // the repository provider requires authentication
            selectedCredentials = (Credentials) this.userAccountsComboBox.getSelectedItem();
            if (selectedCredentials == null) {
                // no credential account is selected
                throw new NullPointerException("No credential account is selected.");
            }
        }
        return selectedCredentials;
    }

    public RemoteProductsRepositoryProvider getProductsRepositoryProvider() {
        return this.productsRepositoryProvider;
    }

    public void setUserAccounts(List<Credentials> repositoryCredentials) {
        if (this.userAccountsComboBox != null && repositoryCredentials.size() > 0) {
            this.userAccountsComboBox.removeAllItems();
            for (int i = 0; i < repositoryCredentials.size(); i++) {
                this.userAccountsComboBox.addItem(repositoryCredentials.get(i));
            }
        }
    }

    private String getSelectedMission() {
        return (String) this.missionsComboBox.getSelectedItem();
    }

    private void newSelectedMission() {
        this.missionParameterListener.newSelectedMission(getSelectedMission(), RemoteProductsRepositoryPanel.this);
    }
}

