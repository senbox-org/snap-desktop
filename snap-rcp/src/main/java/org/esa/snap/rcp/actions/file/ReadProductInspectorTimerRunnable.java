package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;

import java.io.File;
import java.nio.file.Path;

/**
 * Created by jcoravu on 17/2/2020.
 * Updated by Denisa Stefanescu on 18/02/2020
 */
public class ReadProductInspectorTimerRunnable extends AbstractTimerRunnable<MetadataInspector.Metadata> {

    private MetadataInspector metadataInspector;
    private File file;
    private Thread runThread;

    public ReadProductInspectorTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, MetadataInspector metadataInspector, File file) {
        super(loadingIndicator, threadId, 500);
        this.metadataInspector = metadataInspector;
        this.file = file;
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp("Loading...");
    }

    @Override
    protected MetadataInspector.Metadata execute() throws Exception {
        runThread = Thread.currentThread();
        Path input = ProductFileChooser.convertInputToPath(file);
        return metadataInspector.getMetadata(input);
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the product metadata inspector.";
    }

    public void stopRequest() {
        if (runThread != null) {
            runThread.interrupt();
        }
    }
}
