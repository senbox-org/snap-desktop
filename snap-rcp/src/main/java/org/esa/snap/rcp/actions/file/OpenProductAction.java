/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import org.esa.snap.framework.dataio.ProductIOPlugInManager;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.io.SnapFileChooser;
import org.esa.snap.util.io.SnapFileFilter;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
        id = "OpenProductAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenProductActionName",
        menuText = "#CTL_OpenProductActionMenuText",
        iconBase = "org/esa/snap/rcp/icons/Open.gif"
)
@ActionReferences({
        @ActionReference(path = "Menu/File", position = 10),
        @ActionReference(path = "Toolbars/File")
})
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

        File[] configuredFiles = getFiles();
        if (configuredFiles != null) {
            return openProductFilesCheckOpened(getFileFormat(), configuredFiles);
        }

        Iterator<ProductReaderPlugIn> readerPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();

        List<FileFilter> filters = new ArrayList<>();
        while (readerPlugIns.hasNext()) {
            ProductReaderPlugIn readerPlugIn = readerPlugIns.next();
            filters.add(readerPlugIn.getProductFileFilter());
        }
        Collections.sort(filters, new Comparator<FileFilter>() {
            @Override
            public int compare(FileFilter f1, FileFilter f2) {
                String d1 = f1.getDescription();
                String d2 = f2.getDescription();
                return d1 != null ? d1.compareTo(d2) : d2 == null ? 0 : 1;
            }
        });
        if (filters.isEmpty()) {
            SnapDialogs.showError(Bundle.LBL_NoReaderFoundText());
            return false;
        }

        Preferences preferences = SnapApp.getDefault().getPreferences();

        SnapFileChooser fc = new SnapFileChooser(new File(preferences.get(PREFERENCES_KEY_LAST_PRODUCT_DIR, ".")));
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

        String formatName = (fc.getFileFilter() instanceof SnapFileFilter)
                ? ((SnapFileFilter) fc.getFileFilter()).getFormatName()
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
