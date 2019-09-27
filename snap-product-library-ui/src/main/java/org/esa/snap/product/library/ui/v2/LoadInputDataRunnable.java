package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.preferences.RepositoriesCredentialsController;
import org.esa.snap.product.library.ui.v2.preferences.model.RemoteRepositoryCredentials;
import org.esa.snap.product.library.ui.v2.repository.local.LocalParameterValues;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.v2.database.H2DatabaseAccessor;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.product.library.v2.database.RemoteMission;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 16/9/2019.
 */
public class LoadInputDataRunnable extends AbstractRunnable<LocalParameterValues> {

    private static final Logger logger = Logger.getLogger(LoadInputDataRunnable.class.getName());

    public LoadInputDataRunnable() {
    }

    @Override
    protected LocalParameterValues execute() throws Exception {
        List<RemoteRepositoryCredentials> repositoriesCredentials = null;
        try {
            repositoriesCredentials = RepositoriesCredentialsController.getInstance().getRepositoriesCredentials();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to load the remote repository credentials.", exception);
        }

        Map<Short, Set<String>> attributeNamesPerMission = null;
        List<RemoteMission> missions = null;
        try (Connection connection = H2DatabaseAccessor.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                attributeNamesPerMission = ProductLibraryDAL.loadAttributesNamesPerMission(statement);
                missions = ProductLibraryDAL.loadMissions(statement);
                if (missions.size() > 1) {
                    Comparator<RemoteMission> comparator = new Comparator<RemoteMission>() {
                        @Override
                        public int compare(RemoteMission o1, RemoteMission o2) {
                            return o1.getName().compareToIgnoreCase(o2.getName());
                        }
                    };
                    Collections.sort(missions, comparator);
                }
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to load input data from the database.", exception);
        }
        return new LocalParameterValues(repositoriesCredentials, missions, attributeNamesPerMission);
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
