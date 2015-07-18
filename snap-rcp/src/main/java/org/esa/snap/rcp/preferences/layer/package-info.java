/**
 * This package contains the layer preferences handling.
 *
 * @author thomas
 */
@OptionsPanelController.ContainerRegistration(
        id = "LayerPreferences",
        categoryName = "#OptionsCategory_Name_Layer",
        iconBase = "org/esa/snap/rcp/icons/Layers_32x32.png",
        keywords = "#OptionsCategory_Keywords_Layers",
        keywordsCategory = "#OptionsCategory_Keywords_Layer_Prefs",
        position = 2)
@org.openide.util.NbBundle.Messages({
        "OptionsCategory_Name_Layer=Layer",
        "OptionsCategory_Keywords_Layers=layer, application, platform",
        "OptionsCategory_Keywords_Layer_Prefs=layer"
}) package org.esa.snap.rcp.preferences.layer;

import org.netbeans.spi.options.OptionsPanelController;