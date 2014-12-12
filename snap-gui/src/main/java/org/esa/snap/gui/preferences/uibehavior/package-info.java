/**
 * This package contains the UI behavior preferences handling of SNAP.
 *
 * @author thomas
 */
@OptionsPanelController.ContainerRegistration(
        id = "SnapPreferences",
        categoryName = "#OptionsCategory_Name_SNAP",
        iconBase = "org/esa/snap/gui/icons/SelectTool24.gif",
        keywords = "#OptionsCategory_Keywords_SNAP",
        keywordsCategory = "#OptionsCategory_Keywords_SNAP_Prefs")
@org.openide.util.NbBundle.Messages({
                                            "OptionsCategory_Name_SNAP=UI",
                                            "OptionsCategory_Keywords_SNAP=sentinel, application, platform",
                                            "OptionsCategory_Keywords_SNAP_Prefs=keywordCategory"
                                    })
package org.esa.snap.gui.preferences.uibehavior;

import org.netbeans.spi.options.OptionsPanelController;