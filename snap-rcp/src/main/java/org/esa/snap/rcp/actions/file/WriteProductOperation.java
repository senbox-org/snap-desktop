package org.esa.snap.rcp.actions.file;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.util.Debug;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.util.ProgressHandleMonitor;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Cancellable;

import java.io.File;

/**
 * @author Norman
 */
class WriteProductOperation implements Runnable, Cancellable {

    private final Product product;
    private final Boolean incremental;
    private final ProgressHandleMonitor pm;
    private Boolean status;

    public WriteProductOperation(Product product, Boolean incremental) {
        Assert.notNull(product, "product");
        this.product = product;
        if (incremental != null) {
            this.incremental = incremental;
        } else {
            this.incremental = SnapApp.getDefault().getPreferences().getBoolean(SaveProductAction.PROPERTY_KEY_SAVE_INCREMENTAL, SaveProductAction.DEFAULT_VALUE_SAVE_INCREMENTAL);
        }
        this.pm = ProgressHandleMonitor.create(Bundle.CTL_SaveProductActionName(), this);
    }

    public Boolean getStatus() {
        return status;
    }

    public ProgressHandle getProgressHandle() {
        return pm.getProgressHandle();
    }

    @Override
    public boolean cancel() {
        SnapDialogs.Answer answer = SnapDialogs.requestDecision(Bundle.CTL_SaveProductActionName(),
                                                                "Cancellation of saving may lead to an unreadable product.\n\n"
                                                                        + "Do you really want to cancel the save process?",
                                                                false, null);
        return answer == SnapDialogs.Answer.YES;
    }

    @Override
    public void run() {

        boolean saveProductHeaders = SnapApp.getDefault().getPreferences().getBoolean(SaveProductAction.PROPERTY_KEY_SAVE_PRODUCT_HEADERS,
                                                                                      SaveProductAction.DEFAULT_VALUE_SAVE_PRODUCT_HEADERS);
        boolean saveProductHistory = SnapApp.getDefault().getPreferences().getBoolean(SaveProductAction.PROPERTY_KEY_SAVE_PRODUCT_HISTORY,
                                                                                      SaveProductAction.DEFAULT_VALUE_SAVE_PRODUCT_HISTORY);
        boolean saveADS = SnapApp.getDefault().getPreferences().getBoolean(SaveProductAction.PROPERTY_KEY_SAVE_PRODUCT_ANNOTATIONS,
                                                                           SaveProductAction.DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS);
        MetadataElement metadataRoot = product.getMetadataRoot();
        ProductNodeList<MetadataElement> metadataElementBackup = new ProductNodeList<>();

        if (!saveProductHeaders) {
            String[] headerNames = new String[]{
                    "MPH", "SPH",
                    "Earth_Explorer_Header", "Fixed_Header", "Variable_Header", "Specific_Product_Header",
                    "Global_Attributes", "GlobalAttributes", "Variable_Attributes"
            };
            for (String headerName : headerNames) {
                MetadataElement element = metadataRoot.getElement(headerName);
                metadataElementBackup.add(element);
                metadataRoot.removeElement(element);
            }
        }

        if (!saveProductHistory) {
            final MetadataElement element = metadataRoot.getElement("History");
            metadataElementBackup.add(element);
            metadataRoot.removeElement(element);
        }

        if (!saveADS) {
            final String[] names = metadataRoot.getElementNames();
            for (final String name : names) {
                if (name.endsWith("ADS") || name.endsWith("Ads") || name.endsWith("ads")) {
                    final MetadataElement element = metadataRoot.getElement(name);
                    metadataElementBackup.add(element);
                    metadataRoot.removeElement(element);
                }
            }
        }

        File file = product.getFileLocation();
        boolean saveOk = saveProduct(product, file,
                                     DimapProductConstants.DIMAP_FORMAT_NAME,
                                     incremental,
                                     pm);

        if (saveOk) {
            product.setModified(false);
            OpenProductAction.getRecentProductPaths().add(file.getPath());
        } else {
            if (metadataRoot != null) {
                final MetadataElement[] elementsArray = new MetadataElement[metadataElementBackup.size()];
                metadataElementBackup.toArray(elementsArray);
                for (final MetadataElement metadataElement : elementsArray) {
                    metadataRoot.addElement(metadataElement);
                }
            }
        }

        status = saveOk;
    }

    private static Boolean saveProduct(Product product,
                                       File file,
                                       String formatName,
                                       boolean incremental,
                                       ProgressMonitor pm) {
        Debug.assertNotNull(product);
        try {
            // todo - really add GPF dependency?!?
            /*
            if (product.getProductReader() instanceof OperatorProductReader) {
                GPF.writeProduct(product, file, formatName, incremental, pm);
            } else {
                ProductIO.writeProduct(product, file, formatName, incremental, pm);
            }
            */
            ProductIO.writeProduct(product, file, formatName, incremental, pm);
            return !pm.isCanceled() ? true : null;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Save failed", e);
            return false;
        }
    }


}
