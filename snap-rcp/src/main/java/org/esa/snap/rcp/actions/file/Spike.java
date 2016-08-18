/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.rcp.actions.file;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.InstallSupport.Installer;
import org.netbeans.api.autoupdate.InstallSupport.Validator;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.OperationContainer.OperationInfo;
import org.netbeans.api.autoupdate.OperationException;
import org.netbeans.api.autoupdate.OperationSupport.Restarter;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.autoupdate.UpdateUnitProvider;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle;

/**
 * @author muhammad.bc.
 */

class Spike {

}

class UpdateHandler {

    public static final String NAMESPACE = "org.myorg.myplugin";
    public static final String CODE_NAME = "org_myorg_myplugin_update_center"; // NOI18N

    public static boolean timeToCheck() {
        // every startup
        return true;
    }

    public static class UpdateHandlerException extends Exception {
        public UpdateHandlerException(String msg) {
            super(msg);
        }

        public UpdateHandlerException(String msg, Throwable th) {
            super(msg, th);
        }
    }

    public static void checkAndHandleUpdates() {


        // refresh silent update center first
        refreshSilentUpdateProvider();

        Collection<UpdateElement> updates = findUpdates();
        Collection<UpdateElement> available = Collections.emptySet();
        if (installNewModules()) {
            available = findNewModules();
        }
        if (updates.isEmpty() && available.isEmpty()) {
            // none for install
            return;
        }


        // create a container for install
        OperationContainer<InstallSupport> containerForInstall = feedContainer(available, false);
        if (containerForInstall != null) {
            try {
                handleInstall(containerForInstall);
            } catch (UpdateHandlerException ex) {
                return;
            }
        }

        // create a container for update
        OperationContainer<InstallSupport> containerForUpdate = feedContainer(updates, true);
        if (containerForUpdate != null) {
            try {
                handleInstall(containerForUpdate);
//                MyPlugin.log.info("Plugin update finished.");
            } catch (UpdateHandlerException ex) {
//                MyPlugin.log.log(Level.SEVERE, ex.toString());
                return;
            }
        }

    }

    public static boolean isLicenseApproved(String license) {
        // place your code there
        return true;
    }

    // package private methods
    static Collection<UpdateElement> findUpdates() {
        // check updates
        Collection<UpdateElement> elements4update = new HashSet<UpdateElement>();
        List<UpdateUnit> updateUnits = UpdateManager.getDefault().getUpdateUnits();
        for (UpdateUnit unit : updateUnits) {
            if (unit.getInstalled() != null) { // means the plugin already installed
                if (!unit.getAvailableUpdates().isEmpty()) { // has updates
                    elements4update.add(unit.getAvailableUpdates().get(0)); // add plugin with highest version
                }
            }
        }
        return elements4update;
    }

    static void handleInstall(OperationContainer<InstallSupport> container) throws UpdateHandlerException {
        // check licenses
        if (!allLicensesApproved(container)) {
            // have a problem => cannot continue
            throw new UpdateHandlerException("Cannot continue because license approval is missing.");
        }

        // download
        InstallSupport support = container.getSupport();
        Validator v = null;
        try {
            v = doDownload(support);
        } catch (OperationException ex) {
            // caught a exception
            throw new UpdateHandlerException("A problem caught while downloading, cause: ", ex);
        }
        if (v == null) {
            // have a problem => cannot continue
            throw new UpdateHandlerException("Missing Update Validator => cannot continue.");
        }

        // verify
        Installer i = null;
        try {
            i = doVerify(support, v);
        } catch (OperationException ex) {
            // caught a exception
            throw new UpdateHandlerException("A problem caught while verification of updates, cause: ", ex);
        }
        if (i == null) {
            // have a problem => cannot continue
            throw new UpdateHandlerException("Missing Update Installer => cannot continue.");
        }

        // install
        Restarter r = null;
        try {
            r = doInstall(support, i);
        } catch (OperationException ex) {
            // caught a exception
            throw new UpdateHandlerException("A problem caught while installation of updates, cause: ", ex);
        }

        // restart later
        support.doRestartLater(r);
    }

    static Collection<UpdateElement> findNewModules() {
        // check updates
        Collection<UpdateElement> elements4install = new HashSet<UpdateElement>();
        List<UpdateUnit> updateUnits = UpdateManager.getDefault().getUpdateUnits();
        for (UpdateUnit unit : updateUnits) {
            if (unit.getInstalled() == null) { // means the plugin is not installed yet
                if (!unit.getAvailableUpdates().isEmpty()) { // is available
                    elements4install.add(unit.getAvailableUpdates().get(0)); // add plugin with highest version
                }
            }
        }
        return elements4install;
    }

    static void refreshSilentUpdateProvider() {
        UpdateUnitProvider silentUpdateProvider = getSilentUpdateProvider();
        if (silentUpdateProvider == null) {
            // have a problem => cannot continue
//            MyPlugin.log.info("Missing Silent Update Provider => cannot continue.");
            return;
        }
        try {
            final String displayName = "Checking for plugin updates...";
            silentUpdateProvider.refresh(
                    ProgressHandleFactory.createHandle(
                            displayName,
                            new Cancellable() {
                                @Override
                                public boolean cancel() {
                                    return true;
                                }
                            }
                    ),
                    true
            );
        } catch (IOException ex) {
            // caught a exception
//            MyPlugin.log.log(Level.SEVERE, "A problem caught while refreshing Update Centers, cause: " + ex.toString());
        }
    }

    static UpdateUnitProvider getSilentUpdateProvider() {
        List<UpdateUnitProvider> providers = UpdateUnitProviderFactory.getDefault().getUpdateUnitProviders(true);
        for (UpdateUnitProvider p : providers) {
            if (CODE_NAME.equals(p.getName())) {
                try {
                    final String displayName = "Checking for plugin updates...";
                    p.refresh(
                            ProgressHandleFactory.createHandle(
                                    displayName,
                                    new Cancellable() {
                                        @Override
                                        public boolean cancel() {
                                            return true;
                                        }
                                    }
                            ),
                            true
                    );
                } catch (IOException ex) {
                    // caught a exception
//                    MyPlugin.log.log(Level.SEVERE, "A problem caught while refreshing Update Centers, cause: " + ex.toString());
                }
                return p;
            }
        }
        return null;
    }

    static OperationContainer<InstallSupport> feedContainer(Collection<UpdateElement> updates, boolean update) {
        if (updates == null || updates.isEmpty()) {
            return null;
        }
        // create a container for update
        OperationContainer<InstallSupport> container;
        if (update) {
            container = OperationContainer.createForUpdate();
        } else {
            container = OperationContainer.createForInstall();
        }

        // loop all updates and add to container for update
        for (UpdateElement ue : updates) {
            if (container.canBeAdded(ue.getUpdateUnit(), ue)) {
//                MyPlugin.log.info("Update found: " + ue);
                OperationInfo<InstallSupport> operationInfo = container.add(ue);
                if (operationInfo == null) {
                    continue;
                }
                container.add(operationInfo.getRequiredElements());
                if (!operationInfo.getBrokenDependencies().isEmpty()) {
                    // have a problem => cannot continue
//                    MyPlugin.log.info("There are broken dependencies => cannot continue, broken deps: " + operationInfo.getBrokenDependencies());
                    return null;
                }
            }
        }
        return container;
    }

    static boolean allLicensesApproved(OperationContainer<InstallSupport> container) {
        if (!container.listInvalid().isEmpty()) {
            return false;
        }
        for (OperationInfo<InstallSupport> info : container.listAll()) {
            String license = info.getUpdateElement().getLicence();
            if (!isLicenseApproved(license)) {
                return false;
            }
        }
        return true;
    }

    static Validator doDownload(InstallSupport support) throws OperationException {
        final String displayName = "Downloading new plugin version...";
        ProgressHandle downloadHandle = ProgressHandleFactory.createHandle(
                displayName,
                new Cancellable() {
                    @Override
                    public boolean cancel() {
                        return true;
                    }
                }
        );
        return support.doDownload(downloadHandle, true);
    }

    static Installer doVerify(InstallSupport support, Validator validator) throws OperationException {
        final String displayName = "Validating plugin...";
        ProgressHandle validateHandle = ProgressHandleFactory.createHandle(
                displayName,
                new Cancellable() {
                    @Override
                    public boolean cancel() {
                        return true;
                    }
                }
        );
        Installer installer = support.doValidate(validator, validateHandle);
        return installer;
    }

    static Restarter doInstall(InstallSupport support, Installer installer) throws OperationException {
        final String displayName = "Installing plugin...";
        ProgressHandle installHandle = ProgressHandleFactory.createHandle(
                displayName,
                new Cancellable() {
                    @Override
                    public boolean cancel() {
                        return true;
                    }
                }
        );
        return support.doInstall(installer, installHandle);
    }

    private static boolean installNewModules() {
        String s = NbBundle.getBundle(NAMESPACE + ".Bundle").getString("UpdateHandler.NewModules");
        return Boolean.parseBoolean(s);
    }
}
