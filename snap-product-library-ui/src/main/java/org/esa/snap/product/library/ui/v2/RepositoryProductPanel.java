package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 23/9/2019.
 */
public class RepositoryProductPanel extends JPanel {

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
    private final JButton expandOrCollapseButton;
    private final ImageIcon expandImageIcon;
    private final ImageIcon collapseImageIcon;

    private RepositoryProduct repositoryProduct;
    private JPanel attributesPanel;

    public RepositoryProductPanel(ComponentDimension componentDimension) {
        super(new GridBagLayout());

        this.nameLabel = new JLabel("");
        this.quickLookImageLabel = new JLabel("");
        this.quickLookImageLabel.setIcon(EMPTY_ICON);
        this.firstAttributeLabel = new JLabel("");
        this.acquisitionDateLabel = new JLabel("");
        this.sizeLabel = new JLabel("");
        this.urlLabel = new JLabel("");
        this.secondAttributeLabel = new JLabel("");
        this.missionLabel = new JLabel("");

        Dimension buttonSize = new Dimension(componentDimension.getTextFieldPreferredHeight(), componentDimension.getTextFieldPreferredHeight());

        this.expandImageIcon = RepositorySelectionPanel.loadImage("/org/esa/snap/product/library/ui/v2/icons/expand-arrow-40.png", buttonSize, 2);
        this.collapseImageIcon = RepositorySelectionPanel.loadImage("/org/esa/snap/product/library/ui/v2/icons/collapse-arrow-40.png", buttonSize, 2);

        this.expandOrCollapseButton = new JButton(this.expandImageIcon);
        this.expandOrCollapseButton.setPreferredSize(buttonSize);
        this.expandOrCollapseButton.setMinimumSize(buttonSize);
        this.expandOrCollapseButton.setMaximumSize(buttonSize);
        this.expandOrCollapseButton.setFocusable(false);
        this.expandOrCollapseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (attributesPanel == null) {
                    addAttributesPanel();
                } else {
                    remoteAttributesPanel();
                }
            }
        });

        this.downloadingStatusLabel = new JLabel("");

        Border border = new CompoundBorder(new MatteBorder(0, 0, 1, 0, UIManager.getColor("controlShadow")), new EmptyBorder(5, 5, 5, 5));
        setBorder(border);

        int gapBetweenRows = componentDimension.getGapBetweenRows();
        int gapBetweenColumns = componentDimension.getGapBetweenColumns();
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

        c = SwingUtils.buildConstraints(3, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 3, gapBetweenRows, 0);
        panel.add(new JLabel(""), c);

        c = SwingUtils.buildConstraints(4, 0, GridBagConstraints.NONE, GridBagConstraints.SOUTH, 1, 3, gapBetweenRows, 0);
        panel.add(this.expandOrCollapseButton, c);

        c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, 0, 0);
        add(this.nameLabel, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 2, gapBetweenRows, 0);
        add(this.quickLookImageLabel, c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        add(this.urlLabel, c);

        c = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        add(panel, c);
    }

    private void addAttributesPanel() {
        int gapBetweenRows = 5;
        int gapBetweenColumns = 5;

        int columnCount = 3;
        int rowCount = this.repositoryProduct.getAttributes().size() / columnCount;
        if (this.repositoryProduct.getAttributes().size() % columnCount != 0) {
            rowCount++;
        }
        attributesPanel = new JPanel(new GridLayout(rowCount, columnCount, 5, 5));
        attributesPanel.setOpaque(false);
        for (int i=0; i<repositoryProduct.getAttributes().size(); i++) {
            Attribute attribute = repositoryProduct.getAttributes().get(i);
            JLabel label = new JLabel(attribute.getName() + ": " + attribute.getValue());
            attributesPanel.add(label);
        }

        GridBagConstraints c = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        add(attributesPanel, c);

        this.expandOrCollapseButton.setIcon(this.collapseImageIcon);
        revalidate();
        repaint();
    }

    private void remoteAttributesPanel() {
        remove(this.attributesPanel);
        this.expandOrCollapseButton.setIcon(this.expandImageIcon);
        this.attributesPanel = null;
        revalidate();
        repaint();
    }

    public RepositoryProduct getProduct() {
        return repositoryProduct;
    }

    public void updateQuickLookImage() {
        BufferedImage quickLookImage = repositoryProduct.getQuickLookImage();
        ImageIcon imageIcon = EMPTY_ICON;
        if (quickLookImage != null) {
            Image scaledQuickLookImage = quickLookImage.getScaledInstance(EMPTY_ICON.getIconWidth(), EMPTY_ICON.getIconHeight(), BufferedImage.SCALE_FAST);
            imageIcon = new ImageIcon(scaledQuickLookImage);
        }
        this.quickLookImageLabel.setIcon(imageIcon);
    }

    private void updateVisibleAttributes(Map<String, String> visibleAttributes) {
        String firstLabelText = "";
        String secondLabelText = "";
        List<Attribute> attributes = repositoryProduct.getAttributes();
        if (visibleAttributes != null && visibleAttributes.size() > 0 && attributes != null && attributes.size() > 0) {
            for (Map.Entry<String, String> entry : visibleAttributes.entrySet()) {
                String attributeName = entry.getKey();
                String attributeDisplayName = entry.getValue();
                Attribute foundAttribute = null;
                for (int i = 0; i < attributes.size() && foundAttribute == null; i++) {
                    Attribute attribute = attributes.get(i);
                    if (attributeName.equals(attribute.getName())) {
                        foundAttribute = attribute;
                    }
                }
                if (foundAttribute != null) {
                    if (firstLabelText.length() == 0) {
                        firstLabelText = buildAttributeLabelText(attributeDisplayName, foundAttribute.getValue());
                    } else if (secondLabelText.length() == 0) {
                        secondLabelText = buildAttributeLabelText(attributeDisplayName, foundAttribute.getValue());
                        break;
                    }
                }
            }
        }
        this.firstAttributeLabel.setText(firstLabelText);
        this.secondAttributeLabel.setText(secondLabelText);
    }

    public void updateDownloadingPercent(ProductListModel productListModel) {
        ProgressPercent progressPercent = productListModel.getProductDownloadPercent(repositoryProduct);

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
    }

    public void setProduct(RepositoryProduct repositoryProduct, ProductListModel productListModel) {
        if (this.attributesPanel != null) {
            remoteAttributesPanel();
        }
        this.repositoryProduct = repositoryProduct;

        this.nameLabel.setText(repositoryProduct.getName());
        this.urlLabel.setText("URL: " + repositoryProduct.getDownloadURL());
        this.missionLabel.setText("Mission: " + repositoryProduct.getMission());

        Map<String, String> visibleAttributes = productListModel.getMissionVisibleAttributes(repositoryProduct.getMission());

        updateVisibleAttributes(visibleAttributes);

        updateQuickLookImage();

        updateDownloadingPercent(productListModel);

        String dateAsString = DATE_FORMAT.format(repositoryProduct.getAcquisitionDate());
        this.acquisitionDateLabel.setText("Acquisition date: " + dateAsString);

        String sizeText = "Size: ";
        if (repositoryProduct.getApproximateSize() > 0) {
            float oneKyloByte = 1024.0f;
            double sizeInMegaBytes = repositoryProduct.getApproximateSize() / (oneKyloByte * oneKyloByte);
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
    }

    private static String buildAttributeLabelText(String attributeDisplayName, String attributeValue) {
        return attributeDisplayName + ": " + attributeValue;
    }
}
