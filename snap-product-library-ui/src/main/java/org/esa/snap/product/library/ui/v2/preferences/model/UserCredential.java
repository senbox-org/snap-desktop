package org.esa.snap.product.library.ui.v2.preferences.model;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.http.auth.Credentials;

import java.security.Principal;

public final class UserCredential implements Credentials {

    private String username;
    private String password;

    public UserCredential(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Principal getUserPrincipal() {
        return new BasicUserPrincipal(username);
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
