package org.esa.snap.product.library.ui.v2.repository.local;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.input.AbstractParameterComponent;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.AbstractRepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.thread.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.input.ParametersPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldMapPanelWrapper;
import org.esa.snap.product.library.v2.database.*;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.CustomComboBox;
import org.esa.snap.ui.loading.ItemRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final JComboBox<LocalRepositoryFolder> foldersComboBox;
    private final JComboBox<String> remoteMissionsComboBox;
    private final JComboBox<String> attributesComboBox;
    private final JComboBox<String> attributeValuesEditableComboBox;
    private final JButton scanFoldersButton;
    private final JButton addFolderButton;
    private final JButton removeFoldersButton;

    private LocalProductsPopupListeners localProductsPopupListeners;
    private LocalInputParameterValues localInputParameterValues;

    public AllLocalProductsRepositoryPanel(ComponentDimension componentDimension, WorldMapPanelWrapper worlWindPanel) {
        super(worlWindPanel, componentDimension, new BorderLayout(0, componentDimension.getGapBetweenRows()));

        Path databaseParentFolderPath = H2DatabaseAccessor.getDatabaseParentFolder();
        H2DatabaseParameters databaseParameters = new H2DatabaseParameters(databaseParentFolderPath);
        this.allLocalFolderProductsRepository = new AllLocalFolderProductsRepository(databaseParameters);

        Dimension buttonSize = new Dimension(componentDimension.getTextFieldPreferredHeight(), componentDimension.getTextFieldPreferredHeight());
        ItemRenderer<LocalRepositoryFolder> foldersItemRenderer = new ItemRenderer<LocalRepositoryFolder>() {
            @Override
            public String getItemDisplayText(LocalRepositoryFolder item) {
                return (item == null) ? " " : item.getPath().toString();
            }
        };
        this.foldersComboBox = new CustomComboBox(foldersItemRenderer, componentDimension.getTextFieldPreferredHeight(), false, componentDimension.getTextFieldBackgroundColor());

        ItemRenderer<String> missionsItemRenderer = new ItemRenderer<String>() {
            @Override
            public String getItemDisplayText(String item) {
                return (item == null) ? " " : item;
            }
        };
        this.remoteMissionsComboBox = new CustomComboBox(missionsItemRenderer, componentDimension.getTextFieldPreferredHeight(), false, componentDimension.getTextFieldBackgroundColor());

        this.attributesComboBox = SwingUtils.buildComboBox(null, null, componentDimension.getTextFieldPreferredHeight(), false);
        this.attributesComboBox.setBackground(componentDimension.getTextFieldBackgroundColor());

        this.addFolderButton = RepositorySelectionPanel.buildButton("/org/esa/snap/resources/images/icons/Add16.png", null, buttonSize, 1);
        this.addFolderButton.setToolTipText("Add new local folder");

        this.scanFoldersButton = RepositorySelectionPanel.buildButton("/org/esa/snap/productlibrary/icons/refresh24.png", null, buttonSize, 1);
        this.scanFoldersButton.setToolTipText("Scan all local folders");

        this.removeFoldersButton = RepositorySelectionPanel.buildButton("/org/esa/snap/resources/images/icons/Remove16.png", null, buttonSize, 1);
        this.removeFoldersButton.setToolTipText("Remove all local folders");

        ItemRenderer<String> attributeValuesItemRenderer = new ItemRenderer<String>() {
            @Override
            public String getItemDisplayText(String item) {
                return (item == null) ? " " : item;
            }
        };
        this.attributeValuesEditableComboBox = new CustomComboBox(attributeValuesItemRenderer, componentDimension.getTextFieldPreferredHeight(), true, componentDimension.getTextFieldBackgroundColor());
        this.attributeValuesEditableComboBox.addItem(null);
    }

    @Override
    public String getRepositoryName() {
        return "All Local Folders";
    }

    @Override
    protected void addInputParameterComponentsToPanel() {
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

        this.attributesComboBox.setSelectedItem(null); // reset the selected attribute name when refreshing the parent panel
        this.attributeValuesEditableComboBox.setSelectedItem(null); // reset the selected attribute value when refreshing the parent panel

        if (attributesParameter != null) {
            int nextRowIndex = startRowIndex + this.parameterComponents.size() + 1;
            AttributesParameterComponent attributesParameterComponent = new AttributesParameterComponent(this.attributesComboBox, this.attributeValuesEditableComboBox,
                                                                                                attributesParameter.getName(), attributesParameter.getLabel(),
                                                                                                attributesParameter.isRequired(), this.componentDimension);
            this.parameterComponents.add(attributesParameterComponent);

            int difference = this.componentDimension.getTextFieldPreferredHeight() - attributesParameterComponent.getLabel().getPreferredSize().height;

            c = SwingUtils.buildConstraints(0, nextRowIndex, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows + (difference / 2), 0);
            panel.add(attributesParameterComponent.getLabel(), c);
            c = SwingUtils.buildConstraints(1, nextRowIndex, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
            panel.add(attributesParameterComponent.getComponent(), c);
        }

        add(panel, BorderLayout.NORTH);

        if (areaOfInterestParameter != null) {
            addAreaParameterComponent(areaOfInterestParameter);
        }
    }

    @Override
    public AbstractProgressTimerRunnable<List<RepositoryProduct>> buildSearchProductListThread(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                                               RemoteRepositoriesSemaphore remoteRepositoriesSemaphore, RepositoryOutputProductListPanel repositoryProductListPanel) {

        Map<String, Object> parameterValues = getParameterValues();
        if (parameterValues != null) {
            LocalRepositoryFolder localRepositoryFolder = (LocalRepositoryFolder)this.foldersComboBox.getSelectedItem();
            String selectedMissionName = (String) this.remoteMissionsComboBox.getSelectedItem(); // the selected mission may be null
            this.localInputParameterValues = new LocalInputParameterValues(parameterValues, selectedMissionName, localRepositoryFolder);
            return new LoadProductListTimerRunnable(progressPanel, threadId, threadListener, localRepositoryFolder, selectedMissionName,
                                                    parameterValues, repositoryProductListPanel, this.allLocalFolderProductsRepository);
        }
        return null;
    }

    @Override
    public JPopupMenu buildProductListPopupMenu(RepositoryProduct[] selectedProducts, OutputProductListModel productListModel) {
        JMenuItem selectAllMenuItem = new JMenuItem("Select All");
        selectAllMenuItem.addActionListener(this.localProductsPopupListeners.getSelectAllListener());
        JMenuItem selectNoneMenuItem = new JMenuItem("Select None");
        selectNoneMenuItem.addActionListener(this.localProductsPopupListeners.getSelectNoneListener());
        JMenuItem copyMenuItem = new JMenuItem("Copy");
        copyMenuItem.addActionListener(this.localProductsPopupListeners.getCopyListener());
        JMenuItem copyToMenuItem = new JMenuItem("Copy To...");
        copyToMenuItem.addActionListener(this.localProductsPopupListeners.getCopyToListener());
        JMenuItem moveToMenuItem = new JMenuItem("Move To...");
        moveToMenuItem.addActionListener(this.localProductsPopupListeners.getMoveToListener());
        JMenuItem exportListMenuItem = new JMenuItem("Export List...");
        exportListMenuItem.addActionListener(this.localProductsPopupListeners.getExportListListener());
        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(this.localProductsPopupListeners.getOpenListener());
        JMenuItem deleteMenuItem = new JMenuItem("Delete");
        deleteMenuItem.addActionListener(this.localProductsPopupListeners.getDeleteListener());
        JMenuItem batchProcessingMenuItem = new JMenuItem("Batch Processing...");
        batchProcessingMenuItem.addActionListener(this.localProductsPopupListeners.getBatchProcessingListener());
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(selectAllMenuItem);
        popupMenu.add(selectNoneMenuItem);
        popupMenu.add(copyMenuItem);
        popupMenu.add(copyToMenuItem);
        popupMenu.add(moveToMenuItem);
        popupMenu.add(exportListMenuItem);
        popupMenu.add(openMenuItem);
        popupMenu.add(deleteMenuItem);
        popupMenu.add(batchProcessingMenuItem);
        if (selectedProducts.length == 1) {
            JMenuItem showInExplorerMenuItem = new JMenuItem("Show in Explorer...");
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
    public void resetInputParameterValues() {
        this.localInputParameterValues = null;
    }

    @Override
    public void clearInputParameterComponentValues() {
        this.foldersComboBox.setSelectedItem(null);
        this.remoteMissionsComboBox.setSelectedItem(null);

        super.clearInputParameterComponentValues();
    }

    @Override
    public boolean refreshInputParameterComponentValues() {
        if (this.localInputParameterValues != null) {
            this.foldersComboBox.setSelectedItem(this.localInputParameterValues.getLocalRepositoryFolder());
            this.remoteMissionsComboBox.setSelectedItem(this.localInputParameterValues.getMissionName());
            for (int i=0; i<this.parameterComponents.size(); i++) {
                AbstractParameterComponent<?> inputParameterComponent = this.parameterComponents.get(i);
                Object parameterValue = this.localInputParameterValues.getParameterValue(inputParameterComponent.getParameterName());
                inputParameterComponent.setParameterValue(parameterValue);
            }
            return true;
        }
        return false;
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

        if (attributeNamesPerMission != null && attributeNamesPerMission.size() > 0) {
            Comparator<String> comparator = buildAttributeNamesComparator();
            SortedSet<String> uniqueAttributes = new TreeSet<>(comparator);
            for (Map.Entry<Short, Set<String>> entry : attributeNamesPerMission.entrySet()) {
                uniqueAttributes.addAll(entry.getValue());
            }
            setAttributes(uniqueAttributes);
            this.attributesComboBox.setSelectedItem(null);
        } else {
            this.attributesComboBox.removeAllItems();
        }
    }

    private void setAttributes(SortedSet<String> uniqueAttributes) {
        this.attributesComboBox.removeAllItems();
        // add an empty attribute on the first position
        this.attributesComboBox.addItem(null);
        for (String attributeName : uniqueAttributes) {
            this.attributesComboBox.addItem(attributeName);
        }
    }

    public void addAttributesIfMissing(Set<String> productAttributeNames) {
        Comparator<String> comparator = buildAttributeNamesComparator();
        SortedSet<String> uniqueAttributes = new TreeSet<>(comparator);
        ComboBoxModel<String> attributesModel = this.attributesComboBox.getModel();
        for (int i = 0; i < attributesModel.getSize(); i++) {
            String existingAtributeName = attributesModel.getElementAt(i);
            if (!StringUtils.isBlank(existingAtributeName)) {
                uniqueAttributes.add(existingAtributeName);
            }
        }
        boolean newAttribute = false;
        for (String attributeName : productAttributeNames) {
            if (uniqueAttributes.add(attributeName)) {
                newAttribute = true;
            }
        }
        if (newAttribute) {
            int oldSize = attributesModel.getSize();
            setAttributes(uniqueAttributes);
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
