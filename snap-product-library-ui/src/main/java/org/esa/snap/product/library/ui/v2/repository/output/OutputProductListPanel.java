package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.RepositoryProductPanelBackground;
import org.esa.snap.product.library.ui.v2.VerticalScrollablePanel;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.AbstractRepositoryProductPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.remote.products.repository.AbstractGeometry2D;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 23/9/2019.
 */
public class OutputProductListPanel extends VerticalScrollablePanel implements RepositoryProductPanelBackground {

    private static final String LIST_SELECTION_CHANGED = "listSelectionChanged";
    private static final String LIST_DATA_CHANGED = "listDataChanged";

    private final ComponentDimension componentDimension;
    private final Color backgroundColor;
    private final Color selectionBackgroundColor;
    private final MouseListener mouseListener;
    private final RepositorySelectionPanel repositorySelectionPanel;
    private final OutputProductListModel productListModel;
    private final ImageIcon expandImageIcon;
    private final ImageIcon collapseImageIcon;

    private Set<AbstractRepositoryProductPanel> selectedProductPanels;

    public OutputProductListPanel(RepositorySelectionPanel repositorySelectionPanel, ComponentDimension componentDimension, OutputProductResultsCallback outputProductResultsCallback) {
        super(null);

        this.repositorySelectionPanel = repositorySelectionPanel;
        this.componentDimension = componentDimension;

        this.expandImageIcon = RepositorySelectionPanel.loadImage("/org/esa/snap/product/library/ui/v2/icons/expand-arrow-18.png");
        this.collapseImageIcon = RepositorySelectionPanel.loadImage("/org/esa/snap/product/library/ui/v2/icons/collapse-arrow-18.png");

        this.selectedProductPanels = new HashSet<>();

        this.productListModel = new OutputProductListModel(outputProductResultsCallback) {
            @Override
            protected void fireIntervalAdded(int startIndex, int endIndex) {
                performProductsAdded(startIndex, endIndex);
            }

            @Override
            protected void fireIntervalRemoved(int startIndex, int endIndex) {
                performProductsRemoved(startIndex, endIndex);
            }

            @Override
            protected void fireIntervalChanged(int startIndex, int endIndex) {
                performProductsChanged(startIndex, endIndex);
            }
        };

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
    public Map<String, String> getRemoteMissionVisibleAttributes(String mission) {
        return this.repositorySelectionPanel.getRemoteMissionVisibleAttributes(mission);
    }

    @Override
    public Color getProductPanelBackground(AbstractRepositoryProductPanel productPanel) {
        if (this.selectedProductPanels.contains(productPanel)) {
            return this.selectionBackgroundColor;
        }
        return this.backgroundColor;
    }

    @Override
    public RepositoryProduct getProductPanelItem(AbstractRepositoryProductPanel repositoryProductPanelToFind) {
        for (int i=0; i<getComponentCount(); i++) {
            AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) getComponent(i);
            if (repositoryProductPanel == repositoryProductPanelToFind) {
                return this.productListModel.getProductAt(i);
            }
        }
        return null;
    }

    public RepositorySelectionPanel getRepositorySelectionPanel() {
        return repositorySelectionPanel;
    }

    public OutputProductListModel getProductListModel() {
        return productListModel;
    }

    public void setProducts(List<RepositoryProduct> products) {
        this.selectedProductPanels = new HashSet<>();
        revalidate();
        repaint();
        firePropertyChange(LIST_SELECTION_CHANGED, null, null);
        this.productListModel.setProducts(products);
    }

    private void performProductsChanged(int startIndex, int endIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("The start index " + startIndex +" is negative.");
        }
        int productPanelCount = getComponentCount();
        if (endIndex >= productPanelCount) {
            throw new IllegalArgumentException("The end index " + endIndex +" cannot be >= the product panel count " + productPanelCount +".");
        }
        boolean fireListSelectionChanged = false;
        for (int i=startIndex; i<=endIndex; i++) {
            AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) getComponent(i);
            repositoryProductPanel.refresh(this.productListModel);
            if (this.selectedProductPanels.contains(repositoryProductPanel)) {
                fireListSelectionChanged = true;
            }
        }
        revalidate();
        repaint();
        fireBothListeners(fireListSelectionChanged);
    }

    private void performProductsAdded(int startIndex, int endIndex) {
        AbstractProductsRepositoryPanel selectedProductsRepositoryPanel = this.repositorySelectionPanel.getSelectedProductsRepositoryPanel();
        for (int i=startIndex; i<=endIndex; i++) {
            AbstractRepositoryProductPanel repositoryProductPanel = selectedProductsRepositoryPanel.buildProductProductPanel(this, this.componentDimension, this.expandImageIcon, this.collapseImageIcon);
            repositoryProductPanel.setOpaque(true);
            repositoryProductPanel.setBackground(this.backgroundColor);
            repositoryProductPanel.addMouseListener(this.mouseListener);
            add(repositoryProductPanel);

            repositoryProductPanel.refresh(this.productListModel);
        }
        revalidate();
        repaint();
        firePropertyChange(LIST_DATA_CHANGED, null, null);
    }

    private void performProductsRemoved(int startIndex, int endIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("The start index " + startIndex +" is negative.");
        }
        int productPanelCount = getComponentCount();
        if (endIndex >= productPanelCount) {
            throw new IllegalArgumentException("The end index " + endIndex +" cannot be >= the product panel count " + productPanelCount +".");
        }
        boolean fireListSelectionChanged = false;
        for (int i=endIndex; i>=startIndex; i--) {
            AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) getComponent(i);
            if (this.selectedProductPanels.remove(repositoryProductPanel)) {
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

    public RepositoryProduct[] getSelectedProducts() {
        RepositoryProduct[] selectedProducts = new RepositoryProduct[this.selectedProductPanels.size()];
        for (int i=0, k=0; i<getComponentCount(); i++) {
            AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) getComponent(i);
            if (this.selectedProductPanels.contains(repositoryProductPanel)) {
                selectedProducts[k++] = repositoryProductPanel.getRepositoryProduct();
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

    public Path2D.Double[] getPolygonPaths() {
        int totalPathCount = 0;
        for (int i=0; i<this.productListModel.getProductCount(); i++) {
            AbstractGeometry2D productGeometry = this.productListModel.getProductAt(i).getPolygon();
            totalPathCount += productGeometry.getPathCount();
        }
        Path2D.Double[] polygonPaths = new Path2D.Double[totalPathCount];
        for (int i=0, index=0; i<this.productListModel.getProductCount(); i++) {
            AbstractGeometry2D productGeometry = this.productListModel.getProductAt(i).getPolygon();
            for (int p=0; p<productGeometry.getPathCount(); p++) {
                polygonPaths[index++] = productGeometry.getPathAt(p);
            }
        }
        return polygonPaths;
    }

    public void selectProductsByPolygonPath(List<Path2D.Double> polygonPaths) {
        int count = 0;
        int productListSize = this.productListModel.getProductCount();
        for (int k=0; k<polygonPaths.size(); k++) {
            AbstractRepositoryProductPanel foundRepositoryProductPanel = null;
            for (int i=0; i<productListSize && foundRepositoryProductPanel == null; i++) {
                AbstractGeometry2D productGeometry = this.productListModel.getProductAt(i).getPolygon();
                for (int p=0; p<productGeometry.getPathCount(); p++) {
                    if (productGeometry.getPathAt(p) == polygonPaths.get(k)) {
                        foundRepositoryProductPanel = (AbstractRepositoryProductPanel) getComponent(i);
                        break;
                    }
                }
            }
            if (foundRepositoryProductPanel != null) {
                if (count == 0) {
                    this.selectedProductPanels.clear();
                }
                count++;
                this.selectedProductPanels.add(foundRepositoryProductPanel);
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
        if (!this.selectedProductPanels.contains(repositoryProductPanel)) {
            // clear the previous selected products
            this.selectedProductPanels.clear();
            //mark as selected the clicked panel product
            this.selectedProductPanels.add(repositoryProductPanel);
            repaint();
            firePropertyChange(LIST_SELECTION_CHANGED, null, null);
        }
        showProductsPopupMenu(repositoryProductPanel, mouseEvent.getX(), mouseEvent.getY());
    }

    private void leftMouseClicked(MouseEvent mouseEvent) {
        AbstractRepositoryProductPanel repositoryProductPanel = (AbstractRepositoryProductPanel) mouseEvent.getSource();
        if (mouseEvent.isControlDown()) {
            if (!this.selectedProductPanels.add(repositoryProductPanel)) {
                // the panel is already selected
                this.selectedProductPanels.remove(repositoryProductPanel);
            }
        } else {
            this.selectedProductPanels.clear();
            this.selectedProductPanels.add(repositoryProductPanel);
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
        JPopupMenu popup = this.repositorySelectionPanel.getSelectedProductsRepositoryPanel().buildProductListPopupMenu(getSelectedProducts(), this.productListModel);
        popup.show(repositoryProductPanel, mouseX, mouseY);
    }
}
