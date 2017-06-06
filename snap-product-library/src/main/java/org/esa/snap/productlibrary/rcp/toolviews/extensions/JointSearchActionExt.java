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
import org.esa.snap.engine_utilities.db.DBQuery;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.engine_utilities.db.ProductQueryInterface;
import org.esa.snap.engine_utilities.download.opensearch.CopernicusProductQuery;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryToolView;
import org.esa.snap.productlibrary.rcp.utils.ProductOpener;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Export a list of files to text file
 */
public class JointSearchActionExt implements ProductLibraryActionExt {

    // TODO need a new icon
    private static final ImageIcon jointSearchIcon = UIUtils.loadImageIcon("/org/esa/snap/productlibrary/icons/open24.png", ProductLibraryToolView.class);
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
        // TODO

        final ProductEntry[] selections = actionHandler.getSelectedProductEntries();

        System.out.println("JointSearchActionExt.performAction: joint search selected for " + selections[0].getMission());

        final DBQuery dbQuery = new DBQuery();

        dbQuery.setSelectionRect(selections[0].getGeoBoundary());

        Calendar startDate = selections[0].getFirstLineTime().getAsCalendar();
        startDate.add(Calendar.DAY_OF_MONTH, -7);
        Calendar endDate = selections[0].getFirstLineTime().getAsCalendar();
        endDate.add(Calendar.DAY_OF_MONTH, 7);
        dbQuery.setStartEndDate(startDate, endDate);

        ProductQueryInterface productQueryInterface = CopernicusProductQuery.instance();

        try {
            productQueryInterface.fullQuery(dbQuery, pm);
        } catch (Exception e) {
            System.out.println("JointSearchActionExt.performAction: caught error " + e.getMessage());
        }
    }
}
