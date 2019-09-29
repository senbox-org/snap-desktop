package org.esa.snap.remote.execution.operator;

import org.esa.snap.remote.execution.RemoteExecutionOp;
import org.esa.snap.remote.execution.exceptions.OperatorExecutionException;
import org.esa.snap.remote.execution.exceptions.OperatorInitializeException;
import org.esa.snap.remote.execution.exceptions.WaitingTimeoutException;
import org.esa.snap.remote.execution.local.folder.IMountLocalSharedFolderResult;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.remote.execution.topology.RemoteTopology;
import org.esa.snap.remote.execution.topology.RemoteTopologyUtils;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.IMessageDialog;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.ui.AppContext;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Created by jcoravu on 27/12/2018.
 */
public class RemoteExecutionTimerRunnable extends AbstractTimerRunnable<Product> {

    private final AppContext appContext;
    private final IMessageDialog parentWindow;
    private final Map<String, Object> parameterMap;
    private final boolean openTargetProductInApplication;
    private final IMountLocalSharedFolderResult mountLocalSharedFolderResult;
    private final RemoteTopology remoteTopologyToSave;
    private final Path remoteTopologyFilePath;

    public RemoteExecutionTimerRunnable(AppContext appContext, IMessageDialog parentWindow, ILoadingIndicator loadingIndicator,
                                        int threadId, Map<String, Object> parameterMap, boolean openTargetProductInApplication,
                                        IMountLocalSharedFolderResult mountLocalSharedFolderResult,
                                        RemoteTopology remoteTopologyToSave, Path remoteTopologyFilePath) {

        super(loadingIndicator, threadId, 500);

        this.appContext = appContext;
        this.parentWindow = parentWindow;
        this.parameterMap = parameterMap;
        this.remoteTopologyToSave = remoteTopologyToSave;
        this.remoteTopologyFilePath = remoteTopologyFilePath;
        this.openTargetProductInApplication = openTargetProductInApplication;
        this.mountLocalSharedFolderResult = mountLocalSharedFolderResult;
    }

    @Override
    protected final void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp("Processing...");
    }

    @Override
    protected final Product execute() throws Exception {
        Path parentFolder = this.remoteTopologyFilePath.getParent();
        if (!Files.exists(parentFolder)) {
            Files.createDirectories(parentFolder);
        }
        RemoteTopologyUtils.writeTopology(this.remoteTopologyFilePath, this.remoteTopologyToSave);

        if (this.mountLocalSharedFolderResult != null) {
            // unmount the local shared folder
            String localSharedFolderPath = (String) this.parameterMap.get(RemoteExecutionDialog.LOCAL_SHARED_FOLDER_PATH_PROPERTY);
            String localPassword = (String) this.parameterMap.get(RemoteExecutionDialog.LOCAL_PASSWORD_PROPERTY);
            this.mountLocalSharedFolderResult.unmountLocalSharedFolder(localSharedFolderPath, localPassword);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    onSuccessfullyUnmountLocalFolder();
                }
            });
        }

        Map<String, Product> sourceProducts = Collections.emptyMap();

        String operatorName = OperatorSpi.getOperatorAlias(RemoteExecutionOp.class);

        // create the operator
        RemoteExecutionOp operator = (RemoteExecutionOp)GPF.getDefaultInstance().createOperator(operatorName, this.parameterMap, sourceProducts, null);

        // execute the operator
        operator.execute(null);

        if (operator.canCreateTargetProduct()) {
            return ProductIO.readProduct(operator.getMasterProductFilePath());
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to execute the operator.";
    }

    @Override
    protected void onSuccessfullyFinish(Product targetProduct) {
        this.parentWindow.close();

        if (this.openTargetProductInApplication) {
            if (targetProduct == null) {
                throw new NullPointerException("The target product cannot be null.");
            } else {
                this.appContext.getProductManager().addProduct(targetProduct);
            }
        }
    }

    @Override
    protected void onFailed(Exception exception) {
        String message;
        if (exception instanceof OperatorInitializeException) {
            message = exception.getMessage();
        } else if (exception instanceof OperatorExecutionException) {
            message = exception.getMessage();
        } else if (exception instanceof WaitingTimeoutException) {
            message = exception.getMessage();
        } else if (exception instanceof OperatorException) {
            message = exception.getMessage();
        } else {
            message = "Failed to execute the operator.";
        }
        this.parentWindow.showErrorDialog(message, "Operator execution");
    }

    protected void onSuccessfullyUnmountLocalFolder() {
    }
}
