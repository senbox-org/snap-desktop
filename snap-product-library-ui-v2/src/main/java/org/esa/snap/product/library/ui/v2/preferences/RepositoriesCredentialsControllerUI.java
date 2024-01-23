package org.esa.snap.product.library.ui.v2.preferences;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.product.library.v2.preferences.RepositoriesCredentialsController;
import org.esa.snap.product.library.v2.preferences.RepositoriesCredentialsPersistence;
import org.esa.snap.product.library.v2.preferences.model.RemoteRepositoryCredentials;
import org.esa.snap.product.library.v2.preferences.model.RepositoriesCredentialsConfigurations;
import org.esa.snap.product.library.ui.v2.preferences.model.RepositoriesCredentialsTableModel;
import org.esa.snap.product.library.ui.v2.preferences.model.RepositoriesTableModel;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.ui.AppContext;
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

    public static final String REMOTE_PRODUCTS_REPOSITORY_CREDENTIALS = "remoteProductsRepositoryCredentials";
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
    private JRadioButton autoUncompressEnabled;
    private JRadioButton downloadAllPagesEnabled;
    private JComboBox<Integer> recordsOnPageCb;
    private RepositoriesCredentialsBean repositoriesCredentialsBean = new RepositoriesCredentialsBean();
    private List<RemoteRepositoryCredentials> repositoriesCredentials;
    private boolean autoUncompress;
    private boolean downloadAllPages;
    private int nrRecordsOnPage;
    private boolean isInitialized = false;
    private int currentSelectedRow = -1;

    public RepositoriesCredentialsControllerUI() {
        RepositoriesCredentialsController repositoriesCredentialsController = RepositoriesCredentialsController.getInstance();
        this.repositoriesCredentials = createCopy(repositoriesCredentialsController.getRepositoriesCredentials());
        this.autoUncompress = repositoriesCredentialsController.isAutoUncompress();
        this.downloadAllPages = repositoriesCredentialsController.downloadsAllPages();
        this.nrRecordsOnPage = repositoriesCredentialsController.getNrRecordsOnPage();
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

    private static List<RemoteRepositoryCredentials> createCopy(List<RemoteRepositoryCredentials> repositoriesCredentialsSource) {
        List<RemoteRepositoryCredentials> repositoriesCredentialsCopy = new ArrayList<>();
        for (RemoteRepositoryCredentials repositoryCredentialsSource : repositoriesCredentialsSource) {
            List<Credentials> credentialsCopy = new ArrayList<>();
            for (Credentials credentialsSource : repositoryCredentialsSource.getCredentialsList()) {
                UsernamePasswordCredentials repositoryCredential = new UsernamePasswordCredentials(credentialsSource.getUserPrincipal().getName(), credentialsSource.getPassword());
                credentialsCopy.add(repositoryCredential);
            }
            repositoriesCredentialsCopy.add(new RemoteRepositoryCredentials(repositoryCredentialsSource.getRepositoryName(), credentialsCopy));
        }
        return repositoriesCredentialsCopy;
    }

    private static boolean isRepositoriesChanged(List<RemoteRepositoryCredentials> savedRepositoriesCredentials, List<RemoteRepositoryCredentials> repositoriesCredentials) {
        if (repositoriesCredentials.size() != savedRepositoriesCredentials.size()) {
            return true;
        }
        for (RemoteRepositoryCredentials repositoryCredentials : repositoriesCredentials) {
            for (RemoteRepositoryCredentials savedRepositoryCredentials : savedRepositoriesCredentials) {
                if (repositoryCredentials.getRepositoryName().contentEquals(savedRepositoryCredentials.getRepositoryName()) && isRepositoryChanged(savedRepositoryCredentials, repositoryCredentials)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRepositoryChanged(RemoteRepositoryCredentials savedRepositoryCredentials, RemoteRepositoryCredentials repositoryCredentials) {
        List<Credentials> credentials = repositoryCredentials.getCredentialsList();
        if (credentials.size() != savedRepositoryCredentials.getCredentialsList().size()) {
            // some credentials deleted or added = changed
            return true;
        }
        for (Credentials credential : credentials) {
            if (!savedRepositoryCredentials.credentialExists(credential)) {
                return true; // the credential was not found = changed
            }
        }
        return false;
    }

    private void loadRemoteRepositories() {
        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = RemoteProductsRepositoryProvider.getRemoteProductsRepositoryProviders();
        for (RemoteProductsRepositoryProvider remoteRepositoryProductProvider : remoteRepositoryProductProviders) {
            if (remoteRepositoryProductProvider.requiresAuthentication()) {
                this.remoteRepositories.add(remoteRepositoryProductProvider);
            }
        }
    }

    private List<RemoteRepositoryCredentials> getChangedRemoteRepositories() {
        List<RemoteRepositoryCredentials> changedRepositoriesCredentials = new ArrayList<>();

        RepositoriesTableModel repositoriesTableModel = (RepositoriesTableModel) this.repositoriesListTable.getModel();
        RepositoriesCredentialsTableModel repositoriesCredentialsTableModel = (RepositoriesCredentialsTableModel) this.credentialsListTable.getModel();
        String selectedRemoteRepositoryName = repositoriesTableModel.get(this.currentSelectedRow).getRepositoryName();
        List<Credentials> selectedRepositoryCredentials = repositoriesCredentialsTableModel.fetchData();
        RemoteRepositoryCredentials repositoryCredentialsFromTable = new RemoteRepositoryCredentials(selectedRemoteRepositoryName, selectedRepositoryCredentials);

        for (RemoteRepositoryCredentials repositoryCredentials : this.repositoriesCredentials) {
            if (!repositoryCredentials.getRepositoryName().contentEquals(repositoryCredentialsFromTable.getRepositoryName())) {
                changedRepositoriesCredentials.add(repositoryCredentials);
            }
        }
        if (!repositoryCredentialsFromTable.getCredentialsList().isEmpty()) {
            changedRepositoriesCredentials.add(repositoryCredentialsFromTable);
        }
        return changedRepositoriesCredentials;
    }

    private boolean getChangedAutoUncompress() {
        return autoUncompressEnabled.isSelected();
    }

    private boolean getChangedDownloadAllPagesEnabled() {
        return downloadAllPagesEnabled.isSelected();
    }

    private int getChangedNrRecordsOnPage() {
        return recordsOnPageCb.getItemAt(recordsOnPageCb.getSelectedIndex());
    }

    private List<Credentials> getRemoteRepositoryCredentials(String remoteRepositoryId) {
        for (RemoteRepositoryCredentials repositoryCredentials : repositoriesCredentials) {
            if (repositoryCredentials.getRepositoryName().contentEquals(remoteRepositoryId)) {
                return repositoryCredentials.getCredentialsList();
            }
        }
        return new ArrayList<>();
    }

    private void cleanupRemoteRepositories() {
        for (RemoteRepositoryCredentials repositoryCredentials : repositoriesCredentials) {
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
            if (repositoriesListTable.getSelectedRow() < 0) {
                SwingUtilities.invokeLater(() -> repositoriesListTable.changeSelection(0, RepositoriesTableModel.REPO_NAME_COLUMN, false, false));
            } else {
                refreshCredentialsTable();
                refreshSearchResultsConfigurations();
            }
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
                List<RemoteRepositoryCredentials> changedRepositoriesCredentials = getChangedRemoteRepositories();
                boolean changedAutoUncompress = getChangedAutoUncompress();
                boolean changedDownloadAllPagesEnabled = getChangedDownloadAllPagesEnabled();
                int changedNrRecordsOnPage = getChangedNrRecordsOnPage();
                repositoriesCredentialsController.saveConfigurations(new RepositoriesCredentialsConfigurations(createCopy(changedRepositoriesCredentials), repositoriesCredentialsController.getRepositoriesCollectionsCredentials(), changedAutoUncompress, changedDownloadAllPagesEnabled, changedNrRecordsOnPage));
                this.autoUncompress = changedAutoUncompress;
                this.downloadAllPages = changedDownloadAllPagesEnabled;
                this.nrRecordsOnPage = changedNrRecordsOnPage;
                AppContext appContext = SnapApp.getDefault().getAppContext();
                SwingUtilities.invokeLater(() -> appContext.getApplicationWindow().firePropertyChange(REMOTE_PRODUCTS_REPOSITORY_CREDENTIALS, 1, 2));
            } catch (Exception ex) {
                String title = "Error saving remote repositories credentials";
                String msg = "Unable to save Remote Repositories Credentials to SNAP configuration file." + " Details: " + ex.getMessage();
                logger.log(Level.SEVERE, msg, ex);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(credentialsListPanel, msg, title, JOptionPane.ERROR_MESSAGE));
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
        this.currentSelectedRow = -1;
        this.autoUncompress = repositoriesCredentialsController.isAutoUncompress();
        this.downloadAllPages = repositoriesCredentialsController.downloadsAllPages();
        this.nrRecordsOnPage = repositoriesCredentialsController.getNrRecordsOnPage();
    }

    /**
     * Check whether options changes.
     *
     * @return {@code true} if options is changed
     */
    @Override
    public boolean isChanged() {
        boolean changed = false;
        RepositoriesCredentialsController repositoriesCredentialsController = RepositoriesCredentialsController.getInstance();
        if (repositoriesListTable.getSelectedRow() >= 0) {

            List<RemoteRepositoryCredentials> savedRepositoriesCredentials = repositoriesCredentialsController.getRepositoriesCredentials();

            List<RemoteRepositoryCredentials> changedRepositoriesCredentials = getChangedRemoteRepositories();

            if (RepositoriesCredentialsPersistence.validCredentials(changedRepositoriesCredentials)) {
                changed = isRepositoriesChanged(savedRepositoriesCredentials, changedRepositoriesCredentials);
            }
        }
        boolean savedAutoUncompress = repositoriesCredentialsController.isAutoUncompress();
        boolean savedDownloadAllPages = repositoriesCredentialsController.downloadsAllPages();
        int savedRecordsOnPage = repositoriesCredentialsController.getNrRecordsOnPage();
        return changed || savedAutoUncompress != getChangedAutoUncompress() || savedDownloadAllPages != getChangedDownloadAllPagesEnabled() || savedRecordsOnPage != getChangedNrRecordsOnPage();
    }

    /**
     * Gets the Help Context for this Options Controller
     *
     * @return The Help Context
     */
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("productLibraryToolV2");
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
        newRepositoriesListTable.getSelectionModel().addListSelectionListener(event -> refreshCredentialsTable());
        newRepositoriesListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        RepositoriesCredentialsTableModel repositoriesCredentialsTableModel = (RepositoriesCredentialsTableModel) credentialsListTable.getModel();
        Credentials newCredential = new UsernamePasswordCredentials("", "");
        repositoriesCredentialsTableModel.add(newCredential);
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
        credentialsListTable.getDefaultEditor(JTextField.class).stopCellEditing();
        int selectedRowIndex = credentialsListTable.getSelectedRow();
        if (selectedRowIndex >= 0) {
            RepositoriesCredentialsTableModel repositoriesCredentialsTableModel = (RepositoriesCredentialsTableModel) credentialsListTable.getModel();
            repositoriesCredentialsTableModel.remove(selectedRowIndex);
        } else {
            String title = "Delete repository credential";
            String msg = "Please select a repository credential from list.";
            JOptionPane.showMessageDialog(credentialsListTable, msg, title, JOptionPane.WARNING_MESSAGE);
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

    private DefaultTableCellRenderer buildButtonCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                return (JButton) value;
            }
        };
    }

    private DefaultCellEditor buildTextCellEditor(RepositoriesCredentialsTableModel repositoriesCredentialsTableModel) {
        DefaultCellEditor textCellEditor = new DefaultCellEditor(new JTextField()) {
            private JTextField textField;
            private int row;
            private int column;

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                Credentials credentials = repositoriesCredentialsTableModel.get(row);
                if (column < 1) {
                    textField = new JTextField(credentials.getUserPrincipal().getName());
                } else {
                    textField = new JPasswordField(credentials.getPassword());
                    ((JPasswordField) textField).setEchoChar('\u25cf');
                }
                this.row = row;
                this.column = column;
                return textField;
            }

            @Override
            public boolean stopCellEditing() {
                repositoriesCredentialsTableModel.updateCellData(this.row, this.column, this.textField.getText());
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
        RepositoriesCredentialsTableModel repositoriesCredentialsTableModel = new RepositoriesCredentialsTableModel();
        newCredentialsListTable.setModel(repositoriesCredentialsTableModel);
        newCredentialsListTable.setDefaultRenderer(JButton.class, buildButtonCellRenderer());
        newCredentialsListTable.setDefaultEditor(JTextField.class, buildTextCellEditor(repositoriesCredentialsTableModel));
        newCredentialsListTable.setDefaultEditor(JPasswordField.class, buildTextCellEditor(repositoriesCredentialsTableModel));
        newCredentialsListTable.setDefaultEditor(JButton.class, buildButtonCellEditor());
        newCredentialsListTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        newCredentialsListTable.getColumnModel().getColumn(RepositoriesCredentialsTableModel.REPO_CRED_PASS_SEE_COLUMN).setMaxWidth(20);
        newCredentialsListTable.setRowHeight(new JTextField().getPreferredSize().height);
        newCredentialsListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        Dimension minimumSize = new Dimension(350, 350);
        remoteRepositoriesListPane.setPreferredSize(minimumSize);
        remoteRepositoryCredentialsListPane.setPreferredSize(minimumSize);
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

    private JPanel buildAutoUncompressPanel() {
        JPanel autoUncompressPanel = new JPanel();
        autoUncompressPanel.setLayout(new BoxLayout(autoUncompressPanel, BoxLayout.LINE_AXIS));
        autoUncompressEnabled = new JRadioButton("Yes");
        JRadioButton autoUncompressDisabled = new JRadioButton("No", true);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(autoUncompressEnabled);
        buttonGroup.add(autoUncompressDisabled);
        autoUncompressEnabled.setSelected(this.autoUncompress);
        JPanel autoUncompressRBsPanel = new JPanel();
        autoUncompressRBsPanel.setLayout(new BoxLayout(autoUncompressRBsPanel, BoxLayout.LINE_AXIS));
        autoUncompressRBsPanel.add(autoUncompressEnabled);
        autoUncompressRBsPanel.add(autoUncompressDisabled);
        autoUncompressPanel.add(new JLabel("Auto uncompress downloaded archive products:", SwingConstants.LEFT));
        autoUncompressPanel.add(autoUncompressRBsPanel);
        autoUncompressPanel.add(Box.createHorizontalGlue());
        return autoUncompressPanel;
    }

    private JPanel buildDownloadAllPagesPanel() {
        JPanel downloadAllPagesPanel = new JPanel();
        downloadAllPagesPanel.setLayout(new BoxLayout(downloadAllPagesPanel, BoxLayout.LINE_AXIS));
        downloadAllPagesEnabled = new JRadioButton("Yes");
        JRadioButton downloadAllPagesDisabled = new JRadioButton("No", true);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(downloadAllPagesEnabled);
        buttonGroup.add(downloadAllPagesDisabled);
        downloadAllPagesEnabled.setSelected(this.downloadAllPages);
        JPanel downloadAllPagesRBsPanel = new JPanel();
        downloadAllPagesRBsPanel.setLayout(new BoxLayout(downloadAllPagesRBsPanel, BoxLayout.LINE_AXIS));
        downloadAllPagesRBsPanel.add(downloadAllPagesEnabled);
        downloadAllPagesRBsPanel.add(downloadAllPagesDisabled);
        downloadAllPagesPanel.add(new JLabel("Download all pages of result search:", SwingConstants.LEFT));
        downloadAllPagesPanel.add(downloadAllPagesRBsPanel);
        downloadAllPagesPanel.add(Box.createHorizontalGlue());
        return downloadAllPagesPanel;
    }

    private JPanel buildRecordsOnPagePanel() {
        JPanel recordsOnPagePanel = new JPanel();
        recordsOnPagePanel.setLayout(new BoxLayout(recordsOnPagePanel, BoxLayout.LINE_AXIS));
        recordsOnPageCb = new JComboBox<>(new Integer[]{10, 20, 30, 40, 50});
        recordsOnPageCb.setMaximumSize(new Dimension(20, 50));
        recordsOnPageCb.setSelectedItem(this.nrRecordsOnPage);
        recordsOnPagePanel.add(new JLabel("Number of records on search result page:", SwingConstants.LEFT));
        recordsOnPagePanel.add(recordsOnPageCb);
        recordsOnPagePanel.add(Box.createHorizontalGlue());
        return recordsOnPagePanel;
    }

    private JPanel buildExtraConfigurationsPanel() {
        JPanel extraConfigurationsPanel = new JPanel();
        extraConfigurationsPanel.setBorder(BorderFactory.createTitledBorder("Other options"));
        extraConfigurationsPanel.setLayout(new BoxLayout(extraConfigurationsPanel, BoxLayout.PAGE_AXIS));
        extraConfigurationsPanel.add(buildAutoUncompressPanel());
        extraConfigurationsPanel.add(Box.createHorizontalGlue());
        extraConfigurationsPanel.add(buildDownloadAllPagesPanel());
        extraConfigurationsPanel.add(Box.createHorizontalGlue());
        extraConfigurationsPanel.add(buildRecordsOnPagePanel());
        extraConfigurationsPanel.add(Box.createHorizontalGlue());
        return extraConfigurationsPanel;
    }

    /**
     * Creates and gets the root panel for remote repositories tab ui.
     *
     * @return The root panel for remote repositories tab ui
     */
    private JPanel buildRemoteRepositoriesTabUI() {
        JPanel remoteRepositoriesTabUI = new JPanel(new BorderLayout());
        remoteRepositoriesTabUI.add(buildRemoteRepositoriesPanel(), BorderLayout.CENTER);
        remoteRepositoriesTabUI.add(buildExtraConfigurationsPanel(), BorderLayout.PAGE_END);
        return remoteRepositoriesTabUI;
    }

    /**
     * Loads the remote repository credentials usernames and passwords from/on remote repository credentials table
     */
    private void refreshCredentialsTable() {
        RepositoriesTableModel repositoriesTableModel = (RepositoriesTableModel) repositoriesListTable.getModel();
        RepositoriesCredentialsTableModel repositoriesCredentialsTableModel = (RepositoriesCredentialsTableModel) credentialsListTable.getModel();
        if (credentialsListTable.getSelectedColumn() >= 0) {
            credentialsListTable.getDefaultEditor(credentialsListTable.getColumnClass(credentialsListTable.getSelectedColumn())).stopCellEditing();
        }
        String remoteRepositoryName;
        List<Credentials> repositoryCredentials;
        List<Credentials> repositoryCredentialsFromTable;
        if (this.currentSelectedRow >= 0) {
            remoteRepositoryName = repositoriesTableModel.get(this.currentSelectedRow).getRepositoryName();
            repositoryCredentials = getRemoteRepositoryCredentials(remoteRepositoryName);
            repositoryCredentialsFromTable = repositoriesCredentialsTableModel.fetchData();
            if (!repositoryCredentials.isEmpty()) {
                repositoryCredentials.clear();
                repositoryCredentials.addAll(repositoryCredentialsFromTable);
                cleanupRemoteRepositories();
            } else {
                if (!repositoryCredentialsFromTable.isEmpty()) {
                    this.repositoriesCredentials.add(new RemoteRepositoryCredentials(remoteRepositoryName, repositoryCredentialsFromTable));
                }
            }
        }
        int selectedRow = repositoriesListTable.getSelectedRow();
        if (selectedRow >= 0) {
            remoteRepositoryName = repositoriesTableModel.get(selectedRow).getRepositoryName();
            repositoryCredentials = getRemoteRepositoryCredentials(remoteRepositoryName);
            repositoriesCredentialsTableModel.setData(repositoryCredentials);
            this.currentSelectedRow = selectedRow;
        }
    }

    private void refreshSearchResultsConfigurations() {
        this.autoUncompressEnabled.setSelected(this.autoUncompress);
        this.downloadAllPagesEnabled.setSelected(this.downloadAllPages);
        this.recordsOnPageCb.setSelectedItem(this.nrRecordsOnPage);
    }

    /**
     * The bean with fields annoted with {@link Preference} for Product Library Remote Repositories Credentials Options.
     */
    static class RepositoriesCredentialsBean {
        @Preference(label = "S", key = "s")
        String s;
    }

}
