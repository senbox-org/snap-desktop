package org.esa.snap.ui.product.metadata;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class MetadataElementLeafNode extends AbstractNode {

    private final MetadataTableLeaf leaf;

    MetadataElementLeafNode(MetadataTableLeaf leaf) {
        this(leaf, new InstanceContent());
    }

    private MetadataElementLeafNode(MetadataTableLeaf leaf, InstanceContent content) {
        super(Children.LEAF, new AbstractLookup(content));
        this.leaf = leaf;
        content.add(leaf);
        setName(leaf.getName());
        createSheet();
    }

    @Override
    protected final Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        PropertySupport[] properties = createAttributeProperties();
        for (PropertySupport attributeProperty : properties) {
            set.put(attributeProperty);
        }
        sheet.put(set);
        return sheet;
    }

    private PropertySupport[] createAttributeProperties() {
        List<PropertySupport> attributePropertyList = new ArrayList<>();

        final int numElems = leaf.getData().getNumElems();
        if (numElems > 0) {
            addDataTypeSpecificProperty(leaf, attributePropertyList);
        } else {
            attributePropertyList.add(new EmptyProperty("Value"));
        }

        PropertySupport.ReadOnly<String> typeProperty =
                new PropertySupport.ReadOnly<String>("Type", String.class, "Type", null) {
                    @Override
                    public String getValue() throws IllegalAccessException, InvocationTargetException {
                        return ProductData.getTypeString(leaf.getDataType());
                    }
                };
        attributePropertyList.add(typeProperty);
        String unit = leaf.getUnit();
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            PropertySupport.ReadOnly<String> unitProperty =
                    new PropertySupport.ReadOnly<String>("Unit", String.class, "Unit", null) {
                        @Override
                        public String getValue() throws IllegalAccessException, InvocationTargetException {
                            return leaf.getUnit();
                        }
                    };
            attributePropertyList.add(unitProperty);
        }
        String description = leaf.getDescription();
        if (StringUtils.isNotNullAndNotEmpty(description)) {
            PropertySupport.ReadOnly<String> descriptionProperty =
                    new PropertySupport.ReadOnly<String>("Description", String.class, "Description", null) {
                        @Override
                        public String getValue() throws IllegalAccessException, InvocationTargetException {
                            return leaf.getDescription();
                        }
                    };
            attributePropertyList.add(descriptionProperty);
        }
        return attributePropertyList.toArray(new PropertySupport[attributePropertyList.size()]);
    }

    void addDataTypeSpecificProperty(MetadataTableLeaf leaf, List<PropertySupport> attributePropertyList) {
        final int dataType = leaf.getDataType();
        switch (dataType) {
            case ProductData.TYPE_INT32:
                attributePropertyList.add(new IntegerProperty("Value"));
                break;
            case ProductData.TYPE_UINT32:
                if (leaf.getData() instanceof ProductData.UTC) {
                    attributePropertyList.add(new StringProperty("Value"));
                    break;
                }
                attributePropertyList.add(new UIntegerProperty("Value"));
                break;
            case ProductData.TYPE_FLOAT64:
                attributePropertyList.add(new DoubleProperty("Value"));
                break;
            default:
                attributePropertyList.add(new StringProperty("Value"));
        }
    }

    public class IntegerProperty extends PropertySupport.ReadOnly<Integer> {

        public IntegerProperty(String name) {
            super(name, Integer.class, name, null);
        }

        @Override
        public Integer getValue() throws IllegalAccessException, InvocationTargetException {
            return leaf.getData().getElemInt();
        }
    }

    public class UIntegerProperty extends PropertySupport.ReadOnly<Long> {

        public UIntegerProperty(String name) {
            super(name, Long.class, name, null);
        }

        @Override
        public Long getValue() throws IllegalAccessException, InvocationTargetException {
            return leaf.getData().getElemUInt();
        }
    }

    public class DoubleProperty extends PropertySupport.ReadOnly<Double> {

        public DoubleProperty(String name) {
            super(name, Double.class, name, null);
        }

        @Override
        public Double getValue() throws IllegalAccessException, InvocationTargetException {
            return leaf.getData().getElemDouble();
        }
    }

    public class StringProperty extends PropertySupport.ReadOnly<String> {

        public StringProperty(String name) {
            super(name, String.class, name, null);
        }

        @Override
        public String getValue() throws IllegalAccessException, InvocationTargetException {
            return leaf.getData().getElemString();
        }
    }

    public static class EmptyProperty extends PropertySupport.ReadOnly<String> {

        public EmptyProperty(String name) {
            super(name, String.class, name, null);
        }

        @Override
        public String getValue() throws IllegalAccessException, InvocationTargetException {
            return "<empty>";
        }
    }
}
