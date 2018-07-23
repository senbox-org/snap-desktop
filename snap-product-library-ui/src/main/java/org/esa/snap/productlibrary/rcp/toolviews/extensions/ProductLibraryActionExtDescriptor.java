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

import com.bc.ceres.core.Assert;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;

/**
 * Provides a standard implementation for ProductLibraryActionExtDescriptor.
 */
public class ProductLibraryActionExtDescriptor {

    private String id;
    private Class<? extends ProductLibraryActionExt> actionExtClass;
    private int position;

    ProductLibraryActionExtDescriptor(final String id,
                                             final Class<? extends ProductLibraryActionExt> actionExtClass,
                                             final int position) {
        this.id = id;
        this.actionExtClass = actionExtClass;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public boolean isSeperator() {
        return actionExtClass == null;
    }

    public ProductLibraryActionExt createActionExt(final ProductLibraryActions actionHandler) {
        if(isSeperator()) {
            return null;
        }
        Object object;
        try {
            object = actionExtClass.newInstance();
            ((ProductLibraryActionExt) object).setActionHandler(actionHandler);
        } catch (Throwable e) {
            throw new IllegalStateException("actionExtClass.newInstance()", e);
        }
        Assert.state(object instanceof ProductLibraryActionExt, "object instanceof ProductLibraryActionExt");
        return (ProductLibraryActionExt) object;
    }
}
