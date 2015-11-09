package org.esa.snap.rcp;

import org.esa.snap.core.datamodel.Product;

/**
 * @author Marco Peters
 */
public class MultiSizeIssue {

    private MultiSizeIssue(){
    }

    public static void showMultiSizeWarning() {
        SnapDialogs.showInformation("Limited Functionality",
                                    "<html>Please note that you have opened a product which contains <br/>" +
                                    "bands of different sizes. Not all features of SNAP will work with this product. <br/>" +
                                    "For example reprojection, subset and some masks functions will not work.",
                                    "snap.multiSizeInfo");
    }

    public static boolean isMultiSize(Product selectedProduct) {
        return selectedProduct != null && selectedProduct.isMultiSizeProduct();
    }


}
