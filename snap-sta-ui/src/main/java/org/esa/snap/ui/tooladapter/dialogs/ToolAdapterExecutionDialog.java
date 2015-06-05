/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.ui.tooladapter.dialogs;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.descriptor.ParameterDescriptor;
import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterOp;
import org.esa.snap.framework.gpf.ui.OperatorMenu;
import org.esa.snap.framework.gpf.ui.OperatorParameterSupport;
import org.esa.snap.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.util.Cancellable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Form dialog for running a tool adapter operator.
 *
 * @author Lucian Barbulescu.
 * @author Cosmin Cara
 */
public class ToolAdapterExecutionDialog extends SingleTargetProductDialog {

    public static final String SOURCE_PRODUCT_FIELD = "sourceProduct";
    /**
     * Operator identifier.
     */
    private ToolAdapterOperatorDescriptor operatorDescriptor;
    /**
     * Parameters related info.
     */
    private OperatorParameterSupport parameterSupport;
    /**
     * The form used to get the user's input
     */
    private ToolExecutionForm form;

    private Product result;

    private OperatorTask operatorTask;

    public static final String helpID = "sta_execution";

    /**
     * Constructor.
     *
     * @param operatorSpi
     * @param appContext
     * @param title
     */
    public ToolAdapterExecutionDialog(ToolAdapterOperatorDescriptor operatorSpi, AppContext appContext, String title) {
        super(appContext, title, helpID);
        this.operatorDescriptor = operatorSpi;

        this.parameterSupport = new OperatorParameterSupport(operatorSpi);

        form = new ToolExecutionForm(appContext, operatorSpi, parameterSupport.getPropertySet(),
                getTargetProductSelector());
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                operatorSpi,
                parameterSupport,
                appContext,
                helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    /* Begin @Override methods section */

    @Override
    protected void onApply() {
        final Product[] sourceProducts = form.getSourceProducts();
        if (Arrays.stream(sourceProducts).anyMatch(p -> p == null)) {
            SnapDialogs.showWarning("Please make sure you have selected the necessary input products");
        } else {
            if (!canApply()) {
                onClose();
                ToolAdapterEditorDialog dialog = new ToolAdapterEditorDialog(appContext, operatorDescriptor, false);
                dialog.show();
            } else {
                if (validateUserInput()) {
                    String productDir = targetProductSelector.getModel().getProductDir().getAbsolutePath();
                    appContext.getPreferences().setPropertyString(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, productDir);
                    Map<String, Product> sourceProductMap = new HashMap<>();
                    sourceProductMap.put(SOURCE_PRODUCT_FIELD, sourceProducts[0]);
                    Operator op = GPF.getDefaultInstance().createOperator(operatorDescriptor.getName(), parameterSupport.getParameterMap(), sourceProductMap, null);
                    op.setSourceProducts(sourceProducts);
                    operatorTask = new OperatorTask(op, ToolAdapterExecutionDialog.this::operatorCompleted);
                    ProgressHandle progressHandle = ProgressHandleFactory.createHandle(this.getTitle());
                    ((ToolAdapterOp) op).setProgressMonitor(progressHandle);
                    ProgressUtils.runOffEventThreadWithProgressDialog(operatorTask, this.getTitle(), progressHandle, true, 1, 1);
                }
            }
        }
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        return result;
    }

    @Override
    protected boolean canApply() {
        try {
            Path toolLocation = operatorDescriptor.getExpandedLocation(operatorDescriptor.getMainToolFileLocation()).toPath();
            if (!(Files.exists(toolLocation) && Files.isExecutable(toolLocation))) {
                SnapDialogs.showWarning(Bundle.MSG_Inexistent_Tool_Path_Text());
                return false;
            }
            File workingDir = operatorDescriptor.getExpandedLocation(operatorDescriptor.getWorkingDir());
            if (!(workingDir != null && workingDir.exists() && workingDir.isDirectory())) {
                SnapDialogs.showWarning(Bundle.MSG_Inexistent_WorkDir_Text());
                return false;
            }
            ParameterDescriptor[] parameterDescriptors = operatorDescriptor.getParameterDescriptors();
            if (parameterDescriptors != null && parameterDescriptors.length > 0) {
                for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
                    Class<?> dataType = parameterDescriptor.getDataType();
                    String defaultValue = parameterDescriptor.getDefaultValue();
                    if (File.class.isAssignableFrom(dataType) &&
                            (parameterDescriptor.isNotNull() || parameterDescriptor.isNotEmpty()) &&
                            (defaultValue == null || defaultValue.isEmpty() || !Files.exists(Paths.get(defaultValue)))) {
                        SnapDialogs.showWarning(String.format(Bundle.MSG_Inexistem_Parameter_Value_Text(),
                                parameterDescriptor.getName(), parameterDescriptor.isNotNull() ? "NotNull" : "NotEmpty"));
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void onCancel() {
        super.onCancel();
    }

    @Override
    protected void onClose() {
        super.onClose();
    }

    /* End @Override methods section */

    /**
     * Performs any validation on the user input.
     *
     * @return  <code>true</code> if the input is valid, <code>false</code> otherwise
     */
    private boolean validateUserInput() {
        boolean isValid = true;
        File productDir = targetProductSelector.getModel().getProductDir();
        isValid &= (productDir != null) && productDir.exists();
        Product[] sourceProducts = form.getSourceProducts();
        isValid &= (sourceProducts != null) && sourceProducts.length > 0;

        return isValid;
    }

    /**
     * This is actually the callback method to be passed to the runnable
     * wrapping the operator execution.
     *
     * @param result    The output product
     */
    private void operatorCompleted(Product result) {
        this.result = result;
        super.onApply();
    }

    private void tearDown(Throwable throwable) {
        boolean hasBeenCancelled = operatorTask != null && !operatorTask.hasCompleted;
        if (operatorTask != null) {
            operatorTask.cancel();
        }
        if (throwable != null && !hasBeenCancelled) {
            handleInitialisationError(throwable);
        }
    }

    /**
     * Runnable for executing the operator. It requires a callback
     * method that is to be called when the operator has finished its
     * execution.
     */
    public class OperatorTask implements Runnable, Cancellable {

        private Operator operator;
        private Consumer<Product> callbackMethod;
        private Product result;
        private boolean hasCompleted;

        /**
         * Constructs a runnable for the given operator that will
         * call back the given method when the execution has finished.
         *
         * @param op        The operator to be executed
         * @param callback  The callback method to be invoked at completion
         */
        public OperatorTask(Operator op, Consumer<Product> callback) {
            operator = op;
            callbackMethod = callback;
        }

        @Override
        public boolean cancel() {
            if (!hasCompleted) {
                if (operator instanceof ToolAdapterOp) {
                    ((ToolAdapterOp) operator).stop();
                    onCancel();
                }
                hasCompleted = true;
            }
            return true;
        }

        @Override
        public void run() {
            try {

                callbackMethod.accept(operator.getTargetProduct());
            } catch (Throwable t) {
                tearDown(t);
            } finally {
                hasCompleted = true;
            }
        }
    }

}
