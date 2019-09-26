package org.esa.snap.product.library.ui.v2.preferences;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import com.bc.ceres.swing.binding.BindingContext;
import org.apache.http.auth.Credentials;
import org.esa.snap.core.util.ServiceLoader;
import org.esa.snap.product.library.ui.v2.preferences.model.RepositoriesCredentialsTableModel;
import org.esa.snap.product.library.ui.v2.preferences.model.RepositoriesTableModel;
import org.esa.snap.product.library.v2.preferences.RepositoriesCredentialsController;
import org.esa.snap.product.library.v2.preferences.model.RepositoryCredential;
import org.esa.snap.product.library.v2.preferences.model.RepositoryCredentials;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller UI for Product Library Remote Repositories Credentials.
 * Used for provide a UI to the strategy with storing Remote Repositories credentials data.
 *
 * @author Adrian Draghici
 */
@OptionsPanelController.SubRegistration(location = "GeneralPreferences",
        displayName = "#Options_DisplayName_PLOptions",
        keywords = "#Options_Keywords_PLOptions",
        keywordsCategory = "Remote Data Sources",
        id = "PL",
        position = 12)
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_PLOptions=Product Library",
        "Options_Keywords_PLOptions=product library, remote, data, sources, credentials"
})
public class RepositoriesCredentialsControllerUI extends DefaultConfigController {

    private static final String SAVE_ERROR_MESSAGE = "Unable to save Remote Repositories Credentials to SNAP configuration file.";

    private static Logger logger = Logger.getLogger(RepositoriesCredentialsControllerUI.class.getName());
    private static ImageIcon addButtonIcon;
    private static ImageIcon removeButtonIcon;
    private static ImageIcon passwordSeeIcon;

    static {
        try {
            addButtonIcon = loadImageIcon("/tango/16x16/actions/list-add.png");
            removeButtonIcon = loadImageIcon("/tango/16x16/actions/list-remove.png");
            passwordSeeIcon = loadImageIcon("/org/esa/snap/rcp/icons/quicklook16.png");
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Unable to load image resource. Details: " + ex.getMessage());
        }
    }

    private final JTable repositoriesListTable;
    private final JTable credentialsListTable;
    private final List<RemoteProductsRepositoryProvider> remoteRepositories = new ArrayList<>();
    private JPanel credentialsListPanel;
    private RepositoriesCredentialsBean repositoriesCredentialsBean = new RepositoriesCredentialsBean();
    private List<RepositoryCredentials> repositoriesCredentials;
    private boolean isInitialized = false;

    public RepositoriesCredentialsControllerUI() {
        RepositoriesCredentialsController repositoriesCredentialsController = RepositoriesCredentialsController.getInstance();
        this.repositoriesCredentials = createCopy(repositoriesCredentialsController.getRepositoriesCredentials());
        loadRemoteRepositories();
        repositoriesListTable = buildRepositoriesListTable();
        credentialsListTable = buildCredentialsListTable();
    }

    public static ImageIcon getPasswordSeeIcon() {
        return passwordSeeIcon;
    }

    private static ImageIcon loadImageIcon(String imagePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL imageURL = classLoader.getResource(imagePath);
        return (imageURL == null) ? null : new ImageIcon(imageURL);
    }

    private static List<RepositoryCredentials> createCopy(List<RepositoryCredentials> repositoriesCredentialsSource) {
        List<RepositoryCredentials> repositoriesCredentialsCopy = new ArrayList<>();
        for (RepositoryCredentials repositoryCredentialsSource : repositoriesCredentialsSource) {
            List<Credentials> credentialsCopy = new ArrayList<>();
            for (Credentials credentialsSource : repositoryCredentialsSource.getCredentialsList()) {
                credentialsCopy.add(new RepositoryCredential(credentialsSource));
            }
            repositoriesCredentialsCopy.add(new RepositoryCredentials(repositoryCredentialsSource.getRepositoryId(), credentialsCopy));
        }
        return repositoriesCredentialsCopy;
    }

    private void loadRemoteRepositories() {
        ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        ServiceRegistry<RemoteProductsRepositoryProvider> serviceRegistry = serviceRegistryManager.getServiceRegistry(RemoteProductsRepositoryProvider.class);
        ServiceLoader.loadServices(serviceRegistry);
        Set<RemoteProductsRepositoryProvider> repositoryProductsProviders = serviceRegistry.getServices();

        for (RemoteProductsRepositoryProvider repositoryProductsProvider : repositoryProductsProviders) {
            if (repositoryProductsProvider.requiresAuthentication()) {
                remoteRepositories.add(repositoryProductsProvider);
            }
        }
    }

    private List<Credentials> getRemoteRepositoryCredentials(String remoteRepositoryId) {
        for (RepositoryCredentials repositoryCredentials : repositoriesCredentials) {
            if (repositoryCredentials.getRepositoryId().contentEquals(remoteRepositoryId)) {
                return repositoryCredentials.getCredentialsList();
            }
        }
        return null;
    }

    private void cleanupRemoteRepositories() {
        for (RepositoryCredentials repositoryCredentials : repositoriesCredentials) {
            if (repositoryCredentials.getCredentialsList().isEmpty()) {
                repositoriesCredentials.remove(repositoryCredentials);
                break;
            }
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
        return createPropertySet(repositoriesCredentialsBean);
    }

    /**
     * Create a panel that allows the user to set the parameters in the given {@link BindingContext}. Clients that want to create their own panel representation on the given properties need to overwrite this method.
     *
     * @param context The {@link BindingContext} for the panel.
     * @return A JPanel instance for the given {@link BindingContext}, never {@code null}.
     */
    @Override
    protected JPanel createPanel(BindingContext context) {
        JPanel remoteFileRepositoriesTabUI = buildRemoteRepositoriesTabUI();
        SwingUtilities.invokeLater(() -> repositoriesListTable.changeSelection(0, RepositoriesTableModel.REPO_NAME_COLUMN, false, false));
        isInitialized = true;
        return remoteFileRepositoriesTabUI;
    }

    /**
     * Updates the UI.
     */
    @Override
    public void update() {
        if (isInitialized) {
            SwingUtilities.invokeLater(() -> repositoriesListTable.changeSelection(0, RepositoriesTableModel.REPO_NAME_COLUMN, false, false));
        }
    }

    /**
     * Saves the changes.
     */
    @Override
    public void applyChanges() {
        if (isChanged()) {
            try {
                RepositoriesCredentialsController repositoriesCredentialsController = RepositoriesCredentialsController.getInstance();
                repositoriesCredentialsController.saveCredentials(createCopy(repositoriesCredentials));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, SAVE_ERROR_MESSAGE + " Details: " + ex.getMessage(), ex);
                JOptionPane.showMessageDialog(credentialsListPanel, SAVE_ERROR_MESSAGE, "Error saving remote repositories credentials", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Cancels the changes.
     */
    @Override
    public void cancel() {
        RepositoriesCredentialsController repositoriesCredentialsController = RepositoriesCredentialsController.getInstance();
        this.repositoriesCredentials = createCopy(repositoriesCredentialsController.getRepositoriesCredentials());
    }

    /**
     * Check whether options changes.
     *
     * @return {@code true} if options is changed
     */
    @Override
    public boolean isChanged() {
        if (repositoriesListTable.getSelectedRow() >= 0) {
            RepositoriesCredentialsController repositoriesCredentialsController = RepositoriesCredentialsController.getInstance();
            List<RepositoryCredentials> savedRepositoriesCredentials = repositoriesCredentialsController.getRepositoriesCredentials();
            if (savedRepositoriesCredentials.size() != repositoriesCredentials.size()) {
                return true;
            }
            for (RepositoryCredentials savedRepositoryCredentials : savedRepositoriesCredentials) {
                boolean repositoryChanged = true;
                for (RepositoryCredentials repositoryCredentials : repositoriesCredentials) {
                    if (savedRepositoryCredentials.getRepositoryId().equals(repositoryCredentials.getRepositoryId()) && savedRepositoryCredentials.getCredentialsList().size() == repositoryCredentials.getCredentialsList().size()) {
                        for (Credentials savedCredentials : savedRepositoryCredentials.getCredentialsList()) {
                            boolean credentialChanged = true;
                            for (Credentials credentials : repositoryCredentials.getCredentialsList()) {
                                String savedUsername = savedCredentials.getUserPrincipal().getName();
                                String username = credentials.getUserPrincipal().getName();
                                String savedPassword = savedCredentials.getPassword();
                                String password = credentials.getPassword();
                                if (savedUsername.contentEquals(username) && savedPassword.contentEquals(password)) {
                                    credentialChanged = false;
                                    break;
                                }
                            }
                            if (credentialChanged) {
                                return true;
                            }
                        }
                        repositoryChanged = false;
                        break;
                    }
                }
                if (repositoryChanged) {
                    return true;
                }
            }
        }
        return false;
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
     * Creates and gets the remote repositories table.
     *
     * @return The remote repositories table
     */
    private JTable buildRepositoriesListTable() {
        JTable newRepositoriesListTable = new JTable();
        RepositoriesTableModel repositoriesTableModel = new RepositoriesTableModel(this.remoteRepositories);
        newRepositoriesListTable.setModel(repositoriesTableModel);
        newRepositoriesListTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        newRepositoriesListTable.getSelectionModel().addListSelectionListener(event -> loadCredentialsOnTable());
        return newRepositoriesListTable;
    }

    /**
     * Creates and gets the panel with remote repositories table.
     *
     * @return The panel with add and remove buttons panel and remote file repositories table
     */
    private JScrollPane buildRepositoriesListPanel() {
        JScrollPane remoteRepositoriesListSP = new JScrollPane();
        remoteRepositoriesListSP.setViewportView(repositoriesListTable);
        remoteRepositoriesListSP.setBorder(BorderFactory.createTitledBorder("Remote Repositories (Data Sources) List"));
        remoteRepositoriesListSP.setLayout(new ScrollPaneLayout());
        remoteRepositoriesListSP.setAutoscrolls(false);
        return remoteRepositoriesListSP;
    }

    /**
     * Runs the event associated with button for adding remote repository credential.
     */
    private void runAddCredentialEvent() {
        RepositoriesTableModel repositoriesTableModel = (RepositoriesTableModel) repositoriesListTable.getModel();
        String remoteRepositoryId = repositoriesTableModel.get(repositoriesListTable.getSelectedRow()).getRepositoryId();
        List<Credentials> repositoryCredentials = getRemoteRepositoryCredentials(remoteRepositoryId);
        if (repositoryCredentials == null) {
            // the repository not exists, so it wil be created with empty Credentials list
            repositoryCredentials = new ArrayList<>();
            repositoriesCredentials.add(new RepositoryCredentials(remoteRepositoryId, repositoryCredentials));
        }
        RepositoriesCredentialsTableModel repositoriesCredentialsTableModel = (RepositoriesCredentialsTableModel) credentialsListTable.getModel();
        Credentials newCredential = new RepositoryCredential("", "");
        if (repositoriesCredentialsTableModel.add(newCredential)) {
            repositoryCredentials.add(newCredential);
        }
    }

    /**
     * Creates and gets the button for adding remote repository credential.
     *
     * @return The button for adding remote repository credential
     */
    private JButton buildAddCredentialButton() {
        JButton addCredentialButton = new JButton(addButtonIcon);
        addCredentialButton.setPreferredSize(new Dimension(20, 20));
        addCredentialButton.addActionListener(e -> runAddCredentialEvent());
        return addCredentialButton;
    }

    /**
     * Runs the event associated with button for removing remote repository credential.
     */
    private void runRemoveCredentialEvent() {
        try {
            if (credentialsListTable.getSelectedRow() >= 0) {
                JTextField textField = (JTextField) credentialsListTable.getValueAt(credentialsListTable.getSelectedRow(), RepositoriesCredentialsTableModel.REPO_CRED_USER_COLUMN);
                String credentialUsername = textField.getText();
                if (JOptionPane.showConfirmDialog(credentialsListTable, "Are you sure to delete the following repository credential?\n" + credentialUsername, "Delete repository credential confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    RepositoriesCredentialsTableModel repositoriesCredentialsTableModel = (RepositoriesCredentialsTableModel) credentialsListTable.getModel();
                    RepositoriesTableModel repositoriesTableModel = (RepositoriesTableModel) repositoriesListTable.getModel();
                    String remoteRepositoryId = repositoriesTableModel.get(repositoriesListTable.getSelectedRow()).getRepositoryId();
                    List<Credentials> repositoryCredentials = getRemoteRepositoryCredentials(remoteRepositoryId);
                    if (repositoryCredentials != null) {
                        repositoryCredentials.remove(repositoriesCredentialsTableModel.get(credentialsListTable.getSelectedRow()));
                        repositoriesCredentialsTableModel.remove(credentialsListTable.getSelectedRow());
                        cleanupRemoteRepositories();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(credentialsListTable, "Please select a repository credential from list.", "Delete repository credential", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            logger.log(Level.SEVERE, "Unable to delete remote repository credential. Details: " + ex.getMessage());
            JOptionPane.showMessageDialog(credentialsListPanel, "Failed to delete remote repository credential.\nreason: " + ex, "Delete remote repository credential", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates and gets the button for removing remote repository credential.
     *
     * @return The button for removing remote repository credential
     */
    private JButton buildRemoveCredentialButton() {
        JButton removeCredentialButton = new JButton(removeButtonIcon);
        removeCredentialButton.setPreferredSize(new Dimension(20, 20));
        removeCredentialButton.addActionListener(e -> runRemoveCredentialEvent());
        return removeCredentialButton;
    }

    private DefaultTableCellRenderer buildTextCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JTextField textField = (JTextField) value;
                if (isSelected) {
                    textField.setForeground(table.getSelectionForeground());
                    textField.setBackground(table.getSelectionBackground());
                } else {
                    textField.setForeground(table.getForeground());
                    textField.setBackground(table.getBackground());
                }
                return textField;
            }
        };
    }

    private DefaultTableCellRenderer buildButtonCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                return (JButton) value;
            }
        };
    }

    private DefaultCellEditor buildTextCellEditor(JTextField textField) {
        DefaultCellEditor textCellEditor = new DefaultCellEditor(textField) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                JTextField textField = (JTextField) value;
                textField.setForeground(table.getSelectionForeground());
                textField.setBackground(table.getSelectionBackground());
                return textField;
            }

            @Override
            public boolean stopCellEditing() {
                fireEditingStopped();
                return true;
            }
        };
        textCellEditor.setClickCountToStart(1);
        return textCellEditor;
    }

    private DefaultCellEditor buildButtonCellEditor() {
        DefaultCellEditor buttonCellEditor = new DefaultCellEditor(new JTextField()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                return (JButton) value;
            }

            @Override
            public boolean stopCellEditing() {
                fireEditingStopped();
                return true;
            }
        };
        buttonCellEditor.setClickCountToStart(1);
        return buttonCellEditor;
    }

    /**
     * Creates and gets the remote repository credentials table.
     *
     * @return The remote repository credentials table
     */
    private JTable buildCredentialsListTable() {
        JTable newCredentialsListTable = new JTable();
        RepositoriesCredentialsTableModel repositoriesCredentialsTableModel = new RepositoriesCredentialsTableModel(new ArrayList<>());
        newCredentialsListTable.setModel(repositoriesCredentialsTableModel);
        newCredentialsListTable.setDefaultRenderer(JTextField.class, buildTextCellRenderer());
        newCredentialsListTable.setDefaultRenderer(JPasswordField.class, buildTextCellRenderer());
        newCredentialsListTable.setDefaultRenderer(JButton.class, buildButtonCellRenderer());
        newCredentialsListTable.setDefaultEditor(JTextField.class, buildTextCellEditor(new JTextField()));
        newCredentialsListTable.setDefaultEditor(JPasswordField.class, buildTextCellEditor(new JPasswordField()));
        newCredentialsListTable.setDefaultEditor(JButton.class, buildButtonCellEditor());
        newCredentialsListTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        newCredentialsListTable.getColumnModel().getColumn(RepositoriesCredentialsTableModel.REPO_CRED_PASS_SEE_COLUMN).setMaxWidth(20);
        newCredentialsListTable.setRowHeight(new JTextField().getPreferredSize().height);
        return newCredentialsListTable;
    }

    /**
     * Creates and gets the panel with add and remove buttons for remote repository credentials table.
     *
     * @return The panel with add and remove buttons
     */
    private JPanel buildCredentialsListActionsPanel() {
        JPanel credentialsListActionsPanel = new JPanel();
        credentialsListActionsPanel.setLayout(new BoxLayout(credentialsListActionsPanel, BoxLayout.PAGE_AXIS));
        credentialsListActionsPanel.add(buildAddCredentialButton());
        credentialsListActionsPanel.add(buildRemoveCredentialButton());
        credentialsListActionsPanel.add(Box.createVerticalGlue());
        return credentialsListActionsPanel;
    }

    /**
     * Creates and gets the panel with add and remove buttons panel and remote repository credentials table.
     *
     * @return The panel with add and remove buttons panel and remote repository credentials table
     */
    private JPanel buildCredentialsListPanel() {
        JScrollPane credentialsListPanelSP = new JScrollPane();
        credentialsListPanelSP.setViewportView(credentialsListTable);

        credentialsListPanel = new JPanel();
        credentialsListPanel.setBorder(BorderFactory.createTitledBorder("Credentials List"));
        credentialsListPanel.setLayout(new BoxLayout(credentialsListPanel, BoxLayout.LINE_AXIS));
        credentialsListPanel.add(credentialsListPanelSP);
        credentialsListPanel.add(buildCredentialsListActionsPanel());
        return credentialsListPanel;
    }

    /**
     * Creates and gets the panel with remote repositories table and remote repository credentials table.
     *
     * @return The panel with remote repositories table and remote repository credentials table
     */
    private JSplitPane buildRemoteRepositoriesPanel() {
        JSplitPane remoteRepositoriesPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        remoteRepositoriesPanel.setOneTouchExpandable(false);
        remoteRepositoriesPanel.setContinuousLayout(true);
        remoteRepositoriesPanel.setDividerSize(5);
        remoteRepositoriesPanel.setDividerLocation(0.5);
        JScrollPane remoteRepositoriesListPane = buildRepositoriesListPanel();
        JPanel remoteRepositoryCredentialsListPane = buildCredentialsListPanel();
        Dimension minimumSize = new Dimension(350, 410);
        remoteRepositoriesListPane.setMinimumSize(minimumSize);
        remoteRepositoryCredentialsListPane.setMinimumSize(minimumSize);
        remoteRepositoryCredentialsListPane.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                //nothing
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                //nothing
            }

            @Override
            public void componentShown(ComponentEvent e) {
                remoteRepositoriesPanel.setDividerLocation(0.5);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                //nothing
            }
        });
        remoteRepositoriesPanel.setLeftComponent(remoteRepositoriesListPane);
        remoteRepositoriesPanel.setRightComponent(remoteRepositoryCredentialsListPane);
        return remoteRepositoriesPanel;
    }

    /**
     * Creates and gets the root panel for remote repositories tab ui.
     *
     * @return The root panel for remote repositories tab ui
     */
    private JPanel buildRemoteRepositoriesTabUI() {
        JPanel remoteRepositoriesTabUI = new JPanel(new BorderLayout());
        remoteRepositoriesTabUI.add(buildRemoteRepositoriesPanel());
        remoteRepositoriesTabUI.setPreferredSize(new Dimension(730, 410));
        return remoteRepositoriesTabUI;
    }

    /**
     * Loads the remote repository credentials usernames and passwords on remote repository credentials table
     */
    private void loadCredentialsOnTable() {
        int selectedRow = repositoriesListTable.getSelectedRow();
        if (selectedRow >= 0) {
            RepositoriesTableModel repositoriesTableModel = (RepositoriesTableModel) repositoriesListTable.getModel();
            String remoteRepositoryId = repositoriesTableModel.get(selectedRow).getRepositoryId();
            List<Credentials> repositoryCredentials = getRemoteRepositoryCredentials(remoteRepositoryId);
            RepositoriesCredentialsTableModel repositoriesCredentialsTableModel = (RepositoriesCredentialsTableModel) credentialsListTable.getModel();
            if (credentialsListTable.getSelectedColumn() >= 0) {
                credentialsListTable.getDefaultEditor(credentialsListTable.getColumnClass(credentialsListTable.getSelectedColumn())).stopCellEditing();
            }
            repositoriesCredentialsTableModel.setData(repositoryCredentials);
        }
    }

    /**
     * The bean with fields annoted with {@link Preference} for VFS Options.
     */
    static class RepositoriesCredentialsBean {
        @Preference(label = "S", key = "s")
        String s;
    }

}
