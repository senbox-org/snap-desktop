package org.esa.snap.rcp.actions.file;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.dimap.DimapProductWriter;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeList;
import org.esa.snap.core.util.Debug;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.util.ProgressHandleMonitor;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle;

import java.io.File;

import static org.esa.snap.rcp.preferences.general.WriteOptionsController.DEFAULT_VALUE_SAVE_INCREMENTAL;
import static org.esa.snap.rcp.preferences.general.WriteOptionsController.DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS;
import static org.esa.snap.rcp.preferences.general.WriteOptionsController.DEFAULT_VALUE_SAVE_PRODUCT_HEADERS;
import static org.esa.snap.rcp.preferences.general.WriteOptionsController.DEFAULT_VALUE_SAVE_PRODUCT_HISTORY;
import static org.esa.snap.rcp.preferences.general.WriteOptionsController.PREFERENCE_KEY_SAVE_INCREMENTAL;
import static org.esa.snap.rcp.preferences.general.WriteOptionsController.PREFERENCE_KEY_SAVE_PRODUCT_ANNOTATIONS;
import static org.esa.snap.rcp.preferences.general.WriteOptionsController.PREFERENCE_KEY_SAVE_PRODUCT_HEADERS;
import static org.esa.snap.rcp.preferences.general.WriteOptionsController.PREFERENCE_KEY_SAVE_PRODUCT_HISTORY;

/**
 * @author Norman Fomferra
 * @author Marco Peters
 */
@NbBundle.Messages({"CTL_WriteProductOperationName=Write Product"})
public class WriteProductOperation implements Runnable, Cancellable {

    private final Product product;
    private final Boolean incremental;
    private final ProgressHandleMonitor pm;
    private final File fileLocation;
    private final String formatName;
    private Boolean status;

    public WriteProductOperation(Product product, Boolean incremental) {
        this(product, product.getFileLocation(), DimapProductConstants.DIMAP_FORMAT_NAME, incremental);
    }

    public WriteProductOperation(Product product, File fileLocation, String formatName, Boolean incremental) {
        Assert.notNull(product, "product");
        Assert.notNull(fileLocation, "fileLocation");
        Assert.notNull(formatName, "formatName");
        this.product = product;
        this.fileLocation = fileLocation;
        this.formatName = formatName;
        if (incremental != null) {
            this.incremental = incremental;
        } else {
            this.incremental = SnapApp.getDefault().getPreferences().getBoolean(PREFERENCE_KEY_SAVE_INCREMENTAL,
                                                                                DEFAULT_VALUE_SAVE_INCREMENTAL);
        }
        this.pm = ProgressHandleMonitor.create(Bundle.CTL_WriteProductOperationName(), this);
    }

    public Boolean getStatus() {
        return status;
    }

    public ProgressHandle getProgressHandle() {
        return pm.getProgressHandle();
    }

    @Override
    public boolean cancel() {
        Dialogs.Answer answer = Dialogs.requestDecision(Bundle.CTL_WriteProductOperationName(),
                                                                "Cancellation of writing may lead to an unreadable product.\n\n"
                                                                        + "Do you really want to cancel the write process?",
                                                        false, null);
        boolean cancel = answer == Dialogs.Answer.YES;
        if (cancel) {
            pm.setCanceled(cancel);
            ProductWriter productWriter = product.getProductWriter();
            if (productWriter instanceof DimapProductWriter dimapProductWriter) {
                dimapProductWriter.setCanceled(cancel);
            }
        }
        return cancel;
    }

    @Override
    public void run() {

        boolean saveProductHeaders = SnapApp.getDefault().getPreferences().getBoolean(PREFERENCE_KEY_SAVE_PRODUCT_HEADERS,
                                                                                      DEFAULT_VALUE_SAVE_PRODUCT_HEADERS);
        boolean saveProductHistory = SnapApp.getDefault().getPreferences().getBoolean(PREFERENCE_KEY_SAVE_PRODUCT_HISTORY,
                                                                                      DEFAULT_VALUE_SAVE_PRODUCT_HISTORY);
        boolean saveADS = SnapApp.getDefault().getPreferences().getBoolean(PREFERENCE_KEY_SAVE_PRODUCT_ANNOTATIONS,
                                                                           DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS);
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

        Boolean saveOk = writeProduct(product, fileLocation, formatName, incremental, pm);

        if (saveOk != null && saveOk) {
            product.setModified(false);
            OpenProductAction.getRecentProductPaths().add(fileLocation.getPath());
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

    private static Boolean writeProduct(Product product,
                                        File file,
                                        String formatName,
                                        boolean incremental,
                                        ProgressMonitor pm) {
        Debug.assertNotNull(product);
        try {
            ProductIO.writeProduct(product, file, formatName, incremental, pm);
            return !pm.isCanceled() ? true : null;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            javax.swing.SwingUtilities.invokeLater(() -> Dialogs.showError("Writing failed", e.getMessage()));
            return false;
        }
    }


}
