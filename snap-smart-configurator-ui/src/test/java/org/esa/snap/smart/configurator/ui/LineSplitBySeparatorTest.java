package org.esa.snap.smart.configurator.ui;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Nicolas Ducoin
 */
public class LineSplitBySeparatorTest {

    @Test
    public void testBlankSpaces() {
        String stringWithBlanks = "a blank \"Str ing\" with \"  's p a c e s'";
        String multiLineString = LineSplitTextEditDialog.toMultiLine(stringWithBlanks, " ");
        String[] linesArray = multiLineString.split(System.lineSeparator());

        assertEquals (5, linesArray.length);
    }

    @Test
    public void testCommas() {
        String stringWithBlanks = "a,blank,\"Str,ing\",with,\",,'s,p,a,c,e,s'";
        String multiLineString = LineSplitTextEditDialog.toMultiLine(stringWithBlanks, ",");
        String[] linesArray = multiLineString.split(System.lineSeparator());

        assertEquals (5, linesArray.length);
    }

}