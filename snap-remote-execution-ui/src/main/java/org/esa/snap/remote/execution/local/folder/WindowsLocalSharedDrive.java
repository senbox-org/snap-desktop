package org.esa.snap.remote.execution.local.folder;

import org.apache.commons.lang.StringUtils;

/**
 * Created by jcoravu on 23/5/2019.
 */
public class WindowsLocalSharedDrive extends AbstractLocalSharedFolder {

    private final String localSharedDrive;

    public WindowsLocalSharedDrive(String remoteSharedFolderPath, String remoteUsername, String remotePassword, String localSharedDrive) {
        super(remoteSharedFolderPath, remoteUsername, remotePassword);

        this.localSharedDrive = localSharedDrive;
    }

    public String getLocalSharedDrive() {
        return localSharedDrive;
    }

    @Override
    public boolean hasChangedParameters(AbstractLocalSharedFolder newLocalSharedFolder) {
        boolean changedParameters = super.hasChangedParameters(newLocalSharedFolder);

        if (!changedParameters) {
            WindowsLocalSharedDrive newWindowsLocalSharedDrive = (WindowsLocalSharedDrive)newLocalSharedFolder;
            String localDrive = StringUtils.isBlank(this.localSharedDrive) ? "" : this.localSharedDrive;
            String newLocalDrive = StringUtils.isBlank(newWindowsLocalSharedDrive.getLocalSharedDrive()) ? "" : newWindowsLocalSharedDrive.getLocalSharedDrive();
            changedParameters = !localDrive.equals(newLocalDrive);
        }
        return changedParameters;
    }
}
