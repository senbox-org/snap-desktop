package org.esa.snap.rcp.imgfilter.model;

import com.bc.ceres.annotation.STTM;
import com.thoughtworks.xstream.XStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FilterSetTest {

    @Test
    @STTM("SNAP-3765")
    public void testCreateXStream() {
       final String xmlInputString = "<filterSet>\n" +
               "  <name>User</name>\n" +
               "  <editable>true</editable>\n" +
               "  <filters>\n" +
               "    <filter>\n" +
               "      <name>Tom2</name>\n" +
               "      <shorthand>my2</shorthand>\n" +
               "      <operation>CONVOLVE</operation>\n" +
               "      <editable>true</editable>\n" +
               "      <tags/>\n" +
               "      <kernelElements>0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0</kernelElements>\n" +
               "      <kernelWidth>5</kernelWidth>\n" +
               "      <kernelHeight>5</kernelHeight>\n" +
               "      <kernelQuotient>1.0</kernelQuotient>\n" +
               "      <kernelOffsetX>2</kernelOffsetX>\n" +
               "      <kernelOffsetY>2</kernelOffsetY>\n" +
               "    </filter>\n" +
               "  </filters>\n" +
               "</filterSet>";
        XStream xStream = FilterSet.createXStream();

        final FilterSet filterSet = new FilterSet();
        xStream.fromXML(new ByteArrayInputStream(xmlInputString.getBytes()), filterSet);

        final List<Filter> filters = filterSet.getFilters();
        assertEquals(1, filters.size());

        final Filter filter = filters.get(0);
        assertEquals("Tom2", filter.getName());
    }
}
