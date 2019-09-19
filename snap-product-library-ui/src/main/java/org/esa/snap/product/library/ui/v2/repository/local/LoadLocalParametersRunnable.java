package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.v2.database.DatabaseTableNames;
import org.esa.snap.product.library.v2.database.H2DatabaseAccessor;
import org.esa.snap.product.library.v2.database.RemoteMission;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 16/9/2019.
 */
public class LoadLocalParametersRunnable extends AbstractRunnable<LocalParameterValues> {

    public LoadLocalParametersRunnable() {
    }

    @Override
    protected LocalParameterValues execute() throws Exception {
        try (Connection connection = H2DatabaseAccessor.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                Map<Short, Set<String>> attributes = ProductLibraryDAL.loadAttributesNames(statement);
                List<RemoteMission> missions = ProductLibraryDAL.loadMissions(statement);
                return new LocalParameterValues(missions, attributes);
            }
        }
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the parameters from the database.";
    }

    @Override
    protected final void successfullyExecuting(LocalParameterValues parameterValues) {
        GenericRunnable<LocalParameterValues> runnable = new GenericRunnable<LocalParameterValues>(parameterValues) {
            @Override
            protected void execute(LocalParameterValues item) {
                onSuccessfullyExecuting(item);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    protected void onSuccessfullyExecuting(LocalParameterValues parameterValues) {
    }
}
