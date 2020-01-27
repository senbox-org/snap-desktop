package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.v2.database.LocalRepositoryParameterValues;
import org.esa.snap.product.library.ui.v2.preferences.model.RemoteRepositoryCredentials;

import java.util.List;

/**
 * Created by jcoravu on 18/9/2019.
 */
public class LocalParameterValues {

    private final List<RemoteRepositoryCredentials> repositoriesCredentials;
    private final LocalRepositoryParameterValues localRepositoryParameterValues;

    public LocalParameterValues(List<RemoteRepositoryCredentials> repositoriesCredentials, LocalRepositoryParameterValues localRepositoryParameterValues) {

        this.repositoriesCredentials = repositoriesCredentials;
        this.localRepositoryParameterValues = localRepositoryParameterValues;
    }

    public List<RemoteRepositoryCredentials> getRepositoriesCredentials() {
        return repositoriesCredentials;
    }

    public LocalRepositoryParameterValues getLocalRepositoryParameterValues() {
        return localRepositoryParameterValues;
    }
}
