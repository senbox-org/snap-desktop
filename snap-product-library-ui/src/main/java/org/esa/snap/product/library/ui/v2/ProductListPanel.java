package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 23/9/2019.
 */
public class ProductListPanel extends VerticalScrollablePanel implements RepositoryProductPanelBackground {

    private static final String LIST_SELECTION_CHANGED = "listSelectionChanged";
    private static final String LIST_DATA_CHANGED = "listDataChanged";

    private final ComponentDimension componentDimension;
    private final Color backgroundColor;
    private final Color selectionBackgroundColor;
    private final MouseListener mouseListener;
    private final RepositorySelectionPanel repositorySelectionPanel;
    private final Set<AbstractRepositoryProductPanel> selectedProducts;
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
            public Map<String, String> getRemoteMissionVisibleAttributes(String mission) {
                return ProductListPanel.this.repositorySelectionPanel.getRemoteMissionVisibleAttributes(mission);
            }

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

    @Override
    public Color getProductPanelBackground(AbstractRepositoryProductPanel productPanel) {
        if (this.selectedProducts.contains(productPanel)) {
            return this.selectionBackgroundColor;
        }
        return this.backgroundColor;
    }

    public ProductListModel getProductListModel() {
        return productListModel;
    }

    private void productsChanged(int startIndex, int endIndex) {
        boolean fireListSelectionChanged = false;
        for (int i=startIndex; i<=endIndex; i++) {
            AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) getComponent(i);
            repositoryProductPanel.refresh(i, this.productListModel);
            if (this.selectedProducts.contains(repositoryProductPanel)) {
                fireListSelectionChanged = true;
            }
        }
        repaint();
        fireBothListeners(fireListSelectionChanged);
    }

    private void productsAdded(int startIndex, int endIndex) {
        AbstractProductsRepositoryPanel selectedProductsRepositoryPanel = this.repositorySelectionPanel.getSelectedRepository();
        for (int i=startIndex; i<=endIndex; i++) {
            AbstractRepositoryProductPanel repositoryProductPanel = selectedProductsRepositoryPanel.buildProductProductPanel(this, this.componentDimension, this.expandImageIcon, this.collapseImageIcon);
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
            AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) getComponent(i);
            if (this.selectedProducts.remove(repositoryProductPanel)) {
                fireListSelectionChanged = true;
            }
            remove(i);
        }
        revalidate();
        repaint();
        fireBothListeners(fireListSelectionChanged);
    }

    private void fireBothListeners(boolean fireListSelectionChanged) {
        firePropertyChange(LIST_DATA_CHANGED, null, null);
        if (fireListSelectionChanged) {
            firePropertyChange(LIST_SELECTION_CHANGED, null, null);
        }
    }

    public int getProductCount() {
        return this.productListModel.getProductCount();
    }

    public RepositoryProduct[] getSelectedProducts() {
        RepositoryProduct[] selectedProducts = new RepositoryProduct[this.selectedProducts.size()];
        for (int i=0, k=0; i<getComponentCount(); i++) {
            AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) getComponent(i);
            if (this.selectedProducts.contains(repositoryProductPanel)) {
                selectedProducts[k++] = this.productListModel.getProductAt(i);
            }
        }
        return selectedProducts;
    }

    public void removePendingDownloadProducts() {
        this.productListModel.removePendingDownloadProducts();
    }

    public List<RepositoryProduct> addPendingDownloadProducts(RepositoryProduct[] pendingProducts) {
        return this.productListModel.addPendingDownloadProducts(pendingProducts);
    }

    public void sortProducts(Comparator<RepositoryProduct> comparator) {
        this.productListModel.sortProducts(comparator);
    }

    public Path2D.Double[] getPolygonPaths() {
        Path2D.Double[] polygonPaths = new Path2D.Double[this.productListModel.getProductCount()];
        for (int i=0; i<this.productListModel.getProductCount(); i++) {
            polygonPaths[i] = this.productListModel.getProductAt(i).getPolygon().getPath();
        }
        return polygonPaths;
    }

    public void selectProductsByPolygonPath(List<Path2D.Double> polygonPaths) {
        int count = 0;
        int productListSize = this.productListModel.getProductCount();
        for (int k=0; k<polygonPaths.size(); k++) {
            AbstractRepositoryProductPanel foundRepositoryProductPanel = null;
            for (int i=0; i<productListSize && foundRepositoryProductPanel == null; i++) {
                Polygon2D polygon = this.productListModel.getProductAt(i).getPolygon();
                if (polygon.getPath() == polygonPaths.get(k)) {
                    foundRepositoryProductPanel = (AbstractRepositoryProductPanel) getComponent(i);
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
        AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) mouseEvent.getSource();
        if (!this.selectedProducts.contains(repositoryProductPanel)) {
            this.selectedProducts.clear();
            this.selectedProducts.add(repositoryProductPanel);
            repaint();
            firePropertyChange(LIST_SELECTION_CHANGED, null, null);
        }
        showProductsPopupMenu(repositoryProductPanel, mouseEvent.getX(), mouseEvent.getY());
    }

    private void leftMouseClicked(MouseEvent mouseEvent) {
        AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) mouseEvent.getSource();
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

    public void addDataChangedListener(PropertyChangeListener listDataChangedListener) {
        addPropertyChangeListener(LIST_DATA_CHANGED, listDataChangedListener);
    }

    public void addSelectionChangedListener(PropertyChangeListener listSelectionChangedListener) {
        addPropertyChangeListener(LIST_SELECTION_CHANGED, listSelectionChangedListener);
    }

    private void showProductsPopupMenu(AbstractRepositoryProductPanel repositoryProductPanel, int mouseX, int mouseY) {
        JPopupMenu popup = this.repositorySelectionPanel.getSelectedRepository().buildProductListPopupMenu(getSelectedProducts(), this.productListModel);
        popup.show(repositoryProductPanel, mouseX, mouseY);
    }
}
