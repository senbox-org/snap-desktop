/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.action;

import org.esa.snap.core.Product;
import org.esa.snap.core.io.ProductReader;
import org.esa.snap.core.io.ProductReaderSpi;
import org.esa.snap.gui.node.ProductChildFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.prefs.Preferences;

/**
 *
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "org.snap.gui.OpenProductAction"
)
@ActionRegistration(
        displayName = "Open Product",
        lazy = true,
        menuText = "Open Product..."
)
@ActionReference(
        path = "Menu/File",
        position = 0
)
public final class OpenProductAction extends AbstractAction /*implements Presenter.Toolbar*/ {
    /*
     @Override
     public Component getToolbarPresenter() {
     JPanel panel = new JPanel();
     // define a panel here, which will be added to the toolbar
     return panel;
     }
     */

    @Override
    public void actionPerformed(ActionEvent e) {

        Iterator<ProductReaderSpi> iterator = ServiceLoader.load(ProductReaderSpi.class).iterator();
        List<FileFilter> filters = new ArrayList<>();
        while ( iterator.hasNext()) {
            ProductReaderSpi readerSpi = iterator.next();
            filters.add(new FileNameExtFilter(readerSpi));
        }
        if (filters.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No importers found!");
            return;
        }

/*
        Lookup lookup = Lookups.forPath("Snap/ProductReaders");
        Collection<? extends ProductReaderSpi> serviceProviders = lookup.lookupAll(ProductReaderSpi.class);

        if (serviceProviders.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No importers found!");
            return;
        }

        List<FileFilter> filters = new ArrayList<>(serviceProviders.size());
        for (ProductReaderSpi serviceProvider : serviceProviders) {
            filters.add(new FileNameExtFilter(serviceProvider));
        }
*/
        Preferences preferences = Preferences.userNodeForPackage(getClass());

        JFileChooser fc = new JFileChooser(new File(preferences.get("lastDir", ".")));

        fc.setDialogTitle("Select Product File");
        fc.setAcceptAllFileFilterUsed(true);
        for (FileFilter filter : filters) {
            fc.addChoosableFileFilter(filter);
        }
        fc.setFileFilter(filters.get(0));
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
            preferences.put("lastDir", currentDirectory.toString());
        }
        
        ProductReaderSpi serviceProvider = ((FileNameExtFilter) fc.getFileFilter()).getServiceProvider();
        List<IOException> problems = new ArrayList<>();
        for (File file : files) {
            ProductReader pr = serviceProvider.createProductReader(file, null);
            try {
                Product product = pr.readProduct();
                ProductChildFactory.getInstance().addProduct(product);
            } catch (IOException problem) {
                problems.add(problem);
            }
        }

        if (!problems.isEmpty()) {
            StringBuilder problemsMessage = new StringBuilder();
            problemsMessage.append("<html>").append(problems.size()).append(" problem(s) occurred:<br/>").toString();
            for (IOException problem : problems) {
                problemsMessage.append(String.format("<b>  %s</b>%s<br/>", problem.getClass().getSimpleName(), problem.getMessage()));
            }
            JOptionPane.showMessageDialog(null, problemsMessage.toString());
        }
    }

    private static class FileNameExtFilter extends FileFilter {

        private final ProductReaderSpi serviceProvider;

        public FileNameExtFilter(ProductReaderSpi serviceProvider) {
            this.serviceProvider = serviceProvider;
        }

        public ProductReaderSpi getServiceProvider() {
            return serviceProvider;
        }                

        @Override
        public boolean accept(File f) {
            if (f.isFile()) {
                List<String> extensions = serviceProvider.getFileExtensions();
                for (String ext : extensions) {
                    if (f.getName().toLowerCase().endsWith("." + ext.toLowerCase())) {
                        return true;
                    }
                }
            }
            return f.isDirectory();
        }

        @Override
        public String getDescription() {
            return serviceProvider.getDescription();
        }
    }
}
