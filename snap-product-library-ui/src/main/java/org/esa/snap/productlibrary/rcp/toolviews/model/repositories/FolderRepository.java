/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.model.repositories;

import org.esa.snap.productlibrary.db.DBProductQuery;
import org.esa.snap.productlibrary.db.ProductQueryInterface;
import org.esa.snap.tango.TangoIcons;

import javax.swing.*;
import java.io.File;

/**
 * Created by luis on 24/02/2017.
 */
public class FolderRepository implements RepositoryInterface {

    private final String name;
    private final File baseDir;

    private static final ImageIcon icon = TangoIcons.places_folder(TangoIcons.Res.R22);

    public FolderRepository(final String name, final File baseDir) {
        this.name = name;
        this.baseDir = baseDir;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public File getBaseDir() {
        return baseDir;
    }

    public ImageIcon getIconImage() {
        return icon;
    }

    public ProductQueryInterface getProductQueryInterface() {
        return DBProductQuery.instance();
    }
}
