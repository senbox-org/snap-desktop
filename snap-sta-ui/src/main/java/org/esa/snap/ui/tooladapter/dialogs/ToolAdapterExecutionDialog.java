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

import com.bc.ceres.binding.Property;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.descriptor.*;
import org.esa.snap.core.gpf.descriptor.template.TemplateFile;
import org.esa.snap.core.gpf.operators.tooladapter.DefaultOutputConsumer;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterOp;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.tooladapter.actions.EscapeAction;
import org.esa.snap.ui.tooladapter.model.OperationType;
import org.esa.snap.ui.tooladapter.preferences.ToolAdapterOptionsController;
import org.esa.snap.utils.PrivilegedAccessor;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Form dialog for running a tool adapter operator.
 *
 * @author Lucian Barbulescu.
 * @author Cosmin Cara
 */
@NbBundle.Messages({
        "NoSourceProductWarning_Text=No input product was selected.\nAre you sure you want to continue?",
        "RequiredTargetProductMissingWarning_Text=A target product is required in adapter's template, but none was provided",
        "NoOutput_Text=The operator did not produce any output",
        "BeginOfErrorMessages_Text=The operator completed with the following errors:\n",
        "OutputTitle_Text=Process output",
        "ExecutionFailed_Text=Execution Failed",
        "ExecutionFailed_Message=The execution completed with errors: \n%s\n\nDo you want to try to open the resulting product?"
})
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

    private Logger logger;

    private List<String> warnings;

    public static final String helpID = "sta_execution";

    private List<ToolParameterDescriptor> artificiallyAddedParams;

    /**
     * Constructor.
     *
     * @param descriptor    The operator descriptor
     * @param appContext    The application context
     * @param title         The dialog title
     */
    public ToolAdapterExecutionDialog(ToolAdapterOperatorDescriptor descriptor, AppContext appContext, String title) {
        super(appContext, title, helpID);
        logger = Logger.getLogger(ToolAdapterExecutionDialog.class.getName());
        initialize(descriptor);
        warnings = new ArrayList<>();
    }

    /* Add workaround for SNAP-402 (JIRA) issue */
    private void updatePrimitiveZeroValuesHashMap(){

        try {

            HashMap<Class<?>, Object> primitiveZeroValuesMap = (HashMap<Class<?>, Object>) PrivilegedAccessor.getStaticValue(Property.class, "PRIMITIVE_ZERO_VALUES");

            /* Update dictionary with primitive default values */
            primitiveZeroValuesMap.put(Boolean.class,false);
            primitiveZeroValuesMap.put(Character.class, (char) 0);
            primitiveZeroValuesMap.put(Byte.class, (byte) 0);
            primitiveZeroValuesMap.put(Short.class, (short) 0);
            primitiveZeroValuesMap.put(Integer.class, 0);
            primitiveZeroValuesMap.put(Long.class, (long) 0);
            primitiveZeroValuesMap.put(Float.class, (float) 0);
            primitiveZeroValuesMap.put(Double.class, (double) 0);

        }
        catch (IllegalAccessException | NoSuchFieldException iaExc){
            logger.severe(iaExc.getMessage());
        }

    }

    private void initialize(ToolAdapterOperatorDescriptor descriptor) {
        //this.operatorDescriptor = new ToolAdapterOperatorDescriptor(descriptor);
        this.operatorDescriptor = descriptor;
        //add paraeters of template parameters
        artificiallyAddedParams = new ArrayList<>();
        Arrays.stream(this.operatorDescriptor.getToolParameterDescriptors().toArray()).filter(p -> ((ToolParameterDescriptor)p).isTemplateParameter()).
                forEach(p -> artificiallyAddedParams.addAll(((TemplateParameterDescriptor)p).getParameterDescriptors()));
        this.operatorDescriptor.getToolParameterDescriptors().addAll(artificiallyAddedParams);
        this.parameterSupport = new OperatorParameterSupport(this.operatorDescriptor);
        Arrays.stream(this.operatorDescriptor.getToolParameterDescriptors().toArray()).
                filter(p -> ToolAdapterConstants.FOLDER_PARAM_MASK.equals(((ToolParameterDescriptor)p).getParameterType())).
                forEach(p -> parameterSupport.getPropertySet().getProperty(((ToolParameterDescriptor)p).getName()).getDescriptor().setAttribute("directory", true));
        form = new ToolExecutionForm(appContext, this.operatorDescriptor, parameterSupport.getPropertySet(),
                getTargetProductSelector());
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                this.operatorDescriptor,
                parameterSupport,
                appContext,
                helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
        EscapeAction.register(getJDialog());

        /* Add workaround for SNAP-402 (JIRA) issue */
        updatePrimitiveZeroValuesHashMap();

        this.getJDialog().addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) { form.refreshDimension(); }
        });
        this.getJDialog().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { form.refreshDimension(); }
        });
        this.getJDialog().setMinimumSize(new Dimension(250, 250));
    }

    /* Begin @Override methods section */

    @Override
    protected void onApply() {
        final Product[] sourceProducts = form.getSourceProducts();
        List<ParameterDescriptor> descriptors = Arrays.stream(operatorDescriptor.getParameterDescriptors())
                .filter(p -> ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE.equals(p.getName()))
                .collect(Collectors.toList());
        String templateContents;
        try {
            //templateContents = ToolAdapterIO.readOperatorTemplate(operatorDescriptor.getName());
            TemplateFile template = operatorDescriptor.getTemplate();
            templateContents = template.getContents();
        } catch (Exception ex) {
            showErrorDialog(String.format("Cannot read operator template [%s]", ex.getMessage()));
            return;
        }
        if (Arrays.stream(sourceProducts).anyMatch(p -> p == null)) {
            Dialogs.Answer decision = Dialogs.requestDecision("No Product Selected", Bundle.NoSourceProductWarning_Text(), false,
                                                              ToolAdapterOptionsController.PREFERENCE_KEY_SHOW_EMPTY_PRODUCT_WARNING);
            if (decision.equals(Dialogs.Answer.NO)) {
                return;
            }
        }
        if (descriptors.size() == 1 && form.getPropertyValue(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE) == null &&
                templateContents.contains("$" + ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE)) {
                Dialogs.showWarning(Bundle.RequiredTargetProductMissingWarning_Text());
        } else {
            if (!canApply()) {
                displayWarnings();
                AbstractAdapterEditor dialog = AbstractAdapterEditor.createEditorDialog(appContext, getJDialog(), operatorDescriptor, OperationType.EDIT);
                final int code = dialog.show();
                if (code == AbstractDialog.ID_OK) {
                    onOperatorDescriptorChanged(dialog.getUpdatedOperatorDescriptor());
                }
                dialog.close();
            } else {
                if (validateUserInput()) {
                    Map<String, Product> sourceProductMap = new HashMap<>();
                    if (sourceProducts.length > 0) {
                        sourceProductMap.put(SOURCE_PRODUCT_FIELD, sourceProducts[0]);
                    }
                    Operator op = GPF.getDefaultInstance().createOperator(operatorDescriptor.getName(), parameterSupport.getParameterMap(), sourceProductMap, null);
                    for (Property property : parameterSupport.getPropertySet().getProperties()) {
                        op.setParameter(property.getName(), property.getValue());
                    }
                    op.setSourceProducts(sourceProducts);
                    operatorTask = new OperatorTask(op, ToolAdapterExecutionDialog.this::operatorCompleted);
                    ProgressHandle progressHandle = ProgressHandleFactory.createHandle(this.getTitle());
                    String progressPattern = operatorDescriptor.getProgressPattern();
                    ConsoleConsumer consumer = null;
                    ProgressWrapper progressWrapper = new ProgressWrapper(progressHandle, progressPattern == null || progressPattern.isEmpty());
                    consumer = new ConsoleConsumer(operatorDescriptor.getProgressPattern(),
                                                operatorDescriptor.getErrorPattern(),
                                                operatorDescriptor.getStepPattern(),
                                                progressWrapper,
                                                form.console);
                    form.console.clear();
                    progressWrapper.setConsumer(consumer);
                    ((ToolAdapterOp) op).setProgressMonitor(progressWrapper);
                    ((ToolAdapterOp) op).setConsumer(consumer);
                    ProgressUtils.runOffEventThreadWithProgressDialog(operatorTask, this.getTitle(), progressHandle, true, 1, 1);
                } else {
                    if (warnings.size() > 0) {
                        displayWarnings();
                    }
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
        this.operatorDescriptor.getToolParameterDescriptors().removeAll(artificiallyAddedParams);
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        return result;
    }

    @Override
    protected boolean canApply() {
        warnings.clear();
        try {
            String message;
            Path toolLocation = operatorDescriptor.resolveVariables(operatorDescriptor.getMainToolFileLocation()).toPath();
            if (!(Files.exists(toolLocation) && Files.isExecutable(toolLocation))) {
                message = String.format("Path does not exist: '%s'", toolLocation.toString());
                logger.warning(message);
                warnings.add(message);
            }
            Path workLocation = operatorDescriptor.resolveVariables(operatorDescriptor.getWorkingDir()).toPath();
            if (!(Files.exists(workLocation))) {
                message = String.format("Working path does not exist: '%s'", workLocation.toString());
                logger.warning(message);
                warnings.add(message);
            }
            /*File workingDir = operatorDescriptor.resolveVariables(operatorDescriptor.getWorkingDir());
            if (!(workingDir != null && workingDir.exists() && workingDir.isDirectory())) {
                message = String.format("Working directory does not exist: '%s'", workingDir == null ? "null" : workingDir.getPath());
                logger.warning(message);
                warnings.add(message);
            }*/
            ParameterDescriptor[] parameterDescriptors = operatorDescriptor.getParameterDescriptors();
            if (parameterDescriptors != null && parameterDescriptors.length > 0) {
                for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
                    Class<?> dataType = parameterDescriptor.getDataType();
                    String currentValue = parameterSupport.getParameterMap().get(parameterDescriptor.getName()).toString();
                    if (File.class.isAssignableFrom(dataType) &&
                            (parameterDescriptor.isNotNull() || parameterDescriptor.isNotEmpty()) &&
                            (currentValue == null || currentValue.isEmpty() || !Files.exists(Paths.get(currentValue)))) {
                        message = String.format("Path does not exist: '%s'", currentValue == null ? "null" : currentValue);
                        logger.warning(message);
                        warnings.add(message);
                    }
                }
            }
            for (SystemVariable variable : operatorDescriptor.getVariables()) {
                String value = variable.getValue();
                if (value == null || value.isEmpty()) {
                    message = String.format("Variable %s is not set", variable.getKey());
                    logger.warning(message);
                    warnings.add(message);
                }
            }
        } catch (Exception ignored) {
        }
        return warnings.size() == 0;
    }

    @Override
    protected void onCancel() {
        if (operatorTask != null) {
            operatorTask.cancel();
        }
        super.onCancel();
    }

    @Override
    protected void onClose() {
        super.onClose();
    }

    /* End @Override methods section */

    private void onOperatorDescriptorChanged(ToolAdapterOperatorDescriptor newOperatorDescriptor) {
        initialize(newOperatorDescriptor);
        show();
    }

    /**
     * Performs any validation on the user input.
     *
     * @return  <code>true</code> if the input is valid, <code>false</code> otherwise
     */
    private boolean validateUserInput() {
        boolean isValid = true;
        if(!operatorDescriptor.isHandlingOutputName()) {
            File productDir = null;//targetProductSelector.getModel().getProductDir();
            Object value = form.getPropertyValue(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE);
            if (value != null && value instanceof File) {
                productDir = ((File) value).getParentFile();
                appContext.getPreferences().setPropertyString(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, ((File) value).getAbsolutePath());
            }
            isValid = (productDir != null) && productDir.exists();
            if (!isValid) {
                warnings.add("Target product folder is not accessible or does not exist");
            }
        }
        List<ToolParameterDescriptor> mandatoryParams = operatorDescriptor.getToolParameterDescriptors()
                                                                .stream()
                                                                .filter(d -> d.isNotEmpty() || d.isNotNull())
                                                                .collect(Collectors.toList());
        Map<String, Object> parameterMap = parameterSupport.getParameterMap();
        for (ToolParameterDescriptor mandatoryParam : mandatoryParams) {
            String name = mandatoryParam.getName();
            if (!parameterMap.containsKey(name) ||
                    parameterMap.get(name) == null ||
                    parameterMap.get(name).toString().isEmpty()) {
                isValid = false;
                warnings.add(String.format("No value was assigned for the mandatory parameter [%s]", name));
            }
        }

        if (operatorDescriptor.getSourceProductCount() > 0) {
            Product[] sourceProducts = form.getSourceProducts();
            isValid &= (sourceProducts != null) && sourceProducts.length > 0 && Arrays.stream(sourceProducts).filter(sp -> sp == null).count() == 0;
            if (!isValid) {
                warnings.add("No source product was selected");
            }
        }
        return isValid;
    }

    private void displayWarnings() {
        StringBuilder warnMessage = new StringBuilder();
        warnMessage.append("Before executing the tool, please correct the errors below:")
                .append("\n").append("\n");
        for (String msg : warnings) {
            warnMessage.append("\t").append(msg).append("\n");
        }
        Dialogs.showWarning(warnMessage.toString());
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
        //displayErrors();
    }

    private void tearDown(Throwable throwable, Product result) {
        //boolean hasBeenCancelled = operatorTask != null && !operatorTask.hasCompleted;
        if (operatorTask != null) {
            operatorTask.cancel();
        }
        if (throwable != null) {
            if (result != null) {
                final Dialogs.Answer answer = Dialogs.requestDecision(Bundle.ExecutionFailed_Text(),
                                                                      String.format(Bundle.ExecutionFailed_Message(), throwable.getMessage()),
                                                                      false, null);
                if (answer == Dialogs.Answer.YES) {
                    operatorCompleted(result);
                }
            } /*else
                displayErrors();*/
                //SnapDialogs.showError(Bundle.ExecutionFailed_Text(), throwable.getMessage());
        }
        //displayErrors();
        displayErrorMessage();
    }

    private void displayErrorMessage() {
        if (operatorTask != null) {
            List<String> errors = operatorTask.getErrors();
            if (errors != null && errors.size() > 0){
                Dialogs.showWarning("\nIt seems there was en error on execution or the defined tool output error pattern was found.\nPlease consult the SNAP log file");
            }
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
                    //onCancel();
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
                if (operator instanceof ToolAdapterOp) {
                    tearDown(t, ((ToolAdapterOp) operator).getResult());
                } else {
                    tearDown(t, null);
                }
            } finally {
                hasCompleted = true;
            }
        }

        public List<String> getErrors() {
            List<String> errors = null;
            if (operator != null && operator instanceof ToolAdapterOp) {
                errors = ((ToolAdapterOp) operator).getErrors();
            }
            return errors;
        }

        public List<String> getOutput() {
            List<String> allMessages = null;
            if (operator != null && operator instanceof ToolAdapterOp) {
                allMessages = ((ToolAdapterOp) operator).getExecutionOutput();
            }
            return allMessages;
        }
    }

    class ConsoleConsumer extends DefaultOutputConsumer {
        private ConsolePane consolePane;

        public ConsoleConsumer(String progressPattern, String errorPattern, String stepPattern, ProgressMonitor pm, ConsolePane consolePane) {
            super(progressPattern, errorPattern, stepPattern, pm);
            this.consolePane = consolePane;
        }

        @Override
        public void consumeOutput(String line) {
            super.consumeOutput(line);
            if (consolePane != null) {
                if (SwingUtilities.isEventDispatchThread()) {
                    consolePane.append(line);
                } else {
                    SwingUtilities.invokeLater(() -> consolePane.append(line));
                }
            }
        }

        public void setVisible(boolean value) {
            if (this.consolePane != null) {
                this.consolePane.setVisible(value);
            }
        }
    }

    class ProgressWrapper implements ProgressMonitor {

        private ProgressHandle progressHandle;
        private boolean isIndeterminate;
        private ConsoleConsumer console;

        ProgressWrapper(ProgressHandle handle, boolean indeterminate) {
            this.progressHandle = handle;
            this.isIndeterminate = indeterminate;
        }

        public void setConsumer(ConsoleConsumer consumer) {
            console = consumer;
        }

        @Override
        public void beginTask(String taskName, int totalWork) {
            this.progressHandle.setDisplayName(taskName);
            this.progressHandle.start(totalWork, -1);
            if (this.isIndeterminate) {
                this.progressHandle.switchToIndeterminate();
            }
            if (this.console != null) {
                this.console.setVisible(true);
            }
        }

        @Override
        public void done() {
            this.progressHandle.finish();
        }

        @Override
        public void internalWorked(double work) {
            this.progressHandle.progress((int)work);
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.progressHandle.suspend("Cancelled");
        }

        @Override
        public void setTaskName(String taskName) {
            this.progressHandle.setDisplayName(taskName);
        }

        @Override
        public void setSubTaskName(String subTaskName) {
            this.progressHandle.progress(subTaskName);
        }

        @Override
        public void worked(int work) {
            internalWorked(work);
        }

    }
}
