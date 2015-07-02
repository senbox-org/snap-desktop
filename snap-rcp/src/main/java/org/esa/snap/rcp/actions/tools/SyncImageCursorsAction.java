/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.tools;

import org.esa.snap.rcp.util.BooleanPreferenceKeyAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * @author Norman
 */
@ActionID(category = "View", id = "SyncImageCursorsAction" )
@ActionRegistration(displayName = "#CTL_SyncImageCursorsActionName", lazy = false )
@ActionReference(path = "Menu/View", position = 311, separatorBefore = 310 )
@NbBundle.Messages({
        "CTL_SyncImageCursorsActionName=Synchronise Image Cursors",
        "CTL_SyncImageCursorsActionToolTip=Synchronises cursor positions across multiple image windows."
})
public final class SyncImageCursorsAction extends BooleanPreferenceKeyAction {

    public static final String PREFERENCE_KEY = "auto_sync_image_cursors";
    public static final boolean PREFERENCE_DEFAULT_VALUE = false;

    public SyncImageCursorsAction() {
        super(PREFERENCE_KEY, PREFERENCE_DEFAULT_VALUE);
        putValue(NAME, Bundle.CTL_SyncImageCursorsActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/SyncCursor24.png", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_SyncImageCursorsActionToolTip());
    }
}
