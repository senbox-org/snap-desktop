/**
 * This package contains product preferences handling.
 *
 * @author thomas
 */
@OptionsPanelController.ContainerRegistration(
        id = "GeneralPreferences",
        categoryName = "#OptionsCategory_Name_General",
        iconBase = "org/esa/snap/rcp/icons/generalOptions1.png",
        keywords = "#OptionsCategory_Keywords_General",
        keywordsCategory = "#OptionsCategory_Keywords_Category_General",
        position = 1)
@org.openide.util.NbBundle.Messages({
        "OptionsCategory_Name_General=General",
        "OptionsCategory_Keywords_General=general",
        "OptionsCategory_Keywords_Category_General=general"
}) package org.esa.snap.rcp.preferences.general;

import org.netbeans.spi.options.OptionsPanelController;