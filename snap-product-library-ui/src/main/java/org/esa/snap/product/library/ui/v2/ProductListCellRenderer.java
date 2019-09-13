package org.esa.snap.product.library.ui.v2;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class ProductListCellRenderer extends JPanel implements ListCellRenderer<RepositoryProduct> {

    public static final ImageIcon EMPTY_ICON;
    static {
        BufferedImage emptyImage = new BufferedImage(75, 75, BufferedImage.TYPE_INT_ARGB);
        EMPTY_ICON = new ImageIcon(emptyImage);
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
    private static final DecimalFormat FORMAT = new DecimalFormat("###.##");

    private final JLabel nameLabel;
    private final JLabel quickLookImageLabel;
    private final JLabel firstAttributeLabel;
    private final JLabel acquisitionDateLabel;
    private final JLabel sizeLabel;
    private final JLabel urlLabel;
    private final JLabel secondAttributeLabel;
    private final JLabel missionLabel;
    private final JLabel downloadingStatusLabel;

    public ProductListCellRenderer() {
        super(new GridBagLayout());

        setOpaque(true);
        Border border = new CompoundBorder(new MatteBorder(0, 0, 1, 0, UIManager.getColor("controlShadow")), new EmptyBorder(5, 5, 5, 5));
        setBorder(border);

        this.nameLabel = new JLabel("");
        this.quickLookImageLabel = new JLabel("");
        this.quickLookImageLabel.setIcon(EMPTY_ICON);
        this.firstAttributeLabel = new JLabel("");
        this.acquisitionDateLabel = new JLabel("");
        this.sizeLabel = new JLabel("");
        this.urlLabel = new JLabel("");
        this.secondAttributeLabel = new JLabel("");
        this.missionLabel = new JLabel("");
        this.downloadingStatusLabel = new JLabel("");

        int gapBetweenRows = 5;
        int gapBetweenColumns = 5;
        int number = 7;

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        panel.add(this.missionLabel, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, number * gapBetweenColumns);
        panel.add(this.firstAttributeLabel, c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, number * gapBetweenColumns);
        panel.add(this.secondAttributeLabel, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        panel.add(this.acquisitionDateLabel, c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 2, 1, gapBetweenRows, gapBetweenColumns);
        panel.add(new JLabel(), c);

        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        panel.add(this.sizeLabel, c);
        c = SwingUtils.buildConstraints(1, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 2, 1, gapBetweenRows, number * gapBetweenColumns);
        panel.add(this.downloadingStatusLabel, c);

        c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, 0, 0);
        add(this.nameLabel, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 2, gapBetweenRows, 0);
        add(this.quickLookImageLabel, c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        add(this.urlLabel, c);

        c = SwingUtils.buildConstraints(1, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        add(panel, c);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends RepositoryProduct> list, RepositoryProduct product, int index, boolean isSelected, boolean cellHasFocus) {
        Color backgroundColor;
        Color foregroundColor;
        if (isSelected) {
            backgroundColor = list.getSelectionBackground();
            foregroundColor = list.getSelectionForeground();
        } else {
            backgroundColor = list.getBackground();
            foregroundColor = list.getForeground();
        }
        setBackground(backgroundColor);
        setForeground(foregroundColor);

        this.nameLabel.setText(product.getName());
        this.urlLabel.setText("URL: " + product.getDownloadURL());
        this.missionLabel.setText("Mission: " + product.getMission());

        String firstLabelText = "";
        String secondLabelText = "";
        List<Attribute> attributes = product.getAttributes();
        if (attributes != null) {
            if (attributes.size() >= 1) {
                firstLabelText = buildAttributeLabelText(attributes.get(0));
            }
            if (attributes.size() >= 2) {
                secondLabelText = buildAttributeLabelText(attributes.get(1));
            }
        }
        this.firstAttributeLabel.setText(firstLabelText);
        this.secondAttributeLabel.setText(secondLabelText);

        ProductListModel productListModel = (ProductListModel)list.getModel();

        BufferedImage quickLookImage = product.getQuickLookImage();
        ImageIcon imageIcon = EMPTY_ICON;
        if (quickLookImage != null) {
            Image scaledQuickLookImage = quickLookImage.getScaledInstance(EMPTY_ICON.getIconWidth(), EMPTY_ICON.getIconHeight(), BufferedImage.SCALE_FAST);
            imageIcon = new ImageIcon(scaledQuickLookImage);
        }
        this.quickLookImageLabel.setIcon(imageIcon);

        ProgressPercent progressPercent = productListModel.getProductDownloadPercent(product);
        String percentText = "";
        if (progressPercent != null) {
            // the product is pending download or downloading
            if (progressPercent.isDownloading()) {
                if (progressPercent.getValue() < 100) {
                    percentText = "Downloading: " + Integer.toString(progressPercent.getValue()) + "%";
                } else {
                    percentText = "Downloaded";
                }
            } else if (progressPercent.isPendingDownload()){
                percentText = "Pending download";
            } else if (progressPercent.isStoppedDownload()) {
                percentText = "Downloading: " + Integer.toString(progressPercent.getValue()) + "% (stopped)";
            } else {
                throw new IllegalStateException("The percent progress status is unknown. The value is " + progressPercent.getValue());
            }
        }
        this.downloadingStatusLabel.setText(percentText);

        String dateAsString = DATE_FORMAT.format(product.getAcquisitionDate());
        this.acquisitionDateLabel.setText("Acquisition date: " + dateAsString);

        String sizeText = "Size: ";
        if (product.getApproximateSize() > 0) {
            float oneKyloByte = 1024.0f;
            double sizeInMegaBytes = product.getApproximateSize() / (oneKyloByte * oneKyloByte);
            if (sizeInMegaBytes > oneKyloByte) {
                double sizeInGigaBytes = sizeInMegaBytes / oneKyloByte;
                sizeText += FORMAT.format(sizeInGigaBytes) + " GB";
            } else {
                sizeText += FORMAT.format(sizeInMegaBytes) + " MB";
            }
        } else {
            sizeText += "N/A";
        }
        this.sizeLabel.setText(sizeText);

        return this;
    }

    private static String buildAttributeLabelText(Attribute attribute) {
        String displayName;
        if (attribute.getName().equalsIgnoreCase("producttype")) {
            displayName = "Product Type";
        } else if (attribute.getName().equalsIgnoreCase("instrumentshortname")) {
            displayName = "Instrument";
        } else {
            displayName = attribute.getName();
        }
        return displayName + ": " + attribute.getValue();
    }
}
