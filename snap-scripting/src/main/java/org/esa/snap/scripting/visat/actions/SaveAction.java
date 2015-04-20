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
import org.esa.snap.tango.TangoIcons;

import java.awt.event.ActionEvent;

public class SaveAction extends ScriptConsoleAction {
    public static final String ID = "scriptConsole.save";

    public SaveAction(ScriptConsoleTopComponent scriptConsoleTC) {
        super(scriptConsoleTC, "Save", ID, TangoIcons.actions_document_save(TangoIcons.Res.R16));
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (getScriptConsoleTopComponent().getFile() != null) {
            getScriptConsoleTopComponent().saveScript();
        } else {
            getScriptConsoleTopComponent().getAction(SaveAsAction.ID).actionPerformed(e);
        }
    }
}
