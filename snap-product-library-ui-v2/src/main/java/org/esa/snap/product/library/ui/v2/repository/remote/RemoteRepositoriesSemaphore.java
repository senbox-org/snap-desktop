package org.esa.snap.product.library.ui.v2.repository.remote;

import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * A counting semaphore which allows a fixed number of connections to query a certain remote repository in the same time.
 *
 * Created by jcoravu on 16/10/2019.
 */
public class RemoteRepositoriesSemaphore {

    private final RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders;
    private final Map<String, Semaphore> map;

    public RemoteRepositoriesSemaphore(RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders) {
        this.remoteRepositoryProductProviders = remoteRepositoryProductProviders;
        this.map = new HashMap<>();
    }

    public void acquirePermission(String remoteRepositoryName, Credentials credentials) throws InterruptedException {
        Semaphore semaphore = getSemaphore(remoteRepositoryName, credentials);
        if (semaphore != null) {
            semaphore.acquire(1);
        }
    }

    public void releasePermission(String remoteRepositoryName, Credentials credentials) throws InterruptedException {
        Semaphore semaphore = getSemaphore(remoteRepositoryName, credentials);
        if (semaphore != null) {
            semaphore.release(1);
        }
    }

    private Semaphore getSemaphore(String remoteRepositoryName, Credentials credentials) {
        Semaphore semaphore;
        synchronized (this.map) {
            String key = buildLKey(remoteRepositoryName, credentials);
            semaphore = this.map.get(key);
            if (semaphore == null) {
                RemoteProductsRepositoryProvider remoteRepositoryProvider = null;
                for (int i=0; i<this.remoteRepositoryProductProviders.length && remoteRepositoryProvider == null; i++) {
                    if (this.remoteRepositoryProductProviders[i].getRepositoryName().equals(remoteRepositoryName)) {
                        remoteRepositoryProvider = this.remoteRepositoryProductProviders[i];
                    }
                }
                if (remoteRepositoryProvider == null) {
                    throw new NullPointerException("The remote repository provider with id '" + remoteRepositoryName + "' does not exist.");
                }
                if (remoteRepositoryProvider.getMaximumAllowedTransfersPerAccount() > 0) {
                    semaphore = new Semaphore(remoteRepositoryProvider.getMaximumAllowedTransfersPerAccount());
                    this.map.put(key, semaphore);
                }
            }
        }
        return semaphore; // the result may be null
    }

    private static String buildLKey(String remoteRepositoryId, Credentials credentials) {
        return remoteRepositoryId + "|" + credentials.getUserPrincipal().getName();
    }
}
