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

    private final MetadataInspector metadataInspector;
    private final Path productFile;

    public ReadProductInspectorTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, MetadataInspector metadataInspector, Path productFile) {
        super(loadingIndicator, threadId, 500);
        this.metadataInspector = metadataInspector;
        this.productFile = productFile;
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        super.onTimerWakeUp("Loading...");
    }

    @Override
    protected MetadataInspector.Metadata execute() throws Exception {
        return metadataInspector.getMetadata(this.productFile);
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the product metadata inspector.";
    }
}
