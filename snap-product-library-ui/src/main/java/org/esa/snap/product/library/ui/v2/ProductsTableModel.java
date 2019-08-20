package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.table.AbstractTableColumn;
import org.esa.snap.product.library.ui.v2.table.CustomTableModel;
import org.esa.snap.product.library.v2.ProductLibraryItem;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 12/8/2019.
 */
public class ProductsTableModel extends CustomTableModel<ProductLibraryItem> {

    public static final BufferedImage EMPTY_ICON = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    private Map<ProductLibraryItem, BufferedImage> quickLookImages;
    private List<ProductLibraryItem> allProducts;

    public ProductsTableModel(List<AbstractTableColumn<ProductLibraryItem>> columnNames) {
        super(columnNames);

        clearItems();
    }

    @Override
    public void setRecordsAndFireEvent(List<ProductLibraryItem> records) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRecordsAndFireEvent(List<ProductLibraryItem> records) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearRecordsAndFireEvent() {
        clearItems();

        super.clearRecordsAndFireEvent();
    }

    public void addAvailableProducts(List<ProductLibraryItem> records) {
        this.allProducts.addAll(records);

        super.addRecordsAndFireEvent(records);
    }

    public void filterProducts(AbstractFilterProducts filterProducts, Rectangle2D.Double selectionRectangle) {
        List<ProductLibraryItem> filteredProducts = new ArrayList<>();
        for (int i = 0; i<this.allProducts.size(); i++) {
            ProductLibraryItem product = this.allProducts.get(i);
            if (filterProducts.matches(product.getPath(), selectionRectangle)) {
                filteredProducts.add(product);
            }
        }
        super.setRecordsAndFireEvent(filteredProducts);
    }

    public BufferedImage getProductQuickLookImage(ProductLibraryItem product) {
        return this.quickLookImages.get(product);
    }

    public void setProductQuickLookImage(ProductLibraryItem product, BufferedImage quickLookImage) {
        BufferedImage image = (quickLookImage == null) ? EMPTY_ICON : quickLookImage;
        this.quickLookImages.put(product, image);
    }

    private void clearItems() {
        this.quickLookImages = new HashMap<ProductLibraryItem, BufferedImage>();
        this.allProducts = new ArrayList<>();
    }
}
