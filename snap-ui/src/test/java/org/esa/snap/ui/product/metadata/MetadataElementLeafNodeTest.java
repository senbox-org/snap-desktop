package org.esa.snap.ui.product.metadata;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;
import org.openide.nodes.PropertySupport;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
public class MetadataElementLeafNodeTest {

    @Test
    @STTM("SNAP-1684")
    public void testEmptyProperty() throws InvocationTargetException, IllegalAccessException {
        final MetadataElementLeafNode.EmptyProperty emptyProperty = new MetadataElementLeafNode.EmptyProperty("TEST_ME");

        assertEquals("<empty>", emptyProperty.getValue());
    }

    @Test
    @STTM("SNAP-1684")
    public void testAddDataSpecificProperty_int32() throws InvocationTargetException, IllegalAccessException {
        final List<PropertySupport> attributePropertyList = new ArrayList<>();
        final MetadataTableLeaf leaf = new MetadataTableLeaf("don_t_care", ProductData.TYPE_INT32, ProductData.createInstance(new int[]{19}), null, null);
        final MetadataElementLeafNode leafNode = new MetadataElementLeafNode(leaf);

        leafNode.addDataTypeSpecificProperty(leaf, attributePropertyList);

        assertEquals(1, attributePropertyList.size());
        final PropertySupport propertySupport = attributePropertyList.get(0);
        assertEquals(19, ((Integer) propertySupport.getValue()).intValue());
    }

    @Test
    @STTM("SNAP-1684")
    public void testAddDataSpecificProperty_uint32() throws InvocationTargetException, IllegalAccessException {
        final List<PropertySupport> attributePropertyList = new ArrayList<>();
        final MetadataTableLeaf leaf = new MetadataTableLeaf("don_t_care", ProductData.TYPE_UINT32, ProductData.createInstance(new int[]{20}), null, null);
        final MetadataElementLeafNode leafNode = new MetadataElementLeafNode(leaf);

        leafNode.addDataTypeSpecificProperty(leaf, attributePropertyList);

        assertEquals(1, attributePropertyList.size());
        final PropertySupport propertySupport = attributePropertyList.get(0);
        assertEquals(20, ((Long) propertySupport.getValue()).longValue());
    }

    @Test
    @STTM("SNAP-1684")
    public void testAddDataSpecificProperty_UTC() throws InvocationTargetException, IllegalAccessException {
        final List<PropertySupport> attributePropertyList = new ArrayList<>();
        final MetadataTableLeaf leaf = new MetadataTableLeaf("don_t_care", ProductData.TYPE_UTC, new ProductData.UTC(589, 134, 33), null, null);
        final MetadataElementLeafNode leafNode = new MetadataElementLeafNode(leaf);

        leafNode.addDataTypeSpecificProperty(leaf, attributePropertyList);

        assertEquals(1, attributePropertyList.size());
        final PropertySupport propertySupport = attributePropertyList.get(0);
        assertEquals("12-AUG-2001 00:02:14.000033", propertySupport.getValue());
    }

    @Test
    @STTM("SNAP-1684")
    public void testAddDataSpecificProperty_float64() throws InvocationTargetException, IllegalAccessException {
        final List<PropertySupport> attributePropertyList = new ArrayList<>();
        final MetadataTableLeaf leaf = new MetadataTableLeaf("don_t_care", ProductData.TYPE_FLOAT64, ProductData.createInstance(new double[]{21.0}), null, null);
        final MetadataElementLeafNode leafNode = new MetadataElementLeafNode(leaf);

        leafNode.addDataTypeSpecificProperty(leaf, attributePropertyList);

        assertEquals(1, attributePropertyList.size());
        final PropertySupport propertySupport = attributePropertyList.get(0);
        assertEquals(21.0, (Double) propertySupport.getValue(), 1e-8);
    }

    @Test
    @STTM("SNAP-1684")
    public void testAddDataSpecificProperty_default() throws InvocationTargetException, IllegalAccessException {
        final List<PropertySupport> attributePropertyList = new ArrayList<>();
        final MetadataTableLeaf leaf = new MetadataTableLeaf("don_t_care", ProductData.TYPE_ASCII, ProductData.createInstance("Heffalump"), null, null);
        final MetadataElementLeafNode leafNode = new MetadataElementLeafNode(leaf);

        leafNode.addDataTypeSpecificProperty(leaf, attributePropertyList);

        assertEquals(1, attributePropertyList.size());
        final PropertySupport propertySupport = attributePropertyList.get(0);
        assertEquals("Heffalump", propertySupport.getValue());
    }
}
