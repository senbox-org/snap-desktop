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
package org.esa.snap.graphbuilder.gpf.ui;


import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.ui.AppContext;

import javax.swing.JComponent;
import java.util.Map;

/**
 * An <code>OperatorUI</code> is used as a user interface for an <code>Operator</code>.
 */
public interface OperatorUI {

    String getOperatorName();

    JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext);

    void initParameters();

    UIValidation validateParameters();

    void updateParameters();

    void setSourceProducts(Product[] products);

    boolean hasSourceProducts();

    void convertToDOM(XppDomElement parentElement) throws GraphException;

    Map<String, Object> getParameters();

    boolean hasError();

    String getErrorMessage();

    void addSelectionChangeListener(SelectionChangeListener listener);
}
