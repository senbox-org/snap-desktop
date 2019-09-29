package org.esa.snap.remote.execution.machines;

import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.GenericRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;
import org.esa.snap.ui.loading.IMessageDialog;

import javax.swing.SwingUtilities;

/**
 * Created by jcoravu on 18/12/2018.
 */
public class TestConnectionTimerRunnable extends AbstractTimerRunnable<RemoteMachineConnectionResult> {

    private final IMessageDialog parentWindow;
    private final RemoteMachineProperties sshServer;

    private String loadingIndicatorMessage;

    public TestConnectionTimerRunnable(IMessageDialog parentWindow, ILoadingIndicator loadingIndicator, int threadId, RemoteMachineProperties sshServerCredentials) {
        super(loadingIndicator, threadId, 500);

        this.parentWindow = parentWindow;
        this.sshServer = sshServerCredentials;

        this.loadingIndicatorMessage = "Testing connection...";
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp(this.loadingIndicatorMessage);

        this.loadingIndicatorMessage = null; // reset the message
    }

    @Override
    protected RemoteMachineConnectionResult execute() throws Exception {
        ITestRemoteMachineConnection callback = new ITestRemoteMachineConnection() {
            @Override
            public void testSSHConnection() {
                displayLoadingIndicatorMessageLater("Testing connection...");
            }

            @Override
            public void testGPTApplication() {
                displayLoadingIndicatorMessageLater("Testing GPT application...");
            }
        };
        return CommandExecutorUtils.canConnectToRemoteMachine(this.sshServer, callback);
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to connect to the remote machine '" + this.sshServer.getHostName()+"'.";
    }

    @Override
    protected void onFailed(Exception exception) {
        this.parentWindow.showErrorDialog("Failed to connect to the remote machine.");
    }

    @Override
    protected void onSuccessfullyFinish(RemoteMachineConnectionResult result) {
        if (result.isRemoteMachineAvailable()) {
            StringBuilder message = new StringBuilder();
            message.append("The connection to the remote machine is successfully.")
                    .append("\n\n");
            if (result.isGPTApplicationAvailable()) {
                message.append("The GPT application is available on the remote machine.");
                this.parentWindow.showInformationDialog(message.toString());
            } else {
                message.append("The GPT application is not available on the remote machine.");
                this.parentWindow.showErrorDialog(message.toString());
            }
        } else {
            this.parentWindow.showErrorDialog("The connection to the remote machine has failed.");
        }
    }

    private void displayLoadingIndicatorMessageLater(String messageToDisplay) {
        GenericRunnable<String> runnable = new GenericRunnable<String>(messageToDisplay) {
            @Override
            protected void execute(String message) {
                onDisplayMessage(message);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void onDisplayMessage(String messageToDisplay) {
        if (this.loadingIndicatorMessage == null) {
            // the timer is has been activated
            onDisplayLoadingIndicatorMessage(messageToDisplay);
        } else {
            this.loadingIndicatorMessage = messageToDisplay;
        }
    }
}
