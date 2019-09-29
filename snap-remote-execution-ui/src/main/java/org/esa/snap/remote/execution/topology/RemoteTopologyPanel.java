package org.esa.snap.remote.execution.topology;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.local.folder.AbstractLocalSharedFolder;
import org.esa.snap.remote.execution.local.folder.IMountLocalSharedFolderCallback;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.IMessageDialog;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Created by jcoravu on 9/1/2019.
 */
public abstract class RemoteTopologyPanel extends JPanel {

    private final Window parentWindow;
    private final JTextField remoteSharedFolderPathTextField;
    private final JTextField remoteUsernameTextField;
    private final JPasswordField remotePasswordTextField;
    private final JTextField localSharedFolderPathTextField;
    private final JList<RemoteMachineProperties> remoteMachinesList;

    public RemoteTopologyPanel(Window parentWindow, Insets defaultTextFieldMargins, Insets defaultListItemMargins) {
        super(new GridBagLayout());

        this.parentWindow = parentWindow;

        this.remoteSharedFolderPathTextField = new JTextField();
        this.remoteSharedFolderPathTextField.setMargin(defaultTextFieldMargins);

        this.remoteUsernameTextField = new JTextField();
        this.remoteUsernameTextField.setMargin(defaultTextFieldMargins);

        this.remotePasswordTextField = new JPasswordField();
        this.remotePasswordTextField.setMargin(defaultTextFieldMargins);

        this.localSharedFolderPathTextField = new JTextField();
        this.localSharedFolderPathTextField.setMargin(defaultTextFieldMargins);

        this.remoteMachinesList = new JList<RemoteMachineProperties>(new DefaultListModel<RemoteMachineProperties>());
        this.remoteMachinesList.setVisibleRowCount(15);
        this.remoteMachinesList.setCellRenderer(new OperatingSystemLabelListCellRenderer(defaultListItemMargins));
    }

    public abstract String normalizePath(String path);

    public abstract void addComponents(int gapBetweenColumns, int gapBetweenRows, ActionListener browseLocalSharedFolderButtonListener, JPanel remoteMachinesButtonsPanel);

    public abstract void mountLocalSharedFolderAsync(IMessageDialog parentWindow, ILoadingIndicator loadingIndicator, int threadId, IMountLocalSharedFolderCallback callback);

    protected abstract AbstractLocalSharedFolder buildLocalSharedFolder();

    public final boolean hasChangedParameters(AbstractLocalSharedFolder oldLocalSharedFolder) {
        AbstractLocalSharedFolder newLocalSharedFolder = buildLocalSharedFolder();
        return oldLocalSharedFolder.hasChangedParameters(newLocalSharedFolder);
    }

    public JList<RemoteMachineProperties> getRemoteMachinesList() {
        return remoteMachinesList;
    }

    public JPasswordField getRemotePasswordTextField() {
        return remotePasswordTextField;
    }

    public JTextField getRemoteSharedFolderPathTextField() {
        return remoteSharedFolderPathTextField;
    }

    public JTextField getRemoteUsernameTextField() {
        return remoteUsernameTextField;
    }

    public RemoteTopology buildRemoteTopology() {
        String localSharedFolderPath = getLocalSharedFolderPath();
        String localPassword = null;
        JPasswordField localPasswordTextField = getLocalPasswordTextField();
        if (localPasswordTextField != null) {
            localPassword = new String(localPasswordTextField.getPassword());
        }

        RemoteTopology remoteTopology = new RemoteTopology(getRemoteSharedFolderPath(), getRemoteUsername(), getRemotePassword());
        remoteTopology.setLocalMachineData(localSharedFolderPath, localPassword);

        ListModel<RemoteMachineProperties> listModel = getRemoteMachinesList().getModel();
        for (int i=0; i<listModel.getSize(); i++) {
            remoteTopology.addRemoteMachine(listModel.getElementAt(i));
        }
        return remoteTopology;
    }

    protected boolean canMountLocalSharedFolder(IMessageDialog parentWindow, AbstractLocalSharedFolder localSharedFolder) {
        if (StringUtils.isBlank(localSharedFolder.getRemoteSharedFolderPath())) {
            parentWindow.showErrorDialog("The remote shared folder path is not specified.");
            getRemoteSharedFolderPathTextField().requestFocus();
            return false;
        }
        if (StringUtils.isBlank(localSharedFolder.getRemoteUsername())) {
            parentWindow.showErrorDialog("The remote username is not specified.");
            getRemoteUsernameTextField().requestFocus();
            return false;
        }
        if (StringUtils.isBlank(localSharedFolder.getRemotePassword())) {
            parentWindow.showErrorDialog("The remote password is not specified.");
            getRemotePasswordTextField().requestFocus();
            return false;
        }
        return true;
    }

    protected final String getRemoteSharedFolderPath() {
        return getRemoteSharedFolderPathTextField().getText();
    }

    protected final String getRemoteUsername() {
        return getRemoteUsernameTextField().getText();
    }

    protected final String getRemotePassword() {
        return new String(getRemotePasswordTextField().getPassword());
    }

    protected final String getLocalSharedFolderPath() {
        return getLocalSharedFolderPathTextField().getText();
    }

    protected void addRemoteSharedFolderPathRow(int rowIndex, int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(new JLabel("Remote shared folder path"), c);
        c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, gapBetweenRows, gapBetweenColumns);
        add(this.remoteSharedFolderPathTextField, c);
    }

    public JTextField getLocalSharedFolderPathTextField() {
        return this.localSharedFolderPathTextField;
    }

    public JPasswordField getLocalPasswordTextField() {
        return null;
    }

    public void setRemoteTopology(RemoteTopology remoteTopology) {
        this.remoteSharedFolderPathTextField.setText(normalizePath(remoteTopology.getRemoteSharedFolderURL()));
        this.remoteUsernameTextField.setText(remoteTopology.getRemoteUsername());
        this.remotePasswordTextField.setText(remoteTopology.getRemotePassword());
        this.localSharedFolderPathTextField.setText(normalizePath(remoteTopology.getLocalSharedFolderPath()));

        DefaultListModel<RemoteMachineProperties> model = (DefaultListModel<RemoteMachineProperties>) this.remoteMachinesList.getModel();
        model.removeAllElements();
        List<RemoteMachineProperties> remoteMachines = remoteTopology.getRemoteMachines();
        for (int i = 0; i < remoteMachines.size(); i++) {
            model.addElement(remoteMachines.get(i));
        }
    }

    protected void addUsernameRow(int rowIndex, int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(new JLabel("Remote username"), c);
        c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, gapBetweenRows, gapBetweenColumns);
        add(this.remoteUsernameTextField, c);
    }

    protected void addPasswordRow(int rowIndex, int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(new JLabel("Remote password"), c);
        c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, gapBetweenRows, gapBetweenColumns);
        add(this.remotePasswordTextField, c);
    }

    protected void addRemoteMachinesRow(int rowIndex, int gapBetweenColumns, int gapBetweenRows, JPanel remoteMachinesButtonsPanel) {
        GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows, 0);
        add(new JLabel("Remote machines"), c);

        c = SwingUtils.buildConstraints(2, rowIndex, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        add(remoteMachinesButtonsPanel, c);

        c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        add(new JScrollPane(this.remoteMachinesList), c);
    }

    protected int getTextFieldPreferredHeight() {
        return this.remoteSharedFolderPathTextField.getPreferredSize().height;
    }

    protected void addLocalSharedFolderRow(int rowIndex, int gapBetweenColumns, int gapBetweenRows, String labelText, ActionListener browseLocalSharedFolderButtonListener) {
        GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(new JLabel(labelText), c);

        int columnSpan = 2;
        if (browseLocalSharedFolderButtonListener != null) {
            columnSpan = 1;
            JButton browseButton = SwingUtils.buildBrowseButton(browseLocalSharedFolderButtonListener, getTextFieldPreferredHeight());
            c = SwingUtils.buildConstraints(2, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
            add(browseButton, c);
        }

        c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, columnSpan, 1, gapBetweenRows, gapBetweenColumns);
        add(this.localSharedFolderPathTextField, c);
    }

    private static JButton buildButton(String resourceImagePath, ActionListener buttonListener, Dimension buttonSize) {
        ImageIcon icon = UIUtils.loadImageIcon(resourceImagePath);
        JButton button = new JButton(icon);
        button.setFocusable(false);
        button.addActionListener(buttonListener);
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);
        return button;
    }

    public static JPanel buildVerticalButtonsPanel(ActionListener addButtonListener, ActionListener editButtonListener, ActionListener removeButtonListener,
                                                    int textFieldPreferredHeight, int gapBetweenRows) {

        Dimension buttonSize = new Dimension(textFieldPreferredHeight, textFieldPreferredHeight);

        JPanel verticalButtonsPanel = new JPanel();
        verticalButtonsPanel.setLayout(new BoxLayout(verticalButtonsPanel, BoxLayout.Y_AXIS));

        JButton addButton = buildButton("icons/Add16.png", addButtonListener, buttonSize);
        verticalButtonsPanel.add(addButton);

        if (editButtonListener != null) {
            verticalButtonsPanel.add(Box.createVerticalStrut(gapBetweenRows));

            JButton editButton = buildButton("icons/Edit16.gif", editButtonListener, buttonSize);
            verticalButtonsPanel.add(editButton);
        }

        verticalButtonsPanel.add(Box.createVerticalStrut(gapBetweenRows));

        JButton removeButton = buildButton("icons/Remove16.png", removeButtonListener, buttonSize);
        verticalButtonsPanel.add(removeButton);

        return verticalButtonsPanel;
    }
}
