/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.file;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.gui.SnapDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Action which saves a selected product using a new name.
 *
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.actions.file.SaveProductAsAction"
)
@ActionRegistration(
        displayName = "#CTL_SaveProductAsActionName"
)
@ActionReference(path = "Menu/File", position = 51, separatorAfter = 59)
@NbBundle.Messages({
        "CTL_SaveProductAsActionName=Save Product As..."
})
public final class SaveProductAsAction extends AbstractAction {

    public static final String PREFERENCES_KEY_PRODUCT_CONVERSION_REQUIRED = "product_conversion_required";
    private final WeakReference<Product> productRef;

    public SaveProductAsAction(Product products) {
        productRef = new WeakReference<>(products);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    public Boolean execute() {
        return saveProductAs(productRef.get());
    }

    Boolean saveProductAs(Product product) {
        final ProductReader reader = product.getProductReader();
        if (reader != null && !(reader instanceof DimapProductReader)) {
            final SnapDialogs.Answer answer = SnapDialogs.requestDecision("Save Product As",
                                                                          "In order to save the product\n" +
                                                                                  "   " + product.getDisplayName() + "\n" +
                                                                                  "it has to be converted to the BEAM-DIMAP format.\n" +
                                                                                  "Depending on the product size the conversion also may take a while.\n\n" +
                                                                                  "Do you really want to convert the product now?\n",
                                                                          false,
                                                                          PREFERENCES_KEY_PRODUCT_CONVERSION_REQUIRED);
            if (answer != SnapDialogs.Answer.YES) {
                return null;
            }
        }

        String fileName;
        if (product.getFileLocation() != null) {
            fileName = product.getFileLocation().getName();
        } else {
            fileName = product.getName();
        }
        final File newFile = SnapDialogs.requestFileForSave("Save Product As",
                                                            false,
                                                            DimapProductHelpers.createDimapFileFilter(),
                                                            DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION,
                                                            fileName,
                                                            OpenProductAction.PREFERENCES_KEY_LAST_PRODUCT_DIR);
        if (newFile == null) {
            return null;
        }

        final String oldProductName = product.getName();
        final File oldFile = product.getFileLocation();

        //  For DIMAP products, check if file path has really changed
        //  if not, just save product
        if (reader instanceof DimapProductReader && newFile.equals(oldFile)) {
            return Boolean.TRUE.equals(new SaveProductAction(product).execute());
        }

        product.setFileLocation(newFile);

        Boolean status = new SaveProductAction(product).execute();
        boolean successfullySaved = Boolean.TRUE.equals(status);
        // todo
        //final boolean incremental = false;
        //final boolean successfullySaved = Boolean.TRUE.equals(new SaveProductAction(product, incremental).execute());
        if (successfullySaved) {
/*
todo
                    if (!SnapApp.getDefault().isShuttingDown()) {
                        OpenProductAction.reopenProduct(product, newFile);
                    }
*/
            OpenProductAction.reopenProduct(product, newFile);
        } else {
            product.setFileLocation(oldFile);
            product.setName(oldProductName);
        }

        return successfullySaved;
    }
}
