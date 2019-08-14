package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.table.AbstractTableColumn;
import org.esa.snap.product.library.ui.v2.table.CustomTableModel;
import org.esa.snap.product.library.v2.ProductLibraryItem;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 12/8/2019.
 */
public class ProductsTableModel extends CustomTableModel<ProductLibraryItem> {

    public static final BufferedImage EMPTY_ICON = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    private Map<ProductLibraryItem, BufferedImage> quickLookImages;

    public ProductsTableModel(List<AbstractTableColumn<ProductLibraryItem>> columnNames) {
        super(columnNames);

        clearImagesMap();
    }

    @Override
    public void setRecordsAndFireEvent(List<ProductLibraryItem> records) {
        clearImagesMap();

        super.setRecordsAndFireEvent(records);
    }

    @Override
    public void clearRecordsAndFireEvent() {
        clearImagesMap();

        super.clearRecordsAndFireEvent();
    }

    public BufferedImage getProductQuickLookImage(ProductLibraryItem product) {
        return this.quickLookImages.get(product);
    }

    public void setProductQuickLookImage(ProductLibraryItem product, BufferedImage quickLookImage) {
        BufferedImage image = (quickLookImage == null) ? EMPTY_ICON : quickLookImage;
        this.quickLookImages.put(product, image);
    }

    private void clearImagesMap() {
        this.quickLookImages = new HashMap<ProductLibraryItem, BufferedImage>();
    }
}
