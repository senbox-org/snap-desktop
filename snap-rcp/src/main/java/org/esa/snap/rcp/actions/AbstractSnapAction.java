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

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;
import org.openide.util.HelpCtx;

import javax.swing.AbstractAction;


public abstract class AbstractSnapAction extends AbstractAction implements HelpCtx.Provider {

    public static final String HELP_ID = "helpId";

    private AppContext appContext;

    public AppContext getAppContext() {
        if (appContext == null) {
            appContext = SnapApp.getDefault().getAppContext();
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

}
