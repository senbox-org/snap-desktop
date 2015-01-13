/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.file;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.snap.gui.SnapApp;
import org.openide.NotifyDescriptor;
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
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
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
        "CTL_OpenProductActionMenuText=Open Product..."
})
public final class OpenProductAction extends AbstractAction {

    public static final String PREFERENCES_KEY_RECENTLY_OPENED_PRODUCTS = "recentlyOpenedProducts";
    public static final String PREFERENCES_KEY_LAST_PRODUCT_DIR = "lastProductDir";
    private static final Logger LOG = Logger.getLogger(OpenProductAction.class.getName());

    static RecentPaths getRecentProductPaths() {
        return new RecentPaths(SnapApp.getInstance().getPreferences(), PREFERENCES_KEY_RECENTLY_OPENED_PRODUCTS, true);
    }

    static List<File> getOpenedProductFiles() {
        return Arrays.stream(SnapApp.getInstance().getProductManager().getProducts())
                .map(Product::getFileLocation)
                .collect(Collectors.toList());
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        Iterator<ProductReaderPlugIn> readerPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();

        List<FileFilter> filters = new ArrayList<>();
        while (readerPlugIns.hasNext()) {
            ProductReaderPlugIn readerPlugIn = readerPlugIns.next();
            filters.add(readerPlugIn.getProductFileFilter());
        }
        if (filters.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No reader found!");
            return;
        }

        Preferences preferences = SnapApp.getInstance().getPreferences();

        JFileChooser fc = new JFileChooser(new File(preferences.get(PREFERENCES_KEY_LAST_PRODUCT_DIR, ".")));
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

        openProductFiles(formatName, files);
    }

    static void openProductFile(final String formatName, final File file) {
        openProductFiles(formatName, file);
    }

    static void openProductFiles(final String formatName, final File... files) {
        List<File> openedFiles = getOpenedProductFiles();
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        for (File file : files) {
            if (openedFiles.contains(file)) {
                int i = SnapApp.getInstance().showQuestionDialog(
                        Bundle.CTL_OpenProductActionName(),
                        MessageFormat.format("Product\n{0}\nis already opened.\nDo you want to open another instance?", file),
                        null);
                if (NotifyDescriptor.NO_OPTION.equals(i)) {
                    fileList.remove(file);
                } else if (!NotifyDescriptor.YES_OPTION.equals(i)) {
                    // cancel!
                    return;
                }
            }
        }

        SwingWorker<List<IOException>, Object> swingWorker = new SwingWorker<List<IOException>, Object>() {
            @Override
            protected List<IOException> doInBackground() {
                List<IOException> problems = new ArrayList<>();
                for (File file : fileList) {
                    try {
                        Product product = formatName != null ? ProductIO.readProduct(file, formatName) : ProductIO.readProduct(file);
                        getRecentProductPaths().add(file.getPath());
                        SwingUtilities.invokeLater(() -> SnapApp.getInstance().getProductManager().addProduct(product));
                    } catch (IOException problem) {
                        problems.add(problem);
                    }
                }
                return problems;
            }

            @Override
            protected void done() {
                List<IOException> problems;
                try {
                    problems = get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                if (!problems.isEmpty()) {
                    StringBuilder problemsMessage = new StringBuilder();
                    problemsMessage.append(MessageFormat.format("<html>{0} problem(s) occurred:<br/>", problems.size()));
                    for (IOException problem : problems) {
                        LOG.log(Level.SEVERE, problem.getMessage(), problem);
                        problemsMessage.append(MessageFormat.format("<b>  {0}</b>: {1}<br/>", problem.getClass().getSimpleName(), problem.getMessage()));
                    }
                    SnapApp.getInstance().showErrorDialog(Bundle.CTL_OpenProductActionName(), problemsMessage.toString());
                }
            }
        };

        swingWorker.execute();
    }
}
