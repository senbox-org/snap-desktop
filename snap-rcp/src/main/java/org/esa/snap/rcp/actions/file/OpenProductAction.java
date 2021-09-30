/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "OpenProductAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenProductActionName",
        menuText = "#CTL_OpenProductActionMenuText",
        iconBase = "org/esa/snap/rcp/icons/" + PackageDefaults.OPEN_PRODUCT_ACTION_ICON
)
@ActionReferences({
        @ActionReference(
                path = "Menu/" + PackageDefaults.OPEN_PRODUCT_ACTION_MENU_PATH,
                position = PackageDefaults.OPEN_PRODUCT_ACTION_MENU_POSITION),
        @ActionReference(
                path = "Toolbars/" + PackageDefaults.OPEN_PRODUCT_ACTION_TOOLBAR_NAME,
                position = PackageDefaults.OPEN_PRODUCT_ACTION_TOOLBAR_POSITION)
})
@NbBundle.Messages({
        "CTL_OpenProductActionName=" + PackageDefaults.OPEN_PRODUCT_ACTION_NAME,
        "CTL_OpenProductActionMenuText=Open Product...",
        "LBL_NoReaderFoundText=No appropriate product reader found.",

})
public final class OpenProductAction extends AbstractAction {

    public static final String PREFERENCES_KEY_RECENTLY_OPENED_PRODUCTS = "recently_opened_products";
    public static final String PREFERENCES_KEY_LAST_PRODUCT_DIR = "last_product_open_dir";


    static RecentPaths getRecentProductPaths() {
        return new RecentPaths(SnapApp.getDefault().getPreferences(), PREFERENCES_KEY_RECENTLY_OPENED_PRODUCTS, true);
    }

    static List<File> getOpenedProductFiles() {
        return Arrays.stream(SnapApp.getDefault().getProductManager().getProducts())
                .map(Product::getFileLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public File[] getFiles() {
        Object value = getValue("files");
        if (value instanceof File[]) {
            return (File[]) value;
        }
        return null;
    }

    public void setFile(File file) {
        setFiles(file);
    }

    public void setFiles(File... files) {
        putValue("files", files);
    }

    public String getFileFormat() {
        Object value = getValue("fileFormat");
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public void setFileFormat(String fileFormat) {
        putValue("fileFormat", fileFormat);
    }

    public void setUseAllFileFilter(boolean useAllFileFilter) {
        putValue("useAllFileFilter", useAllFileFilter);
    }

    public boolean getUseAllFileFilter() {
        // by default the All file filter is used
        return getBooleanProperty("useAllFileFilter", true);
    }

    private boolean getBooleanProperty(String propertyName, Boolean defaultValue) {
        final Object propValue = getValue(propertyName);
        if (propValue == null) {
            return defaultValue;
        } else {
            return Boolean.TRUE.equals(propValue);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    /**
     * Executes the action command.
     *
     * @return {@code Boolean.TRUE} on success, {@code Boolean.FALSE} on failure, or {@code null} on cancellation.
     */
    public Boolean execute() {
        final ProductOpener opener = new ProductOpener();
        opener.setFiles(getFiles());
        opener.setFileFormat(getFileFormat());
        opener.setUseAllFileFilter(getUseAllFileFilter());
        opener.setMultiSelectionEnabled(true);
        opener.setSubsetImportEnabled(false);
        return opener.openProduct();
    }

}
