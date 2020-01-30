package org.esa.snap.product.library.ui.v2.repository;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 23/9/2019.
 */
public abstract class AbstractRepositoryProductPanel extends JPanel {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
    private static final DecimalFormat FORMAT = new DecimalFormat("###.##");
    private static final Color TRANSPARENT_COLOR = new Color(255, 255, 255, 0);

    private final JLabel nameLabel;
    private final JLabel quickLookImageLabel;
    private final JLabel firstAttributeLabel;
    private final JLabel acquisitionDateLabel;
    private final JLabel sizeLabel;
    private final JLabel urlLabel;
    private final JLabel secondAttributeLabel;
    private final JLabel missionLabel;
    private final JButton expandOrCollapseButton;
    private final ImageIcon expandImageIcon;
    private final ImageIcon collapseImageIcon;
    private final ComponentDimension componentDimension;
    private final RepositoryProductPanelBackground repositoryProductPanelBackground;

    protected final JLabel statusLabel;

    private JPanel attributesPanel;
    private RepositoryProduct repositoryProduct;

    protected AbstractRepositoryProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                             ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon) {

        super(new BorderLayout(componentDimension.getGapBetweenColumns(), componentDimension.getGapBetweenRows()));

        this.repositoryProductPanelBackground = repositoryProductPanelBackground;
        this.componentDimension = componentDimension;
        this.expandImageIcon = expandImageIcon;
        this.collapseImageIcon = collapseImageIcon;

        this.nameLabel = new JLabel("");
        this.quickLookImageLabel = new JLabel(OutputProductListModel.EMPTY_ICON);
        this.firstAttributeLabel = new JLabel("");
        this.acquisitionDateLabel = new JLabel("");
        this.sizeLabel = new JLabel("");
        this.urlLabel = new JLabel("");
        this.secondAttributeLabel = new JLabel("");
        this.missionLabel = new JLabel("");
        this.statusLabel = new JLabel("");

        Dimension buttonSize = new Dimension(this.expandImageIcon.getIconWidth() + 2, this.expandImageIcon.getIconHeight() + 2);

        this.expandOrCollapseButton = new JButton(this.expandImageIcon);
        this.expandOrCollapseButton.setBackground(TRANSPARENT_COLOR);
        this.expandOrCollapseButton.setOpaque(false);
        this.expandOrCollapseButton.setPreferredSize(buttonSize);
        this.expandOrCollapseButton.setMinimumSize(buttonSize);
        this.expandOrCollapseButton.setMaximumSize(buttonSize);
        this.expandOrCollapseButton.setFocusable(false);
        this.expandOrCollapseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (attributesPanel == null) {
                    addAttributesPanel(repositoryProduct);
                } else {
                    remoteAttributesPanel();
                }
            }
        });

        int gapBetweenRows = componentDimension.getGapBetweenRows();
        int gapBetweenColumns = componentDimension.getGapBetweenColumns();

        Border outsideBorder = new MatteBorder(0, 0, 1, 0, UIManager.getColor("controlShadow"));
        Border insideBorder = new EmptyBorder(gapBetweenRows, gapBetweenColumns, gapBetweenRows, gapBetweenColumns);
        setBorder(new CompoundBorder(outsideBorder, insideBorder));

        JPanel labelsPanel = buildLabelsPanel();

        JPanel expandCollapsePanel = new JPanel(new GridBagLayout());
        expandCollapsePanel.setOpaque(false);
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.VERTICAL, GridBagConstraints.CENTER, 1, 1, 0, 0);
        expandCollapsePanel.add(new JLabel(), c);
        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.SOUTH, 1, 1, 0, 0);
        expandCollapsePanel.add(this.expandOrCollapseButton, c);

        add(this.nameLabel, BorderLayout.NORTH);
        add(this.quickLookImageLabel, BorderLayout.WEST);
        add(labelsPanel, BorderLayout.CENTER);
        add(expandCollapsePanel, BorderLayout.EAST);
    }

    @Override
    public Color getBackground() {
        if (this.repositoryProductPanelBackground != null) {
            return this.repositoryProductPanelBackground.getProductPanelBackground(this);
        }
        return super.getBackground();
    }

    protected final Color getDefaultForegroundColor() {
        return this.sizeLabel.getForeground();
    }

    public void refresh(int index, OutputProductListModel productListModel) {
        if (this.attributesPanel != null) {
            if (this.repositoryProduct == null || this.repositoryProduct != productListModel.getProductAt(index)) {
                remoteAttributesPanel();
            }
        }

        repositoryProduct = productListModel.getProductAt(index);

        this.nameLabel.setText(repositoryProduct.getName());
        this.urlLabel.setText(buildAttributeLabelText("URL", repositoryProduct.getURL()));

        String mission = (StringUtils.isBlank(repositoryProduct.getMission()) ? "N/A" : repositoryProduct.getMission());
        this.missionLabel.setText(buildAttributeLabelText("Mission", mission));

        Map<String, String> visibleAttributes = productListModel.getRemoteMissionVisibleAttributes(repositoryProduct.getMission());
        updateVisibleAttributes(repositoryProduct, visibleAttributes);

        ImageIcon imageIcon = productListModel.getProductQuickLookImage(repositoryProduct);
        this.quickLookImageLabel.setIcon(imageIcon);

        String dateAsString = "N/A";
        if (repositoryProduct.getAcquisitionDate() != null) {
            dateAsString = DATE_FORMAT.format(repositoryProduct.getAcquisitionDate());
        }
        this.acquisitionDateLabel.setText(buildAttributeLabelText("Acquisition date", dateAsString));

        updateSize(repositoryProduct);
    }

    private JPanel buildColumnPanel(JLabel firstLabel, JLabel secondLabel, JLabel thirdLabel) {
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();

        JPanel columnPanel = new JPanel();
        columnPanel.setLayout(new BoxLayout(columnPanel, BoxLayout.Y_AXIS));
        columnPanel.setOpaque(false);
        columnPanel.add(firstLabel);
        columnPanel.add(Box.createVerticalStrut(gapBetweenRows));
        columnPanel.add(secondLabel);
        columnPanel.add(Box.createVerticalStrut(gapBetweenRows));
        columnPanel.add(thirdLabel);

        return columnPanel;
    }

    private JPanel buildLabelsPanel() {
        JPanel firstColumnPanel = buildColumnPanel(this.missionLabel, this.acquisitionDateLabel, this.sizeLabel);
        JPanel secondColumnPanel = buildColumnPanel(this.firstAttributeLabel, new JLabel("  "), this.statusLabel);
        JPanel thirdColumnPanel = buildColumnPanel(this.secondAttributeLabel, new JLabel("  "), new JLabel("  "));

        int gapBetweenColumns = 7 * this.componentDimension.getGapBetweenColumns();
        JPanel columnsPanel = new JPanel();
        columnsPanel.setLayout(new BoxLayout(columnsPanel, BoxLayout.X_AXIS));
        columnsPanel.setOpaque(false);
        columnsPanel.add(firstColumnPanel);
        columnsPanel.add(Box.createHorizontalStrut(gapBetweenColumns));
        columnsPanel.add(secondColumnPanel);
        columnsPanel.add(Box.createHorizontalStrut(gapBetweenColumns));
        columnsPanel.add(thirdColumnPanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(this.urlLabel, BorderLayout.NORTH);
        panel.add(columnsPanel, BorderLayout.WEST);
        return panel;
    }

    private void updateSize(RepositoryProduct repositoryProduct) {
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

    private void addAttributesPanel(RepositoryProduct repositoryProduct) {
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();

        attributesPanel = new JPanel(new BorderLayout(gapBetweenColumns, gapBetweenRows));
        attributesPanel.setOpaque(false);
        attributesPanel.add(new JLabel("Attributes"), BorderLayout.NORTH);

        int columnCount = 3;
        int rowCount = repositoryProduct.getAttributes().size() / columnCount;
        if (repositoryProduct.getAttributes().size() % columnCount != 0) {
            rowCount++;
        }
        JPanel panel = new JPanel(new GridLayout(rowCount, columnCount, gapBetweenColumns, gapBetweenRows));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 5 * gapBetweenColumns, 0, 0));
        for (int i=0; i<repositoryProduct.getAttributes().size(); i++) {
            Attribute attribute = repositoryProduct.getAttributes().get(i);
            JLabel label = new JLabel(attribute.getName() + ": " + attribute.getValue());
            panel.add(label);
        }
        attributesPanel.add(panel, BorderLayout.CENTER);

        add(attributesPanel, BorderLayout.SOUTH);

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

    private void updateVisibleAttributes(RepositoryProduct repositoryProduct, Map<String, String> visibleAttributes) {
        String firstLabelText = " ";
        String secondLabelText = " ";
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

    private static String buildAttributeLabelText(String attributeDisplayName, String attributeValue) {
        return attributeDisplayName + ": " + attributeValue;
    }
}
