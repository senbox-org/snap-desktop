package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.v2.RepositoryProduct;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class QueryProductResultsPanel extends JPanel {

    private final JLabel titleLabel;
    private final JList<RepositoryProduct> productList;
    private final ActionListener downloadProductListener;

    public QueryProductResultsPanel(ActionListener downloadProductListener) {
        super(new BorderLayout());

        this.downloadProductListener = downloadProductListener;
        this.titleLabel = new JLabel(getTitle());
        this.productList = new JList<RepositoryProduct>(new ProductListModel());
        this.productList.setCellRenderer(new ProductListCellRenderer());
        this.productList.setVisibleRowCount(4);
        this.productList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.productList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                productListMouseClicked(mouseEvent);
            }
        });

        add(this.titleLabel, BorderLayout.NORTH);
        add(new JScrollPane(this.productList), BorderLayout.CENTER);
    }

    public RepositoryProduct getSelectedProduct() {
        return this.productList.getSelectedValue();
    }

    public ProductListModel getListModel() {
        return (ProductListModel)this.productList.getModel();
    }

    public void addProducts(List<RepositoryProduct> products, long totalProductCount, int retrievedProductCount, String dataSourceName) {
        ProductListModel productListModel = getListModel();
        productListModel.addProducts(products);
        this.titleLabel.setText(getTitle() + ": " + "retrieved " + retrievedProductCount + " out of " + totalProductCount + " products from "+ dataSourceName+"...");
    }

    public void setProductQuickLookImage(RepositoryProduct product, Image quickLookImage) {
        ProductListModel productListModel = getListModel();
        productListModel.setProductQuickLookImage(product, quickLookImage);
    }

    public void clearProducts() {
        ProductListModel productListModel = getListModel();
        productListModel.clearProducts();
        this.titleLabel.setText(getTitle());
    }

    public void setProductDownloadPercent(RepositoryProduct productLibraryItem, short percent) {
        ProductListModel productListModel = (ProductListModel)this.productList.getModel();
        productListModel.setProductDownloadPercent(productLibraryItem, percent);
    }

    public void startSearchingProductList(String dataSourceName) {
        this.titleLabel.setText(getTitle() + ": " + "retrieving product list from " + dataSourceName+"...");
        ProductListModel productListModel = getListModel();
        if (productListModel.getSize() > 0) {
            throw new IllegalStateException("The product list must be empty before start retrieving the list.");
        }
    }

    public void startDownloadingProductList(long totalProductCount, String dataSourceName) {
        this.titleLabel.setText(getTitle() + ": " + "retrieving " + totalProductCount + " products from "+ dataSourceName+"...");
    }

    public void finishDownloadingProductList() {
        ProductListModel productListModel = getListModel();
        String text = getTitle() + ": " + productListModel.getSize();
        if (productListModel.getSize() == 1) {
            text += " product";
        } else {
            text += " products";
        }
        this.titleLabel.setText(text);
    }

    private void productListMouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
            int clickedProductIndex = this.productList.locationToIndex(mouseEvent.getPoint());
            if (clickedProductIndex >= 0) {
                Rectangle cellBounds = this.productList.getCellBounds(clickedProductIndex, clickedProductIndex);
                if (cellBounds.contains(mouseEvent.getPoint())) {
                    if (!this.productList.getSelectionModel().isSelectedIndex(clickedProductIndex)) {
                        this.productList.getSelectionModel().setSelectionInterval(clickedProductIndex, clickedProductIndex);
                    }
                    showProductsPopupMenu(mouseEvent.getX(), mouseEvent.getY()); // right mouse click
                }
            }
        }
    }

    private void showProductsPopupMenu(int mouseX, int mouseY) {
        JMenuItem downloadSelectedMenuItem = new JMenuItem("Download");
        downloadSelectedMenuItem.addActionListener(this.downloadProductListener);

        JPopupMenu popup = new JPopupMenu();
        popup.add(downloadSelectedMenuItem);

        JMenu sortMenu = new JMenu("Sort By");
        JMenuItem productNameMenuItem = new JMenuItem("Product Name");
        productNameMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                Comparator<RepositoryProduct> comparator = new Comparator<RepositoryProduct>() {
                    @Override
                    public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                        return o1.getName().compareToIgnoreCase(o2.getName());
                    }
                };
                getListModel().sortProducts(comparator);
            }
        });
        JMenuItem productTypeMenuItem = new JMenuItem("Product Type");
        productTypeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                Comparator<RepositoryProduct> comparator = new Comparator<RepositoryProduct>() {
                    @Override
                    public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                        return o1.getType().compareToIgnoreCase(o2.getType());
                    }
                };
                getListModel().sortProducts(comparator);
            }
        });
        JMenuItem instrumentMenuItem = new JMenuItem("Instrument");
        instrumentMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                Comparator<RepositoryProduct> comparator = new Comparator<RepositoryProduct>() {
                    @Override
                    public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                        return o1.getType().compareToIgnoreCase(o2.getType());
                    }
                };
                getListModel().sortProducts(comparator);
            }
        });
        JMenuItem acquisitionDateMenuItem = new JMenuItem("Acquisition Date");
        acquisitionDateMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                Comparator<RepositoryProduct> comparator = new Comparator<RepositoryProduct>() {
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
                getListModel().sortProducts(comparator);
            }
        });
        JMenuItem missionMenuItem = new JMenuItem("Mission");
        missionMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                Comparator<RepositoryProduct> comparator = new Comparator<RepositoryProduct>() {
                    @Override
                    public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                        return o1.getMission().compareToIgnoreCase(o2.getMission());
                    }
                };
                getListModel().sortProducts(comparator);
            }
        });
        JMenuItem fileSizeMenuItem = new JMenuItem("File Size");
        fileSizeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                Comparator<RepositoryProduct> comparator = new Comparator<RepositoryProduct>() {
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
                getListModel().sortProducts(comparator);
            }
        });
        sortMenu.add(productNameMenuItem);
        sortMenu.add(productTypeMenuItem);
        sortMenu.add(instrumentMenuItem);
        sortMenu.add(acquisitionDateMenuItem);
        sortMenu.add(missionMenuItem);
        sortMenu.add(fileSizeMenuItem);

        popup.add(sortMenu);

        popup.show(this.productList, mouseX, mouseY);
    }

    private String getTitle() {
        return "Product results";
    }
}
