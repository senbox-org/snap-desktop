package org.esa.snap.rcp.session;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.actions.file.CloseAllProductsAction;
import org.esa.snap.rcp.actions.file.CloseProductAction;
import org.esa.snap.rcp.actions.file.SaveProductAction;

import java.io.File;

/**
 * Created by Samurai on 25/06/15.
 */
public class VisatApp {
    private File sessionFile;

    public File getSessionFile() {
        return sessionFile;
    }

    public void setSessionFile(File sessionFile) {
        this.sessionFile = sessionFile;
    }

    public static VisatApp getApp() {
        return new VisatApp();
    }

    public void saveProduct(Product product) {
        SaveProductAction saveProductAction = new SaveProductAction(product);
        saveProductAction.execute();
    }

    public void closeAllProducts() {
        CloseAllProductsAction closeProductAction = new CloseAllProductsAction();
        closeProductAction.execute();
    }
}
