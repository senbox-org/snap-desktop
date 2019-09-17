package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.v2.database.RemoteMission;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Created by jcoravu on 16/9/2019.
 */
public class LoadLocalParametersRunnable extends AbstractRunnable<List<RemoteMission>> {

    public LoadLocalParametersRunnable() {
    }

    @Override
    protected List<RemoteMission> execute() throws Exception {
        return ProductLibraryDAL.loadMissions();
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the parameters from the database.";
    }

    @Override
    protected final void successfullyExecuting(List<RemoteMission> missions) {
        GenericRunnable<List<RemoteMission>> runnable = new GenericRunnable<List<RemoteMission>>(missions) {
            @Override
            protected void execute(List<RemoteMission> item) {
                onSuccessfullyExecuting(item);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    protected void onSuccessfullyExecuting(List<RemoteMission> missions) {
    }
}
