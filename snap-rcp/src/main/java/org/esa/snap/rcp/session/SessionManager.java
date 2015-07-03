package org.esa.snap.rcp.session;

import java.io.File;

/**
 * Created by Samurai on 25/06/15.
 */
class SessionManager  {
    private static SessionManager instance = new SessionManager();

    private File sessionFile;

    public static SessionManager getDefault() {
        return instance;
    }

    public File getSessionFile() {
        return sessionFile;
    }

    public void setSessionFile(File sessionFile) {
        this.sessionFile = sessionFile;
    }

}
