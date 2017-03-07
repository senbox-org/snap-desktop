package org.esa.snap.rcp.util;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.VersionChecker;
import org.openide.modules.OnStop;
import org.openide.windows.OnShowing;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Cursor;

/**
 * @author Marco Peters
 */
@SuppressWarnings("unused")
public class DesktopVersionCheck {

    private static final String STEP_WEB_PAGE = SystemUtils.getApplicationHomepageUrl();
    private static final String MSG_UPDATE_INFO = "<html>A new SNAP version is available for download!<br>" +
                                                  "Currently installed %s, available is %s.<br>" +
                                                  "Please visit the SNAP home page at";
    private static final VersionChecker VERSION_CHECKER = VersionChecker.getInstance();

    private static boolean hasChecked = false;

    private DesktopVersionCheck() {
    }

    @OnShowing
    public static class OnStartup implements Runnable {

        @Override
        public void run() {
            if (VERSION_CHECKER.mustCheck()) {
                hasChecked = true;
                if (VERSION_CHECKER.checkForNewRelease()) {
                    final JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

                    String localVersion = String.valueOf(VERSION_CHECKER.getLocalVersion());
                    String remoteVersion = String.valueOf(VERSION_CHECKER.getRemoteVersion());
                    panel.add(new JLabel(String.format(MSG_UPDATE_INFO + "", localVersion, remoteVersion)));

                    final JLabel LinkLabel = new JLabel("<html><a href=\"" + STEP_WEB_PAGE + "\">" + STEP_WEB_PAGE + "</a>");
                    LinkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    LinkLabel.addMouseListener(new BrowserUtils.URLClickAdaptor(STEP_WEB_PAGE));
                    panel.add(LinkLabel);

                    JOptionPane.showMessageDialog(null, panel);
                }
            }
        }
    }

    @OnStop
    public static class OnShutdown implements Runnable {

        @Override
        public void run() {
            if (hasChecked) {
                VERSION_CHECKER.setChecked();
            }
        }
    }

}
