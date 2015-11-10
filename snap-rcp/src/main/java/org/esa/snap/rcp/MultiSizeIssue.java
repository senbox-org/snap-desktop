package org.esa.snap.rcp;

import org.esa.snap.core.datamodel.Product;
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
 * @author Marco Peters
 */
public class MultiSizeIssue {

    private MultiSizeIssue(){
    }

    public static void showMultiSizeWarning() {
        String title = SnapDialogs.getDialogTitle("Limited Functionality");
        final String msgText = "Please note that you have opened a product which contains <br/>" +
                               "raster of different sizes. Not all features of SNAP will work with this product. <br/>" +
                               "More info about this issue can be found in the " +
                               "<a href=\"http://senbox.atlassian.net/wiki/display/SNAP/Multi-size+Products+Specification\">SNAP Wiki</a>";
        final String prefKey = "snap.multiSizeInfo" + ".dontShow";
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
                    SnapDialogs.showWarning("Could not open URL: " + e.getDescription());
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

    public static boolean isMultiSize(Product selectedProduct) {
        return selectedProduct != null && selectedProduct.isMultiSizeProduct();
    }


    private static void setFont(JEditorPane textPane) {
        if (textPane.getDocument() instanceof HTMLDocument) {
            Font font = UIManager.getFont("Label.font");
            String bodyRule = "body { font-family: " + font.getFamily() + "; font-size: " + font.getSize() + "pt; }";
            ((HTMLDocument)textPane.getDocument()).getStyleSheet().addRule(bodyRule);
        }
    }

}
