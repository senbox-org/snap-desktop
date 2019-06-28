package org.esa.snap.remote.execution.topology;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.local.folder.AbstractLocalSharedFolder;
import org.esa.snap.remote.execution.local.folder.IMountLocalSharedFolderCallback;
import org.esa.snap.remote.execution.local.folder.WindowsLocalSharedDrive;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.IMessageDialog;
import org.esa.snap.remote.execution.local.folder.MountWindowsLocalDriveTimerRunnable;
import org.esa.snap.remote.execution.local.folder.WindowsLocalMachineMountDrive;

import javax.swing.JPanel;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionListener;

public class WindowsRemoteTopologyPanel extends RemoteTopologyPanel {

    public WindowsRemoteTopologyPanel(Window parentWindow, Insets defaultTextFieldMargins, Insets defaultListItemMargins) {
        super(parentWindow, defaultTextFieldMargins, defaultListItemMargins);
    }

    @Override
    public String normalizePath(String path) {
        return path.replace('/', '\\');
    }

    @Override
    public void mountLocalSharedFolderAsync(IMessageDialog parentWindow, ILoadingIndicator loadingIndicator, int threadId, IMountLocalSharedFolderCallback callback) {
        WindowsLocalSharedDrive windowsLocalSharedDrive = buildLocalSharedFolder();
        if (canMountLocalSharedFolder(parentWindow, windowsLocalSharedDrive)) {
            if (StringUtils.isBlank(windowsLocalSharedDrive.getLocalSharedDrive())) {
                // no local shared drive to mount
                callback.onSuccessfullyFinishMountingLocalFolder(new WindowsLocalMachineMountDrive(windowsLocalSharedDrive, false));
            } else {
                MountWindowsLocalDriveTimerRunnable runnable = new MountWindowsLocalDriveTimerRunnable(parentWindow, loadingIndicator, threadId, windowsLocalSharedDrive, callback);
                runnable.executeAsync();
            }
        }
    }

    @Override
    protected boolean canMountLocalSharedFolder(IMessageDialog parentWindow, AbstractLocalSharedFolder localSharedFolder) {
        boolean canMount = super.canMountLocalSharedFolder(parentWindow, localSharedFolder);

        if (canMount) {
            WindowsLocalSharedDrive windowsLocalSharedDrive = (WindowsLocalSharedDrive)localSharedFolder;
            if (!StringUtils.isBlank(windowsLocalSharedDrive.getLocalSharedDrive())) {
                char driveLetter = windowsLocalSharedDrive.getLocalSharedDrive().charAt(0);
                String message = null;
                if (Character.isLetter(driveLetter)) {
                    String colon = windowsLocalSharedDrive.getLocalSharedDrive().substring(1);
                    if (!":".equals(colon)) {
                        message = "The local shared drive letter '" + driveLetter + "' is not followed by ':'.";
                    }
                } else {
                    message = "The local shared drive is invalid. \n\nThe first character '" + driveLetter + "' is not a letter.";
                }
                if (message != null) {
                    parentWindow.showErrorDialog(message);
                    getLocalSharedFolderPathTextField().requestFocus();
                    canMount = false;
                }
            }
        }
        return canMount;
    }

    @Override
    protected WindowsLocalSharedDrive buildLocalSharedFolder() {
        String remoteSharedFolderPath = getRemoteSharedFolderPath();
        String remoteUsername = getRemoteUsername();
        String remotePassword = getRemotePassword();
        String localSharedDrive = getLocalSharedFolderPath();
        return new WindowsLocalSharedDrive(remoteSharedFolderPath, remoteUsername, remotePassword, localSharedDrive);
    }

    @Override
    public void addComponents(int gapBetweenColumns, int gapBetweenRows, ActionListener browseLocalSharedFolderButtonListener, JPanel remoteMachinesButtonsPanel) {
        addRemoteSharedFolderPathRow(0, gapBetweenColumns, 0);
        addUsernameRow(1, gapBetweenColumns, gapBetweenRows);
        addPasswordRow(2, gapBetweenColumns, gapBetweenRows);
        addLocalSharedFolderRow(3, gapBetweenColumns, gapBetweenRows, "Local shared drive", null);
        addRemoteMachinesRow(4, gapBetweenColumns, gapBetweenRows, remoteMachinesButtonsPanel);
    }
}
