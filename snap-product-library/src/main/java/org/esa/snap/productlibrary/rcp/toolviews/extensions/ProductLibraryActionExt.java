/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;

import javax.swing.*;

/**
 * An <code>ProductLibraryActionExt</code> is used as an action in the <code>ProductLibrary</code>.
 */
public interface ProductLibraryActionExt {

    void setActionHandler(final ProductLibraryActions actionHandler);

    JButton getButton(final JPanel panel);

    void selectionChanged(final ProductEntry[] selections);

    void performAction();
}