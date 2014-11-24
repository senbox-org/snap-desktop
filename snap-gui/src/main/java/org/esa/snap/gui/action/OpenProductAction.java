/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.action;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.gui.node.ProductChildFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.prefs.Preferences;

/**
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.action.OpenProductAction"
)
@ActionRegistration(
        displayName = "#CTL_ShowImageViewActionName",
        menuText = "#CTL_ShowImageViewActionMenuText"
)
@ActionReference(
        path = "Menu/File",
        position = 0
)
@NbBundle.Messages({
                           "CTL_ShowImageViewActionName=Open Product",
                           "CTL_ShowImageViewActionMenuText=Open Product..."
                   })
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

        ServiceLoader<ProductReaderPlugIn> serviceLoader = ServiceLoader.load(ProductReaderPlugIn.class);
        List<FileFilter> filters = new ArrayList<>();
        for (ProductReaderPlugIn plugIn : serviceLoader) {
            filters.add(new FileNameExtFilter(plugIn));
        }
        if (filters.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No reader found!");
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

        ProductReaderPlugIn serviceProvider = ((FileNameExtFilter) fc.getFileFilter()).getServiceProvider();
        List<IOException> problems = new ArrayList<>();
        for (File file : files) {
            ProductReader pr = serviceProvider.createReaderInstance();
            try {
                Product product = pr.readProductNodes(file, null);
                ProductChildFactory.getInstance().addProduct(product);
            } catch (IOException problem) {
                problems.add(problem);
            }
        }

        if (!problems.isEmpty()) {
            StringBuilder problemsMessage = new StringBuilder();
            problemsMessage.append("<html>").append(problems.size()).append(" problem(s) occurred:<br/>");
            for (IOException problem : problems) {
                problemsMessage.append(String.format("<b>  %s</b>%s<br/>", problem.getClass().getSimpleName(), problem.getMessage()));
            }
            JOptionPane.showMessageDialog(null, problemsMessage.toString());
        }
    }

    private static class FileNameExtFilter extends FileFilter {

        private final ProductReaderPlugIn serviceProvider;

        public FileNameExtFilter(ProductReaderPlugIn serviceProvider) {
            this.serviceProvider = serviceProvider;
        }

        public ProductReaderPlugIn getServiceProvider() {
            return serviceProvider;
        }

        @Override
        public boolean accept(File f) {
            if (f.isFile()) {
                String[] extensions = serviceProvider.getDefaultFileExtensions();
                for (String ext : extensions) {
                    if (f.getName().toLowerCase().endsWith(ext.toLowerCase())) {
                        return true;
                    }
                }
            }
            return f.isDirectory();
        }

        @Override
        public String getDescription() {
            return serviceProvider.getDescription(Locale.ENGLISH);
        }
    }
}
