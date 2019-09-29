package org.esa.snap.remote.execution.machines;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.ui.loading.AbstractModalDialog;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by jcoravu on 17/12/2018.
 */
public class EditRemoteMachineCredentialsDialog extends AbstractModalDialog {

    private final RemoteMachineProperties remoteMachineCredentialsToEdit;

    private JTextField hostNameTextField;
    private JTextField portNumberTextField;
    private JTextField usernameTextField;
    private JPasswordField passwordTextField;
    private JComboBox<String> operatingSystemsComboBox;
    private JTextField sharedFolderPathTextField;
    private JTextField gptFilePathTextField;
    private JLabel sharedFolderPathLabel;

    public EditRemoteMachineCredentialsDialog(Window parent, RemoteMachineProperties remoteMachineCredentialsToEdit) {
        super(parent, "Remote machine", true, null);

        this.remoteMachineCredentialsToEdit = remoteMachineCredentialsToEdit;
    }

    @Override
    protected void onAboutToShow() {
        Dimension size = getJDialog().getPreferredSize();
        getJDialog().setMinimumSize(size);
    }

    @Override
    protected JPanel buildContentPanel(int gapBetweenColumns, int gapBetweenRows) {
        Insets defaultTextFieldMargins = buildDefaultTextFieldMargins();
        Insets defaultListItemMargins = buildDefaultListItemMargins();
        createComponents(defaultTextFieldMargins, defaultListItemMargins);

        JButton testConnectionButton = new JButton("Test connection");
        testConnectionButton.setFocusable(false);
        testConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                testConnectionButtonPressed();
            }
        });

        JPanel contentPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        contentPanel.add(new JLabel("Host name"), c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        contentPanel.add(this.hostNameTextField, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(new JLabel("Port number"), c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(this.portNumberTextField, c);

        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(new JLabel("Operating system"), c);
        c = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(this.operatingSystemsComboBox, c);

        c = SwingUtils.buildConstraints(0, 3, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(new JLabel("Username"), c);
        c = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(this.usernameTextField, c);

        c = SwingUtils.buildConstraints(0, 4, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(new JLabel("Password"), c);
        c = SwingUtils.buildConstraints(1, 4, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(this.passwordTextField, c);

        c = SwingUtils.buildConstraints(0, 5, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(new JLabel("GPT file path"), c);
        c = SwingUtils.buildConstraints(1, 5, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(this.gptFilePathTextField, c);

        c = SwingUtils.buildConstraints(0, 6, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(this.sharedFolderPathLabel, c);
        c = SwingUtils.buildConstraints(1, 6, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        contentPanel.add(this.sharedFolderPathTextField, c);

        c = SwingUtils.buildConstraints(1, 7, GridBagConstraints.NONE, GridBagConstraints.EAST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(testConnectionButton, c);

        c = SwingUtils.buildConstraints(1, 8, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        contentPanel.add(new JLabel(), c);

        computePanelFirstColumn(contentPanel);

        if (this.remoteMachineCredentialsToEdit != null) {
            this.hostNameTextField.setText(this.remoteMachineCredentialsToEdit.getHostName());
            this.portNumberTextField.setText(Integer.toString(this.remoteMachineCredentialsToEdit.getPortNumber()));
            this.operatingSystemsComboBox.setSelectedItem(this.remoteMachineCredentialsToEdit.getOperatingSystemName());
            this.usernameTextField.setText(this.remoteMachineCredentialsToEdit.getUsername());
            this.passwordTextField.setText(this.remoteMachineCredentialsToEdit.getPassword());
            this.gptFilePathTextField.setText(this.remoteMachineCredentialsToEdit.getGPTFilePath());
            this.sharedFolderPathTextField.setText(this.remoteMachineCredentialsToEdit.getSharedFolderPath());
        }

        return contentPanel;
    }

    @Override
    protected JPanel buildButtonsPanel(ActionListener cancelActionListener) {
        ActionListener okActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                okButtonPressed();
            }
        };
        return buildButtonsPanel("Ok", okActionListener, "Cancel", cancelActionListener);
    }

    protected void successfullyCloseDialog(RemoteMachineProperties oldSSHServerCredentials, RemoteMachineProperties newSSHServerCredentials) {
        getJDialog().dispose();
    }

    private String getLinuxSharedFolderPathLabelText() {
        return "Shared folder path";
    }

    private String getWindowsSharedFolderPathLabelText() {
        return "Shared drive";
    }

    private void createComponents(Insets defaultTextFieldMargins, Insets defaultListItemMargins) {
        this.hostNameTextField = new JTextField();
        this.hostNameTextField.setMargin(defaultTextFieldMargins);
        this.hostNameTextField.setColumns(30);

        this.sharedFolderPathTextField = new JTextField();
        this.sharedFolderPathTextField.setMargin(defaultTextFieldMargins);

        this.sharedFolderPathLabel = new JLabel(getLinuxSharedFolderPathLabelText());

        this.portNumberTextField = new JTextField();
        this.portNumberTextField.setMargin(defaultTextFieldMargins);

        this.operatingSystemsComboBox = new JComboBox<String>();
        this.operatingSystemsComboBox.setPreferredSize(this.hostNameTextField.getPreferredSize());
        this.operatingSystemsComboBox.addItem(RemoteMachineProperties.LINUX_OPERATING_SYSTEM);
        this.operatingSystemsComboBox.addItem(RemoteMachineProperties.WINDOWS_OPERATING_SYSTEM);
        this.operatingSystemsComboBox.setSelectedItem(null);
        LabelListCellRenderer<String> renderer = new LabelListCellRenderer<String>(defaultListItemMargins) {
            @Override
            protected String getItemDisplayText(String value) {
                return value;
            }
        };
        this.operatingSystemsComboBox.setRenderer(renderer);
        this.operatingSystemsComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
        this.operatingSystemsComboBox.setOpaque(true);
        this.operatingSystemsComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                operatingSystemSelected();
            }
        });

        this.usernameTextField = new JTextField();
        this.usernameTextField.setMargin(defaultTextFieldMargins);

        this.gptFilePathTextField = new JTextField();
        this.gptFilePathTextField.setMargin(defaultTextFieldMargins);

        this.passwordTextField = new JPasswordField();
        this.passwordTextField.setMargin(defaultTextFieldMargins);
    }

    private void operatingSystemSelected() {
        String selectedOperatingSystemName = (String) operatingSystemsComboBox.getSelectedItem();
        if (selectedOperatingSystemName.equals(RemoteMachineProperties.LINUX_OPERATING_SYSTEM)) {
            this.sharedFolderPathLabel.setText(getLinuxSharedFolderPathLabelText());
        } else if (selectedOperatingSystemName.equals(RemoteMachineProperties.WINDOWS_OPERATING_SYSTEM)) {
            this.sharedFolderPathLabel.setText(getWindowsSharedFolderPathLabelText());
        } else {
            throw new IllegalStateException("Unknown operating system name '" + selectedOperatingSystemName + "'.");
        }
    }

    private void okButtonPressed() {
        RemoteMachineProperties sshServerCredentials = buildRemoteMachineCredentialsItem(true);
        if (sshServerCredentials != null) {
            successfullyCloseDialog(this.remoteMachineCredentialsToEdit, sshServerCredentials);
        }
    }

    private void testConnectionButtonPressed() {
        RemoteMachineProperties sshServerCredentials = buildRemoteMachineCredentialsItem(false);
        if (sshServerCredentials != null) {
            ILoadingIndicator loadingIndicator = getLoadingIndicator();
            int threadId = getNewCurrentThreadId();
            TestConnectionTimerRunnable runnable = new TestConnectionTimerRunnable(this, loadingIndicator, threadId, sshServerCredentials);
            runnable.executeAsync();
        }
    }

    private RemoteMachineProperties buildRemoteMachineCredentialsItem(boolean validateSharedFolderPath) {
        RemoteMachineProperties remoteMachineCredentials = null;
        String hostName = this.hostNameTextField.getText();
        String portNumberAsString = this.portNumberTextField.getText();
        String username = this.usernameTextField.getText();
        String password = new String(this.passwordTextField.getPassword());
        String operatingSystemName = (String)this.operatingSystemsComboBox.getSelectedItem();
        String sharedFolderPath = this.sharedFolderPathTextField.getText();
        if (StringUtils.isBlank(hostName)) {
            showErrorDialog("Enter the host name.");
            this.hostNameTextField.requestFocus();
        } else if (StringUtils.isBlank(portNumberAsString)) {
            showErrorDialog("Enter the port number.");
            this.portNumberTextField.requestFocus();
        } else {
            // check if the port number is valid
            Integer portNumber;
            try {
                portNumber = Integer.parseInt(portNumberAsString);
            } catch (NumberFormatException exception) {
                portNumber = null;
            }
            if (portNumber == null) {
                showErrorDialog("The port number is not valid.");
                this.portNumberTextField.requestFocus();
            } else {
                if (StringUtils.isBlank(operatingSystemName)) {
                    showErrorDialog("Select the operating system.");
                    this.operatingSystemsComboBox.requestFocus();
                } else if (StringUtils.isBlank(username)) {
                    showErrorDialog("Enter the username.");
                    this.usernameTextField.requestFocus();
                } else if (StringUtils.isBlank(password)) {
                    showErrorDialog("Enter the password.");
                    this.passwordTextField.requestFocus();
                } else if (validateSharedFolderPath && StringUtils.isBlank(sharedFolderPath)) {
                    String message;
                    if (operatingSystemName.equals(RemoteMachineProperties.LINUX_OPERATING_SYSTEM)) {
                        message = "Enter the shared folder path.";
                    } else if (operatingSystemName.equals(RemoteMachineProperties.WINDOWS_OPERATING_SYSTEM)) {
                        message = "Enter the shared drive.";
                    } else {
                        throw new IllegalStateException("Unknown operating system name '" + operatingSystemName + "'.");
                    }
                    showErrorDialog(message);
                    this.sharedFolderPathTextField.requestFocus();
                } else {
                    remoteMachineCredentials = new RemoteMachineProperties(hostName, portNumber.intValue(), username, password, operatingSystemName, sharedFolderPath);
                    remoteMachineCredentials.setGPTFilePath(this.gptFilePathTextField.getText());
                }
            }
        }
        return remoteMachineCredentials;
    }
}
