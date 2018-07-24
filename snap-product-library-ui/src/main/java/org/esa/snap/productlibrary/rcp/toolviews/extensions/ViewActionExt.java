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
import org.esa.snap.productlibrary.db.ProductEntry;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ListView;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ProductEntryList;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ProductEntryTable;
import org.esa.snap.productlibrary.rcp.toolviews.listviews.ThumbnailView;
import org.esa.snap.rcp.quicklooks.ThumbnailPanel;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;

/**
 * Export a list of files to text file
 */
public class ViewActionExt implements ProductLibraryActionExt {

    private static final ImageIcon listViewButtonIcon = UIUtils.loadImageIcon("/org/esa/snap/rcp/icons/view_list24.png", ThumbnailPanel.class);
    private static final ImageIcon tableViewButtonIcon = UIUtils.loadImageIcon("/org/esa/snap/rcp/icons/view_table24.png", ThumbnailPanel.class);
    private static final ImageIcon thumbnailViewButtonIcon = UIUtils.loadImageIcon("/org/esa/snap/rcp/icons/view_thumbnails24.png", ThumbnailPanel.class);
    private JButton button = null;
    private ProductLibraryActions actionHandler;

    public void setActionHandler(final ProductLibraryActions actionHandler) {
        this.actionHandler = actionHandler;
    }

    public JButton getButton(final JPanel panel) {
        if (button == null) {
            button = DialogUtils.createButton("viewButton", "Change View", tableViewButtonIcon, panel, DialogUtils.ButtonStyle.Icon);
        }
        return button;
    }

    public void selectionChanged(final ProductEntry[] selections) {
        button.setEnabled(true);
    }

    public void performAction(final ProgressMonitor pm) {
        actionHandler.getToolView().changeView();

        final ListView currentListView = actionHandler.getToolView().getCurrentListView();
        if (currentListView instanceof ThumbnailView) {
            updateViewButton(thumbnailViewButtonIcon);
        } else if (currentListView instanceof ProductEntryList) {
            updateViewButton(listViewButtonIcon);
        } else if (currentListView instanceof ProductEntryTable) {
            updateViewButton(tableViewButtonIcon);
        }
    }

    private void updateViewButton(final ImageIcon icon) {
        button.setIcon(icon);
        button.setRolloverIcon(icon);
    }
}
