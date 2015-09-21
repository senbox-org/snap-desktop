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
@ActionID(category = "View", id = "SyncImageViewsAction" )
@ActionRegistration(displayName = "#CTL_SyncImageViewsActionName", lazy = false )
@ActionReference(path = "Menu/View", position = 319, separatorAfter = 320 )
@NbBundle.Messages({
        "CTL_SyncImageViewsActionName=Synchronise Image Views",
        "CTL_SyncImageViewsActionToolTip=Synchronises views across multiple image windows."
})
public final class SyncImageViewsAction extends BooleanPreferenceKeyAction {

    public static final String PREFERENCE_KEY = "auto_sync_image_views";
    public static final boolean PREFERENCE_DEFAULT_VALUE = false;

    public SyncImageViewsAction() {
        super(PREFERENCE_KEY, PREFERENCE_DEFAULT_VALUE);
        putValue(NAME, Bundle.CTL_SyncImageViewsActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/esa/snap/rcp/icons/SyncViews24.png", false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_SyncImageViewsActionToolTip());
    }
}
