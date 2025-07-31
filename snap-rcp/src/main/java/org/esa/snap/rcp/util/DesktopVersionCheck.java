package org.esa.snap.rcp.util;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.VersionChecker;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.preferences.Preference;
import org.esa.snap.runtime.EngineConfig;
import org.openide.awt.CheckForUpdatesProvider;
import org.openide.util.Lookup;
import org.openide.windows.OnShowing;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.util.prefs.Preferences;

/**
 * @author Marco Peters
 */
@SuppressWarnings("unused")
public class DesktopVersionCheck {

    private static final String APPLICATION_HOMEPAGE_URL = SystemUtils.getApplicationHomepageUrl();

    private static final VersionChecker VERSION_CHECKER = VersionChecker.getInstance();

    private DesktopVersionCheck() {
    }

    @OnShowing
    public static class OnStartup implements Runnable {

        @Override
        public void run() {
            // @OnShowing is triggered if Utilities.actionsGlobalContext() is called. This happens in some tests (which use actions).
            // So we need to check if the Desktop is really running. If not, we can skip the version check.
            if (!SnapApp.getDefault().getAppContext().getApplicationWindow().isVisible()) {
                return;
            }
            if (VERSION_CHECKER.mustCheck()) {
                if (VERSION_CHECKER.checkForNewRelease()) {
                    VERSION_CHECKER.setChecked();
                    final JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

                    String localVersion = String.valueOf(VERSION_CHECKER.getLocalVersion());
                    String remoteVersion = String.valueOf(VERSION_CHECKER.getRemoteVersion());

                    final String message =
                            "<html><b>" + SnapApp.getDefault().getInstanceName() + " " + remoteVersion + "</b> is now available!<br><br>" +
                                    "Your current version is: " + SnapApp.getDefault().getInstanceName() + " " + localVersion + "<br><br>" +
                                    "Version check frequency: " + VERSION_CHECKER.getCheckIntervalString() + "<br>" +
                                    "- To adjust version check frequency see (Preferences > General > Other)" + "<br><br>" +
                                    "To download latest SeaDAS, please visit the " + SnapApp.getDefault().getInstanceName() + " home page:" +
                                    "";

                    panel.add(new JLabel(message));

                    final JLabel LinkLabel = new JLabel("<html><a href=\"" + APPLICATION_HOMEPAGE_URL + "\">" + APPLICATION_HOMEPAGE_URL + "</a>");
                    LinkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    LinkLabel.addMouseListener(new BrowserUtils.URLClickAdaptor(APPLICATION_HOMEPAGE_URL));
                    panel.add(LinkLabel);

                    JOptionPane.showMessageDialog(null, panel,  "SeaDAS Version Update Notice", JOptionPane.INFORMATION_MESSAGE);
                    return;

                } else {
//
//                    String localVersion = String.valueOf(VERSION_CHECKER.getLocalVersion());
//
//                    final String message =
//                            "<html>You are running the latest SeaDAS version: " + localVersion + " <br><br>" +
//                                    "Version check frequency: " + VERSION_CHECKER.getCheckIntervalString() + "<br>" +
//                                    "- To adjust version check frequency see (Preferences > General > Other)" + "<br><br>" +
//                                    "";
//
//                    Dialogs.showMessage("Version Check", message, JOptionPane.INFORMATION_MESSAGE, "optional.version.check.onstartup");
//                    return;
                }
            }
        }
    }
}
