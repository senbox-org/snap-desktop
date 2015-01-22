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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
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
    private static final Logger LOG = Logger.getLogger(OpenProductAction.class.getName());

    static RecentPaths getRecentProductPaths() {
        return new RecentPaths(SnapApp.getDefault().getPreferences(), PREFERENCES_KEY_RECENTLY_OPENED_PRODUCTS, true);
    }

    static List<File> getOpenedProductFiles() {
        return Arrays.stream(SnapApp.getDefault().getProductManager().getProducts())
                .map(Product::getFileLocation)
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

        if (getFile() != null) {
            openProductFilesCheckOpened(getFileFormat(), getFile());
            return;
        }

        Iterator<ProductReaderPlugIn> readerPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();

        List<FileFilter> filters = new ArrayList<>();
        while (readerPlugIns.hasNext()) {
            ProductReaderPlugIn readerPlugIn = readerPlugIns.next();
            filters.add(readerPlugIn.getProductFileFilter());
        }
        if (filters.isEmpty()) {
            SnapDialogs.showWarning(Bundle.LBL_NoReaderFoundText());
            return;
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
            return;
        }

        File[] files = fc.getSelectedFiles();
        if (files == null || files.length == 0) {
            return;
        }

        File currentDirectory = fc.getCurrentDirectory();
        if (currentDirectory != null) {
            preferences.put(PREFERENCES_KEY_LAST_PRODUCT_DIR, currentDirectory.toString());
        }

        String formatName = (fc.getFileFilter() instanceof BeamFileFilter)
                ? ((BeamFileFilter) fc.getFileFilter()).getFormatName()
                : null;

        openProductFilesCheckOpened(formatName, files);
    }

    private static void openProductFilesCheckOpened(final String formatName, final File... files) {
        List<File> openedFiles = getOpenedProductFiles();
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        for (File file : files) {
            if (openedFiles.contains(file)) {
                SnapDialogs.Answer answer = SnapDialogs.requestDecision(Bundle.CTL_OpenProductActionName(),
                                                    MessageFormat.format("Product\n{0}\n" +
                                                                                 "is already opened.\n" +
                                                                                 "Do you want to open another instance?", file),
                                                    true, null);
                if (answer == SnapDialogs.Answer.NO) {
                    fileList.remove(file);
                } else if (answer == SnapDialogs.Answer.CANCELLED) {
                    return;
                }
            }
        }

        for (File file : fileList) {
            openProductFileDontCheckOpened(file, formatName);
        }
    }

    private static Object openProductFileDontCheckOpened(File file, String formatName) {
        SnapApp.getDefault().setStatusBarMessage(MessageFormat.format("Reading product ''{0}''...", file.getName()));

        AtomicBoolean cancelled = new AtomicBoolean();
        ReadProductOperation operation = new ReadProductOperation(file, formatName);
        ProgressUtils.runOffEventDispatchThread(operation, Bundle.CTL_OpenProductActionName(), cancelled, true);

        SnapApp.getDefault().setStatusBarMessage("");

        if (cancelled.get()) {
            return null;
        }

        if (operation.getStatus() instanceof IOException) {
            SnapDialogs.showError(Bundle.CTL_OpenProductActionName(), ((IOException) operation.getStatus()).getMessage());
        }

        return operation.getStatus();
    }

    public static void reopenProduct(Product product, File newFile) {
        // todo!!!
    }

}
