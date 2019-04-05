package org.esa.snap.ui.vfs.preferences;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepositoriesController;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller for VFS Remote File Repositories.
 * Used for provide a UI to the strategy with storing VFS connection data.
 *
 * @author Adrian Draghici
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_VFSOptions",
        keywords = "#Options_Keywords_VFSOptions",
        keywordsCategory = "Remote File Repositories",
        id = "VFS",
        position = 11)
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_VFSOptions=Remote File Repositories",
        "Options_Keywords_VFSOptions=repositories, remote, file"
})
public class VFSOptionsController extends DefaultConfigController {

    /**
     * The column index for remote file repository name in remote file repositories table.
     */
    private static final int REPO_NAME_COLUMN = 0;

    /**
     * The column index for remote file repository property name in remote file repository properties table.
     */
    private static final int REPO_PROP_NAME_COLUMN = 0;

    /**
     * The column index for remote file repository property value in remote file repository properties table.
     */
    private static final int REPO_PROP_VALUE_COLUMN = 1;

    private static final String LOAD_ERROR_MESSAGE = "Unable to load VFS Remote File Repositories Properties from SNAP configuration file.";
    private static final String SAVE_ERROR_MESSAGE = "Unable to save VFS Remote File Repositories Properties to SNAP configuration file.";

    private static Logger logger = Logger.getLogger(VFSOptionsController.class.getName());
    private static ImageIcon addButtonIcon;
    private static ImageIcon removeButtonIcon;

    static {
        try {
            addButtonIcon = loadImageIcon("icons/list-add.png");
            removeButtonIcon = loadImageIcon("icons/list-remove.png");
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Unable to load image resource. Details: " + ex.getMessage());
        }
    }

    private final JTextField remoteRepositoryNameField = new JTextField(30);
    private final JTextField remoteRepositorySchemaField = new JTextField(30);
    private final JTextField remoteRepositoryAddressField = new JTextField(30);
    private VFSRemoteFileRepositoriesController vfsRemoteFileRepositoriesController;
    private JPanel remoteRepositoriesConfigsPanel;
    private String currentRemoteRepositoryName = "";
    private String currentRemoteRepositorySchema = "";
    private String[] remoteRepositoriesIdsList;
    private String[] remoteRepositoriesPropertiesIdsList;
    private final JTable remoteRepositoriesPropertiesListTable = getRemoteRepositoriesPropertiesListTable();
    private final JTable remoteRepositoriesListTable = getRemoteRepositoriesListTable();
    private VFSOptionsBean vfsOptionsBean = new VFSOptionsBean();
    private boolean isInitialized = false;

    private static ImageIcon loadImageIcon(String imagePath) {
        URL imageURL = VFSOptionsController.class.getResource(imagePath);
        return (imageURL == null) ? null : new ImageIcon(imageURL);
    }

    /**
     * Create a {@link PropertySet} object instance that holds all parameters.
     * Clients that want to maintain properties need to overwrite this method.
     *
     * @return An instance of {@link PropertySet}, holding all configuration parameters.
     * @see #createPropertySet(Object)
     */
    @Override
    protected PropertySet createPropertySet() {
        return createPropertySet(vfsOptionsBean);
    }

    /**
     * Create a panel that allows the user to set the parameters in the given {@link BindingContext}. Clients that want to create their own panel representation on the given properties need to overwrite this method.
     *
     * @param context The {@link BindingContext} for the panel.
     * @return A JPanel instance for the given {@link BindingContext}, never {@code null}.
     */
    @Override
    protected JPanel createPanel(BindingContext context) {
        Path configFile = VFSRemoteFileRepositoriesController.getDefaultConfigFilePath();
        JPanel remoteFileRepositoriesTabUI = getRemoteFileRepositoriesTabUI();
        try {
            vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(configFile);
            loadRemoteRepositoriesOnTable();
            isInitialized = true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, LOAD_ERROR_MESSAGE + " Details: " + ex.getMessage());
            JOptionPane.showMessageDialog(remoteRepositoriesConfigsPanel, LOAD_ERROR_MESSAGE, "Error loading VFS Remote file repositories configurations", JOptionPane.ERROR_MESSAGE);
        }
        return remoteFileRepositoriesTabUI;
    }

    /**
     * Updates the UI.
     */
    @Override
    public void update() {
        if (isInitialized) {
            Path configFile = VFSRemoteFileRepositoriesController.getDefaultConfigFilePath();
            try {
                vfsRemoteFileRepositoriesController = new VFSRemoteFileRepositoriesController(configFile);
                loadRemoteRepositoriesOnTable();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, LOAD_ERROR_MESSAGE + " Details: " + ex.getMessage());
                JOptionPane.showMessageDialog(remoteRepositoriesConfigsPanel, LOAD_ERROR_MESSAGE, "Error loading VFS Remote file repositories configurations", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Saves the changes.
     */
    @Override
    public void applyChanges() {
        try {
            vfsRemoteFileRepositoriesController.saveProperties();
            new Thread(() -> JOptionPane.showMessageDialog(remoteRepositoriesConfigsPanel, "VFS Remote File Repositories Properties saved successfully. Please restart SNAP to take effect.", "Save VFS Remote file repositories configurations", JOptionPane.INFORMATION_MESSAGE)).start();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, SAVE_ERROR_MESSAGE + " Details: " + ex.getMessage(), ex);
            JOptionPane.showMessageDialog(remoteRepositoriesConfigsPanel, SAVE_ERROR_MESSAGE, "Error saving VFS Remote file repositories configurations", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Cancels the changes.
     */
    @Override
    public void cancel() {
        try {
            vfsRemoteFileRepositoriesController.loadProperties();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, LOAD_ERROR_MESSAGE + " Details: " + ex.getMessage());
            JOptionPane.showMessageDialog(remoteRepositoriesConfigsPanel, LOAD_ERROR_MESSAGE, "Error loading VFS Remote file repositories configurations", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Check whether options changes.
     *
     * @return {@code true} if options is changed
     */
    @Override
    public boolean isChanged() {
        return vfsRemoteFileRepositoriesController.isChanged();
    }

    /**
     * Gets the Help Context for this Options Controller
     *
     * @return The Help Context
     */
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("vfs_editor");
    }

    /**
     * Runs the event associated with button for adding remote file repository.
     */
    private void runAddRemoteRepositoryButtonActionEvent() {
        String newRepositoryName = (String) remoteRepositoriesListTable.getModel().getValueAt(0, REPO_NAME_COLUMN);
        try {
            String remoteRepositoryId = vfsRemoteFileRepositoriesController.registerNewRemoteRepository();
            vfsRemoteFileRepositoriesController.setRemoteRepositoryName(remoteRepositoryId, newRepositoryName);
            loadRemoteRepositoriesOnTable();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.FINE, "Unable to add remote file repository. Details: " + ex.getMessage());
            if (ex.getMessage().startsWith("Invalid")) {
                JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Invalid VFS repository name! Please check if it meets following requirements:\n- It must be unique\n- It must be alphanumeric.\n- Underscores are allowed.\n- Length is between 3 and 25 characters.", "Add new remote file repository", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Failed to add new remote repository.\nreason: " + ex, "Add new remote file repository", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Creates and gets the button for adding remote file repository.
     *
     * @return The button for adding remote file repository
     */
    private JButton getAddRemoteRepositoryButton() {
        JButton addRemoteRepositoryButton = new JButton(addButtonIcon);
        addRemoteRepositoryButton.setPreferredSize(new Dimension(20, 20));
        addRemoteRepositoryButton.addActionListener(e -> runAddRemoteRepositoryButtonActionEvent());
        return addRemoteRepositoryButton;
    }

    /**
     * Runs the event associated with button for removing remote file repository.
     */
    private void runRemoveRemoteRepositoryButtonActionEvent() {
        try {
            if (remoteRepositoriesListTable.getSelectedRow() > 0) {
                String repositoryName = (String) remoteRepositoriesListTable.getModel().getValueAt(remoteRepositoriesListTable.getSelectedRow(), REPO_NAME_COLUMN);
                if (JOptionPane.showConfirmDialog(remoteRepositoriesListTable, "Are you sure to delete the following repository?\n" + repositoryName, "Delete Repository Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                    vfsRemoteFileRepositoriesController.removeRemoteRepository(remoteRepositoryId);
                    loadRemoteRepositoriesOnTable();
                }
            } else {
                JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Please select a repository from list.", "Delete Repository", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            logger.log(Level.SEVERE, "Unable to delete remote file repository. Details: " + ex.getMessage());
            JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Failed to delete remote repository.\nreason: " + ex, "Delete remote file repository", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates and gets the button for removing remote file repository.
     *
     * @return The button for removing remote file repository
     */
    private JButton getRemoveRemoteRepositoryButton() {
        JButton removeRemoteRepositoryButton = new JButton(removeButtonIcon);
        removeRemoteRepositoryButton.setPreferredSize(new Dimension(20, 20));
        removeRemoteRepositoryButton.addActionListener(e -> runRemoveRemoteRepositoryButtonActionEvent());
        return removeRemoteRepositoryButton;
    }

    /**
     * Runs the event associated with selecting a row from remote file repositories table.
     */
    private void runRemoteRepositoriesListTableListSelectionEvent() {
        if (remoteRepositoriesListTable.getSelectedRow() > 0) {
            String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
            String remoteRepositoryName = vfsRemoteFileRepositoriesController.getRemoteRepositoryName(remoteRepositoryId).getValue();
            currentRemoteRepositoryName = remoteRepositoryName;
            String remoteRepositorySchema = vfsRemoteFileRepositoriesController.getRemoteRepositorySchema(remoteRepositoryId).getValue();
            currentRemoteRepositorySchema = remoteRepositorySchema;
            String remoteRepositoryAddress = vfsRemoteFileRepositoriesController.getRemoteRepositoryAddress(remoteRepositoryId).getValue();
            remoteRepositoryNameField.setText(remoteRepositoryName == null ? "" : remoteRepositoryName);
            remoteRepositorySchemaField.setText(remoteRepositorySchema == null ? "" : remoteRepositorySchema);
            remoteRepositoryAddressField.setText(remoteRepositoryAddress == null ? "" : remoteRepositoryAddress);
            loadRemoteRepositoryPropertiesOnTable(remoteRepositoryId);
            remoteRepositoriesConfigsPanel.setVisible(true);
        } else {
            remoteRepositoriesConfigsPanel.setVisible(false);
        }
    }

    /**
     * Creates and gets the remote file repositories table.
     *
     * @return The remote file repositories table
     */
    private JTable getRemoteRepositoriesListTable() {
        JTable newRemoteRepositoriesListTable = new JTable();
        DefaultTableModel remoteRepositoriesListTableModel = new DefaultTableModel(
                new Object[][]{
                        new Object[]{""},
                },
                new String[]{
                        "Name"
                }
        ) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return rowIndex == 0;
            }
        };
        newRemoteRepositoriesListTable.setModel(remoteRepositoriesListTableModel);
        newRemoteRepositoriesListTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        newRemoteRepositoriesListTable.getSelectionModel().addListSelectionListener(event -> runRemoteRepositoriesListTableListSelectionEvent());
        return newRemoteRepositoriesListTable;
    }

    /**
     * Creates and gets the panel with add and remove buttons for remote file repositories table.
     *
     * @return The panel with add and remove buttons
     */
    private JPanel getRemoteRepositoriesListActionsPanel() {
        JPanel remoteRepositoriesListActionsPanel = new JPanel();
        remoteRepositoriesListActionsPanel.setLayout(new BoxLayout(remoteRepositoriesListActionsPanel, BoxLayout.PAGE_AXIS));
        remoteRepositoriesListActionsPanel.add(getAddRemoteRepositoryButton());
        remoteRepositoriesListActionsPanel.add(getRemoveRemoteRepositoryButton());
        remoteRepositoriesListActionsPanel.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767)));
        return remoteRepositoriesListActionsPanel;
    }

    /**
     * Creates and gets the panel with add and remove buttons panel and remote file repositories table.
     *
     * @return The panel with add and remove buttons panel and remote file repositories table
     */
    private JPanel getRemoteRepositoriesListPanel() {
        JScrollPane remoteRepositoriesListSP = new JScrollPane();
        remoteRepositoriesListSP.setViewportView(remoteRepositoriesListTable);

        JPanel remoteRepositoriesListPanel = new JPanel();
        remoteRepositoriesListPanel.setBorder(BorderFactory.createTitledBorder("Remote File Repositories List"));
        remoteRepositoriesListPanel.setLayout(new BoxLayout(remoteRepositoriesListPanel, BoxLayout.LINE_AXIS));
        remoteRepositoriesListPanel.setAutoscrolls(false);
        remoteRepositoriesListPanel.add(getRemoteRepositoriesListActionsPanel());
        remoteRepositoriesListPanel.add(remoteRepositoriesListSP);
        remoteRepositoriesListPanel.setPreferredSize(new Dimension(200, 300));
        return remoteRepositoriesListPanel;
    }

    /**
     * Runs the event associated with leaving the remote file repository name field.
     */
    private void runRemoteRepositoryNameFieldFocusLostEvent() {
        String newRepositoryName = remoteRepositoryNameField.getText();
        if (remoteRepositoriesListTable.getSelectedRow() > 0 && !newRepositoryName.contentEquals(currentRemoteRepositoryName)) {
            try {
                String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                vfsRemoteFileRepositoriesController.setRemoteRepositoryName(remoteRepositoryId, newRepositoryName);
                remoteRepositoriesListTable.getModel().setValueAt(remoteRepositoryNameField.getText(), remoteRepositoriesListTable.getSelectedRow(), REPO_NAME_COLUMN);
                currentRemoteRepositoryName = newRepositoryName;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Invalid VFS repository name! Please check if it meets following requirements:\n- It must be unique\n- It must be alphanumeric.\n- Underscores are allowed.\n- Length is between 3 and 25 characters.", "Update name for remote file repository", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Runs the event associated with leaving the remote file repository schema field.
     */
    private void runRemoteRepositorySchemaFieldFocusLostEvent() {
        String newRepositorySchema = remoteRepositorySchemaField.getText();
        if (remoteRepositoriesListTable.getSelectedRow() > 0 && !newRepositorySchema.contentEquals(currentRemoteRepositorySchema)) {
            try {
                String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                vfsRemoteFileRepositoriesController.setRemoteRepositorySchema(remoteRepositoryId, newRepositorySchema);
                currentRemoteRepositorySchema = newRepositorySchema;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Invalid VFS repository schema! Please check if it meets following requirements:\n- It must be unique\n- It must be alpha-numeric and end with \":\" character (colon)", "Update schema for remote file repository", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Runs the event associated with leaving the remote file repository address field.
     */
    private void runRemoteRepositoryAddressFieldFocusLostEvent() {
        String newRepositoryAddress = remoteRepositoryAddressField.getText();
        if (remoteRepositoriesListTable.getSelectedRow() > 0) {
            try {
                String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                vfsRemoteFileRepositoriesController.setRemoteRepositoryAddress(remoteRepositoryId, newRepositoryAddress);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Invalid VFS repository address! Please check if it meets following requirements:\n- It must contains URL specific characters", "Update address for remote file repository", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Creates and gets the panel with fields for remote file repository name, schema and address.
     *
     * @return The panel with fields for remote file repository name, schema and address
     */
    private JPanel getRemoteRepositoriesSettingsPanel() {
        TableLayout remoteRepositoriesSettingsLayout = new TableLayout(2);
        remoteRepositoriesSettingsLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        remoteRepositoriesSettingsLayout.setTablePadding(new Insets(4, 10, 0, 0));
        remoteRepositoriesSettingsLayout.setTableFill(TableLayout.Fill.BOTH);
        remoteRepositoriesSettingsLayout.setColumnWeightX(0, 1.0);

        JLabel remoteRepositoryNameLabel = new JLabel("Name:", SwingConstants.LEFT);

        remoteRepositoryNameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent evt) {
                runRemoteRepositoryNameFieldFocusLostEvent();
            }
        });

        JLabel remoteRepositorySchemaLabel = new JLabel("Schema:", SwingConstants.LEFT);

        remoteRepositorySchemaField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent evt) {
                runRemoteRepositorySchemaFieldFocusLostEvent();
            }
        });

        JLabel remoteRepositoryAddressLabel = new JLabel("Address:", SwingConstants.LEFT);

        remoteRepositoryAddressField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent evt) {
                runRemoteRepositoryAddressFieldFocusLostEvent();
            }
        });

        JPanel remoteRepositoriesSettingsPanel = new JPanel();
        remoteRepositoriesSettingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        remoteRepositoriesSettingsPanel.setLayout(remoteRepositoriesSettingsLayout);
        remoteRepositoriesSettingsPanel.setAutoscrolls(false);
        remoteRepositoriesSettingsPanel.add(remoteRepositoryNameLabel);
        remoteRepositoriesSettingsPanel.add(remoteRepositoryNameField);
        remoteRepositoriesSettingsPanel.add(remoteRepositorySchemaLabel);
        remoteRepositoriesSettingsPanel.add(remoteRepositorySchemaField);
        remoteRepositoriesSettingsPanel.add(remoteRepositoryAddressLabel);
        remoteRepositoriesSettingsPanel.add(remoteRepositoryAddressField);
        return remoteRepositoriesSettingsPanel;
    }

    /**
     * Runs the event associated with button for adding remote file repository property.
     */
    private void runAddRemoteRepositoryPropertyButtonActionEvent() {
        String newRepositoryPropertyName = (String) remoteRepositoriesPropertiesListTable.getModel().getValueAt(0, REPO_PROP_NAME_COLUMN);
        String newRepositoryPropertyValue = (String) remoteRepositoriesPropertiesListTable.getModel().getValueAt(0, REPO_PROP_VALUE_COLUMN);
        try {
            String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
            String remoteRepositoryPropertyId = vfsRemoteFileRepositoriesController.registerNewRemoteRepositoryProperty(remoteRepositoryId);
            vfsRemoteFileRepositoriesController.setRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId, newRepositoryPropertyName);
            vfsRemoteFileRepositoriesController.setRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId, newRepositoryPropertyValue);
            loadRemoteRepositoryPropertiesOnTable(remoteRepositoryId);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.FINE, "Unable to add remote file repository property. Details: " + ex.getMessage());
            if (ex.getMessage().startsWith("Invalid")) {
                JOptionPane.showMessageDialog(remoteRepositoriesPropertiesListTable, "Invalid VFS repository property! Please check if it meets following requirements:\n- Property name must be unique\n- Property name must be alphanumeric.\n- Underscores are allowed in property name.\n- Length of property name is between 3 and 25 characters.\n- Property value must be not null", "Add new remote file repository property", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Failed to add new remote repository property.\nreason: " + ex, "Add new remote file repository property", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Creates and gets the button for adding remote file repository property.
     *
     * @return The button for adding remote file repository property
     */
    private JButton getAddRemoteRepositoryPropertyButton() {
        JButton addRemoteRepositoryPropertyButton = new JButton(addButtonIcon);
        addRemoteRepositoryPropertyButton.setPreferredSize(new Dimension(20, 20));
        addRemoteRepositoryPropertyButton.addActionListener(e -> runAddRemoteRepositoryPropertyButtonActionEvent());
        return addRemoteRepositoryPropertyButton;
    }

    /**
     * Runs the event associated with button for removing remote file repository property.
     */
    private void runRemoveRemoteRepositoryPropertyButtonActionEvent() {
        try {
            if (remoteRepositoriesPropertiesListTable.getSelectedRow() > 0) {
                String repositoryPropertyName = (String) remoteRepositoriesPropertiesListTable.getModel().getValueAt(remoteRepositoriesPropertiesListTable.getSelectedRow(), REPO_PROP_NAME_COLUMN);
                if (JOptionPane.showConfirmDialog(remoteRepositoriesPropertiesListTable, "Are you sure to delete the following repository property?\n" + repositoryPropertyName, "Delete Repository Property Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                    String remoteRepositoryPropertyId = remoteRepositoriesPropertiesIdsList[remoteRepositoriesPropertiesListTable.getSelectedRow() - 1];
                    vfsRemoteFileRepositoriesController.removeRemoteRepositoryProperty(remoteRepositoryId, remoteRepositoryPropertyId);
                    loadRemoteRepositoryPropertiesOnTable(remoteRepositoryId);
                }
            } else {
                JOptionPane.showMessageDialog(remoteRepositoriesPropertiesListTable, "Please select a repository property from list.", "Delete Repository Property", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            logger.log(Level.SEVERE, "Unable to delete remote file repository property. Details: " + ex.getMessage());
            JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Failed to delete remote repository property.\nreason: " + ex, "Delete remote file repository property", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates and gets the button for removing remote file repository property.
     *
     * @return The button for removing remote file repository property
     */
    private JButton getRemoveRemoteRepositoryPropertyButton() {
        JButton removeRemoteRepositoryPropertyButton = new JButton(removeButtonIcon);
        removeRemoteRepositoryPropertyButton.setPreferredSize(new Dimension(20, 20));
        removeRemoteRepositoryPropertyButton.addActionListener(e -> runRemoveRemoteRepositoryPropertyButtonActionEvent());
        return removeRemoteRepositoryPropertyButton;
    }

    /**
     * Runs the event associated with editing a cell from remote file repository properties table.
     */
    private void runRemoteRepositoriesPropertiesListTableModelEvent() {
        int selectedRow = remoteRepositoriesPropertiesListTable.getSelectedRow();
        int selectedColumn = remoteRepositoriesPropertiesListTable.getSelectedColumn();
        if (selectedRow > 0 && selectedRow < remoteRepositoriesPropertiesListTable.getRowCount()) {
            String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
            String remoteRepositoryPropertyId = remoteRepositoriesPropertiesIdsList[selectedRow - 1];
            String newValue = (String) remoteRepositoriesPropertiesListTable.getValueAt(selectedRow, selectedColumn);
            switch (selectedColumn) {
                case REPO_PROP_NAME_COLUMN:
                    try {
                        vfsRemoteFileRepositoriesController.setRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId, newValue);
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(remoteRepositoriesPropertiesListTable, "Invalid VFS repository property name! Please check if it meets following requirements:\n- Property name must be unique\n- Property name must be alphanumeric.\n- Underscores are allowed in property name.\n- Length of property name is between 3 and 25 characters.", "Update remote file repository property name", JOptionPane.WARNING_MESSAGE);
                    }
                    break;
                case REPO_PROP_VALUE_COLUMN:
                    try {
                        vfsRemoteFileRepositoriesController.setRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId, newValue);
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(remoteRepositoriesPropertiesListTable, "Invalid VFS repository property value! Please check if it meets following requirements:\n- Property value must be not null", "Update remote file repository property value", JOptionPane.WARNING_MESSAGE);
                    }
                    break;
                default:
                    break;
            }
            loadRemoteRepositoryPropertiesOnTable(remoteRepositoryId);
        }
    }

    /**
     * Creates and gets the remote file repository properties table.
     *
     * @return The remote file repository properties table
     */
    private JTable getRemoteRepositoriesPropertiesListTable() {
        JTable newRemoteRepositoriesPropertiesListTable = new JTable();
        DefaultTableModel remoteRepositoriesPropertiesListTableModel = new DefaultTableModel(
                new Object[][]{
                        new Object[]{"", ""},
                },
                new String[]{
                        "Name", "Value"
                }
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        newRemoteRepositoriesPropertiesListTable.setModel(remoteRepositoriesPropertiesListTableModel);
        newRemoteRepositoriesPropertiesListTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        newRemoteRepositoriesPropertiesListTable.getModel().addTableModelListener(e -> runRemoteRepositoriesPropertiesListTableModelEvent());
        return newRemoteRepositoriesPropertiesListTable;
    }

    /**
     * Creates and gets the panel with add and remove buttons for remote file repository properties table.
     *
     * @return The panel with add and remove buttons
     */
    private JPanel getRemoteRepositoriesPropertiesListActionsPanel() {
        JPanel remoteRepositoriesPropertiesActionsPanel = new JPanel();
        remoteRepositoriesPropertiesActionsPanel.setLayout(new BoxLayout(remoteRepositoriesPropertiesActionsPanel, BoxLayout.PAGE_AXIS));
        remoteRepositoriesPropertiesActionsPanel.add(getAddRemoteRepositoryPropertyButton());
        remoteRepositoriesPropertiesActionsPanel.add(getRemoveRemoteRepositoryPropertyButton());
        remoteRepositoriesPropertiesActionsPanel.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767)));
        return remoteRepositoriesPropertiesActionsPanel;
    }

    /**
     * Creates and gets the panel with add and remove buttons panel and remote file repository properties table.
     *
     * @return The panel with add and remove buttons panel and remote file repository properties table
     */
    private JPanel getRemoteRepositoriesPropertiesListPanel() {
        JScrollPane remoteRepositoriesPropertiesListSP = new JScrollPane();
        remoteRepositoriesPropertiesListSP.setViewportView(remoteRepositoriesPropertiesListTable);

        JPanel remoteRepositoriesPropertiesListPanel = new JPanel();
        remoteRepositoriesPropertiesListPanel.setBorder(BorderFactory.createTitledBorder("Properties"));
        remoteRepositoriesPropertiesListPanel.setLayout(new BoxLayout(remoteRepositoriesPropertiesListPanel, BoxLayout.LINE_AXIS));
        remoteRepositoriesPropertiesListPanel.add(getRemoteRepositoriesPropertiesListActionsPanel());
        remoteRepositoriesPropertiesListPanel.add(remoteRepositoriesPropertiesListSP);
        remoteRepositoriesPropertiesListPanel.setPreferredSize(new Dimension(150, 250));
        return remoteRepositoriesPropertiesListPanel;
    }

    /**
     * Creates and gets the panel with remote file repository configurations.
     *
     * @return The panel with remote file repository configurations
     */
    private JPanel getRemoteRepositoriesConfigsPanel() {
        TableLayout remoteRepositoriesConfigsLayout = new TableLayout(1);
        remoteRepositoriesConfigsLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        remoteRepositoriesConfigsLayout.setTablePadding(new Insets(4, 10, 0, 0));
        remoteRepositoriesConfigsLayout.setTableFill(TableLayout.Fill.BOTH);
        remoteRepositoriesConfigsLayout.setColumnWeightX(0, 1.0);

        remoteRepositoriesConfigsPanel = new JPanel();
        remoteRepositoriesConfigsPanel.setBorder(BorderFactory.createTitledBorder("Remote File Repository Configurations"));
        remoteRepositoriesConfigsPanel.setLayout(remoteRepositoriesConfigsLayout);
        remoteRepositoriesConfigsPanel.setAutoscrolls(false);
        remoteRepositoriesConfigsPanel.add(getRemoteRepositoriesSettingsPanel());
        remoteRepositoriesConfigsPanel.add(getRemoteRepositoriesPropertiesListPanel());
        remoteRepositoriesConfigsPanel.setVisible(false);
        return remoteRepositoriesConfigsPanel;
    }

    /**
     * Creates and gets the panel with remote file repositories table and remote file repository configurations panel.
     *
     * @return The panel with remote file repositories table and remote file repository configurations panel
     */
    private JPanel getRemoteFileRepositoriesPanel() {
        TableLayout remoteFileRepositoriesLayout = new TableLayout(2);
        remoteFileRepositoriesLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        remoteFileRepositoriesLayout.setTablePadding(new Insets(4, 10, 0, 0));
        remoteFileRepositoriesLayout.setTableFill(TableLayout.Fill.BOTH);
        remoteFileRepositoriesLayout.setColumnWeightX(0, 1.0);

        JPanel remoteFileRepositoriesPanel = new JPanel();
        remoteFileRepositoriesPanel.setLayout(remoteFileRepositoriesLayout);
        remoteFileRepositoriesPanel.add(getRemoteRepositoriesListPanel());
        remoteFileRepositoriesPanel.add(getRemoteRepositoriesConfigsPanel());
        return remoteFileRepositoriesPanel;
    }

    /**
     * Creates and gets the root panel for remote file repositories tab ui.
     *
     * @return The root panel for remote file repositories tab ui
     */
    private JPanel getRemoteFileRepositoriesTabUI() {
        JPanel remoteFileRepositoriesTabUI = new JPanel(new BorderLayout());
        remoteFileRepositoriesTabUI.add(getRemoteFileRepositoriesPanel(), BorderLayout.PAGE_START);
        remoteFileRepositoriesTabUI.add(Box.createVerticalGlue(), BorderLayout.PAGE_END);
        return remoteFileRepositoriesTabUI;
    }

    /**
     * Loads the remote file repositories names on remote file repositories table
     */
    private void loadRemoteRepositoriesOnTable() {
        ((DefaultTableModel) remoteRepositoriesListTable.getModel()).setRowCount(1);
        remoteRepositoriesListTable.getModel().setValueAt("", 0, REPO_NAME_COLUMN);
        vfsOptionsBean.remoteRepositoriesIds = vfsRemoteFileRepositoriesController.getRemoteRepositoriesIds().getValue();
        if (vfsOptionsBean.remoteRepositoriesIds != null && !vfsOptionsBean.remoteRepositoriesIds.isEmpty()) {
            remoteRepositoriesIdsList = vfsOptionsBean.remoteRepositoriesIds.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);
            for (String remoteRepositoryId : remoteRepositoriesIdsList) {
                String remoteRepositoryName = vfsRemoteFileRepositoriesController.getRemoteRepositoryName(remoteRepositoryId).getValue();
                if (remoteRepositoryName != null && !remoteRepositoryName.isEmpty()) {
                    ((DefaultTableModel) remoteRepositoriesListTable.getModel()).addRow(new Object[]{remoteRepositoryName,});
                }
            }
        }
    }

    /**
     * Loads the remote file repository properties names and values on remote file repository properties table
     *
     * @param remoteRepositoryId The remote file repository id
     */
    private void loadRemoteRepositoryPropertiesOnTable(String remoteRepositoryId) {
        ((DefaultTableModel) remoteRepositoriesPropertiesListTable.getModel()).setRowCount(1);
        remoteRepositoriesPropertiesListTable.getModel().setValueAt("", 0, REPO_PROP_NAME_COLUMN);
        remoteRepositoriesPropertiesListTable.getModel().setValueAt("", 0, REPO_PROP_VALUE_COLUMN);
        String remoteRepositoryPropertiesIds = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertiesIds(remoteRepositoryId).getValue();
        if (remoteRepositoryPropertiesIds != null && !remoteRepositoryPropertiesIds.isEmpty()) {
            remoteRepositoriesPropertiesIdsList = remoteRepositoryPropertiesIds.split(VFSRemoteFileRepositoriesController.LIST_ITEM_SEPARATOR);
            for (String remoteRepositoryPropertyId : remoteRepositoriesPropertiesIdsList) {
                String remoteRepositoryPropertyName = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId).getValue();
                String remoteRepositoryPropertyValue = vfsRemoteFileRepositoriesController.getRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId).getValue();
                if (remoteRepositoryPropertyName != null && !remoteRepositoryPropertyName.isEmpty() && remoteRepositoryPropertyValue != null && !remoteRepositoryPropertyValue.isEmpty()) {
                    ((DefaultTableModel) remoteRepositoriesPropertiesListTable.getModel()).addRow(new Object[]{remoteRepositoryPropertyName, remoteRepositoryPropertyName.matches(VFSRemoteFileRepositoriesController.CREDENTIAL_PROPERTY_NAME_REGEX) ? remoteRepositoryPropertyValue.replaceAll("(?s).", "*") : remoteRepositoryPropertyValue});
                }
            }
        }
    }

    /**
     * The bean with fields annoted with {@link Preference} for VFS Options.
     */
    static class VFSOptionsBean {
        @Preference(label = "Remote File Repositories List", key = VFSRemoteFileRepositoriesController.PREFERENCE_KEY_VFS_REPOSITORIES)
        String remoteRepositoriesIds;
    }

}
