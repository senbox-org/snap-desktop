/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.ui;

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.ui.DefaultAppContext;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OperatorMenuTest {

    private static OperatorParameterSupportTest.TestOpSpi testOpSpi;

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeFalse("Cannot run in headless", GraphicsEnvironment.isHeadless());
        testOpSpi = new OperatorParameterSupportTest.TestOpSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(testOpSpi);
    }

    @AfterClass
    public static void afterClass() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(testOpSpi);
    }


    @Test
    public void testOperatorAboutText() throws Exception {
        DefaultAppContext appContext = new DefaultAppContext("test");

        final OperatorMenu support = new OperatorMenu(null, testOpSpi.getOperatorDescriptor(), null, appContext, "");

        assertEquals("Tester", support.getOperatorName());

        String operatorDescription = support.getOperatorAboutText();
        assertTrue(operatorDescription.length() > 80);
    }

    @Test
    public void testEscapingXmlParameters() throws Exception {
        DefaultDomElement domElement = new DefaultDomElement("parameter");
        String unescapedString = "12 < 13 && 56 > 42 & \"true\" + 'a name'";
        String escapedString = "12 &lt; 13 &amp;&amp; 56 &gt; 42 &amp; &quot;true&quot; + &apos;a name&apos;";

        domElement.addChild(new DefaultDomElement("expression", unescapedString));
        DefaultDomElement withAttribute = new DefaultDomElement("withAttribute");
        withAttribute.setAttribute("attrib", unescapedString);
        domElement.addChild(withAttribute);

        OperatorMenu.escapeXmlElements(domElement);

        assertEquals(escapedString, domElement.getChild("expression").getValue());
        assertEquals(escapedString, domElement.getChild("withAttribute").getAttribute("attrib"));

        String xmlString = domElement.toXml();
        XppDom readDomElement = OperatorMenu.createDom(xmlString);

        assertEquals(unescapedString, readDomElement.getChild("expression").getValue());
        assertEquals(unescapedString, readDomElement.getChild("withAttribute").getAttribute("attrib"));
    }
}
