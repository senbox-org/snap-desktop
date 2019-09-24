package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class RepositoryProductListPanel extends JPanel {

    private final JLabel titleLabel;
    private final ProgressBarHelperImpl progressBarHelper;
    private final ProductListPanel productListPanel;

    public RepositoryProductListPanel(RepositorySelectionPanel repositorySelectionPanel, ComponentDimension componentDimension, ActionListener stopButtonListener) {
        super(new BorderLayout(0, componentDimension.getGapBetweenRows()/2));

        this.titleLabel = new JLabel(getTitle());
        Dimension size = this.titleLabel.getPreferredSize();
        size.height += 2; // add more pixels
        this.titleLabel.setPreferredSize(size);

        this.productListPanel = new ProductListPanel(repositorySelectionPanel, componentDimension);

        this.progressBarHelper = new ProgressBarHelperImpl(100, size.height) {
            @Override
            protected void setParametersEnabledWhileDownloading(boolean enabled) {
            }
        };
        this.progressBarHelper.getProgressBar().setStringPainted(true);
        this.progressBarHelper.getStopButton().addActionListener(stopButtonListener);

        int bottomMargin = componentDimension.getGapBetweenRows()/2;
        Insets progressBarMargins = new Insets(0, 0, bottomMargin, 0);
        Insets stopButtonMargins = new Insets(0, componentDimension.getGapBetweenRows(), bottomMargin, 0);

        JPanel northPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.SOUTH, 1, 1, bottomMargin, 0);
        northPanel.add(this.titleLabel, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, progressBarMargins);
        northPanel.add(this.progressBarHelper.getProgressBar(), c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, stopButtonMargins);
        northPanel.add(this.progressBarHelper.getStopButton(), c);

        JScrollPane scrollPane = new JScrollPane(this.productListPanel);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(this.productListPanel.getBackground());

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public ProgressBarHelperImpl getProgressBarHelper() {
        return progressBarHelper;
    }

    public ProductListPanel getProductListPanel() {
        return productListPanel;
    }

    public void addProducts(List<RepositoryProduct> products, long totalProductCount, int retrievedProductCount, String dataSourceName) {
        this.productListPanel.addProducts(products);
        this.titleLabel.setText(getTitle() + ": " + "retrieved " + retrievedProductCount + " out of " + totalProductCount + " products from "+ dataSourceName+"...");
    }

    public void setProducts(List<RepositoryProduct> products) {
        this.productListPanel.setProducts(products);
        finishDownloadingProductList();
    }

    public void clearProducts() {
        this.productListPanel.clearProducts();
        this.titleLabel.setText(getTitle());
    }

    public void startSearchingProductList(String repositoryName) {
        this.titleLabel.setText(getTitle() + ": " + "retrieving product list from " + repositoryName+"...");
        if (this.productListPanel.getProductCount() > 0) {
            throw new IllegalStateException("The product list must be empty before start retrieving the list.");
        }
    }

    public void startDownloadingProductList(long totalProductCount, String dataSourceName) {
        this.titleLabel.setText(getTitle() + ": " + "retrieving " + totalProductCount + " products from "+ dataSourceName+"...");
    }

    public void finishDownloadingProductList() {
        String text = getTitle() + ": " + this.productListPanel.getProductCount();
        if (this.productListPanel.getProductCount() == 1) {
            text += " product";
        } else {
            text += " products";
        }
        this.titleLabel.setText(text);
    }

    private String getTitle() {
        return "Product results";
    }
}
