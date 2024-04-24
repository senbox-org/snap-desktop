package org.esa.snap.gdal.writer.ui;

import org.esa.lib.gdal.activator.GDALDriverInfo;
import org.esa.snap.dataio.gdal.GDALLoader;
import org.esa.snap.dataio.gdal.drivers.GDAL;
import org.esa.snap.dataio.gdal.writer.plugins.AbstractDriverProductWriterPlugIn;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.ExportProductAction;
import org.esa.snap.rcp.actions.file.ProductFileChooser;
import org.esa.snap.rcp.actions.file.ProductOpener;
import org.esa.snap.rcp.actions.file.WriteProductOperation;
import org.esa.snap.rcp.util.Dialogs;
import org.netbeans.api.progress.ProgressUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * @author Jean Coravu
 */
public class WriterPlugInExportProductAction extends ExportProductAction {
    private String enteredFileName;

    WriterPlugInExportProductAction() {
        super();
    }

    @Override
    public boolean isEnabled() {
        return (SnapApp.getDefault().getAppContext().getSelectedProduct() != null);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Product product = SnapApp.getDefault().getAppContext().getSelectedProduct();
        setProduct(product);
        exportProduct(product);
    }

    private Boolean exportProduct(Product product) {
        List<ExportDriversFileFilter> filters = new ArrayList<>();
        Iterator<ProductWriterPlugIn> it = ProductIOPlugInManager.getInstance().getAllWriterPlugIns();
        while (it.hasNext()) {
            ProductWriterPlugIn productWriterPlugIn = it.next();
            if (productWriterPlugIn instanceof AbstractDriverProductWriterPlugIn) {
                GDALDriverInfo writerDriver = ((AbstractDriverProductWriterPlugIn)productWriterPlugIn).getWriterDriver();
                String description = writerDriver.getDriverDisplayName() + " (*" + writerDriver.getExtensionName() + ")";
                filters.add(new ExportDriversFileFilter(description, writerDriver));
            }
        }
        if (filters.size() > 1) {
            Comparator<ExportDriversFileFilter> comparator = (item1, item2) -> item1.getDescription().compareToIgnoreCase(item2.getDescription());
            filters.sort(comparator);
        }
        ProductFileChooser fileChooser = buildFileChooserDialog(product, false, null);
        fileChooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, event -> {
            ExportDriversFileFilter selectedFileFilter = (ExportDriversFileFilter) fileChooser.getFileFilter();
            String fileName = this.enteredFileName;
            if (StringUtils.isNullOrEmpty(fileName)) {
                fileName = selectedFileFilter.getDriverInfo().getDriverDisplayName();
            } else {
                int index = fileName.lastIndexOf(".");
                if (index >= 0) {
                    fileName = fileName.substring(0, index);
                }
            }
            fileName += selectedFileFilter.getDriverInfo().getExtensionName();
            try {
                Method setFileNameMethod = fileChooser.getUI().getClass().getMethod("setFileName", String.class);
                setFileNameMethod.invoke(fileChooser.getUI(), fileName);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        fileChooser.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, event -> {
            if (event.getOldValue() != null && event.getNewValue() == null) {
                try {
                    Method getFileName = fileChooser.getUI().getClass().getMethod("getFileName");
                    this.enteredFileName = (String) getFileName.invoke(fileChooser.getUI());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        for (ExportDriversFileFilter filter : filters) {
            fileChooser.addChoosableFileFilter(filter);
        }

        int returnVal = fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null) {
            return false; // cancelled
        }
        File newFile = fileChooser.getSelectedFile();
        ExportDriversFileFilter selectedFileFilter = (ExportDriversFileFilter)fileChooser.getFileFilter();
        if (!selectedFileFilter.accept(newFile)) {
            String message = MessageFormat.format("The extension of the selected file\n" +
                            "''{0}''\n" +
                            "does not match the selected file type.\n" +
                            "Please set the file extension according to the selected file type.",
                    newFile.getPath());
            Dialogs.showWarning(getDisplayName(), message, null);
            return false;
        }
        if (!canWriteSelectedFile(newFile)) {
            return false;
        }

        Product exportProduct = fileChooser.getSubsetProduct() != null ? fileChooser.getSubsetProduct() : product;
        Band sourceBand = exportProduct.getBandAt(0);
        int gdalDataType = GDALLoader.getInstance().getGDALDataType(sourceBand.getDataType());
        GDALDriverInfo driverInfo = selectedFileFilter.getDriverInfo();
        if (!driverInfo.canExportProduct(gdalDataType)) {
            String gdalDataTypeName = GDAL.getDataTypeName(gdalDataType);
            String message = MessageFormat.format("The GDAL driver ''{0}'' does not support the data type ''{1}'' to create a new product." +
                            "\nThe available types are ''{2}''." ,
                    driverInfo.getDriverDisplayName(), gdalDataTypeName, driverInfo.getCreationDataTypes());
            Dialogs.showWarning(getDisplayName(), message, null);
            return false;
        }

        String formatName = selectedFileFilter.getDriverInfo().getWriterPluginFormatName();
        return exportProduct(exportProduct, newFile, formatName);
    }

    private boolean canWriteSelectedFile(File newFile) {
        if (newFile.isFile() && !newFile.canWrite()) {
            Dialogs.showWarning(getDisplayName(),
                    MessageFormat.format("The product\n" +
                                    "''{0}''\n" +
                                    "exists and cannot be overwritten, because it is read only.\n" +
                                    "Please choose another file or remove the write protection.",
                            newFile.getPath()),
                    null);
            return false;
        }
        return true;
    }

    private Boolean exportProduct(Product exportProduct, File newFile, String formatName) {
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

    private ProductFileChooser buildFileChooserDialog(Product product, boolean useSubset, FileFilter filter) {
        Preferences preferences = SnapApp.getDefault().getPreferences();
        ProductFileChooser fc = new ProductFileChooser(new File(preferences.get(ProductOpener.PREFERENCES_KEY_LAST_PRODUCT_DIR, ".")));
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setSubsetEnabled(useSubset);
        fc.setAdvancedEnabled(false);
        if (filter != null) {
            fc.addChoosableFileFilter(filter);
        }
        fc.setProductToExport(product);
        return fc;
    }
}
