package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.table.AbstractTableColumn;
import org.esa.snap.product.library.ui.v2.table.CustomTableModel;
import org.esa.snap.product.library.v2.ProductLibraryItem;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 12/8/2019.
 */
public class ProductsTableModel extends CustomTableModel<ProductLibraryItem> {

    public static ImageIcon EMPTY_ICON;
    static {
        Image image = new BufferedImage(150, 100, BufferedImage.TYPE_INT_ARGB);
        EMPTY_ICON = new ImageIcon(image);
    }

    private Map<ProductLibraryItem, ImageIcon> quickLookImages;

    public ProductsTableModel(List<AbstractTableColumn<ProductLibraryItem>> columnNames) {
        super(columnNames);

        this.quickLookImages = new HashMap<ProductLibraryItem, ImageIcon>();
    }

    @Override
    public void setRecordsAndFireEvent(List<ProductLibraryItem> records) {
        this.quickLookImages = new HashMap<ProductLibraryItem, ImageIcon>();

        super.setRecordsAndFireEvent(records);
    }

    @Override
    public void clearRecordsAndFireEvent() {
        this.quickLookImages = new HashMap<ProductLibraryItem, ImageIcon>();

        super.clearRecordsAndFireEvent();
    }

    public ImageIcon getProductQuickLookImage(ProductLibraryItem product) {
        return this.quickLookImages.get(product);
    }

    public void setProductQuickLookImage(ProductLibraryItem product, Image quickLookImage) {
        ImageIcon imageIcon;
        if (quickLookImage == null) {
            imageIcon = EMPTY_ICON;
        } else {
            imageIcon = new ImageIcon(quickLookImage);
        }
        this.quickLookImages.put(product, imageIcon);
    }
}
