package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class RepositoryProductListPanel extends JPanel {

    private final JLabel titleLabel;
    private final JLabel sortByLabel;
    private final ProgressBarHelperImpl progressBarHelper;
    private final ProductListPanel productListPanel;
    private final Map<String, Comparator<RepositoryProduct>> comparatorsMap;

    private Comparator<RepositoryProduct> currentComparator;

    public RepositoryProductListPanel(RepositorySelectionPanel repositorySelectionPanel, ComponentDimension componentDimension, ActionListener stopButtonListener) {
        super(new BorderLayout(0, componentDimension.getGapBetweenRows()/2));

        this.titleLabel = new JLabel(getTitle());
        Dimension size = this.titleLabel.getPreferredSize();
        size.height += 2; // add more pixels
        this.titleLabel.setPreferredSize(size);

        this.sortByLabel = new JLabel();
        this.sortByLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                    showProductsPopupMenu(mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });

        this.productListPanel = new ProductListPanel(repositorySelectionPanel, componentDimension);

        this.progressBarHelper = new ProgressBarHelperImpl(100, size.height) {
            @Override
            protected void setParametersEnabledWhileDownloading(boolean enabled) {
            }
        };
        this.progressBarHelper.getProgressBar().setStringPainted(true);
        this.progressBarHelper.getStopButton().addActionListener(stopButtonListener);

        String currentComparatorName = "Product Name";
        this.comparatorsMap = new LinkedHashMap<>();
        this.comparatorsMap.put(currentComparatorName, buildProductNameComparator());
        this.comparatorsMap.put("Mission", buildMissionComparator());
        this.comparatorsMap.put("Acquisition Date", buildAcquisitionDateComparator());
        this.comparatorsMap.put("File Size", buildFileSizeComparator());

        setCurrentComparator(currentComparatorName);

        int bottomMargin = componentDimension.getGapBetweenRows() / 2;
        Insets progressBarMargins = new Insets(0, 0, bottomMargin, 0);
        Insets stopButtonMargins = new Insets(0, componentDimension.getGapBetweenRows(), bottomMargin, 0);

        JPanel northPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.SOUTH, 1, 1, bottomMargin, 0);
        northPanel.add(this.titleLabel, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, progressBarMargins);
        northPanel.add(this.progressBarHelper.getProgressBar(), c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, stopButtonMargins);
        northPanel.add(this.progressBarHelper.getStopButton(), c);
        c = SwingUtils.buildConstraints(3, 0, GridBagConstraints.NONE, GridBagConstraints.SOUTH, 1, 1, bottomMargin, componentDimension.getGapBetweenColumns());
        northPanel.add(this.sortByLabel, c);

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
        this.productListPanel.addProducts(products, this.currentComparator);
        this.titleLabel.setText(getTitle() + ": " + "retrieved " + retrievedProductCount + " out of " + totalProductCount + " products from "+ dataSourceName+"...");
    }

    public void setProducts(List<RepositoryProduct> products) {
        this.productListPanel.setProducts(products, this.currentComparator);
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

    private String getSortBy() {
        return "Sort By " ;
    }

    private Comparator<RepositoryProduct> buildProductNameComparator() {
        return new Comparator<RepositoryProduct>() {
            @Override
            public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        };
    }

    private Comparator<RepositoryProduct> buildAcquisitionDateComparator() {
        return new Comparator<RepositoryProduct>() {
            @Override
            public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                Date acquisitionDate1 = o1.getAcquisitionDate();
                Date acquisitionDate2 = o2.getAcquisitionDate();
                if (acquisitionDate1 == null && acquisitionDate2 == null) {
                    return 0; // both acquisition dates are null
                }
                if (acquisitionDate1 == null && acquisitionDate2 != null) {
                    return -1; // the first acquisition date is null
                }
                if (acquisitionDate1 != null && acquisitionDate2 == null) {
                    return 1; // the second acquisition date is null
                }
                return acquisitionDate1.compareTo(acquisitionDate2);
            }
        };
    }

    private Comparator<RepositoryProduct> buildMissionComparator() {
        return new Comparator<RepositoryProduct>() {
            @Override
            public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                return o1.getMission().compareToIgnoreCase(o2.getMission());
            }
        };
    }

    private Comparator<RepositoryProduct> buildFileSizeComparator() {
        return new Comparator<RepositoryProduct>() {
            @Override
            public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                long fileSize1 = o1.getApproximateSize();
                long fileSize2 = o2.getApproximateSize();
                if (fileSize1 == fileSize2) {
                    return 0;
                }
                if (fileSize1 < fileSize2) {
                    return -1;
                }
                return 1;
            }
        };
    }

    private void showProductsPopupMenu(int mouseX, int mouseY) {
        JPopupMenu popup = new JPopupMenu();
        for (Map.Entry<String, Comparator<RepositoryProduct>> entry : this.comparatorsMap.entrySet()) {
            JMenuItem menuItem = new JMenuItem(entry.getKey());
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    JMenuItem item = (JMenuItem)actionEvent.getSource();
                    setCurrentComparator(item.getText());
                }
            });
            popup.add(menuItem);
        }
        popup.show(this.sortByLabel, mouseX, mouseY);
    }

    private void setCurrentComparator(String displayName) {
        this.currentComparator = this.comparatorsMap.get(displayName);
        this.sortByLabel.setText("Sort By: " + displayName);
        this.productListPanel.sortProducts(this.currentComparator);
    }
}
