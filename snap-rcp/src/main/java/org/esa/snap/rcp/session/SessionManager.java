package org.esa.snap.rcp.session;

import org.esa.snap.core.util.io.SnapFileFilter;

import java.nio.file.Path;

/**
 * A session manager is handling the one and only active session file.
 *
 * @author Muhammad
 */
public class SessionManager {
    private static SessionManager instance = new SessionManager();

    private Path sessionFile;

    public static SessionManager getDefault() {
        return instance;
    }

    public SnapFileFilter getSessionFileFilter() {
        return new SnapFileFilter("SESSION",
                                  new String[]{".snap"},
                                  "SNAP session files");
    }

    public Path getSessionFile() {
        return sessionFile;
    }

    public void setSessionFile(Path sessionFile) {
        this.sessionFile = sessionFile;
    }

}
