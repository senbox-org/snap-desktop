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
package org.esa.snap.graphbuilder.gpf.ui;

import com.bc.ceres.core.Assert;

/**
 * Provides a standard implementation for {@link OperatorUIDescriptor}.
 */
public class DefaultOperatorUIDescriptor implements OperatorUIDescriptor {

    private String id;
    private String operatorName;
    private Boolean disableFromGraphBuilder;
    private Class<? extends OperatorUI> operatorUIClass;

    public DefaultOperatorUIDescriptor(final String id, final String operatorName,
                                       final Class<? extends OperatorUI> operatorUIClass,
                                       final Boolean disableFromGraphBuilder) {
        this.id = id;
        this.operatorName = operatorName;
        this.operatorUIClass = operatorUIClass;
        this.disableFromGraphBuilder = disableFromGraphBuilder;
    }

    public String getId() {
        return id;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public Boolean disableFromGraphBuilder() {
        return disableFromGraphBuilder;
    }

    public OperatorUI createOperatorUI() {
        if(operatorUIClass == null) {
            return new DefaultUI();
        }
        Object object;
        try {
            object = operatorUIClass.newInstance();
        } catch (Throwable e) {
            throw new IllegalStateException("operatorUIClass.newInstance()", e);
        }
        Assert.state(object instanceof OperatorUI, "object instanceof operatorUI");
        return (OperatorUI) object;
    }
}
