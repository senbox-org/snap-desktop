package org.esa.snap.remote.execution.local.folder;

/**
 * Created by jcoravu on 23/5/2019.
 */
public class LinuxLocalSharedFolder extends MacLocalSharedFolder {

    private final String localPassword;

    public LinuxLocalSharedFolder(String remoteSharedFolderPath, String remoteUsername, String remotePassword, String localSharedFolderPath, String localPassword) {
        super(remoteSharedFolderPath, remoteUsername, remotePassword, localSharedFolderPath);

        this.localPassword = localPassword;
    }

    @Override
    public boolean hasChangedParameters(AbstractLocalSharedFolder newLocalSharedFolder) {
        boolean changedParameters = super.hasChangedParameters(newLocalSharedFolder);

        if (!changedParameters) {
            LinuxLocalSharedFolder newLinuxLocalSharedDrive = (LinuxLocalSharedFolder)newLocalSharedFolder;
            changedParameters = !this.localPassword.equals(newLinuxLocalSharedDrive.getLocalPassword());
        }
        return changedParameters;
    }

    public String getLocalPassword() {
        return localPassword;
    }
}
