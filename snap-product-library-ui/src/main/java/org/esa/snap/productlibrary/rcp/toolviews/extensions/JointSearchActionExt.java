/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.extensions;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.productlibrary.db.DBQuery;
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.productlibrary.db.ProductQueryInterface;
import org.esa.snap.productlibrary.opensearch.CopernicusProductQuery;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.dialogs.JointSearchDialog;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryToolView;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import java.util.Calendar;

/**
 * Export a list of files to text file
 */
public class JointSearchActionExt implements ProductLibraryActionExt {

    private static final ImageIcon jointSearchIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/joint_search.png", ProductLibraryToolView.class);
    private JButton button = null;
    private ProductLibraryActions actionHandler;

    public void setActionHandler(final ProductLibraryActions actionHandler) {
        this.actionHandler = actionHandler;
    }

    public JButton getButton(final JPanel panel) {
        if (button == null) {
            button = DialogUtils.createButton("jointSearchButton", "Joint Search", jointSearchIcon, panel, DialogUtils.ButtonStyle.Icon);
        }
        return button;
    }

    public void selectionChanged(final ProductEntry[] selections) {
        button.setEnabled(selections.length == 1 && ProductLibraryActions.allProductsExist(selections));
    }

    public void performAction(final ProgressMonitor pm) {

        final ProductEntry[] selections = actionHandler.getSelectedProductEntries();

        //System.out.println("JointSearchActionExt.performAction: joint search selected for " + selections[0].getMission());

        final JointSearchDialog dlg = new JointSearchDialog("Joint Search Criteria", selections[0].getMission());
        dlg.show();
        if (!dlg.IsOK()) {
            return;
        }

        final DBQuery dbQuery = new DBQuery();

        // Search for products close in space to the selected product
        dbQuery.setSelectionRect(selections[0].getGeoBoundary());

        // Search for products from the missions the user has selected
        dbQuery.setSelectedMissions(dlg.getMissions()); //

        // Search for products within the time range that is +/- number of days from the date of the selected product
        final int daysMinus =  dlg.getDaysMinus();
        final int daysPlus = dlg.getDaysPlus();
        if (daysMinus < 0 || daysPlus < 0) {
            Dialogs.showError("Joint search: invalid number of days\n (must be 0 or +ve integer)");
            return;
        }
        //SystemUtils.LOG.info("Joint Search -" + daysMinus + " days and +" + daysPlus + " days");
        Calendar startDate = selections[0].getFirstLineTime().getAsCalendar();
        startDate.add(Calendar.DAY_OF_MONTH, -daysMinus);
        Calendar endDate = selections[0].getFirstLineTime().getAsCalendar();
        endDate.add(Calendar.DAY_OF_MONTH, daysPlus);
        dbQuery.setStartEndDate(startDate, endDate);

        // Search for optical products based on cloud cover percentage
        dbQuery.setSelectedCloudCover(dlg.getCloudCover());

        // Search for products with this particular acquisition mode
        dbQuery.setSelectedAcquisitionMode(dlg.getAcquisitionMode());

        // Search for products of these product types
        dbQuery.setSelectedProductTypes(dlg.getProductTypes());

        ProductQueryInterface productQueryInterface = CopernicusProductQuery.instance();

        try {
            productQueryInterface.fullQuery(dbQuery, pm);
            if (!pm.isCanceled()) {
                actionHandler.getToolView().setSelectedRepositoryToSciHub();
            }
        } catch (Exception e) {
            Dialogs.showError("unable to do joint search: " + e.getMessage());
        } finally {
            pm.done();
        }
    }
}
