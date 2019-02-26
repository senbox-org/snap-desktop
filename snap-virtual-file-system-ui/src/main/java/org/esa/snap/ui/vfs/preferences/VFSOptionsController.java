package org.esa.snap.ui.vfs.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.vfs.validators.RepositoryNameValidator;
import org.esa.snap.ui.vfs.validators.RepositorySchemaValidator;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.NbPreferences;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Options controller for VFS Remote File Repositories.
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

    private static final int REPO_NAME_COLUMN = 0;
    private static final int REPO_PROP_NAME_COLUMN = 0;
    private static final int REPO_PROP_VALUE_COLUMN = 1;

    private static final int MODE_ADD = 0;
    private static final int MODE_REMOVE = 1;

    private static final String LIST_ITEM_SEPARATOR = ";";

    private static final String REPO_ID_KEY = "%repo_id%";
    private static final String PROP_ID_KEY = "%prop_id%";

    private static final String PREFERENCE_KEY_VFS_REPOSITORIES = "vfs.repositories";
    private static final String PREFERENCE_KEY_VFS_REPOSITORY = PREFERENCE_KEY_VFS_REPOSITORIES + ".repository_" + REPO_ID_KEY;
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_NAME = PREFERENCE_KEY_VFS_REPOSITORY + ".name";
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_SCHEMA = PREFERENCE_KEY_VFS_REPOSITORY + ".schema";

    private static final String PREFERENCE_KEY_VFS_REPOSITORY_PROPERTIES = PREFERENCE_KEY_VFS_REPOSITORY + ".properties";
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY = PREFERENCE_KEY_VFS_REPOSITORY_PROPERTIES + ".property_" + PROP_ID_KEY;
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_NAME = PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY + ".name";
    private static final String PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_VALUE = PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY + ".value";

    private static final String DEFAULT_PROPERTY_VALUE = "def";
    private static final String CREDENTIAL_PROPERTY_NAME_REGEX = "((.*)((key)|(password)|(secret))(.*))";

    private static ImageIcon addButtonIcon;
    private static ImageIcon removeButtonIcon;

    private final JTable remoteRepositoriesListTable = getRemoteRepositoriesListTable();
    private final JTable remoteRepositoriesPropertiesListTable = getRemoteRepositoriesPropertiesListTable();

    private final JTextField remoteRepositoryNameField = new JTextField(30);
    private final JTextField remoteRepositorySchemaField = new JTextField(30);

    private JPanel remoteRepositoriesConfigsPanel;

    private Preferences preferences;

    private String[] remoteRepositoriesIdsList;
    private String[] remoteRepositoriesPropertiesIdsList;

    private String lastRepositoryName = "";

    private VFSOptionsBean vfsOptionsBean = new VFSOptionsBean();

    static {
        try {
            addButtonIcon = new ImageIcon(VFSOptionsController.class.getResource("/org/esa/snap/ui/vfs/preferences/icons/list-add.png"));
            removeButtonIcon = new ImageIcon(VFSOptionsController.class.getResource("/org/esa/snap/ui/vfs/preferences/icons/list-remove.png"));
        } catch (Exception e) {
            Logger.getLogger(VFSOptionsController.class.getName()).warning("Image resource not loaded");
        }
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

    @Override
    protected JPanel createPanel(BindingContext context) {
        preferences = NbPreferences.forModule(Dialogs.class);
        JPanel remoteFileRepositoriesTabUI = getRemoteFileRepositoriesTabUI();
        loadRemoteRepositoriesOnTable();
        return remoteFileRepositoriesTabUI;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("vfs_editor");
    }

    private void writeProperty(Property property) {
        if (property != null) {
            if (preferences == null) {
                preferences = NbPreferences.forModule(Dialogs.class);
            }
            try {
                preferences.put(property.getName(), property.getValue());
                preferences.flush();
            } catch (BackingStoreException ignored) {//
            }
        }
    }

    private Property getProperty(String propertyKey) {
        if (preferences == null) {
            preferences = NbPreferences.forModule(Dialogs.class);
        }
        return Property.create(propertyKey, preferences.get(propertyKey, DEFAULT_PROPERTY_VALUE));
    }

    private void removeProperty(Property property) {
        if (property != null) {
            if (preferences == null) {
                preferences = NbPreferences.forModule(Dialogs.class);
            }
            try {
                preferences.remove(property.getName());
                preferences.flush();
            } catch (BackingStoreException ignored) {//
            }
        }
    }

    private Property getRemoteRepositoriesIds() {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORIES);
    }

    private void updateRemoteRepositoriesIds(String remoteRepositoryId, int mode) throws ValidationException {
        Property remoteRepositoryIdsProperty = getRemoteRepositoriesIds();
        vfsOptionsBean.remoteRepositoriesIds = remoteRepositoryIdsProperty.getValue();
        vfsOptionsBean.remoteRepositoriesIds = vfsOptionsBean.remoteRepositoriesIds != null ? vfsOptionsBean.remoteRepositoriesIds.replaceAll(DEFAULT_PROPERTY_VALUE, "") : "";
        switch (mode) {
            case MODE_ADD:
                vfsOptionsBean.remoteRepositoriesIds = !vfsOptionsBean.remoteRepositoriesIds.isEmpty() ? vfsOptionsBean.remoteRepositoriesIds + LIST_ITEM_SEPARATOR + remoteRepositoryId : remoteRepositoryId;
                break;
            case MODE_REMOVE:
                vfsOptionsBean.remoteRepositoriesIds = vfsOptionsBean.remoteRepositoriesIds.replaceAll("((" + remoteRepositoryId + "(" + LIST_ITEM_SEPARATOR + ")?)|(" + LIST_ITEM_SEPARATOR + ")?" + remoteRepositoryId + ")", "");
                break;
            default:
                break;
        }
        remoteRepositoryIdsProperty.setValue(vfsOptionsBean.remoteRepositoriesIds);
        writeProperty(remoteRepositoryIdsProperty);
    }

    private Property getRemoteRepositoryPropertiesIds(String remoteRepositoryId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTIES.replace(REPO_ID_KEY, remoteRepositoryId));
    }

    private void updateRemoteRepositoryPropertiesIds(String remoteRepositoryId, String remoteRepositoryPropertyId, int mode) throws ValidationException {
        Property remoteRepositoryPropertiesIdsProperty = getRemoteRepositoryPropertiesIds(remoteRepositoryId);
        String remoteRepositoryPropertiesIds = remoteRepositoryPropertiesIdsProperty.getValue();
        remoteRepositoryPropertiesIds = remoteRepositoryPropertiesIds != null ? remoteRepositoryPropertiesIds.replaceAll(DEFAULT_PROPERTY_VALUE, "") : "";
        switch (mode) {
            case MODE_ADD:
                remoteRepositoryPropertiesIds = !remoteRepositoryPropertiesIds.isEmpty() ? remoteRepositoryPropertiesIds + LIST_ITEM_SEPARATOR + remoteRepositoryPropertyId : remoteRepositoryPropertyId;
                break;
            case MODE_REMOVE:
                remoteRepositoryPropertiesIds = remoteRepositoryPropertiesIds.replaceAll("((" + remoteRepositoryPropertyId + "(" + LIST_ITEM_SEPARATOR + ")?)|(" + LIST_ITEM_SEPARATOR + ")?" + remoteRepositoryPropertyId + ")", "");
                break;
            default:
                break;
        }
        remoteRepositoryPropertiesIdsProperty.setValue(remoteRepositoryPropertiesIds);
        writeProperty(remoteRepositoryPropertiesIdsProperty);
    }

    private String registerNewRemoteRepository() throws ValidationException {
        String remoteRepositoryId = "" + System.currentTimeMillis();
        updateRemoteRepositoriesIds(remoteRepositoryId, MODE_ADD);
        return remoteRepositoryId;
    }

    private String registerNewRemoteRepositoryProperty(String remoteRepositoryId) throws ValidationException {
        String remoteRepositoryPropertyId = "" + System.currentTimeMillis();
        updateRemoteRepositoryPropertiesIds(remoteRepositoryId, remoteRepositoryPropertyId, MODE_ADD);
        return remoteRepositoryPropertyId;
    }

    private Property getRemoteRepositoryName(String remoteRepositoryId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_NAME.replace(REPO_ID_KEY, remoteRepositoryId));
    }

    private void setRemoteRepositoryName(String remoteRepositoryId, String remoteRepositoryName) {
        Property remoteRepositoryNameProperty = Property.create(PREFERENCE_KEY_VFS_REPOSITORY_NAME.replace(REPO_ID_KEY, remoteRepositoryId), remoteRepositoryName);
        writeProperty(remoteRepositoryNameProperty);
    }

    private Property getRemoteRepositorySchema(String remoteRepositoryId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_SCHEMA.replace(REPO_ID_KEY, remoteRepositoryId));
    }

    private void setRemoteRepositorySchema(String remoteRepositoryId, String remoteRepositorySchema) {
        Property remoteRepositorySchemaProperty = Property.create(PREFERENCE_KEY_VFS_REPOSITORY_SCHEMA.replace(REPO_ID_KEY, remoteRepositoryId), remoteRepositorySchema);
        writeProperty(remoteRepositorySchemaProperty);
    }

    private Property getRemoteRepositoryPropertyName(String remoteRepositoryId, String remoteRepositoryPropertyId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_NAME.replace(REPO_ID_KEY, remoteRepositoryId).replace(PROP_ID_KEY, remoteRepositoryPropertyId));
    }

    private void setRemoteRepositoryPropertyName(String remoteRepositoryId, String remoteRepositoryPropertyId, String remoteRepositoryPropertyName) {
        Property remoteRepositoryPropertyNameProperty = Property.create(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_NAME.replace(REPO_ID_KEY, remoteRepositoryId).replace(PROP_ID_KEY, remoteRepositoryPropertyId), remoteRepositoryPropertyName);
        writeProperty(remoteRepositoryPropertyNameProperty);
    }

    private Property getRemoteRepositoryPropertyValue(String remoteRepositoryId, String remoteRepositoryPropertyId) {
        return getProperty(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_VALUE.replace(REPO_ID_KEY, remoteRepositoryId).replace(PROP_ID_KEY, remoteRepositoryPropertyId));
    }

    private void setRemoteRepositoryPropertyValue(String remoteRepositoryId, String remoteRepositoryPropertyId, String remoteRepositoryPropertyValue) {
        Property remoteRepositoryPropertyValueProperty = Property.create(PREFERENCE_KEY_VFS_REPOSITORY_PROPERTY_VALUE.replace(REPO_ID_KEY, remoteRepositoryId).replace(PROP_ID_KEY, remoteRepositoryPropertyId), remoteRepositoryPropertyValue);
        writeProperty(remoteRepositoryPropertyValueProperty);
    }

    private void removeRemoteRepositoryProperty(String remoteRepositoryId, String remoteRepositoryPropertyId) throws ValidationException {
        removeProperty(getRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId));
        removeProperty(getRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId));
        updateRemoteRepositoryPropertiesIds(remoteRepositoryId, remoteRepositoryPropertyId, MODE_REMOVE);
    }

    private void removeRemoteRepository(String remoteRepositoryId) throws ValidationException {
        removeProperty(getRemoteRepositoryName(remoteRepositoryId));
        removeProperty(getRemoteRepositorySchema(remoteRepositoryId));
        Property remoteRepositoryPropertiesIdsProperty = getRemoteRepositoryPropertiesIds(remoteRepositoryId);
        String remoteRepositoriesPropertiesIds = remoteRepositoryPropertiesIdsProperty.getValue();
        String[] remoteRepositoriesPropertiesIdsList0 = remoteRepositoriesPropertiesIds.split(LIST_ITEM_SEPARATOR);
        for (String remoteRepositoriesPropertyId : remoteRepositoriesPropertiesIdsList0) {
            removeRemoteRepositoryProperty(remoteRepositoryId, remoteRepositoriesPropertyId);
        }
        removeProperty(remoteRepositoryPropertiesIdsProperty);
        updateRemoteRepositoriesIds(remoteRepositoryId, MODE_REMOVE);
    }

    private boolean isUniqueOnTable(Object target, JTable targetTable, int... targetColumns) {
        if (targetColumns != null && targetColumns.length > 0) {
            for (int targetColumn : targetColumns) {
                if (isDuplicateOnColumn(target, targetTable, targetColumn)) {
                    return false;
                }
            }
        } else {
            for (int targetColumn = 0; targetColumn < targetTable.getColumnCount(); targetColumn++) {
                if (isDuplicateOnColumn(target, targetTable, targetColumn)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isDuplicateOnColumn(Object target, JTable targetTable, int targetColumn) {
        for (int rowIndex = 1; rowIndex < targetTable.getRowCount(); rowIndex++) {
            if (targetTable.getModel().getValueAt(rowIndex, targetColumn).equals(target)) {
                return true;
            }
        }
        return false;
    }

    private JButton getAddRemoteRepositoryButton() {
        JButton addRemoteRepositoryButton = new JButton(addButtonIcon);
        addRemoteRepositoryButton.setPreferredSize(new Dimension(20, 20));
        addRemoteRepositoryButton.addActionListener(e -> {
            String newRepositoryName = (String) remoteRepositoriesListTable.getModel().getValueAt(0, REPO_NAME_COLUMN);
            try {
                if (new RepositoryNameValidator().isValid(newRepositoryName) && isUniqueOnTable(newRepositoryName, remoteRepositoriesListTable, REPO_NAME_COLUMN)) {
                    String remoteRepositoryId = registerNewRemoteRepository();
                    setRemoteRepositoryName(remoteRepositoryId, newRepositoryName);
                    loadRemoteRepositoriesOnTable();
                } else {
                    throw new ValidationException("X");
                }
            } catch (ValidationException ex) {
                if (ex.getMessage().contentEquals("X")) {
                    JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Invalid VFS repository name! Please check if it meets following requirements:\n- It must be unique\n- It must be alphanumeric.\n- Underscores are allowed.\n- Length is between 3 and 25 characters.", "Add new remote file repository", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Failed to add new remote repository.\nreason: " + ex, "Add new remote file repository", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return addRemoteRepositoryButton;
    }

    private JButton getRemoveRemoteRepositoryButton() {
        JButton removeRemoteRepositoryButton = new JButton(removeButtonIcon);
        removeRemoteRepositoryButton.setPreferredSize(new Dimension(20, 20));
        removeRemoteRepositoryButton.addActionListener(e -> {
            try {
                if (remoteRepositoriesListTable.getSelectedRow() > 0) {
                    String repositoryName = (String) remoteRepositoriesListTable.getModel().getValueAt(remoteRepositoriesListTable.getSelectedRow(), REPO_NAME_COLUMN);
                    if (JOptionPane.showConfirmDialog(remoteRepositoriesListTable, "Are you sure to delete the following repository?\n" + repositoryName, "Delete Repository Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                        removeRemoteRepository(remoteRepositoryId);
                        loadRemoteRepositoriesOnTable();
                    }
                } else {
                    JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Please select a repository from list.", "Delete Repository", JOptionPane.WARNING_MESSAGE);
                }
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Failed to delete remote repository.\nreason: " + ex, "Delete remote file repository", JOptionPane.ERROR_MESSAGE);
            }
        });
        return removeRemoteRepositoryButton;
    }

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
        newRemoteRepositoriesListTable.getSelectionModel().addListSelectionListener(event -> {
            if (remoteRepositoriesListTable.getSelectedRow() > 0) {
                String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                String remoteRepositoryName = getRemoteRepositoryName(remoteRepositoryId).getValue();
                String remoteRepositorySchema = getRemoteRepositorySchema(remoteRepositoryId).getValue();
                remoteRepositoryNameField.setText(remoteRepositoryName.contentEquals(DEFAULT_PROPERTY_VALUE) ? "" : remoteRepositoryName);
                remoteRepositorySchemaField.setText(remoteRepositorySchema.contentEquals(DEFAULT_PROPERTY_VALUE) ? "" : remoteRepositorySchema);
                loadRemoteRepositoryPropertiesOnTable(remoteRepositoryId);
                remoteRepositoriesConfigsPanel.setVisible(true);
            } else {
                remoteRepositoriesConfigsPanel.setVisible(false);
            }
        });
        return newRemoteRepositoriesListTable;
    }

    private JPanel getRemoteRepositoriesListActionsPanel() {
        JPanel remoteRepositoriesListActionsPanel = new JPanel();
        remoteRepositoriesListActionsPanel.setLayout(new BoxLayout(remoteRepositoriesListActionsPanel, BoxLayout.PAGE_AXIS));
        remoteRepositoriesListActionsPanel.add(getAddRemoteRepositoryButton());
        remoteRepositoriesListActionsPanel.add(getRemoveRemoteRepositoryButton());
        remoteRepositoriesListActionsPanel.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767)));
        return remoteRepositoriesListActionsPanel;
    }

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
                String newRepositoryName = remoteRepositoryNameField.getText();
                if (!newRepositoryName.contentEquals(lastRepositoryName)) {
                    if (new RepositoryNameValidator().isValid(newRepositoryName) && isUniqueOnTable(newRepositoryName, remoteRepositoriesListTable, REPO_NAME_COLUMN)) {
                        String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                        setRemoteRepositoryName(remoteRepositoryId, newRepositoryName);
                        remoteRepositoriesListTable.getModel().setValueAt(remoteRepositoryNameField.getText(), remoteRepositoriesListTable.getSelectedRow(), REPO_NAME_COLUMN);
                    } else {
                        JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Invalid VFS repository name! Please check if it meets following requirements:\n- It must be unique\n- It must be alphanumeric.\n- Underscores are allowed.\n- Length is between 3 and 25 characters.", "Update name for remote file repository", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }

            @Override
            public void focusGained(FocusEvent evt) {
                lastRepositoryName = remoteRepositoryNameField.getText();
            }
        });

        JLabel remoteRepositorySchemaLabel = new JLabel("Schema:", SwingConstants.LEFT);

        remoteRepositorySchemaField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent evt) {
                String newRepositorySchema = remoteRepositorySchemaField.getText();
                if (new RepositorySchemaValidator().isValid(newRepositorySchema)) {
                    String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                    setRemoteRepositorySchema(remoteRepositoryId, newRepositorySchema);
                } else {
                    JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Invalid VFS repository schema! Please check if it meets following requirements:\n- It must be one from the following list: (\"s3://\";\"http://\";\"oss://\")", "Update schema for remote file repository", JOptionPane.WARNING_MESSAGE);
                }
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
        return remoteRepositoriesSettingsPanel;
    }

    private JButton getAddRemoteRepositoryPropertyButton() {
        JButton addRemoteRepositoryPropertyButton = new JButton(addButtonIcon);
        addRemoteRepositoryPropertyButton.setPreferredSize(new Dimension(20, 20));
        addRemoteRepositoryPropertyButton.addActionListener(e -> {
            String newRepositoryPropertyName = (String) remoteRepositoriesPropertiesListTable.getModel().getValueAt(0, REPO_PROP_NAME_COLUMN);
            String newRepositoryPropertyValue = (String) remoteRepositoriesPropertiesListTable.getModel().getValueAt(0, REPO_PROP_VALUE_COLUMN);
            try {
                if (new RepositoryNameValidator().isValid(newRepositoryPropertyName) && isUniqueOnTable(newRepositoryPropertyName, remoteRepositoriesPropertiesListTable, REPO_PROP_NAME_COLUMN) && !newRepositoryPropertyValue.isEmpty()) {
                    String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                    String remoteRepositoryPropertyId = registerNewRemoteRepositoryProperty(remoteRepositoryId);
                    setRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId, newRepositoryPropertyName);
                    setRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId, newRepositoryPropertyValue);
                    loadRemoteRepositoryPropertiesOnTable(remoteRepositoryId);
                } else {
                    throw new ValidationException("X");
                }
            } catch (ValidationException ex) {
                if (ex.getMessage().contentEquals("X")) {
                    JOptionPane.showMessageDialog(remoteRepositoriesPropertiesListTable, "Invalid VFS repository property! Please check if it meets following requirements:\n- Property name must be unique\n- Property name must be alphanumeric.\n- Underscores are allowed in property name.\n- Length of property name is between 3 and 25 characters.\n- Property value must be not null", "Add new remote file repository property", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Failed to add new remote repository property.\nreason: " + ex, "Add new remote file repository property", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return addRemoteRepositoryPropertyButton;
    }

    private JButton getRemoveRemoteRepositoryPropertyButton() {
        JButton removeRemoteRepositoryPropertyButton = new JButton(removeButtonIcon);
        removeRemoteRepositoryPropertyButton.setPreferredSize(new Dimension(20, 20));
        removeRemoteRepositoryPropertyButton.addActionListener(e -> {
            try {
                if (remoteRepositoriesPropertiesListTable.getSelectedRow() > 0) {
                    String newRepositoryPropertyName = (String) remoteRepositoriesPropertiesListTable.getModel().getValueAt(0, REPO_PROP_NAME_COLUMN);
                    if (JOptionPane.showConfirmDialog(remoteRepositoriesPropertiesListTable, "Are you sure to delete the following repository property?\n" + newRepositoryPropertyName, "Delete Repository Property Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                        String remoteRepositoryPropertyId = remoteRepositoriesPropertiesIdsList[remoteRepositoriesPropertiesListTable.getSelectedRow() - 1];
                        removeRemoteRepositoryProperty(remoteRepositoryId, remoteRepositoryPropertyId);
                        loadRemoteRepositoryPropertiesOnTable(remoteRepositoryId);
                    }
                } else {
                    JOptionPane.showMessageDialog(remoteRepositoriesPropertiesListTable, "Please select a repository property from list.", "Delete Repository Property", JOptionPane.WARNING_MESSAGE);
                }
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(remoteRepositoriesListTable, "Failed to delete remote repository property.\nreason: " + ex, "Delete remote file repository property", JOptionPane.ERROR_MESSAGE);
            }
        });
        return removeRemoteRepositoryPropertyButton;
    }

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
        newRemoteRepositoriesPropertiesListTable.getModel().addTableModelListener(e -> {
            int selectedRow = remoteRepositoriesPropertiesListTable.getSelectedRow();
            int selectedColumn = remoteRepositoriesPropertiesListTable.getSelectedColumn();
            if (selectedRow > 0 && selectedRow < remoteRepositoriesPropertiesListTable.getRowCount()) {
                String remoteRepositoryId = remoteRepositoriesIdsList[remoteRepositoriesListTable.getSelectedRow() - 1];
                String remoteRepositoryPropertyId = remoteRepositoriesPropertiesIdsList[selectedRow - 1];
                String newValue = (String) remoteRepositoriesPropertiesListTable.getValueAt(selectedRow, selectedColumn);
                if (selectedColumn == REPO_PROP_NAME_COLUMN) {
                    setRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId, newValue);
                } else if (selectedColumn == REPO_PROP_VALUE_COLUMN) {
                    setRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId, newValue);
                }
                loadRemoteRepositoryPropertiesOnTable(remoteRepositoryId);
            }
        });
        return newRemoteRepositoriesPropertiesListTable;
    }

    private JPanel getRemoteRepositoriesPropertiesListActionsPanel() {
        JPanel remoteRepositoriesPropertiesActionsPanel = new JPanel();
        remoteRepositoriesPropertiesActionsPanel.setLayout(new BoxLayout(remoteRepositoriesPropertiesActionsPanel, BoxLayout.PAGE_AXIS));
        remoteRepositoriesPropertiesActionsPanel.add(getAddRemoteRepositoryPropertyButton());
        remoteRepositoriesPropertiesActionsPanel.add(getRemoveRemoteRepositoryPropertyButton());
        remoteRepositoriesPropertiesActionsPanel.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767)));
        return remoteRepositoriesPropertiesActionsPanel;
    }

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

    private JPanel getRemoteFileRepositoriesTabUI() {
        JPanel remoteFileRepositoriesTabUI = new JPanel(new BorderLayout());
        remoteFileRepositoriesTabUI.add(getRemoteFileRepositoriesPanel(), BorderLayout.PAGE_START);
        remoteFileRepositoriesTabUI.add(Box.createVerticalGlue(), BorderLayout.PAGE_END);
        return remoteFileRepositoriesTabUI;
    }

    private void loadRemoteRepositoriesOnTable() {
        ((DefaultTableModel) remoteRepositoriesListTable.getModel()).setRowCount(1);
        remoteRepositoriesListTable.getModel().setValueAt("", 0, REPO_NAME_COLUMN);
        vfsOptionsBean.remoteRepositoriesIds = getRemoteRepositoriesIds().getValue();
        if (vfsOptionsBean.remoteRepositoriesIds != null && !vfsOptionsBean.remoteRepositoriesIds.contentEquals(DEFAULT_PROPERTY_VALUE)) {
            remoteRepositoriesIdsList = vfsOptionsBean.remoteRepositoriesIds.split(LIST_ITEM_SEPARATOR);
            for (String remoteRepositoryId : remoteRepositoriesIdsList) {
                String remoteRepositoryName = getRemoteRepositoryName(remoteRepositoryId).getValue();
                if (remoteRepositoryName != null && !remoteRepositoryName.contentEquals(DEFAULT_PROPERTY_VALUE)) {
                    ((DefaultTableModel) remoteRepositoriesListTable.getModel()).addRow(new Object[]{remoteRepositoryName,});
                }
            }
        }
    }

    private void loadRemoteRepositoryPropertiesOnTable(String remoteRepositoryId) {
        ((DefaultTableModel) remoteRepositoriesPropertiesListTable.getModel()).setRowCount(1);
        remoteRepositoriesPropertiesListTable.getModel().setValueAt("", 0, REPO_PROP_NAME_COLUMN);
        remoteRepositoriesPropertiesListTable.getModel().setValueAt("", 0, REPO_PROP_VALUE_COLUMN);
        String remoteRepositoryPropertiesIds = getRemoteRepositoryPropertiesIds(remoteRepositoryId).getValue();
        remoteRepositoryPropertiesIds = remoteRepositoryPropertiesIds.contentEquals(DEFAULT_PROPERTY_VALUE) ? "" : remoteRepositoryPropertiesIds;
        remoteRepositoriesPropertiesIdsList = remoteRepositoryPropertiesIds.split(LIST_ITEM_SEPARATOR);
        for (String remoteRepositoryPropertyId : remoteRepositoriesPropertiesIdsList) {
            String remoteRepositoryPropertyName = getRemoteRepositoryPropertyName(remoteRepositoryId, remoteRepositoryPropertyId).getValue();
            String remoteRepositoryPropertyValue = getRemoteRepositoryPropertyValue(remoteRepositoryId, remoteRepositoryPropertyId).getValue();
            if (remoteRepositoryPropertyName != null && !remoteRepositoryPropertyName.contentEquals(DEFAULT_PROPERTY_VALUE) && remoteRepositoryPropertyValue != null && !remoteRepositoryPropertyValue.contentEquals(DEFAULT_PROPERTY_VALUE)) {
                ((DefaultTableModel) remoteRepositoriesPropertiesListTable.getModel()).addRow(new Object[]{remoteRepositoryPropertyName, remoteRepositoryPropertyName.matches(CREDENTIAL_PROPERTY_NAME_REGEX) ? remoteRepositoryPropertyValue.replaceAll("(?s).", "*") : remoteRepositoryPropertyValue});
            }
        }
    }

    static class VFSOptionsBean {
        @Preference(label = "Remote File Repositories List", key = PREFERENCE_KEY_VFS_REPOSITORIES)
        String remoteRepositoriesIds;
    }

}
