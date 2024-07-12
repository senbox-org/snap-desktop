package eu.esa.snap.rcp.bandgroup;

import eu.esa.snap.core.datamodel.group.BandGroupImpl;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.runtime.Engine;
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
import java.util.logging.Logger;

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
    private JTextField groupNameTextField;
    private JTextArea bandNamesTextField;

    public BandGroupManagerTopComponent() throws IOException {
        controller = new BandGroupsManagerController();
        initUi();
        updateUIState();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(4, 4, 4, 4));
        setDisplayName("Band Groups Manager");

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        final JPanel groupEditPanel = createBandGroupEditPanel();
        mainPanel.add(groupEditPanel);

        final JPanel groupListPanel = createGroupListPanel();
        mainPanel.add(groupListPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createBandGroupEditPanel() {
        final JPanel groupEditPanel = new JPanel(new BorderLayout());
        groupEditPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Edit Band Grouping"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        final JPanel nameEditPanel = new JPanel();
        nameEditPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        nameEditPanel.setLayout(new BoxLayout(nameEditPanel, BoxLayout.Y_AXIS));
        groupNameTextField = new JTextField(1);
        final JLabel textFieldLabel = new JLabel("Band Group Name: ");
        textFieldLabel.setLabelFor(groupNameTextField);
        nameEditPanel.add(textFieldLabel);
        nameEditPanel.add(groupNameTextField);

        groupEditPanel.add(nameEditPanel, BorderLayout.NORTH);

        final JPanel bandNamesPanel = new JPanel();
        bandNamesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Band Names"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        bandNamesPanel.setLayout(new BoxLayout(bandNamesPanel, BoxLayout.Y_AXIS));
        bandNamesTextField = new JTextArea();
        bandNamesPanel.add(bandNamesTextField);

        groupEditPanel.add(bandNamesPanel);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            editOk();
        });
        buttonPanel.add(okButton);

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            editCancel();
        });
        buttonPanel.add(cancelButton);

        groupEditPanel.add(buttonPanel, BorderLayout.SOUTH);

        return groupEditPanel;
    }

    private JPanel createGroupListPanel() {
        final JPanel groupListPanel = new JPanel(new BorderLayout());
        groupListPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Manage Band Groupings"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        groupNamesCBox = new JComboBox<>();
        groupNamesCBox.setName("Band Groups");
        groupNamesCBox.addActionListener(e -> {
            updateBandGroupSelection();
        });
        groupListPanel.add(groupNamesCBox, BorderLayout.PAGE_START);

        final JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            addNewBandGroup();
        });
        groupListPanel.add(addButton, BorderLayout.WEST);

        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> {
            removeBandGroup();
        });
        groupListPanel.add(removeButton, BorderLayout.EAST);

        final JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            saveBandGroups();
        });
        groupListPanel.add(saveButton, BorderLayout.PAGE_END);

        return groupListPanel;
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
        final String selectedGroup = (String) groupNamesCBox.getSelectedItem();
        if (StringUtils.isNullOrEmpty(selectedGroup)) {
            clearEditPanel();
        } else {
            // @todo tb/tb
            // foresee that groups from product do not have a name
            // check if group is editable - if not -> set edit components to read-only (eventally add message text)
            // foresee that the return value might be null
            final BandGroupImpl group = controller.getGroup(selectedGroup);
            groupNameTextField.setText(group.getName());
        }
    }

    protected void saveBandGroups() {
        try {
            controller.saveBandGroups();
        } catch (IOException e) {
            final Logger logger = Engine.getInstance().getLogger();
            logger.severe(e.getMessage());
            Dialogs.showError("Save Band Groups", e.getMessage());
        }
    }

    protected void addNewBandGroup() {
        // @todo tb/tb
        // clear fields of edit panel
        clearEditPanel();
        updateUIState();
    }

    private void clearEditPanel() {
        groupNameTextField.setText("");
        bandNamesTextField.setText("");
    }

    protected void removeBandGroup() {
        // @todo tb/tb
        // getSelected band group
        // check if editable
        // - if so: remove
        // - if not: show dialog
    }

    protected void editOk() {
        final String newBandGroupName = groupNameTextField.getText();
        final String bandNamesText = bandNamesTextField.getText();

        // validate content
        if (StringUtils.isNullOrEmpty(newBandGroupName)) {
            Dialogs.showError("Update Band Group", "Band group name must not be empty.");
            return;
        }
        if (StringUtils.isNullOrEmpty(bandNamesText)) {
            Dialogs.showError("Update Band Group", "Band names must not be empty.");
            return;
        }

        // remove old groups - if exists
        if (controller.groupExists(newBandGroupName)) {
            Dialogs.Answer bandGroupExists = Dialogs.requestDecision("Band Group exists", "A band group with the name '" + newBandGroupName + "' already exists. Do you want to replace it?", true, null);
            if (bandGroupExists == Dialogs.Answer.NO) {
                return;
            }
        }

        final String[] bandNames = parseTextFieldContent(bandNamesText);
        controller.storeGroup(newBandGroupName, bandNames);
        updateUIState();
    }

    protected void editCancel() {
        // @todo
    }

    static String[] parseTextFieldContent(String bandNamesText) {
        return StringUtils.split(bandNamesText, new char[]{'\n', ','}, true);
    }
}
