package org.esa.snap.rcp.statistics;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MetadataPlotSettingsTest {

    @Test
    public void testUsableFieldsRetrieval_WithoutRecords() throws Exception {
        MetadataElement elem = new MetadataElement("Records");
        addAttributes(elem);

        List<String> fieldNames = MetadataPlotSettings.retrieveUsableFieldNames(elem);

        assertEquals(3, fieldNames.size());
        assertTrue(fieldNames.contains("singleValue"));
        assertTrue(fieldNames.contains("array"));
        assertTrue(fieldNames.contains("splittedArray"));
        assertTrue(!fieldNames.contains("string"));
    }

    @Test
    public void testUsableFieldsRetrieval_WithRecords() throws Exception {
        MetadataElement elem = new MetadataElement("Records");
        MetadataElement element1 = new MetadataElement("Records.1");
        addAttributes(element1);
        elem.addElement(element1);
        MetadataElement element2 = new MetadataElement("Records.2");
        addAttributes(element2);
        elem.addElement(element2);

        List<String> fieldNames = MetadataPlotSettings.retrieveUsableFieldNames(elem);

        assertEquals(3, fieldNames.size());
        assertTrue(fieldNames.contains("singleValue"));
        assertTrue(fieldNames.contains("array"));
        assertTrue(fieldNames.contains("splittedArray"));
        assertTrue(!fieldNames.contains("string"));
    }

    private void addAttributes(MetadataElement element) {
        element.addAttribute(new MetadataAttribute("string", ProductData.createInstance("O815"), true));
        element.addAttribute(new MetadataAttribute("singleValue", ProductData.createInstance(new byte[]{108}), true));
        element.addAttribute(new MetadataAttribute("array", ProductData.createInstance(new byte[]{4, 8, 15, 16, 23, 42}), true));
        element.addAttribute(new MetadataAttribute("splittedArray.1", ProductData.createInstance(new byte[]{4}), true));
        element.addAttribute(new MetadataAttribute("splittedArray.2", ProductData.createInstance(new byte[]{8}), true));
        element.addAttribute(new MetadataAttribute("splittedArray.3", ProductData.createInstance(new byte[]{15}), true));
        element.addAttribute(new MetadataAttribute("splittedArray.4", ProductData.createInstance(new byte[]{16}), true));
        element.addAttribute(new MetadataAttribute("splittedArray.5", ProductData.createInstance(new byte[]{23}), true));
        element.addAttribute(new MetadataAttribute("splittedArray.6", ProductData.createInstance(new byte[]{42}), true));
    }
}