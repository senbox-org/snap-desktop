/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.openide.util.HelpCtx;

import javax.swing.AbstractAction;
import java.awt.Window;


public abstract class AbstractSnapAction extends AbstractAction implements HelpCtx.Provider {

    public static final String HELP_ID = "helpId";

    private SnapContext appContext;

    public AppContext getAppContext() {
        if (appContext == null) {
            appContext = new SnapContext();
        }
        return appContext;
    }

    public String getHelpId() {
        Object value = getValue(HELP_ID);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public void setHelpId(String helpId) {
        putValue(HELP_ID, helpId);
    }

    @Override
    public HelpCtx getHelpCtx() {
        String helpId = getHelpId();
        if (helpId != null) {
            return new HelpCtx(helpId);
        }
        return null;
    }

    private static class SnapContext implements AppContext {

        private final SnapApp app = SnapApp.getDefault();

        @Override
        public ApplicationPage getApplicationPage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProductManager getProductManager() {
            return app.getProductManager();
        }

        @Override
        public Product getSelectedProduct() {
            return app.getSelectedProduct();
        }

        @Override
        public Window getApplicationWindow() {
            return app.getMainFrame();
        }

        @Override
        public String getApplicationName() {
            return app.getInstanceName();
        }

        @Override
        public void handleError(String message, Throwable t) {
            app.handleError(message, t);
        }

        @Override
        @Deprecated
        public PropertyMap getPreferences() {
            return app.getCompatiblePreferences();
        }

        @Override
        public ProductSceneView getSelectedProductSceneView() {
            return app.getSelectedProductSceneView();
        }
    }
}