/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.file;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.netbeans.docwin.DocumentWindow;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action which closes a selected product.
 *
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.actions.file.CloseProductAction"
)
@ActionRegistration(
        displayName = "#CTL_CloseProductActionName"
)
@ActionReference(path = "Menu/File", position = 30)
@NbBundle.Messages({
        "CTL_CloseProductActionName=Close Product"
})
public final class CloseProductAction extends AbstractAction {

    private static final Logger LOG = Logger.getLogger(CloseProductAction.class.getName());

    private final Product product;

    public CloseProductAction(Product product) {
        this.product = product;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        closeProduct(product);
    }

    static void closeProduct(Product product) {
        closeProducts(product);
    }

    static void closeProducts(Product... products) {

        List<Product> closeList = new ArrayList<>(Arrays.asList(products));
        List<Product> saveList = new ArrayList<>();

        // todo - check for product dependencies, e.g. graphs, virtual bands, etc

        for (Product product : products) {
            if (product.isModified()) {
                int i = SnapApp.getInstance().showQuestionDialog(
                        Bundle.CTL_OpenProductActionName(),
                        MessageFormat.format("Product '{0}' has been modified.\nDo you want to save it?", product.getName()), null);
                if (NotifyDescriptor.YES_OPTION.equals(i)) {
                    saveList.add(product);
                } else if (!NotifyDescriptor.NO_OPTION.equals(i)) {
                    // cancel!
                    return;
                }
            }
        }

        SwingWorker<List<IOException>, Object> swingWorker = new SwingWorker<List<IOException>, Object>() {
            @Override
            protected List<IOException> doInBackground() {
                List<IOException> problems = new ArrayList<>();
                for (Product product : saveList) {
                    // todo - save product
                    LOG.info("Saving to " + product.getFileLocation());
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

                for (Product product : closeList) {
                    WindowUtilities.getOpened(DocumentWindow.class)
                            .filter(dw -> (dw.getDocument() instanceof ProductNode)
                                    && ((ProductNode) dw.getDocument()).getProduct() == product)
                            .forEach(DocumentWindow::documentClosing);
                    SnapApp.getInstance().getProductManager().removeProduct(product);
                }

                closeList.forEach(Product::dispose);
            }
        };

        swingWorker.execute();
    }
}
