package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Created by jcoravu on 3/2/2020.
 */
public class RepositoryProductAttributesPanel extends JPanel {

    private final RepositoryProduct repositoryProduct;

    public RepositoryProductAttributesPanel(ComponentDimension componentDimension, RepositoryProduct repositoryProduct) {
        super();

        this.repositoryProduct = repositoryProduct;

        int gapBetweenRows = componentDimension.getGapBetweenRows();
        int gapBetweenColumns = componentDimension.getGapBetweenColumns();

        setLayout(new BorderLayout(gapBetweenColumns, gapBetweenRows));

        add(new JLabel("Attributes"), BorderLayout.NORTH);

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
        add(panel, BorderLayout.CENTER);
    }

    public RepositoryProduct getRepositoryProduct() {
        return repositoryProduct;
    }
}
