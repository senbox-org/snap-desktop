package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.v2.database.RemoteMission;
import org.esa.snap.product.library.v2.preferences.model.RepositoryCredentials;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 18/9/2019.
 */
public class LocalParameterValues {

    private final List<RepositoryCredentials> repositoriesCredentials;
    private final List<RemoteMission> missions;
    private final Map<Short, Set<String>> attributes;

    public LocalParameterValues(List<RepositoryCredentials> repositoriesCredentials, List<RemoteMission> missions, Map<Short, Set<String>> attributes) {
        this.repositoriesCredentials = repositoriesCredentials;
        this.missions = missions;
        this.attributes = attributes;
    }

    public List<RemoteMission> getMissions() {
        return missions;
    }

    public Map<Short, Set<String>> getAttributes() {
        return attributes;
    }

    public List<RepositoryCredentials> getRepositoriesCredentials() {
        return repositoriesCredentials;
    }
}
