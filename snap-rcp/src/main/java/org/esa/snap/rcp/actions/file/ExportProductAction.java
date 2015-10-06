/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Action for exporting a product.
 *
 * @author Marco Peters
 */
public class ExportProductAction extends AbstractAction implements HelpCtx.Provider, ContextAwareAction {

    private static final String PROPERTY_FORMAT_NAME = "formatName";
    private static final String PROPERTY_HELP_CTX = "helpCtx";
    private static final String PROPERTY_USE_ALL_FILE_FILTER = "useAllFileFilter";

    private WeakReference<Product> productRef;

    /**
     * Action factory method used in NetBeans {@code layer.xml} file, e.g.
     * <p>
     * <pre>
     * &lt;file name="org-esa-snap-csv-dataio-ExportCSVProduct.instance"&gt;
     *      &lt;attr name="instanceCreate" methodvalue="org.openide.awt.Actions.context"/&gt;
     *      &lt;attr name="type" stringvalue="ProductNode"/&gt;
     *      &lt;attr name="delegate" methodvalue="ExportProductAction.create"/&gt;
     *      &lt;attr name="selectionType" stringvalue="EXACTLY_ONE"/&gt;
     *      &lt;attr name="displayName" stringvalue="CSV Product"/&gt;
     *      &lt;attr name="formatName" stringvalue="CSV"/&gt;
     *      &lt;attr name="useAllFileFilter" boolvalue="true"/&gt;
     *      &lt;attr name="helpId" stringvalue="exportCsvProduct"/&gt;
     *      &lt;attr name="ShortDescription" stringvalue="Writes a product in CSV format."/&gt;
     * &lt;/file&gt;
     * </pre>
     *
     * @param configuration Configuration attributes from layer.xml.
     * @return The action.
     *
     * @since SNAP 2
     */
    public static ExportProductAction create(Map<String, Object> configuration) {
        ExportProductAction exportProductAction = new ExportProductAction();
        exportProductAction.setFormatName((String) configuration.get(PROPERTY_FORMAT_NAME));
        exportProductAction.setHelpCtx((String) configuration.get("helpId"));
        exportProductAction.setUseAllFileFilter((Boolean) configuration.get(PROPERTY_USE_ALL_FILE_FILTER));
        return exportProductAction;
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        ProductNode productNode = actionContext.lookup(ProductNode.class);
        setProduct(productNode.getProduct());
        return this;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return (HelpCtx) getValue(PROPERTY_HELP_CTX);
    }

    public void setHelpCtx(String helpId) {
        putValue(PROPERTY_HELP_CTX, helpId != null ? new HelpCtx(helpId) : null);
    }

    public String getDisplayName() {
        return (String) getValue("displayName");
    }

    public void setFormatName(String formatName) {
        putValue(PROPERTY_FORMAT_NAME, formatName);
    }

    public void setUseAllFileFilter(Boolean useAllFileFilter) {
        putValue(PROPERTY_USE_ALL_FILE_FILTER, useAllFileFilter);
    }

    public void setProduct(Product p) {
        productRef = new WeakReference<>(p);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    /**
     * @return {@code Boolean.TRUE} on success, {@code Boolean.FALSE} on failure, or {@code null} on cancellation.
     */
    public Boolean execute() {
        Product product = productRef.get();
        if (product != null) {
            return exportProduct(product, (String) getValue(PROPERTY_FORMAT_NAME));
        } else {
            // reference was garbage collected, that's fine, no need to save.
            return true;
        }

    }

    private Boolean exportProduct(Product product, String formatName) {
        Preferences preferences = SnapApp.getDefault().getPreferences();
        ProductFileChooser fc = new ProductFileChooser(new File(preferences.get(ProductOpener.PREFERENCES_KEY_LAST_PRODUCT_DIR, ".")));
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setSubsetEnabled(true);
        fc.addChoosableFileFilter(getFileFilter(formatName));
        fc.setProductToExport(product);
        int returnVal = fc.showSaveDialog(SnapApp.getDefault().getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // cancelled
            return null;
        }

        File newFile = fc.getSelectedFile();
        if (newFile == null) {
            // cancelled
            return null;
        }

        if (newFile.isFile() && !newFile.canWrite()) {
            SnapDialogs.showWarning(getDisplayName(),
                                    MessageFormat.format("The product\n" +
                                                         "''{0}''\n" +
                                                         "exists and cannot be overwritten, because it is read only.\n" +
                                                         "Please choose another file or remove the write protection.",
                                                         newFile.getPath()),
                                    null);
            return false;
        }

        Product exportProduct = fc.getSubsetProduct() != null ? fc.getSubsetProduct() : product;


        SnapApp.getDefault().setStatusBarMessage(MessageFormat.format("Exporting product ''{0}'' to {1}...", exportProduct.getDisplayName(), newFile));

        WriteProductOperation operation = new WriteProductOperation(exportProduct, newFile, formatName, false);
        ProgressUtils.runOffEventThreadWithProgressDialog(operation,
                                                          getDisplayName(),
                                                          operation.getProgressHandle(),
                                                          true,
                                                          50,
                                                          1000);

        SnapApp.getDefault().setStatusBarMessage("");

        return operation.getStatus();


    }

    private FileFilter getFileFilter(String formatName) {
        Iterator<ProductWriterPlugIn> writerPlugIns = ProductIOPlugInManager.getInstance().getWriterPlugIns(formatName);
        if(writerPlugIns.hasNext()) {
            return writerPlugIns.next().getProductFileFilter();
        }
        return null;
    }

    private String getFileExtension(String formatName) {
        Iterator<ProductWriterPlugIn> writerPlugIns = ProductIOPlugInManager.getInstance().getWriterPlugIns(formatName);
        String fileExtension = null;
        if(writerPlugIns.hasNext()) {
            SnapFileFilter fileFilter = writerPlugIns.next().getProductFileFilter();
            if(fileFilter != null) {
                fileExtension = fileFilter.getDefaultExtension();
            }
        }
        return fileExtension;
    }

}
