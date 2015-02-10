/**
 * This package contains product preferences handling.
 *
 * @author thomas
 */
@OptionsPanelController.ContainerRegistration(
        id = "ProductPreferences",
        categoryName = "#OptionsCategory_Name_Product",
        iconBase = "org/esa/snap/rcp/icons/Product32.png",
        keywords = "#OptionsCategory_Keywords_Product",
        keywordsCategory = "#OptionsCategory_Keywords_Category_Product")
@org.openide.util.NbBundle.Messages({
        "OptionsCategory_Name_Product=Product Profiles",
        "OptionsCategory_Keywords_Product=product",
        "OptionsCategory_Keywords_Category_Product=product"
}) package org.esa.snap.rcp.preferences.product;

import org.netbeans.spi.options.OptionsPanelController;