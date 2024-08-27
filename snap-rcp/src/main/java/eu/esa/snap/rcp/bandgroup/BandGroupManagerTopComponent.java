package eu.esa.snap.rcp.bandgroup;

import eu.esa.snap.core.datamodel.group.BandGroupImpl;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.actions.help.HelpAction;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.runtime.Engine;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.tool.ToolButtonFactory;
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
        iconBase = "org/esa/snap/rcp/icons/BandGroupsManager.gif",
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

    private static final String HELP_ID = "bandGroupsEditor";

    private final BandGroupsManagerController controller;

    private JComboBox<String> groupNamesCBox;
    private JTextField groupNameTextField;
    private JTextArea bandNamesTextField;
    private JButton editOkButton;

    public BandGroupManagerTopComponent() throws IOException {
        controller = new BandGroupsManagerController();
        initUi();
        updateGroupNamesBox(null);
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
        editOkButton = new JButton("OK");
        editOkButton.addActionListener(e -> {
            editOk();
        });
        buttonPanel.add(editOkButton);

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            editCancel();
        });
        buttonPanel.add(cancelButton);

        groupEditPanel.add(buttonPanel, BorderLayout.SOUTH);

        return groupEditPanel;
    }

    private JPanel createGroupListPanel() {
        JPanel groupListPanel = new JPanel(new BorderLayout());

        final JPanel containerPanel = new JPanel();
        containerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Manage Band Groupings"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.PAGE_AXIS));

        groupNamesCBox = new JComboBox<>();
        groupNamesCBox.setName("Band Groups");
        groupNamesCBox.addActionListener(e -> {
            updateBandGroupSelection();
        });
        groupNamesCBox.setMaximumSize(new Dimension(300, 25));

        containerPanel.add(groupNamesCBox);

        final JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

        final JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            addNewBandGroup();
        });
        buttonsPanel.add(addButton);

        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> {
            removeBandGroup();
        });
        buttonsPanel.add(removeButton);

        final JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            saveBandGroups();
        });
        buttonsPanel.add(saveButton);

        containerPanel.add(buttonsPanel);

        final AbstractButton helpButton = ToolButtonFactory.createButton(new HelpAction(this), false);
        helpButton.setName("helpButton");
        helpButton.setToolTipText("Help.");
        containerPanel.add(helpButton);

        groupListPanel.add(containerPanel, BorderLayout.PAGE_START);

        return containerPanel;
    }

    private void updateGroupNamesBox(String selectedName) {
        final String[] bandGroupNames = controller.getBandGroupNames();
        groupNamesCBox.removeAllItems();
        int idx = 0;
        int selectedIdx = -1;
        for (final String groupName : bandGroupNames) {
            groupNamesCBox.addItem(groupName);
            if (groupName.equals(selectedName)) {
                selectedIdx = idx;
            }
            ++idx;
        }

        if (selectedIdx >= 0) {
            groupNamesCBox.setSelectedIndex(selectedIdx);
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    @Override
    protected void productSceneViewSelected(ProductSceneView view) {
        final Product product = view.getProduct();
        controller.setSelectedProduct(product);
        updateGroupNamesBox(null);
    }

    @Override
    protected void productSceneViewDeselected(ProductSceneView view) {
        controller.deselectProduct();
        updateGroupNamesBox(null);
    }

    @Override
    protected void productSelected(Product product) {
        controller.setSelectedProduct(product);
        updateGroupNamesBox(null);
    }

    @Override
    protected void productDeselected(Product product) {
        controller.deselectProduct();
        updateGroupNamesBox(null);
    }

    protected void updateBandGroupSelection() {
        final String selectedGroup = (String) groupNamesCBox.getSelectedItem();
        if (StringUtils.isNullOrEmpty(selectedGroup)) {
            clearEditPanel();
        } else {
            final BandGroupImpl group = controller.getGroup(selectedGroup);
            if (group == null) {
                clearEditPanel();
                return;
            }

            final boolean editable = group.isEditable();
            setBandGroupEditable(editable);

            groupNameTextField.setText(group.getName());

            final StringBuilder textFieldEntry = new StringBuilder();
            for (String[] items : group) {
                for (final String item : items) {
                    textFieldEntry.append(item);
                    textFieldEntry.append(System.lineSeparator());
                }
            }
            bandNamesTextField.setText(textFieldEntry.toString());
        }
    }

    private void setBandGroupEditable(boolean editable) {
        groupNameTextField.setEditable(editable);
        bandNamesTextField.setEditable(editable);
        editOkButton.setEnabled(editable);
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
        clearEditPanel();
        setBandGroupEditable(true);
    }

    private void clearEditPanel() {
        groupNameTextField.setText("");
        bandNamesTextField.setText("");
    }

    protected void removeBandGroup() {
        final String groupToRemove = (String) groupNamesCBox.getSelectedItem();

        if (groupToRemove.startsWith("<unnamed>")) {
            Dialogs.showWarning("This band group is supplied by the product.\nIt cannot be edited.");
        } else {
            controller.removeGroup(groupToRemove);

            updateGroupNamesBox(null);
        }
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
        updateGroupNamesBox(newBandGroupName);
    }

    protected void editCancel() {
        clearEditPanel();
        updateGroupNamesBox(null);
    }

    static String[] parseTextFieldContent(String bandNamesText) {
        return StringUtils.split(bandNamesText.trim(), new char[]{'\n', ','}, true);
    }
}
