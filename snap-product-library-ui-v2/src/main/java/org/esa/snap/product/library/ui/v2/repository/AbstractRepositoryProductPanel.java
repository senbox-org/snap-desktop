package org.esa.snap.product.library.ui.v2.repository;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductResults;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 23/9/2019.
 */
public abstract class AbstractRepositoryProductPanel extends JPanel {

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
    private final JButton expandOrCollapseButton;
    private final ImageIcon expandImageIcon;
    private final ImageIcon collapseImageIcon;
    private final ComponentDimension componentDimension;
    private final RepositoryProductPanelBackground repositoryProductPanelBackground;

    protected final JLabel statusLabel;

    private RepositoryProductAttributesPanel attributesPanel;

    protected AbstractRepositoryProductPanel(RepositoryProductPanelBackground repositoryProductPanelBackground,
                                             ComponentDimension componentDimension, ImageIcon expandImageIcon, ImageIcon collapseImageIcon) {

        super(new BorderLayout(componentDimension.getGapBetweenColumns(), componentDimension.getGapBetweenRows()));

        if (repositoryProductPanelBackground == null) {
            throw new NullPointerException("The repository product panel background is null.");
        }

        this.repositoryProductPanelBackground = repositoryProductPanelBackground;
        this.componentDimension = componentDimension;
        this.expandImageIcon = expandImageIcon;
        this.collapseImageIcon = collapseImageIcon;

        this.nameLabel = new JLabel("");
        this.quickLookImageLabel = new JLabel(OutputProductResults.EMPTY_ICON);
        this.firstAttributeLabel = new JLabel("");
        this.acquisitionDateLabel = new JLabel("");
        this.sizeLabel = new JLabel("");
        this.urlLabel = new JLabel("");
        this.secondAttributeLabel = new JLabel("");
        this.missionLabel = new JLabel("");
        this.statusLabel = buildStatusLabel();

        Dimension buttonSize = new Dimension(this.expandImageIcon.getIconWidth() + 2, this.expandImageIcon.getIconHeight() + 2);

        this.expandOrCollapseButton = new JButton(this.expandImageIcon);
        this.expandOrCollapseButton.setBackground(SwingUtils.TRANSPARENT_COLOR);
        this.expandOrCollapseButton.setOpaque(false);
        this.expandOrCollapseButton.setPreferredSize(buttonSize);
        this.expandOrCollapseButton.setMinimumSize(buttonSize);
        this.expandOrCollapseButton.setMaximumSize(buttonSize);
        this.expandOrCollapseButton.setFocusable(false);
        this.expandOrCollapseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                RepositoryProduct repositoryProduct = getRepositoryProduct();
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

    protected JLabel buildStatusLabel() {
        return new JLabel("");
    }

    protected final Color getDefaultForegroundColor() {
        return this.sizeLabel.getForeground();
    }

    public void refresh(OutputProductResults outputProductResults) {
        RepositoryProduct repositoryProduct = getRepositoryProduct();

        if (this.attributesPanel != null) {
            if (this.attributesPanel.getRepositoryProduct() != repositoryProduct) {
                remoteAttributesPanel();
            }
        }

        this.nameLabel.setText(repositoryProduct.getName());
        this.urlLabel.setText(buildAttributeLabelText("URL", repositoryProduct.getURL()));
        String mission = (repositoryProduct.getRemoteMission() == null) ? null : repositoryProduct.getRemoteMission().getName();
        this.missionLabel.setText(buildMissionLabelText(mission));
        this.acquisitionDateLabel.setText(buildAcquisitionDateLabelText(repositoryProduct.getAcquisitionDate()));

        Map<String, String> visibleAttributes = this.repositoryProductPanelBackground.getRemoteMissionVisibleAttributes(mission);
        updateVisibleAttributes(repositoryProduct, visibleAttributes);

        ImageIcon imageIcon = outputProductResults.getProductQuickLookImage(repositoryProduct);
        this.quickLookImageLabel.setIcon(imageIcon);

        this.sizeLabel.setText(buildSizeLabelText(repositoryProduct.getApproximateSize()));
    }

    public final RepositoryProduct getRepositoryProduct() {
        RepositoryProduct repositoryProduct = this.repositoryProductPanelBackground.getProductPanelItem(this);
        if (repositoryProduct == null) {
            throw new NullPointerException("The repository product is null.");
        }
        return repositoryProduct;
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

    private void addAttributesPanel(RepositoryProduct repositoryProduct) {
        this.attributesPanel = new RepositoryProductAttributesPanel(this.componentDimension, repositoryProduct);
        this.attributesPanel.setOpaque(false);
        add(this.attributesPanel, BorderLayout.SOUTH);
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
        List<Attribute> attributes = repositoryProduct.getRemoteAttributes();
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

    public static String buildAcquisitionDateLabelText(Date acquisitionDate) {
        String dateAsString = "N/A";
        if (acquisitionDate != null) {
            dateAsString = DATE_FORMAT.format(acquisitionDate);
        }
        return buildAttributeLabelText("Acquisition date", dateAsString);
    }

    public static String buildMissionLabelText(String mission) {
        return buildAttributeLabelText("Mission", (StringUtils.isBlank(mission) ? "N/A" : mission));
    }

    public static String buildRepositoryLabelText(String repository) {
        return buildAttributeLabelText("Repository", (StringUtils.isBlank(repository) ? "N/A" : repository));
    }

    public static String buildSizeLabelText(long sizeInBytes) {
        String sizeText = "Size: ";
        if (sizeInBytes > 0) {
            float oneKyloByte = 1024.0f;
            double sizeInMegaBytes = sizeInBytes / (oneKyloByte * oneKyloByte);
            if (sizeInMegaBytes > oneKyloByte) {
                double sizeInGigaBytes = sizeInMegaBytes / oneKyloByte;
                sizeText += FORMAT.format(sizeInGigaBytes) + " GB";
            } else {
                sizeText += FORMAT.format(sizeInMegaBytes) + " MB";
            }
        } else {
            sizeText += "N/A";
        }
        return sizeText;
    }
}
