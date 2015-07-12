package org.esa.snap.smart.configurator.ui;


import org.junit.Test;


/**
 * @author Nicolas Ducoin
 */
public class LineSplitBySeparatorTest {

    @Test
    public void testBlankSpaces() {
        String stringWithBlanks = "a blank \"Str ing\" with \"  's p a c e s'";
        String multiLineString = LineSplitTextEditDialog.toMultiLine(stringWithBlanks, " ");
        String[] linesArray = multiLineString.split(System.lineSeparator());

        assert (linesArray.length == 5);
    }

    @Test
    public void testCommas() {
        String stringWithBlanks = "a,blank,\"Str,ing\",with,\",,'s,p,a,c,e,s'";
        String multiLineString = LineSplitTextEditDialog.toMultiLine(stringWithBlanks, ",");
        String[] linesArray = multiLineString.split(System.lineSeparator());

        assert (linesArray.length == 5);
    }

}