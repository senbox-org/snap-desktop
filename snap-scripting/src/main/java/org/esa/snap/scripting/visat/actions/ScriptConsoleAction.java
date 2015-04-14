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

package org.esa.snap.scripting.visat.actions;

import org.esa.snap.scripting.visat.ScriptConsoleTopComponent;
import org.esa.snap.scripting.visat.ScriptManager;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

public abstract class ScriptConsoleAction extends AbstractAction {
    private final ScriptConsoleTopComponent scriptConsoleTC;

    protected ScriptConsoleAction(ScriptConsoleTopComponent scriptConsoleTC, String name, String commandKey, ImageIcon iconResource) {
        this.scriptConsoleTC = scriptConsoleTC;
        putValue(AbstractAction.NAME, name);
        putValue(AbstractAction.ACTION_COMMAND_KEY, commandKey);
        putValue(AbstractAction.SMALL_ICON, iconResource);
        putValue(AbstractAction.LARGE_ICON_KEY, iconResource);
    }

    public ScriptConsoleTopComponent getScriptConsoleTopComponent() {
        return scriptConsoleTC;
    }

    public ScriptManager getScriptManager() {
        return scriptConsoleTC.getScriptManager();
    }
}
