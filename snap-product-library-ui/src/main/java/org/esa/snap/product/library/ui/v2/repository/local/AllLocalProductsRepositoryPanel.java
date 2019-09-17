package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RemoteRepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.product.library.v2.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.RemoteMission;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class AllLocalProductsRepositoryPanel extends AbstractProductsRepositoryPanel {

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final JComboBox<RemoteMission> missionsComboBox;

    private boolean initialized;

    public AllLocalProductsRepositoryPanel(ComponentDimension componentDimension, WorldWindowPanelWrapper worlWindPanel) {
        super(worlWindPanel, componentDimension, new GridBagLayout());

        this.allLocalFolderProductsRepository = new AllLocalFolderProductsRepository();
        this.initialized = false;

        this.missionsComboBox = RemoteProductsRepositoryPanel.buildComboBox(componentDimension);
        LabelListCellRenderer<RemoteMission> renderer = new LabelListCellRenderer<RemoteMission>(componentDimension.getListItemMargins()) {
            @Override
            protected String getItemDisplayText(RemoteMission value) {
                return (value == null) ? " " : value.getName();
            }
        };
        this.missionsComboBox.setRenderer(renderer);
    }

    @Override
    public void addNotify() {
        super.addNotify();

        if (!this.initialized) {
            this.initialized = true;
            LoadLocalParametersRunnable thread = new LoadLocalParametersRunnable() {
                @Override
                protected void onSuccessfullyExecuting(List<RemoteMission> missions) {
                    setInputData(missions);
                }
            };
            thread.executeAsync();
        }
    }

    @Override
    public String getName() {
        return "All Local Folders";
    }

    @Override
    protected void addParameterComponents() {
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        add(new JLabel("Mission"), c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        add(this.missionsComboBox, c);

        addParameterComponents(this.allLocalFolderProductsRepository.getParameters(), 1, gapBetweenRows);
    }

    @Override
    public AbstractProgressTimerRunnable<List<RepositoryProduct>> buildThreadToSearchProducts(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener,
                                                                                              RemoteRepositoryProductListPanel repositoryProductListPanel) {

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
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(openMenuItem);
        return popupMenu;
    }

    public void addMissionIfMissing(RemoteMission mission) {
        ComboBoxModel<RemoteMission> model = this.missionsComboBox.getModel();
        boolean found = false;
        for (int i=0; i<model.getSize() && !found; i++) {
            RemoteMission existingMision = model.getElementAt(i);
            if (existingMision != null && existingMision.getId() == mission.getId()) {
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

    private void setInputData(List<RemoteMission> missions) {
        if (missions.size() > 0) {
            this.missionsComboBox.addItem(null);
            for (int i = 0; i < missions.size(); i++) {
                this.missionsComboBox.addItem(missions.get(i));
            }
        }
    }
}
