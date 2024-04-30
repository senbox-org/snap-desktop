/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.ui;

import org.esa.snap.core.param.ParamChangeEvent;
import org.esa.snap.core.param.ParamChangeListener;
import org.esa.snap.core.param.ParamValidateException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Description of DemSelectorTest
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class DemSelectorTest {

    private DemSelector _demSelector;
    private MyParamChangeListener _paramChangeListener;

    @Before
    public void setUp() throws Exception {
        _paramChangeListener = new MyParamChangeListener();
        _demSelector = new DemSelector(_paramChangeListener);
    }

    @Test
    public void test_UsingExternalDEM_IsDefault() {
        assertTrue(_demSelector.isUsingExternalDem());
    }

    @Test
    public void test_SetUsingExternalDem() throws ParamValidateException {
        _demSelector.setUsingExternalDem(true);

        assertTrue(_demSelector.isUsingExternalDem());
        assertFalse(_demSelector.isUsingProductDem());
        assertEquals("", _paramChangeListener.getCallString());
    }

    @Test
    public void test_SetUsingProductDem() throws ParamValidateException {
        _demSelector.setUsingProductDem(true);

        assertTrue(_demSelector.isUsingProductDem());
        assertFalse(_demSelector.isUsingExternalDem());

        assertEquals("useProductDem|useExternalDem|", _paramChangeListener.getCallString());
    }

    @Test
    public void test_ToggleUsingDem() throws ParamValidateException {
        _demSelector.setUsingProductDem(true);
        _demSelector.setUsingExternalDem(true);

        assertTrue(_demSelector.isUsingExternalDem());
        assertFalse(_demSelector.isUsingProductDem());
        assertEquals("useProductDem|useExternalDem|useExternalDem|useProductDem|", _paramChangeListener.getCallString());
    }

    private static class MyParamChangeListener implements ParamChangeListener {
        StringBuilder callString = new StringBuilder();

        public String getCallString() {
            return callString.toString();
        }

        public void parameterValueChanged(ParamChangeEvent event) {
            callString.append(event.getParameter().getName());
            callString.append("|");
        }
    }
}
