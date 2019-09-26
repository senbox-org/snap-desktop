package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jcoravu on 23/9/2019.
 */
public class ProductListPanel extends VerticalScrollablePanel {

    private static final String LIST_SELECTION_CHANGED = "listSelectionChanged";
    private static final String LIST_DATA_CHANGED = "listDataChanged";

    private final ComponentDimension componentDimension;
    private final Color backgroundColor;
    private final Color selectionBackgroundColor;
    private final MouseListener mouseListener;
    private final RepositorySelectionPanel repositorySelectionPanel;
    private final Set<RepositoryProductPanel> selectedProducts;
    private final ProductListModel productListModel;
    private final ImageIcon expandImageIcon;
    private final ImageIcon collapseImageIcon;

    public ProductListPanel(RepositorySelectionPanel repositorySelectionPanel, ComponentDimension componentDimension) {
        super(null);

        this.repositorySelectionPanel = repositorySelectionPanel;
        this.componentDimension = componentDimension;

        this.expandImageIcon = RepositorySelectionPanel.loadImage("/org/esa/snap/product/library/ui/v2/icons/expand-arrow-18.png");
        this.collapseImageIcon = RepositorySelectionPanel.loadImage("/org/esa/snap/product/library/ui/v2/icons/collapse-arrow-18.png");

        this.productListModel = new ProductListModel() {
            @Override
            protected void fireIntervalAdded(int startIndex, int endIndex) {
                productsAdded(startIndex, endIndex);
            }

            @Override
            protected void fireIntervalRemoved(int startIndex, int endIndex) {
                productsRemoved(startIndex, endIndex);
            }

            @Override
            protected void fireIntervalChanged(int startIndex, int endIndex) {
                productsChanged(startIndex, endIndex);
            }
        };
        this.selectedProducts = new HashSet<>();

        JList list = new JList();
        this.backgroundColor = list.getBackground();
        this.selectionBackgroundColor = list.getSelectionBackground();

        this.mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                    leftMouseClicked(mouseEvent);
                } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    rightMouseClicked(mouseEvent);
                }
            }
        };

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        setBackground(this.backgroundColor);
    }

    public void addProducts(List<RepositoryProduct> products) {
        this.productListModel.addProducts(products);
    }

    private void productsChanged(int startIndex, int endIndex) {
        boolean fireListSelectionChanged = false;
        for (int i=startIndex; i<=endIndex; i++) {
            RepositoryProductPanel repositoryProductPanel = (RepositoryProductPanel) getComponent(i);
            repositoryProductPanel.refresh(i, this.productListModel);
            if (this.selectedProducts.contains(repositoryProductPanel)) {
                fireListSelectionChanged = true;
            }
        }
        repaint();
        firePropertyChange(LIST_DATA_CHANGED, null, null);
        if (fireListSelectionChanged) {
            firePropertyChange(LIST_SELECTION_CHANGED, null, null);
        }
    }

    private void productsAdded(int startIndex, int endIndex) {
        for (int i=startIndex; i<=endIndex; i++) {
            RepositoryProductPanel repositoryProductPanel = new RepositoryProductPanel(this.componentDimension, this.expandImageIcon, this.collapseImageIcon) {
                @Override
                public Color getBackground() {
                    return getProductPanelBackground(this);
                }
            };
            repositoryProductPanel.setOpaque(true);
            repositoryProductPanel.setBackground(this.backgroundColor);
            repositoryProductPanel.addMouseListener(this.mouseListener);
            add(repositoryProductPanel);

            repositoryProductPanel.refresh(i, this.productListModel);
        }
        revalidate();
        repaint();
        firePropertyChange(LIST_DATA_CHANGED, null, null);
    }

    private void productsRemoved(int startIndex, int endIndex) {
        boolean fireListSelectionChanged = false;
        for (int i=endIndex; i>=startIndex; i--) {
            RepositoryProductPanel repositoryProductPanel = (RepositoryProductPanel) getComponent(i);
            if (this.selectedProducts.remove(repositoryProductPanel)) {
                fireListSelectionChanged = true;
            }
            remove(i);
        }
        revalidate();
        repaint();
        firePropertyChange(LIST_DATA_CHANGED, null, null);
        if (fireListSelectionChanged) {
            firePropertyChange(LIST_SELECTION_CHANGED, null, null);
        }
    }

    public int getProductCount() {
        return this.productListModel.getProductCount();
    }

    public void clearProducts() {
        this.productListModel.clear();
    }

    public RepositoryProduct[] getSelectedProducts() {
        RepositoryProduct[] selectedProducts = new RepositoryProduct[this.selectedProducts.size()];
        for (int i=0, k=0; i<getComponentCount(); i++) {
            RepositoryProductPanel repositoryProductPanel = (RepositoryProductPanel) getComponent(i);
            if (this.selectedProducts.contains(repositoryProductPanel)) {
                selectedProducts[k++] = this.productListModel.getProductAt(i);
            }
        }
        return selectedProducts;
    }

    public void setProductQuickLookImage(RepositoryProduct repositoryProduct, BufferedImage quickLookImage) {
        this.productListModel.setProductQuickLookImage(repositoryProduct, quickLookImage);
    }

    public void removePendingDownloadProducts() {
        this.productListModel.removePendingDownloadProducts();
    }

    public List<RepositoryProduct> addPendingDownloadProducts(RepositoryProduct[] pendingProducts) {
        return this.productListModel.addPendingDownloadProducts(pendingProducts);
    }

    public void setProductDownloadPercent(RepositoryProduct repositoryProduct, short progressPercent) {
        this.productListModel.setProductDownloadPercent(repositoryProduct, progressPercent);
    }

    public void setProducts(List<RepositoryProduct> products) {
        clearProducts();
        addProducts(products);
    }

    public Path2D.Double[] getPolygonPaths() {
        Path2D.Double[] polygonPaths = new Path2D.Double[this.productListModel.getProductCount()];
        for (int i=0; i<getComponentCount(); i++) {
            polygonPaths[i] = this.productListModel.getProductAt(i).getPolygon().getPath();
        }
        return polygonPaths;
    }

    public void selectProductsByPolygonPath(List<Path2D.Double> polygonPaths) {
        int count = 0;
        int productListSize = this.productListModel.getProductCount();
        for (int k=0; k<polygonPaths.size(); k++) {
            RepositoryProductPanel foundRepositoryProductPanel = null;
            for (int i=0; i<productListSize && foundRepositoryProductPanel == null; i++) {
                Polygon2D polygon = this.productListModel.getProductAt(i).getPolygon();
                if (polygon.getPath() == polygonPaths.get(k)) {
                    foundRepositoryProductPanel = (RepositoryProductPanel) getComponent(i);
                }
            }
            if (foundRepositoryProductPanel != null) {
                if (count == 0) {
                    this.selectedProducts.clear();
                }
                count++;
                this.selectedProducts.add(foundRepositoryProductPanel);
                scrollRectToVisible(foundRepositoryProductPanel.getBounds());
                repaint();
                firePropertyChange(LIST_SELECTION_CHANGED, null, null);
            } else {
                throw new IllegalArgumentException("The polygon path does not exist in the list.");
            }
        }
    }

    private void rightMouseClicked(MouseEvent mouseEvent) {
        RepositoryProductPanel repositoryProductPanel = (RepositoryProductPanel) mouseEvent.getSource();
        if (!this.selectedProducts.contains(repositoryProductPanel)) {
            this.selectedProducts.clear();
            this.selectedProducts.add(repositoryProductPanel);
            repaint();
            firePropertyChange(LIST_SELECTION_CHANGED, null, null);
        }
        showProductsPopupMenu(repositoryProductPanel, mouseEvent.getX(), mouseEvent.getY());
    }

    private void leftMouseClicked(MouseEvent mouseEvent) {
        RepositoryProductPanel repositoryProductPanel = (RepositoryProductPanel) mouseEvent.getSource();
        if (mouseEvent.isControlDown()) {
            if (!this.selectedProducts.add(repositoryProductPanel)) {
                // the panel is already selected
                this.selectedProducts.remove(repositoryProductPanel);
            }
        } else {
            this.selectedProducts.clear();
            this.selectedProducts.add(repositoryProductPanel);
        }
        repaint();
        firePropertyChange(LIST_SELECTION_CHANGED, null, null);
    }

    public void setDataChangedListener(PropertyChangeListener listDataChangedListener) {
        addPropertyChangeListener(LIST_DATA_CHANGED, listDataChangedListener);
    }

    public void setSelectionChangedListener(PropertyChangeListener listSelectionChangedListener) {
        addPropertyChangeListener(LIST_SELECTION_CHANGED, listSelectionChangedListener);
    }

    private Color getProductPanelBackground(RepositoryProductPanel productPanel) {
        if (this.selectedProducts.contains(productPanel)) {
            return this.selectionBackgroundColor;
        }
        return this.backgroundColor;
    }

    private void showProductsPopupMenu(RepositoryProductPanel repositoryProductPanel, int mouseX, int mouseY) {
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
                productListModel.sortProducts(comparator);
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
                productListModel.sortProducts(comparator);
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
                productListModel.sortProducts(comparator);
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
                productListModel.sortProducts(comparator);
            }
        });
        sortMenu.add(productNameMenuItem);
        sortMenu.add(acquisitionDateMenuItem);
        sortMenu.add(missionMenuItem);
        sortMenu.add(fileSizeMenuItem);

        JPopupMenu popup = this.repositorySelectionPanel.getSelectedRepository().buildProductListPopupMenu();
        popup.add(sortMenu);

        popup.show(repositoryProductPanel, mouseX, mouseY);
    }
}
