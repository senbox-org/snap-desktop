package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.ParametersPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.product.library.v2.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.RemoteMission;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.ComboBoxModel;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final JComboBox<RemoteMission> missionsComboBox;
    private final JComboBox<String> attributesComboBox;
    private final JButton scanFoldersButton;

    private ActionListener openProductListener;
    private ActionListener deleteProductListener;

    public AllLocalProductsRepositoryPanel(ComponentDimension componentDimension, WorldWindowPanelWrapper worlWindPanel) {
        super(worlWindPanel, componentDimension, new BorderLayout(0, componentDimension.getGapBetweenRows()));

        this.allLocalFolderProductsRepository = new AllLocalFolderProductsRepository();

        Dimension buttonSize = new Dimension(componentDimension.getTextFieldPreferredHeight(), componentDimension.getTextFieldPreferredHeight());

        this.missionsComboBox = RemoteProductsRepositoryPanel.buildComboBox(componentDimension);
        int cellItemHeight = this.missionsComboBox.getPreferredSize().height;
        LabelListCellRenderer<RemoteMission> renderer = new LabelListCellRenderer<RemoteMission>(cellItemHeight) {
            @Override
            protected String getItemDisplayText(RemoteMission value) {
                return (value == null) ? " " : value.getName();
            }
        };
        this.missionsComboBox.setRenderer(renderer);

        this.attributesComboBox = RemoteProductsRepositoryPanel.buildComboBox(null, null, componentDimension);

        this.scanFoldersButton = RepositorySelectionPanel.buildButton("/org/esa/snap/productlibrary/icons/refresh24.png", null, buttonSize, 1);
        this.scanFoldersButton.setToolTipText("Scan folders");
    }

    @Override
    public JButton getTopBarButton() {
        return this.scanFoldersButton;
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
        panel.add(new JLabel("Mission"), c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        panel.add(this.missionsComboBox, c);

        Class<?> areaOfInterestClass = Rectangle2D.class;
        Class<?> attributesClass = Attribute.class;
        Class<?>[] classesToIgnore = new Class<?>[] {areaOfInterestClass, attributesClass};
        List<QueryFilter> parameters = this.allLocalFolderProductsRepository.getParameters();
        int startRowIndex = 1;
        this.parameterComponents = panel.addParameterComponents(parameters, startRowIndex, gapBetweenRows, this.componentDimension, classesToIgnore);

        QueryFilter areaOfInterestParameter = null;
        QueryFilter attributesParameter = null;
        for (int i=0; i<parameters.size(); i++) {
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

            c = SwingUtils.buildConstraints(0, nextRowIndex, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows + (difference/2) , 0);
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
                                                                                              RepositoryProductListPanel repositoryProductListPanel) {

        Map<String, Object> parameterValues = getParameterValues();
        if (parameterValues != null) {
            RemoteMission selectedMission = (RemoteMission)this.missionsComboBox.getSelectedItem();
            return new LoadProductListTimerRunnable(progressPanel, threadId, threadListener, selectedMission, parameterValues, repositoryProductListPanel);
        }
        return null;
    }

    @Override
    public JPopupMenu buildProductListPopupMenu() {
        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(this.openProductListener);
        JMenuItem deleteMenuItem = new JMenuItem("Delete");
        deleteMenuItem.addActionListener(this.deleteProductListener);
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(openMenuItem);
        popupMenu.add(deleteMenuItem);
        return popupMenu;
    }

    public void addMissionIfMissing(RemoteMission mission) {
        ComboBoxModel<RemoteMission> model = this.missionsComboBox.getModel();
        boolean found = false;
        for (int i=0; i<model.getSize() && !found; i++) {
            RemoteMission existingMission = model.getElementAt(i);
            if (existingMission != null && existingMission.getId() == mission.getId()) {
                found = true;
            }
        }
        if (!found) {
            if (model.getSize() == 0) {
                this.missionsComboBox.addItem(null);
            }
            this.missionsComboBox.addItem(mission);
        }
    }

    public void setOpenAndDeleteProductListeners(ActionListener openProductListener, ActionListener deleteProductListener) {
        this.openProductListener = openProductListener;
        this.deleteProductListener = deleteProductListener;
    }

    public void setLocalParameterValues(LocalParameterValues parameterValues) {
        List<RemoteMission> missions = parameterValues.getMissions();
        if (missions.size() > 0) {
            this.missionsComboBox.addItem(null);
            for (int i = 0; i < missions.size(); i++) {
                this.missionsComboBox.addItem(missions.get(i));
            }
        }
        Map<Short, Set<String>> attributes = parameterValues.getAttributes();
        if (attributes.size() > 0) {
            Set<String> uniqueAttributes = new HashSet<>();
            for (Map.Entry<Short, Set<String>> entry : attributes.entrySet()) {
                uniqueAttributes.addAll(entry.getValue());
            }
            for (String attributeName : uniqueAttributes) {
                this.attributesComboBox.addItem(attributeName);
            }
            this.attributesComboBox.setSelectedItem(null);
        }
    }
}
