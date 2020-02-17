package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;

/**
 * Created by jcoravu on 17/2/2020.
 */
public class ReadProductInspectorTimerRunnable extends AbstractTimerRunnable<MetadataInspector.Metadata> {

    public ReadProductInspectorTimerRunnable(ILoadingIndicator loadingIndicator, int threadId) {
        super(loadingIndicator, threadId, 500);
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp("Loading...");
    }

    @Override
    protected MetadataInspector.Metadata execute() throws Exception {
        Thread.sleep(3000);
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the product metadata inspector.";
    }
}
