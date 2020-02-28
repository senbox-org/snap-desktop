package org.esa.snap.remote.execution.topology;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.local.folder.MountMacLocalFolderTimerRunnable;
import org.esa.snap.remote.execution.local.folder.AbstractLocalSharedFolder;
import org.esa.snap.remote.execution.local.folder.IMountLocalSharedFolderCallback;
import org.esa.snap.remote.execution.local.folder.MacLocalSharedFolder;
import org.esa.snap.ui.loading.LoadingIndicator;
import org.esa.snap.ui.loading.MessageDialog;

import javax.swing.JPanel;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionListener;

public class MacRemoteTopologyPanel extends RemoteTopologyPanel {

    public MacRemoteTopologyPanel(Window parentWindow, Insets defaultTextFieldMargins, Insets defaultListItemMargins) {
        super(parentWindow, defaultTextFieldMargins, defaultListItemMargins);
    }

    @Override
    public String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    @Override
    public void mountLocalSharedFolderAsync(MessageDialog parentWindow, LoadingIndicator loadingIndicator, int threadId, IMountLocalSharedFolderCallback callback) {
        MacLocalSharedFolder macLocalSharedDrive = buildLocalSharedFolder();
        if (canMountLocalSharedFolder(parentWindow, macLocalSharedDrive)) {
            MountMacLocalFolderTimerRunnable runnable = new MountMacLocalFolderTimerRunnable(parentWindow, loadingIndicator, threadId, macLocalSharedDrive, callback);
            runnable.executeAsync();
        }
    }

    @Override
    protected boolean canMountLocalSharedFolder(MessageDialog parentWindow, AbstractLocalSharedFolder localSharedFolder) {
        boolean canMount = super.canMountLocalSharedFolder(parentWindow, localSharedFolder);

        if (canMount) {
            MacLocalSharedFolder macLocalSharedDrive = (MacLocalSharedFolder)localSharedFolder;
            if (StringUtils.isBlank(macLocalSharedDrive.getLocalSharedFolderPath())) {
                parentWindow.showErrorDialog("The local shared folder path is not specified.");
                getLocalSharedFolderPathTextField().requestFocus();
                canMount = false;
            }
        }
        return canMount;
    }

    @Override
    protected MacLocalSharedFolder buildLocalSharedFolder() {
        String remoteSharedFolderPath = getRemoteSharedFolderPath();
        String remoteUsername = getRemoteUsername();
        String remotePassword = getRemotePassword();
        String localSharedFolderPath = getLocalSharedFolderPath();
        return new MacLocalSharedFolder(remoteSharedFolderPath, remoteUsername, remotePassword, localSharedFolderPath);
    }

    @Override
    public void addComponents(int gapBetweenColumns, int gapBetweenRows, ActionListener browseLocalSharedFolderButtonListener, JPanel remoteMachinesButtonsPanel) {
        addRemoteSharedFolderPathRow(0, gapBetweenColumns, 0);
        addUsernameRow(1, gapBetweenColumns, gapBetweenRows);
        addPasswordRow(2, gapBetweenColumns, gapBetweenRows);
        addLocalSharedFolderRow(3, gapBetweenColumns, gapBetweenRows, "Local shared folder path", browseLocalSharedFolderButtonListener);
        addRemoteMachinesRow(4, gapBetweenColumns, gapBetweenRows, remoteMachinesButtonsPanel);
    }
}
