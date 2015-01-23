/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.file;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.SnapDialogs;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.actions.file.OpenProductAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenProductActionName",
        menuText = "#CTL_OpenProductActionMenuText"
)
@ActionReference(path = "Menu/File", position = 10)
@NbBundle.Messages({
        "CTL_OpenProductActionName=Open Product",
        "CTL_OpenProductActionMenuText=Open Product...",
        "LBL_NoReaderFoundText=No appropriate product reader found.",

})
public final class OpenProductAction extends AbstractAction {

    public static final String PREFERENCES_KEY_RECENTLY_OPENED_PRODUCTS = "recently_opened_products";
    public static final String PREFERENCES_KEY_LAST_PRODUCT_DIR = "last_product_dir";

    static RecentPaths getRecentProductPaths() {
        return new RecentPaths(SnapApp.getDefault().getPreferences(), PREFERENCES_KEY_RECENTLY_OPENED_PRODUCTS, true);
    }

    static List<File> getOpenedProductFiles() {
        return Arrays.stream(SnapApp.getDefault().getProductManager().getProducts())
                .map(Product::getFileLocation)
                .filter(file -> file != null)
                .collect(Collectors.toList());
    }

    public File getFile() {
        Object value = getValue("file");
        if (value instanceof File) {
            return (File) value;
        }
        return null;
    }

    public void setFile(File file) {
        putValue("file", file);
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

        if (getFile() != null) {
            return openProductFilesCheckOpened(getFileFormat(), getFile());
        }

        Iterator<ProductReaderPlugIn> readerPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();

        List<FileFilter> filters = new ArrayList<>();
        while (readerPlugIns.hasNext()) {
            ProductReaderPlugIn readerPlugIn = readerPlugIns.next();
            filters.add(readerPlugIn.getProductFileFilter());
        }
        if (filters.isEmpty()) {
            SnapDialogs.showError(Bundle.LBL_NoReaderFoundText());
            return false;
        }

        Preferences preferences = SnapApp.getDefault().getPreferences();

        BeamFileChooser fc = new BeamFileChooser(new File(preferences.get(PREFERENCES_KEY_LAST_PRODUCT_DIR, ".")));
        fc.setDialogTitle(Bundle.CTL_OpenProductActionName());
        fc.setAcceptAllFileFilterUsed(true);
        filters.forEach(fc::addChoosableFileFilter);
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setMultiSelectionEnabled(true);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnVal = fc.showOpenDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // cancelled
            return null;
        }

        File[] files = fc.getSelectedFiles();
        if (files == null || files.length == 0) {
            // cancelled
            return null;
        }

        File currentDirectory = fc.getCurrentDirectory();
        if (currentDirectory != null) {
            preferences.put(PREFERENCES_KEY_LAST_PRODUCT_DIR, currentDirectory.toString());
        }

        String formatName = (fc.getFileFilter() instanceof BeamFileFilter)
                ? ((BeamFileFilter) fc.getFileFilter()).getFormatName()
                : null;

        return openProductFilesCheckOpened(formatName, files);
    }

    private static Boolean openProductFilesCheckOpened(final String formatName, final File... files) {
        List<File> openedFiles = getOpenedProductFiles();
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        for (File file : files) {
            if (openedFiles.contains(file)) {
                SnapDialogs.Answer answer = SnapDialogs.requestDecision(Bundle.CTL_OpenProductActionName(),
                                                                        MessageFormat.format("Product\n" +
                                                                                                     "{0}\n" +
                                                                                                     "is already opened.\n" +
                                                                                                     "Do you want to open another instance?", file),
                                                                        true, null);
                if (answer == SnapDialogs.Answer.NO) {
                    fileList.remove(file);
                } else if (answer == SnapDialogs.Answer.CANCELLED) {
                    return null;
                }
            }
        }

        Boolean summaryStatus = true;
        for (File file : fileList) {
            Boolean status = openProductFileDoNotCheckOpened(file, formatName);
            if (status == null) {
                // Cancelled
                summaryStatus = null;
                break;
            } else if (!Boolean.TRUE.equals(status)) {
                summaryStatus = status;
            }
        }

        return summaryStatus;
    }

    private static Boolean openProductFileDoNotCheckOpened(File file, String formatName) {
        SnapApp.getDefault().setStatusBarMessage(MessageFormat.format("Reading product ''{0}''...", file.getName()));

        AtomicBoolean cancelled = new AtomicBoolean();
        ReadProductOperation operation = new ReadProductOperation(file, formatName);
        ProgressUtils.runOffEventDispatchThread(operation, Bundle.CTL_OpenProductActionName(), cancelled, true, 50, 3000);

        SnapApp.getDefault().setStatusBarMessage("");

        if (cancelled.get()) {
            return null;
        }

        return operation.getStatus();
    }
}
