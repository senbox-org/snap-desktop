/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BaseOperatorUITest {

    @Test
    public void initializeOperatorUIInitializesPropertySetCorrectly() {
        BaseOperatorUI baseOperatorUI = mock(BaseOperatorUI.class, CALLS_REAL_METHODS);
        Map<String, Object> parameterMap = mock(Map.class);
        baseOperatorUI.initializeOperatorUI("Read", parameterMap);
        assertNotNull(baseOperatorUI.getPropertySet());
    }

    @Test
    public void initializeOperatorUIHandlesEmptyParameterMap() {
        BaseOperatorUI baseOperatorUI = mock(BaseOperatorUI.class, CALLS_REAL_METHODS);
        Map<String, Object> parameterMap = mock(Map.class);
        when(parameterMap.isEmpty()).thenReturn(true);
        baseOperatorUI.initializeOperatorUI("Read", parameterMap);
        assertNull(baseOperatorUI.getErrorMessage());
    }

    @Test
    @STTM("SNAP-3867")
    public void invalidParameter() {
        BaseOperatorUI baseOperatorUI = mock(BaseOperatorUI.class, CALLS_REAL_METHODS);
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("sourceBands", "");
        baseOperatorUI.initializeOperatorUI("Read", parameterMap);
        assertNotNull(baseOperatorUI.getPropertySet());
        assertNotNull(baseOperatorUI.getErrorMessage());
    }

    @Test
    public void setSourceProductsUpdatesSourceProducts() {
        BaseOperatorUI baseOperatorUI = mock(BaseOperatorUI.class, CALLS_REAL_METHODS);
        Product[] products = new Product[]{mock(Product.class)};
        baseOperatorUI.setSourceProducts(products);
        assertArrayEquals(products, baseOperatorUI.sourceProducts);
    }

    @Test
    public void hasSourceProductsReturnsTrueWhenSourceProductsAreSet() {
        BaseOperatorUI baseOperatorUI = mock(BaseOperatorUI.class, CALLS_REAL_METHODS);
        Product[] products = new Product[]{mock(Product.class)};
        baseOperatorUI.setSourceProducts(products);
        assertTrue(baseOperatorUI.hasSourceProducts());
    }

    @Test
    public void hasSourceProductsReturnsFalseWhenSourceProductsAreNotSet() {
        BaseOperatorUI baseOperatorUI = mock(BaseOperatorUI.class, CALLS_REAL_METHODS);
        assertFalse(baseOperatorUI.hasSourceProducts());
    }

    @Test
    public void getBandNamesReturnsCorrectBandNames() {
        BaseOperatorUI baseOperatorUI = mock(BaseOperatorUI.class, CALLS_REAL_METHODS);
        Product product = mock(Product.class);
        when(product.getBandNames()).thenReturn(new String[]{"Band1", "Band2"});
        baseOperatorUI.setSourceProducts(new Product[]{product});
        String[] bandNames = baseOperatorUI.getBandNames();
        assertArrayEquals(new String[]{"Band1", "Band2"}, bandNames);
    }
}