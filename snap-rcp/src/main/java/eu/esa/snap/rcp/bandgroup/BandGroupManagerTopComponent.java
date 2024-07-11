package eu.esa.snap.rcp.bandgroup;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

@TopComponent.Description(
        preferredID = "BandGroupManagerTopComponent",
        iconBase = "org/esa/snap/rcp/icons/BandGroupManager.gif",
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED
)
@TopComponent.Registration(
        mode = "output",
        openAtStartup = false,
        position = 11
)
@ActionID(category = "Window", id = "eu.esa.snap.rcp.bandgroup.BandGroupManagerTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows", position = 31),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_BandGroupManagerTopComponent_Name",
        preferredID = "BandGroupManagerTopComponent"
)
@NbBundle.Messages({
        "CTL_BandGroupManagerTopComponent_Name=Band Groups Manager",
        "CTL_BandGroupManagerTopComponent_HelpId=showBandGroupManagerWnd"
})
public class BandGroupManagerTopComponent extends ToolTopComponent {

    private final BandGroupsManagerController controller;
    private JComboBox<String> groupNamesCBox;

    public BandGroupManagerTopComponent() throws IOException {
        controller = new BandGroupsManagerController();
        initUi();
        updateUIState();
    }

    private void initUi() {
        // add / remove / edit
        // save
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(4, 4, 4, 4));
        setDisplayName("Band Groups Manager");

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        JPanel productPanel = new JPanel(new BorderLayout());
        productPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Selected Product Bands"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        mainPanel.add(productPanel);

        JPanel groupEditPanel = new JPanel(new BorderLayout());
        groupEditPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Edit Band Grouping"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        mainPanel.add(groupEditPanel);

        JPanel groupListPanel = new JPanel(new BorderLayout());
        groupListPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Manage Band Groupings"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        groupNamesCBox = new JComboBox<>();
        groupNamesCBox.setName("Band Groups");
        groupNamesCBox.addActionListener(e -> {
            updateBandGroupSelection();
        });

        groupListPanel.add(groupNamesCBox, BorderLayout.PAGE_START);
        mainPanel.add(groupListPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void updateUIState() {
        final String[] bandGroupNames = controller.getBandGroupNames();
        groupNamesCBox.removeAllItems();
        for (final String groupName : bandGroupNames) {
            groupNamesCBox.addItem(groupName);
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_BandGroupManagerTopComponent_HelpId());
    }

    @Override
    protected void productSceneViewSelected(ProductSceneView view) {
        final Product product = view.getProduct();
        controller.setSelectedProduct(product);
        updateUIState();
    }

    @Override
    protected void productSceneViewDeselected(ProductSceneView view) {
        controller.deselectProduct();
        updateUIState();
    }

    @Override
    protected void productSelected(Product product) {
        controller.setSelectedProduct(product);
        updateUIState();
    }

    @Override
    protected void productDeselected(Product product) {
        controller.deselectProduct();
        updateUIState();
    }

    protected void updateBandGroupSelection() {
        String selectedGroup = (String) groupNamesCBox.getSelectedItem();
        if (StringUtils.isNullOrEmpty(selectedGroup)) {
            // clear band group editor
        } else {
            // get BandGroup from manager
            // set to editor
            // trigger repaint (?)
        }
    }
}
