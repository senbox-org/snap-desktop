package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;

/**
 * Created by jcoravu on 3/2/2020.
 */
public class RepositoryProductAttributesPanel extends JPanel {

    private final RepositoryProduct repositoryProduct;

    public RepositoryProductAttributesPanel(ComponentDimension componentDimension, RepositoryProduct repositoryProduct) {
        super(new GridBagLayout());

        this.repositoryProduct = repositoryProduct;

        int gapBetweenRows = componentDimension.getGapBetweenRows();
        int leftPanelPadding = 5 * componentDimension.getGapBetweenColumns();
        String noAttributesMessage = "No attributes";

        JLabel remoteAttributesTitleLabel = new JLabel("Remote Attributes");
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, 0);
        add(remoteAttributesTitleLabel, c);

        JComponent remoteComponent;
        java.util.List<Attribute> remoteAttributes = this.repositoryProduct.getRemoteAttributes();
        if (remoteAttributes != null && remoteAttributes.size() > 0) {
            remoteComponent = buildPanelAttributes(remoteAttributes, componentDimension);
        } else {
            remoteComponent = new JLabel(noAttributesMessage);
        }
        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, leftPanelPadding);
        add(remoteComponent, c);

        JLabel localAttributesTitleLabel = new JLabel("Local Attributes");
        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        add(localAttributesTitleLabel, c);

        JComponent localComponent;
        java.util.List<Attribute> localAttributes = this.repositoryProduct.getLocalAttributes();
        if (localAttributes != null && localAttributes.size() > 0) {
            localComponent = buildPanelAttributes(localAttributes, componentDimension);
        } else {
            localComponent = new JLabel(noAttributesMessage);
        }
        c = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, leftPanelPadding);
        add(localComponent, c);
    }

    public RepositoryProduct getRepositoryProduct() {
        return repositoryProduct;
    }

    private static JPanel buildPanelAttributes(java.util.List<Attribute> attributes, ComponentDimension componentDimension) {
        int gapBetweenRows = componentDimension.getGapBetweenRows();
        int gapBetweenColumns = componentDimension.getGapBetweenColumns();
        int columnCount = 3;
        int rowCount = attributes.size() / columnCount;
        if (attributes.size() % columnCount != 0) {
            rowCount++;
        }
        JPanel panel = new JPanel(new GridLayout(rowCount, columnCount, gapBetweenColumns, gapBetweenRows));
        panel.setOpaque(false);
        for (int i=0; i<attributes.size(); i++) {
            Attribute attribute = attributes.get(i);
            JLabel label = new JLabel(attribute.getName() + ": " + attribute.getValue());
            panel.add(label);
        }
        return panel;
    }
}
