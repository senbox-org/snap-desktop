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
package org.esa.snap.rcp.mask;

import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.event.ListSelectionListener;

@TopComponent.Description(
        preferredID = "MaskManagerTopComponent",
        iconBase = "org/esa/snap/rcp/icons/MaskManager.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = PackageDefaults.MASK_MANAGER_MODE,
        openAtStartup = PackageDefaults.MASK_MANAGER_OPEN,
        position = PackageDefaults.MASK_MANAGER_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.mask.MaskManagerTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MaskManagerTopComponent_Name",
        preferredID = "MaskManagerTopComponent"
)
@NbBundle.Messages({
        "CTL_MaskManagerTopComponent_Name=" + PackageDefaults.MASK_MANAGER_NAME,
        "CTL_MaskManagerTopComponent_HelpId=showMaskManagerWnd"
})
public class MaskManagerToolTopComponent extends MaskToolTopComponent {
    public static final String ID = MaskManagerToolTopComponent.class.getName();

    public MaskManagerToolTopComponent() {
        initUI();
    }

    @Override
    protected MaskForm createMaskForm(ToolTopComponent maskTopComponent, ListSelectionListener selectionListener) {
        return new MaskManagerForm(this, selectionListener);
    }

    @Override
    protected String getTitle() {
        return Bundle.CTL_MaskManagerTopComponent_Name();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_MaskManagerTopComponent_HelpId());
    }
}