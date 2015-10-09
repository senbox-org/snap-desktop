package org.esa.snap.rcp.session;

import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;

/**
 * A session manager is handling the one and only active session file.
 *
 * @author Muhammad
 */
public class SessionManager {
    private static SessionManager instance = new SessionManager();

    private File sessionFile;

    public static SessionManager getDefault() {
        return instance;
    }

    public SnapFileFilter getSessionFileFilter() {
        return new SnapFileFilter("SESSION",
                                  new String[]{".snap"},
                                  "SNAP session files");
    }

    public File getSessionFile() {
        return sessionFile;
    }

    public void setSessionFile(File sessionFile) {
        this.sessionFile = sessionFile;
    }

}
