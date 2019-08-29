package org.esa.snap.product.library.ui.v2;

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
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

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
    private final JLabel typeLabel;
    private final JLabel acquisitionDateLabel;
    private final JLabel sizeLabel;
    private final JLabel urlLabel;
    private final JLabel instrumentLabel;
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
        this.typeLabel = new JLabel("");
        this.acquisitionDateLabel = new JLabel("");
        this.sizeLabel = new JLabel("");
        this.urlLabel = new JLabel("");
        this.instrumentLabel = new JLabel("");
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
        panel.add(this.typeLabel, c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, number * gapBetweenColumns);
        panel.add(this.instrumentLabel, c);

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
        this.typeLabel.setText("Product type: " + product.getType());
        this.urlLabel.setText("URL: " + product.getLocation());
        this.missionLabel.setText("Mission: " + product.getMission());
        this.instrumentLabel.setText("Instrument: " + product.getInstrument());

        ProductListModel productListModel = (ProductListModel)list.getModel();

        ImageIcon productQuickLookImage = productListModel.getProductQuickLookImage(product);
        ImageIcon imageIcon = (productQuickLookImage == null) ? EMPTY_ICON : productQuickLookImage;
        this.quickLookImageLabel.setIcon(imageIcon);

        Short percent = productListModel.getProductDownloadPercent(product);
        String percentText = "";
        if (percent != null) {
            if (percent < 100) {
                percentText = "Downloading: " + percent.toString() + "%";
            } else {
                percentText = "Downloaded";
            }
        }
        this.downloadingStatusLabel.setText(percentText);

        String dateAsString = DATE_FORMAT.format(product.getAcquisitionDate());
        this.acquisitionDateLabel.setText("Date: " + dateAsString);

        float oneKyloByte = 1024.0f;
        double sizeInMegaBytes = product.getApproximateSize() / (oneKyloByte * oneKyloByte);
        String size;
        if (sizeInMegaBytes > oneKyloByte) {
            double sizeInGigaBytes = sizeInMegaBytes / oneKyloByte;
            size = FORMAT.format(sizeInGigaBytes) + " GB";
        } else {
            size = FORMAT.format(sizeInMegaBytes) + " MB";
        }
        this.sizeLabel.setText("Size: "  + size);

        return this;
    }
}
