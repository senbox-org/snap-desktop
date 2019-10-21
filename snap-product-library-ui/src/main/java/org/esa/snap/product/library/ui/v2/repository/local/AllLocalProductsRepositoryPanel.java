package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.ParametersPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.product.library.v2.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.RemoteMission;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.QueryFilter;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final JComboBox<LocalRepositoryFolder> foldersComboBox;
    private final JComboBox<RemoteMission> missionsComboBox;
    private final JComboBox<String> attributesComboBox;
    private final JButton scanFoldersButton;
    private final JButton addFolderButton;
    private final JButton removeFoldersButton;

    private LocalProductsPopupListeners localProductsPopupListeners;

    public AllLocalProductsRepositoryPanel(ComponentDimension componentDimension, WorldWindowPanelWrapper worlWindPanel) {
        super(worlWindPanel, componentDimension, new BorderLayout(0, componentDimension.getGapBetweenRows()));

        this.allLocalFolderProductsRepository = new AllLocalFolderProductsRepository();

        Dimension buttonSize = new Dimension(componentDimension.getTextFieldPreferredHeight(), componentDimension.getTextFieldPreferredHeight());

        this.foldersComboBox = RemoteProductsRepositoryPanel.buildComboBox(componentDimension);
        LabelListCellRenderer<LocalRepositoryFolder> foldersRenderer = new LabelListCellRenderer<LocalRepositoryFolder>(this.foldersComboBox.getPreferredSize().height) {
            @Override
            protected String getItemDisplayText(LocalRepositoryFolder value) {
                return (value == null) ? " " : value.getPath().toString();
            }
        };
        this.foldersComboBox.setRenderer(foldersRenderer);

        this.missionsComboBox = RemoteProductsRepositoryPanel.buildComboBox(componentDimension);
        LabelListCellRenderer<RemoteMission> missionsRenderer = new LabelListCellRenderer<RemoteMission>(this.missionsComboBox.getPreferredSize().height) {
            @Override
            protected String getItemDisplayText(RemoteMission value) {
                return (value == null) ? " " : value.getName();
            }
        };
        this.missionsComboBox.setRenderer(missionsRenderer);

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
        panel.add(this.missionsComboBox, c);

        Class<?> areaOfInterestClass = Rectangle2D.class;
        Class<?> attributesClass = Attribute.class;
        Class<?>[] classesToIgnore = new Class<?>[]{areaOfInterestClass, attributesClass};
        List<QueryFilter> parameters = this.allLocalFolderProductsRepository.getParameters();
        int startRowIndex = 2;
        this.parameterComponents = panel.addParameterComponents(parameters, startRowIndex, gapBetweenRows, this.componentDimension, classesToIgnore);

        QueryFilter areaOfInterestParameter = null;
        QueryFilter attributesParameter = null;
        for (int i = 0; i < parameters.size(); i++) {
            QueryFilter param = parameters.get(i);
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
            RemoteMission selectedMission = (RemoteMission) this.missionsComboBox.getSelectedItem();
            return new LoadProductListTimerRunnable(progressPanel, threadId, threadListener, localRepositoryFolder, selectedMission, parameterValues, repositoryProductListPanel);
        }
        return null;
    }

    @Override
    public JPopupMenu buildProductListPopupMenu(RepositoryProduct[] selectedProducts) {
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
    public RepositoryProductPanel buildProductProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
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
        this.missionsComboBox.setSelectedItem(null);

        super.clearParameterValues();
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

    public void addMissionIfMissing(RemoteMission mission) {
        ComboBoxModel<RemoteMission> missionsModel = this.missionsComboBox.getModel();
        boolean foundMission = false;
        for (int i = 0; i < missionsModel.getSize() && !foundMission; i++) {
            RemoteMission existingMission = missionsModel.getElementAt(i);
            if (existingMission != null && existingMission.getId() == mission.getId()) {
                foundMission = true;
            }
        }
        if (!foundMission) {
            if (missionsModel.getSize() == 0) {
                this.missionsComboBox.addItem(null);
            }
            this.missionsComboBox.addItem(mission);
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

    public void setLocalParameterValues(List<LocalRepositoryFolder> localRepositoryFolders, List<RemoteMission> missions, Map<Short, Set<String>> attributeNamesPerMission) {
        this.foldersComboBox.removeAllItems();
        if (localRepositoryFolders != null && localRepositoryFolders.size() > 0) {
            this.foldersComboBox.addItem(null);
            for (int i = 0; i < localRepositoryFolders.size(); i++) {
                this.foldersComboBox.addItem(localRepositoryFolders.get(i));
            }
            this.foldersComboBox.setSelectedItem(null);
        }

        this.missionsComboBox.removeAllItems();
        if (missions != null && missions.size() > 0) {
            this.missionsComboBox.addItem(null);
            for (int i = 0; i < missions.size(); i++) {
                this.missionsComboBox.addItem(missions.get(i));
            }
            this.missionsComboBox.setSelectedItem(null);
        }

        this.attributesComboBox.removeAllItems();
        if (attributeNamesPerMission != null && attributeNamesPerMission.size() > 0) {
            Comparator<String> comparator = buildAttributeNamesComparator();
            SortedSet<String> uniqueAttributes = new TreeSet<>(comparator);
            for (Map.Entry<Short, Set<String>> entry : attributeNamesPerMission.entrySet()) {
                uniqueAttributes.addAll(entry.getValue());
            }
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
