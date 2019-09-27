package org.esa.snap.product.library.ui.v2.preferences.model;

import org.apache.http.auth.Credentials;

import java.util.List;

public class RemoteRepositoryCredentials {

    private final String repositoryId;
    private final List<Credentials> credentialsList;

    public RemoteRepositoryCredentials(String repositoryId, List<Credentials> credentialsList) {
        this.repositoryId = repositoryId;
        this.credentialsList = credentialsList;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public List<Credentials> getCredentialsList() {
        return credentialsList;
    }
}
