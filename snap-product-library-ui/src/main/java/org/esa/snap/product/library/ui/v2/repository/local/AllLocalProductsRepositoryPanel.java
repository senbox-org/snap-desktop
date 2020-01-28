package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.ProductListModel;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.AbstractRepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.ParametersPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldMapPanelWrapper;
import org.esa.snap.product.library.v2.database.*;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.RemoteMission;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final JComboBox<LocalRepositoryFolder> foldersComboBox;
    private final JComboBox<String> remoteMissionsComboBox;
    private final JComboBox<String> attributesComboBox;
    private final JButton scanFoldersButton;
    private final JButton addFolderButton;
    private final JButton removeFoldersButton;

    private LocalProductsPopupListeners localProductsPopupListeners;

    public AllLocalProductsRepositoryPanel(ComponentDimension componentDimension, WorldMapPanelWrapper worlWindPanel) {
        super(worlWindPanel, componentDimension, new BorderLayout(0, componentDimension.getGapBetweenRows()));

        Path databaseParentFolderPath = H2DatabaseAccessor.getDatabaseParentFolder();
        H2DatabaseParameters databaseParameters = new H2DatabaseParameters(databaseParentFolderPath);
        this.allLocalFolderProductsRepository = new AllLocalFolderProductsRepository(databaseParameters);

        Dimension buttonSize = new Dimension(componentDimension.getTextFieldPreferredHeight(), componentDimension.getTextFieldPreferredHeight());

        this.foldersComboBox = RemoteProductsRepositoryPanel.buildComboBox(componentDimension);
        LabelListCellRenderer<LocalRepositoryFolder> foldersRenderer = new LabelListCellRenderer<LocalRepositoryFolder>(this.foldersComboBox.getPreferredSize().height) {
            @Override
            protected String getItemDisplayText(LocalRepositoryFolder value) {
                return (value == null) ? " " : value.getPath().toString();
            }
        };
        this.foldersComboBox.setRenderer(foldersRenderer);

        this.remoteMissionsComboBox = RemoteProductsRepositoryPanel.buildComboBox(componentDimension);
        LabelListCellRenderer<String> missionsRenderer = new LabelListCellRenderer<String>(this.remoteMissionsComboBox.getPreferredSize().height) {
            @Override
            protected String getItemDisplayText(String value) {
                return (value == null) ? " " : value;
            }
        };
        this.remoteMissionsComboBox.setRenderer(missionsRenderer);

        this.attributesComboBox = RemoteProductsRepositoryPanel.buildComboBox(null, null, componentDimension);

        this.addFolderButton = RepositorySelectionPanel.buildButton("/org/esa/snap/resources/images/icons/Add16.png", null, buttonSize, 1);
        this.addFolderButton.setToolTipText("Add new local folder");

        this.scanFoldersButton = RepositorySelectionPanel.buildButton("/org/esa/snap/productlibrary/icons/refresh24.png", null, buttonSize, 1);
        this.scanFoldersButton.setToolTipText("Scan all local folders");

        this.removeFoldersButton = RepositorySelectionPanel.buildButton("/org/esa/snap/resources/images/icons/Remove16.png", null, buttonSize, 1);
        this.removeFoldersButton.setToolTipText("Remove all local folders");
    }

    @Override
    public String getName() {
        return "All Local Folders";
    }

    @Override
    protected void addParameterComponents() {
        ParametersPanel panel = new ParametersPanel();
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        panel.add(new JLabel("Folder"), c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        panel.add(this.foldersComboBox, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        panel.add(new JLabel("Mission"), c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        panel.add(this.remoteMissionsComboBox, c);

        Class<?> areaOfInterestClass = Rectangle2D.class;
        Class<?> attributesClass = Attribute.class;
        Class<?>[] classesToIgnore = new Class<?>[]{areaOfInterestClass, attributesClass};
        List<RepositoryQueryParameter> parameters = this.allLocalFolderProductsRepository.getParameters();
        int startRowIndex = 2;
        this.parameterComponents = panel.addParameterComponents(parameters, startRowIndex, gapBetweenRows, this.componentDimension, classesToIgnore);

        RepositoryQueryParameter areaOfInterestParameter = null;
        RepositoryQueryParameter attributesParameter = null;
        for (int i = 0; i < parameters.size(); i++) {
            RepositoryQueryParameter param = parameters.get(i);
            if (param.getType() == areaOfInterestClass) {
                areaOfInterestParameter = param;
            } else if (param.getType() == attributesClass) {
                attributesParameter = param;
            }
        }

        if (attributesParameter != null) {
            int nextRowIndex = startRowIndex + this.parameterComponents.size() + 1;
            AttributesParameterComponent parameterComponent = new AttributesParameterComponent(this.attributesComboBox, attributesParameter.getName(), attributesParameter.getLabel(), attributesParameter.isRequired(), this.componentDimension);
            this.parameterComponents.add(parameterComponent);

            int difference = this.componentDimension.getTextFieldPreferredHeight() - parameterComponent.getLabel().getPreferredSize().height;

            c = SwingUtils.buildConstraints(0, nextRowIndex, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows + (difference / 2), 0);
            panel.add(parameterComponent.getLabel(), c);
            c = SwingUtils.buildConstraints(1, nextRowIndex, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
            panel.add(parameterComponent.getComponent(), c);
        }

        add(panel, BorderLayout.NORTH);

        if (areaOfInterestParameter != null) {
            addAreaParameterComponent(areaOfInterestParameter);
        }

        refreshLabelWidths();
    }

    @Override
    public AbstractProgressTimerRunnable<List<RepositoryProduct>> buildThreadToSearchProducts(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                                              RemoteRepositoriesSemaphore remoteRepositoriesSemaphore, RepositoryProductListPanel repositoryProductListPanel) {

        Map<String, Object> parameterValues = getParameterValues();
        if (parameterValues != null) {
            LocalRepositoryFolder localRepositoryFolder = (LocalRepositoryFolder)this.foldersComboBox.getSelectedItem();
            String selectedMissionName = (String) this.remoteMissionsComboBox.getSelectedItem();
            return new LoadProductListTimerRunnable(progressPanel, threadId, threadListener, localRepositoryFolder, selectedMissionName,
                                                    parameterValues, repositoryProductListPanel, this.allLocalFolderProductsRepository);
        }
        return null;
    }

    @Override
    public JPopupMenu buildProductListPopupMenu(RepositoryProduct[] selectedProducts, ProductListModel productListModel) {
        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(this.localProductsPopupListeners.getOpenProductListener());
        JMenuItem deleteMenuItem = new JMenuItem("Delete");
        deleteMenuItem.addActionListener(this.localProductsPopupListeners.getDeleteProductListener());
        JMenuItem batchProcessingMenuItem = new JMenuItem("Batch Processing");
        batchProcessingMenuItem.addActionListener(this.localProductsPopupListeners.getBatchProcessingListener());
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(openMenuItem);
        popupMenu.add(deleteMenuItem);
        popupMenu.add(batchProcessingMenuItem);
        if (selectedProducts.length == 1) {
            JMenuItem showInExplorerMenuItem = new JMenuItem("Show in Explorer");
            showInExplorerMenuItem.addActionListener(this.localProductsPopupListeners.getShowInExplorerListener());
            popupMenu.add(showInExplorerMenuItem);
        }
        return popupMenu;
    }

    @Override
    public AbstractRepositoryProductPanel buildProductProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                                                   ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon) {

        return new LocalRepositoryProductPanel(repositoryProductPanelBackground, componentDimension, expandImageIcon, collapseImageIcon);
    }

    @Override
    public JButton[] getTopBarButton() {
        return new JButton[]{this.scanFoldersButton, this.addFolderButton, this.removeFoldersButton};
    }

    @Override
    public void clearParameterValues() {
        this.foldersComboBox.setSelectedItem(null);
        this.remoteMissionsComboBox.setSelectedItem(null);

        super.clearParameterValues();
    }

    public AllLocalFolderProductsRepository getAllLocalFolderProductsRepository() {
        return allLocalFolderProductsRepository;
    }

    public void deleteLocalRepositoryFolder(LocalRepositoryFolder localRepositoryFolderToRemove) {
        ComboBoxModel<LocalRepositoryFolder> foldersModel = this.foldersComboBox.getModel();
        for (int i = 0; i < foldersModel.getSize(); i++) {
            LocalRepositoryFolder existingFolder = foldersModel.getElementAt(i);
            if (existingFolder != null && existingFolder.getId() == localRepositoryFolderToRemove.getId()) {
                this.foldersComboBox.removeItemAt(i);
                break;
            }
        }
        if (foldersModel.getSize() == 1 && foldersModel.getElementAt(0) == null) {
            this.foldersComboBox.removeItemAt(0);
        }
    }

    public void setTopBarButtonListeners(ActionListener scanRepositoryFoldersListener, ActionListener addRepositoryFoldersListener, ActionListener deleteRepositoryFoldersListener) {
        this.scanFoldersButton.addActionListener(scanRepositoryFoldersListener);
        this.addFolderButton.addActionListener(addRepositoryFoldersListener);
        this.removeFoldersButton.addActionListener(deleteRepositoryFoldersListener);
    }

    public void addMissionIfMissing(String mission) {
        ComboBoxModel<String> missionsModel = this.remoteMissionsComboBox.getModel();
        boolean foundMission = false;
        for (int i = 0; i < missionsModel.getSize() && !foundMission; i++) {
            String existingMission = missionsModel.getElementAt(i);
            if (existingMission != null && existingMission.equalsIgnoreCase(mission)) {
                foundMission = true;
            }
        }
        if (!foundMission) {
            if (missionsModel.getSize() == 0) {
                this.remoteMissionsComboBox.addItem(null);
            }
            this.remoteMissionsComboBox.addItem(mission);
        }
    }

    public void addLocalRepositoryFolderIfMissing(LocalRepositoryFolder localRepositoryFolder) {
        ComboBoxModel<LocalRepositoryFolder> foldersModel = this.foldersComboBox.getModel();
        boolean foundFolder = false;
        for (int i = 0; i < foldersModel.getSize() && !foundFolder; i++) {
            LocalRepositoryFolder existingFolder = foldersModel.getElementAt(i);
            if (existingFolder != null && existingFolder.getId() == localRepositoryFolder.getId()) {
                foundFolder = true;
            }
        }
        if (!foundFolder) {
            if (foldersModel.getSize() == 0) {
                this.foldersComboBox.addItem(null);
            }
            this.foldersComboBox.addItem(localRepositoryFolder);
        }
    }

    public List<LocalRepositoryFolder> getLocalRepositoryFolders() {
        ComboBoxModel<LocalRepositoryFolder> foldersModel = this.foldersComboBox.getModel();
        List<LocalRepositoryFolder> result = new ArrayList<>(foldersModel.getSize());
        for (int i = 0; i < foldersModel.getSize(); i++) {
            LocalRepositoryFolder existingFolder = foldersModel.getElementAt(i);
            if (existingFolder != null) {
                result.add(existingFolder);
            }
        }
        return result;
    }

    public void setLocalProductsPopupListeners(LocalProductsPopupListeners localProductsPopupListeners) {
        this.localProductsPopupListeners = localProductsPopupListeners;
    }

    public void setLocalParameterValues(LocalRepositoryParameterValues localRepositoryParameterValues) {
        List<LocalRepositoryFolder> localRepositoryFolders = null;
        List<String> remoteMissionNames = null;
        Map<Short, Set<String>> attributeNamesPerMission = null;
        if (localRepositoryParameterValues != null) {
            localRepositoryFolders = localRepositoryParameterValues.getLocalRepositoryFolders();
            remoteMissionNames = localRepositoryParameterValues.getRemoteMissionNames();
            attributeNamesPerMission = localRepositoryParameterValues.getAttributes();
        }
        this.foldersComboBox.removeAllItems();
        if (localRepositoryFolders != null && localRepositoryFolders.size() > 0) {
            this.foldersComboBox.addItem(null);
            for (int i = 0; i < localRepositoryFolders.size(); i++) {
                this.foldersComboBox.addItem(localRepositoryFolders.get(i));
            }
            this.foldersComboBox.setSelectedItem(null);
        }

        this.remoteMissionsComboBox.removeAllItems();
        if (remoteMissionNames != null && remoteMissionNames.size() > 0) {
            this.remoteMissionsComboBox.addItem(null);
            for (int i = 0; i < remoteMissionNames.size(); i++) {
                this.remoteMissionsComboBox.addItem(remoteMissionNames.get(i));
            }
            this.remoteMissionsComboBox.setSelectedItem(null);
        }

        this.attributesComboBox.removeAllItems();
        if (attributeNamesPerMission != null && attributeNamesPerMission.size() > 0) {
            Comparator<String> comparator = buildAttributeNamesComparator();
            SortedSet<String> uniqueAttributes = new TreeSet<>(comparator);
            for (Map.Entry<Short, Set<String>> entry : attributeNamesPerMission.entrySet()) {
                uniqueAttributes.addAll(entry.getValue());
            }
            this.attributesComboBox.addItem(null);
            for (String attributeName : uniqueAttributes) {
                this.attributesComboBox.addItem(attributeName);
            }
            this.attributesComboBox.setSelectedItem(null);
        }
    }

    public void addAttributesIfMissing(Set<String> productAttributeNames) {
        Comparator<String> comparator = buildAttributeNamesComparator();
        SortedSet<String> uniqueAttributes = new TreeSet<>(comparator);
        ComboBoxModel<String> attributesModel = this.attributesComboBox.getModel();
        for (int i = 0; i < attributesModel.getSize(); i++) {
            uniqueAttributes.add(attributesModel.getElementAt(i));
        }
        boolean newAttribute = false;
        for (String attributeName : productAttributeNames) {
            if (uniqueAttributes.add(attributeName)) {
                newAttribute = true;
            }
        }
        if (newAttribute) {
            int oldSize = attributesModel.getSize();
            this.attributesComboBox.removeAllItems();
            for (String attributeName : uniqueAttributes) {
                this.attributesComboBox.addItem(attributeName);
            }
            if (oldSize == 0) {
                // reset the first selected attribute name
                this.attributesComboBox.setSelectedItem(null);
            }
        }
    }

    private static Comparator<String> buildAttributeNamesComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        };
    }
}
