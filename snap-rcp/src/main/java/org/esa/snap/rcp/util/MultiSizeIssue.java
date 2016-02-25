package org.esa.snap.rcp.util;

import org.esa.snap.rcp.SnapApp;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * <i>This class is not part of the public API.</i>
 * <p>
 * Its purpose is to show a warning when users invoke a SNAP function
 * that is not capable to work with multi-size products such as Sentinel-2 MSI L1C or Sentinel-3 SLSTR L1b products.
 *
 * @author Marco Peters
 */
public class MultiSizeIssue {

    private MultiSizeIssue(){
    }

    public static void showMultiSizeWarning() {
        String title = Dialogs.getDialogTitle("Limited Functionality");
        final String msgText = "Please note that you have opened a product which contains <br/>" +
                               "raster of different sizes. Not all features of SNAP will work with this product. <br/>" +
                               "More info about this issue and its status can be found in the " +
                               "<a href=\"https://senbox.atlassian.net/browse/SNAP-1\">SNAP Issue Tracker</a>";
        final String prefKey = "snap.multiSizeInfo" + Dialogs.PREF_KEY_SUFFIX_DONTSHOW;
        String decision = SnapApp.getDefault().getPreferences().get(prefKey, "");
        if ("true".equals(decision)) {
            return;
        }
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        final JEditorPane textPane = new JEditorPane("text/html", msgText);
        setFont(textPane);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException e1) {
                    Dialogs.showWarning("Could not open URL: " + e.getDescription());
                }
            }
        });
        panel.add(textPane, BorderLayout.CENTER);
        JCheckBox dontShowCheckBox = new JCheckBox("Don't show this message anymore.", false);
        panel.add(dontShowCheckBox, BorderLayout.SOUTH);
        NotifyDescriptor d = new NotifyDescriptor(panel, title, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null);
        DialogDisplayer.getDefault().notify(d);
        if (d.getValue() != NotifyDescriptor.CANCEL_OPTION) {
            boolean storeResult = dontShowCheckBox.isSelected();
            if (storeResult) {
                SnapApp.getDefault().getPreferences().put(prefKey, "true");
            }
        }
    }

    private static void setFont(JEditorPane textPane) {
        if (textPane.getDocument() instanceof HTMLDocument) {
            Font font = UIManager.getFont("Label.font");
            String bodyRule = "body { font-family: " + font.getFamily() + "; font-size: " + font.getSize() + "pt; }";
            ((HTMLDocument)textPane.getDocument()).getStyleSheet().addRule(bodyRule);
        }
    }

}
