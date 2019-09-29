package org.esa.snap.remote.execution.topology;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.local.folder.MountLinuxLocalFolderTimerRunnable;
import org.esa.snap.remote.execution.local.folder.AbstractLocalSharedFolder;
import org.esa.snap.remote.execution.local.folder.IMountLocalSharedFolderCallback;
import org.esa.snap.remote.execution.local.folder.LinuxLocalSharedFolder;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.IMessageDialog;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionListener;

public class LinuxRemoteTopologyPanel extends RemoteTopologyPanel {

    private final JPasswordField localPasswordTextField;

    public LinuxRemoteTopologyPanel(Window parentWindow, Insets defaultTextFieldMargins, Insets defaultListItemMargins) {
        super(parentWindow, defaultTextFieldMargins, defaultListItemMargins);

        this.localPasswordTextField = new JPasswordField();
        this.localPasswordTextField.setMargin(defaultTextFieldMargins);
    }

    @Override
    public String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    @Override
    public JPasswordField getLocalPasswordTextField() {
        return this.localPasswordTextField;
    }

    @Override
    public void setRemoteTopology(RemoteTopology remoteTopology) {
        super.setRemoteTopology(remoteTopology);

        this.localPasswordTextField.setText(remoteTopology.getLocalPassword());
    }

    @Override
    public void mountLocalSharedFolderAsync(IMessageDialog parentWindow, ILoadingIndicator loadingIndicator, int threadId, IMountLocalSharedFolderCallback callback) {
        LinuxLocalSharedFolder linuxLocalSharedDrive = buildLocalSharedFolder();
        if (canMountLocalSharedFolder(parentWindow, linuxLocalSharedDrive)) {
            MountLinuxLocalFolderTimerRunnable runnable = new MountLinuxLocalFolderTimerRunnable(parentWindow, loadingIndicator, threadId, linuxLocalSharedDrive, callback);
            runnable.executeAsync();
        }
    }

    @Override
    protected LinuxLocalSharedFolder buildLocalSharedFolder() {
        String remoteSharedFolderPath = getRemoteSharedFolderPath();
        String remoteUsername = getRemoteUsername();
        String remotePassword = getRemotePassword();
        String localSharedFolderPath = getLocalSharedFolderPath();
        String localPassword = new String(this.localPasswordTextField.getPassword());
        return new LinuxLocalSharedFolder(remoteSharedFolderPath, remoteUsername, remotePassword, localSharedFolderPath, localPassword);
    }

    @Override
    protected boolean canMountLocalSharedFolder(IMessageDialog parentWindow, AbstractLocalSharedFolder localSharedFolder) {
        boolean canMount = super.canMountLocalSharedFolder(parentWindow, localSharedFolder);

        if (canMount) {
            LinuxLocalSharedFolder linuxLocalSharedDrive = (LinuxLocalSharedFolder)localSharedFolder;
            if (StringUtils.isBlank(linuxLocalSharedDrive.getLocalSharedFolderPath())) {
                parentWindow.showErrorDialog("The local shared folder path is not specified.");
                getLocalSharedFolderPathTextField().requestFocus();
                canMount = false;
            } else if (StringUtils.isBlank(linuxLocalSharedDrive.getLocalPassword())) {
                parentWindow.showErrorDialog("The local password is not specified.");
                this.localPasswordTextField.requestFocus();
                canMount = false;
            }
        }
        return canMount;
    }

    @Override
    public void addComponents(int gapBetweenColumns, int gapBetweenRows, ActionListener browseLocalSharedFolderButtonListener, JPanel remoteMachinesButtonsPanel) {
        addRemoteSharedFolderPathRow(0, gapBetweenColumns, 0);
        addUsernameRow(1, gapBetweenColumns, gapBetweenRows);
        addPasswordRow(2, gapBetweenColumns, gapBetweenRows);
        addLocalSharedFolderRow(3, gapBetweenColumns, gapBetweenRows, "Local shared folder path", browseLocalSharedFolderButtonListener);
        addLocalPasswordRow(4, gapBetweenColumns, gapBetweenRows);
        addRemoteMachinesRow(5, gapBetweenColumns, gapBetweenRows, remoteMachinesButtonsPanel);
    }

    protected void addLocalPasswordRow(int rowIndex, int gapBetweenColumns, int gapBetweenRows) {
        GridBagConstraints c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(new JLabel("Local password"), c);
        c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, gapBetweenRows, gapBetweenColumns);
        add(this.localPasswordTextField, c);
    }
}
