package org.esa.snap.rcp.util;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Resampler;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;

import javax.swing.JComboBox;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <i>This class is not part of the public API.</i>
 * <p>
 * Its purpose is to suggest to use the resampling operator when users invoke a SNAP function
 * that is not capable to work with multi-size products such as Sentinel-2 MSI L1C or Sentinel-3 SLSTR L1b products.
 *
 * @author Tonio Fincke
 */
public class MultiSizeIssue {

    public static Product maybeResample(Product product) {
        String title = Dialogs.getDialogTitle("Resampling Required");
        final List<Resampler> availableResamplers = getAvailableResamplers(product);
        int optionType;
        int messageType;
        final StringBuilder msgTextBuilder = new StringBuilder("The functionality you have chosen is not supported for products with bands of different sizes.<br/>");
        if (availableResamplers.isEmpty()) {
            optionType = JOptionPane.OK_CANCEL_OPTION;
            messageType = JOptionPane.INFORMATION_MESSAGE;
        } else if (availableResamplers.size() == 1) {
            msgTextBuilder.append("You can use the ").append(availableResamplers.get(0).getName()).
                    append(" to resample this product so that all bands have the same size, <br/>" +
                                   "which will enable you to use this feature.<br/>" +
                                   "Do you want to resample the product now?");
            optionType = JOptionPane.YES_NO_OPTION;
            messageType = JOptionPane.QUESTION_MESSAGE;
        } else {
            msgTextBuilder.append("You can use one of these resamplers to resample this product so that all bands have the same size, <br/>" +
                                          "which will enable you to use this feature.<br/>" +
                                          "Do you want to resample the product now?");
            optionType = JOptionPane.YES_NO_OPTION;
            messageType = JOptionPane.QUESTION_MESSAGE;
        }
        msgTextBuilder.append("<br/>" +
                                      "<br/>" +
                                      "More info about this issue and its status can be found in the " +
                                      "<a href=\"https://senbox.atlassian.net/browse/SNAP-1\">SNAP Issue Tracker</a>."
        );
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        final JEditorPane textPane = new JEditorPane("text/html", msgTextBuilder.toString());
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
        final JComboBox<Object> resamplerBox = new JComboBox<>();
        if (availableResamplers.size() > 1) {
            String[] resamplerNames = new String[availableResamplers.size()];
            for (int i = 0; i < availableResamplers.size(); i++) {
                resamplerNames[i] = availableResamplers.get(i).getName();
                resamplerBox.addItem(resamplerNames[i]);
            }
            panel.add(resamplerBox, BorderLayout.SOUTH);
        }
        NotifyDescriptor d = new NotifyDescriptor(panel, title, optionType, messageType, null, null);
        DialogDisplayer.getDefault().notify(d);
        if (d.getValue() == NotifyDescriptor.YES_OPTION) {
            Resampler selectedResampler;
            if (availableResamplers.size() == 1) {
                selectedResampler = availableResamplers.get(0);
            } else {
                selectedResampler = availableResamplers.get(resamplerBox.getSelectedIndex());
            }
            return selectedResampler.resample(product);
        }
        return null;
    }

    public static void chooseBandsWithSameSize() {
        String title = Dialogs.getDialogTitle("Choose bands with same size.");
        int optionType = JOptionPane.OK_CANCEL_OPTION;
        int messageType = JOptionPane.INFORMATION_MESSAGE;

        final StringBuilder msgTextBuilder = new StringBuilder("The functionality you have chosen is not supported for bands of different sizes.<br/>");

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        final JEditorPane textPane = new JEditorPane("text/html", msgTextBuilder.toString());
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
        final JComboBox<Object> resamplerBox = new JComboBox<>();

        NotifyDescriptor d = new NotifyDescriptor(panel, title, optionType, messageType, null, null);
        DialogDisplayer.getDefault().notify(d);
    }

    public static void warningMaskForBandsWithDifferentSize() {
        String title = Dialogs.getDialogTitle("Mask deleted due to bands with different sizes.");
        int optionType = JOptionPane.OK_OPTION;
        int messageType = JOptionPane.INFORMATION_MESSAGE;

        final StringBuilder msgTextBuilder = new StringBuilder("The current mask will be deleted as you chose bands with different sizes.<br/>");

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        final JEditorPane textPane = new JEditorPane("text/html", msgTextBuilder.toString());
        setFont(textPane);
        textPane.setEditable(false);
        textPane.setOpaque(false);
//        textPane.addHyperlinkListener(e -> {
//            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
//                try {
//                    Desktop.getDesktop().browse(e.getURL().toURI());
//                } catch (IOException | URISyntaxException e1) {
//                    Dialogs.showWarning("Could not open URL: " + e.getDescription());
//                }
//            }
//        });
        panel.add(textPane, BorderLayout.CENTER);
        final JComboBox<Object> resamplerBox = new JComboBox<>();

        Object[] options = {"CLOSE"};
        NotifyDescriptor d = new NotifyDescriptor(panel, title, optionType, messageType, options, options[0]);
        DialogDisplayer.getDefault().notify(d);
    }


    private static List<Resampler> getAvailableResamplers(Product product) {
        final Collection<? extends Resampler> allResamplers = Lookup.getDefault().lookupAll(Resampler.class);
        List<Resampler> availableResamplers = new ArrayList<>();
        for (Resampler resampler : allResamplers) {
            if (resampler.canResample(product)) {
                availableResamplers.add(resampler);
            }
        }
        return availableResamplers;
    }

    public static boolean isMultiSize(Product selectedProduct) {
        return selectedProduct != null && selectedProduct.isMultiSize();
    }

    private static void setFont(JEditorPane textPane) {
        if (textPane.getDocument() instanceof HTMLDocument) {
            Font font = UIManager.getFont("Label.font");
            String bodyRule = "body { font-family: " + font.getFamily() + "; font-size: " + font.getSize() + "pt; }";
            ((HTMLDocument) textPane.getDocument()).getStyleSheet().addRule(bodyRule);
        }
    }

}
