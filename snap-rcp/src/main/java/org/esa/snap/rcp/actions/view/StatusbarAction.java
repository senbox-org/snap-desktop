/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.view;

import org.esa.snap.rcp.util.BooleanPreferenceKeyAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * @author Norman
 */
@ActionID(category = "View", id = "StatusbarAction" )
@ActionRegistration(displayName = "#CTL_StatusbarAction_Text", lazy = false )
@ActionReference(path = "Menu/View", position = 300)
@NbBundle.Messages({
        "CTL_StatusbarAction_Text=Statusbar",
        "CTL_StatusbarAction_ToolTip=Change visibility of the Statusbar."
})
public final class StatusbarAction extends BooleanPreferenceKeyAction {

    public static final String PREFERENCE_KEY = "statusbar_visibility";
    public static final boolean PREFERENCE_DEFAULT_VALUE = true;

    public StatusbarAction() {
        super(PREFERENCE_KEY, PREFERENCE_DEFAULT_VALUE);
        putValue(NAME, Bundle.CTL_StatusbarAction_Text());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_StatusbarAction_ToolTip());
    }
}
