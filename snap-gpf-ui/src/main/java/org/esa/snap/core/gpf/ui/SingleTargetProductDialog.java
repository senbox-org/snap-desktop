/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.gpf.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorCancelException;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.internal.OperatorExecutor;
import org.esa.snap.core.gpf.internal.OperatorProductReader;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;

import javax.swing.AbstractButton;
import javax.swing.JOptionPane;
import java.awt.Toolkit;
import java.io.File;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 */
public abstract class SingleTargetProductDialog extends ModelessDialog {

    protected TargetProductSelector targetProductSelector;
    protected AppContext appContext;
    private long createTargetProductTime;

    protected SingleTargetProductDialog(AppContext appContext, String title, String helpID) {
        this(appContext, title, ID_APPLY_CLOSE_HELP, helpID);
    }

    protected SingleTargetProductDialog(AppContext appContext, String title, int buttonMask, String helpID) {
        this(appContext, title, buttonMask, helpID, new TargetProductSelectorModel());
    }

    protected SingleTargetProductDialog(AppContext appContext, String title, int buttonMask, String helpID, TargetProductSelectorModel model) {
        this(appContext, title, buttonMask, helpID, model, false);
    }

    protected SingleTargetProductDialog(AppContext appContext, String title, int buttonMask, String helpID, TargetProductSelectorModel model, boolean alwaysWriteOutput) {
        super(appContext.getApplicationWindow(), title, buttonMask, helpID);
        this.appContext = appContext;
        targetProductSelector = new TargetProductSelector(model, alwaysWriteOutput);
        String homeDirPath = SystemUtils.getUserHomeDir().getPath();
        String saveDir = appContext.getPreferences().getPropertyString(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, homeDirPath);
        targetProductSelector.getModel().setProductDir(new File(saveDir));
        if (!alwaysWriteOutput) {
            targetProductSelector.getOpenInAppCheckBox().setText("Open in " + appContext.getApplicationName());
        }
        targetProductSelector.getModel().getValueContainer().addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals("saveToFileSelected") ||
                    evt.getPropertyName().equals("openInAppSelected")) {
                updateRunButton();
            }
        });
        AbstractButton button = getButton(ID_APPLY);
        button.setText("Run");
        button.setMnemonic('R');
        updateRunButton();
    }

    private void updateRunButton() {
        AbstractButton button = getButton(ID_APPLY);
        boolean save = targetProductSelector.getModel().isSaveToFileSelected();
        boolean open = targetProductSelector.getModel().isOpenInAppSelected();
        String toolTipText = "";
        boolean enabled = true;
        if (save && open) {
            toolTipText = "Save target product and open it in " + getAppContext().getApplicationName();
        } else if (save) {
            toolTipText = "Save target product";
        } else if (open) {
            toolTipText = "Open target product in " + getAppContext().getApplicationName();
        } else {
            enabled = false;
        }
        button.setToolTipText(toolTipText);
        button.setEnabled(enabled);
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public TargetProductSelector getTargetProductSelector() {
        return targetProductSelector;
    }

    @Override
    protected void onApply() {
        if (!canApply()) {
            return;
        }

        String productDir = targetProductSelector.getModel().getProductDir().getAbsolutePath();
        appContext.getPreferences().setPropertyString(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, productDir);

        Product targetProduct = null;
        try {
            long t0 = System.currentTimeMillis();
            targetProduct = createTargetProduct();
            createTargetProductTime = System.currentTimeMillis() - t0;
            if (targetProduct == null) {
                throw new NullPointerException("Target product is null.");
            }

        } catch (Throwable t) {
            handleInitialisationError(t);
        }
        if (targetProduct == null) {
            return;
        }

        targetProduct.setName(targetProductSelector.getModel().getProductName());
        if (targetProductSelector.getModel().isSaveToFileSelected()) {
            targetProduct.setFileLocation(targetProductSelector.getModel().getProductFile());
            final ProgressMonitorSwingWorker worker = new ProductWriterSwingWorker(targetProduct);
            worker.executeWithBlocking();
        } else if (targetProductSelector.getModel().isOpenInAppSelected()) {
            appContext.getProductManager().addProduct(targetProduct);
            showOpenInAppInfo();
        }
    }

    protected void handleInitialisationError(Throwable t) {
        String msg;
        if (t instanceof OperatorCancelException) {
            msg = MessageFormat.format("An internal error occurred during the target product initialisation.\n{0}",
                                       formatThrowable(t));
            showErrorDialog(msg);

            return;
        }

        if (isInternalException(t)) {
            msg = MessageFormat.format("An internal error occurred during the target product initialisation.\n{0}",
                                       formatThrowable(t));
        } else {
            msg = MessageFormat.format("A problem occurred during the target product initialisation.\n{0}",
                                       formatThrowable(t));
        }
        appContext.handleError(msg, t);
    }

    protected void handleProcessingError(Throwable t) {
        String msg;

        if (t instanceof OperatorCancelException) {
            return;
        }

        if (isInternalException(t)) {
            msg = MessageFormat.format("An internal error occurred during the target product processing.\n{0}",
                                       formatThrowable(t));
        } else {
            msg = MessageFormat.format("A problem occurred during the target product processing.\n{0}",
                                       formatThrowable(t));
        }
        appContext.handleError(msg, t);
    }

    private boolean isInternalException(Throwable t) {
        return (t instanceof RuntimeException && !(t instanceof OperatorException)) || t instanceof Error;
    }

    private String formatThrowable(Throwable t) {
        return MessageFormat.format("Type: {0}\nMessage: {1}\n",
                                    t.getClass().getSimpleName(),
                                    t.getMessage());
    }

    protected boolean canApply() {
        final String productName = targetProductSelector.getModel().getProductName();
        if (productName == null || productName.isEmpty()) {
            showErrorDialog("Please specify a target product name.");
            targetProductSelector.getProductNameTextField().requestFocus();
            return false;
        }

        if (targetProductSelector.getModel().isOpenInAppSelected()) {
            final Product existingProduct = appContext.getProductManager().getProduct(productName);
            if (existingProduct != null) {
                String message = MessageFormat.format(
                        "<html>A product with the name ''{0}'' is already opened in {1}.<br><br>" +
                                "Do you want to continue?",
                        productName, appContext.getApplicationName()
                );
                final int answer = JOptionPane.showConfirmDialog(getJDialog(), message,
                                                                 getTitle(), JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        }
        if (targetProductSelector.getModel().isSaveToFileSelected()) {
            File productFile = targetProductSelector.getModel().getProductFile();
            if (productFile.exists()) {
                String message = MessageFormat.format(
                        "<html>The specified output file<br>\"{0}\"<br> already exists.<br><br>" +
                                "Do you want to overwrite the existing file?",
                        productFile.getPath()
                );
                final int answer = JOptionPane.showConfirmDialog(getJDialog(), message,
                                                                 getTitle(), JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        }
        return true;
    }

    private void showSaveInfo(long saveTime) {
        File productFile = getTargetProductSelector().getModel().getProductFile();
        final String message = MessageFormat.format(
                "<html>The target product has been successfully written to<br>{0}<br>" +
                        "Total time spend for processing: {1}",
                formatFile(productFile),
                formatDuration(saveTime)
        );
        showSuppressibleInformationDialog(message, "saveInfo");
    }

    protected void showOpenInAppInfo() {
        final String message = MessageFormat.format(
                "<html>The target product has successfully been created and opened in {0}.<br><br>" +
                        "Actual processing of source to target data will be performed only on demand,<br>" +
                        "for example, if the target product is saved or an image view is opened.",
                appContext.getApplicationName()
        );
        showSuppressibleInformationDialog(message, "openInAppInfo");
    }

    private void showSaveAndOpenInAppInfo(long saveTime) {
        File productFile = getTargetProductSelector().getModel().getProductFile();
        final String message = MessageFormat.format(
                "<html>The target product has been successfully written to<br>" +
                        "<p>{0}</p><br>" +
                        "and has been opened in {1}.<br><br>" +
                        "Total time spend for processing: {2}<br>",
                formatFile(productFile),
                appContext.getApplicationName(),
                formatDuration(saveTime)
        );
        showSuppressibleInformationDialog(message, "saveAndOpenInAppInfo");
    }

    String formatFile(File file) {
        return FileUtils.getDisplayText(file, 54);
    }

    String formatDuration(long millis) {
        long seconds = millis / 1000;
        millis -= seconds * 1000;
        long minutes = seconds / 60;
        seconds -= minutes * 60;
        long hours = minutes / 60;
        minutes -= hours * 60;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    /**
     * Shows an information dialog on top of this dialog.
     * The shown dialog will contain a user option allowing to not show the message anymore.
     *
     * @param infoMessage  The message.
     * @param propertyName The (simple) property name used to store the user option in the application's preferences.
     */
    public void showSuppressibleInformationDialog(String infoMessage, String propertyName) {
        Dialogs.showInformation(getTitle(), infoMessage, getQualifiedPropertyName(propertyName));
    }

    private class ProductWriterSwingWorker extends ProgressMonitorSwingWorker<Product, Object> {

        private final Product targetProduct;
        private long saveTime;

        private ProductWriterSwingWorker(Product targetProduct) {
            super(getJDialog(), "Writing Target Product");
            this.targetProduct = targetProduct;
        }

        @Override
        protected Product doInBackground(ProgressMonitor pm) throws Exception {
            final TargetProductSelectorModel model = getTargetProductSelector().getModel();
            pm.beginTask("Writing...", model.isOpenInAppSelected() ? 100 : 95);
            saveTime = 0L;
            Product product = null;
            try {
                long t0 = System.currentTimeMillis();
                Operator execOp = null;
                if (targetProduct.getProductReader() instanceof OperatorProductReader) {
                    final OperatorProductReader opReader = (OperatorProductReader) targetProduct.getProductReader();
                    Operator operator = opReader.getOperatorContext().getOperator();
                    if (operator.getSpi().getOperatorDescriptor().isAutoWriteDisabled()) {
                        execOp = operator;
                    }
                }
                if (execOp == null) {
                    WriteOp writeOp = new WriteOp(targetProduct, model.getProductFile(), model.getFormatName());
                    writeOp.setDeleteOutputOnFailure(true);
                    writeOp.setWriteEntireTileRows(true);
                    writeOp.setClearCacheAfterRowWrite(false);
                    execOp = writeOp;
                }

                final OperatorExecutor executor = OperatorExecutor.create(execOp);
                executor.execute(SubProgressMonitor.create(pm, 95));

                saveTime = System.currentTimeMillis() - t0;
                if (model.isOpenInAppSelected()) {
                    File targetFile = model.getProductFile();
                    if (!targetFile.exists())
                        targetFile = targetProduct.getFileLocation();
                    if (targetFile.exists()) {
                        product = ProductIO.readProduct(targetFile);
                        if (product == null) {
                            product = targetProduct; // todo - check - this cannot be ok!!! (nf)
                        }
                    }
                    pm.worked(5);
                }
            } finally {
                pm.done();
                if (product != targetProduct) {
                    targetProduct.dispose();
                }
                Preferences preferences = SnapApp.getDefault().getPreferences();
                if (preferences.getBoolean(GPF.BEEP_AFTER_PROCESSING_PROPERTY, false)) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
            return product;
        }

        @Override
        protected void done() {
            final TargetProductSelectorModel model = getTargetProductSelector().getModel();
            long totalSaveTime = saveTime + createTargetProductTime;
            try {
                final Product targetProduct = get();
                if (model.isOpenInAppSelected()) {
                    appContext.getProductManager().addProduct(targetProduct);
                    showSaveAndOpenInAppInfo(totalSaveTime);
                } else {
                    showSaveInfo(totalSaveTime);
                }
            } catch (InterruptedException e) {
                // ignore
            } catch (ExecutionException e) {
                handleProcessingError(e.getCause());
            } catch (Throwable t) {
                handleProcessingError(t);
            }
        }
    }

    /**
     * Creates the desired target product.
     * Usually, this method will be implemented by invoking one of the multiple {@link GPF GPF}
     * {@code createProduct} methods.
     * <p>
     * The method should throw a {@link OperatorException} in order to signal "nominal" processing errors,
     * other exeption types are treated as internal errors.
     *
     * @return The target product.
     * @throws Exception if an error occurs, an {@link OperatorException} is signaling "nominal" processing errors.
     */
    protected abstract Product createTargetProduct() throws Exception;
}
