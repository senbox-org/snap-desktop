package org.esa.snap.rcp.util;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.VersionChecker;
import org.openide.modules.OnStop;
import org.openide.windows.OnShowing;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import java.awt.Color;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Marco Peters
 */
@SuppressWarnings("unused")
public class DesktopVersionCheck {

    private static final String STEP_WEB_PAGE = SystemUtils.getApplicationHomepageUrl();
    private static final String MSG_UPDATE_INFO = "A new SNAP version is available for download.<br>Currently installed %s, available is %s.<br>Please visit %s";
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
                    String localVersion = String.valueOf(VERSION_CHECKER.getLocalVersion());
                    String remoteVersion = String.valueOf(VERSION_CHECKER.getRemoteVersion());
                    String linkText = "<a href=\"" + STEP_WEB_PAGE + "\">" + STEP_WEB_PAGE + "</a>";
                    String msg = String.format("<html>" + MSG_UPDATE_INFO+"</html>", localVersion, remoteVersion, linkText);
                    JEditorPane editorPane = createHtmlDialogPane(msg);
                    JOptionPane.showMessageDialog(null, editorPane);
                }
            }
        }
    }

    private static JEditorPane createHtmlDialogPane(String msg) {
        JEditorPane editorPane = new JEditorPane("text/html", msg);
        editorPane.setEditable(false);
        editorPane.setBorder(null);
        editorPane.setBackground(new Color(0, 0, 0, 0));
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    BrowserUtils.openInBrowser(new URI(STEP_WEB_PAGE));
                } catch (URISyntaxException e1) {
                    Dialogs.showError("Could not open SNAP home page.");
                }
            }
        });
        return editorPane;
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
