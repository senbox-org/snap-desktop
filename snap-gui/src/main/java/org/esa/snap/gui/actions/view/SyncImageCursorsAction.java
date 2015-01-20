/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.view;

import org.esa.snap.gui.util.BooleanPreferenceKeyAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * @author Norman
 */
@ActionID(
        category = "View",
        id = "org.esa.snap.gui.actions.view.SyncImageCursorsAction"
)
@ActionRegistration(
        displayName = "#CTL_SyncImageCursorsActionName",
        lazy = false
)
@ActionReference(
        path = "Menu/View",
        position = 1010
)
@NbBundle.Messages({
        "CTL_SyncImageCursorsActionName=Synchronise Image Cursors",
        "CTL_SyncImageCursorsActionToolTip=Synchronises cursor positions across multiple image views."
})
public final class SyncImageCursorsAction extends BooleanPreferenceKeyAction {

    public static final String PREFERENCE_KEY = "auto_sync_image_cursors";

    public SyncImageCursorsAction() {
        super(PREFERENCE_KEY);
        putValue(NAME, Bundle.CTL_SyncImageCursorsActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/gui/icons/SyncCursor24.png", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_SyncImageCursorsActionToolTip());
    }
}
