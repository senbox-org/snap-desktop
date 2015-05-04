package org.esa.snap.framework.ui.product.metadata;

import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.util.StringUtils;
import org.openide.nodes.PropertySupport;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class MetadataAttributePropertySetter {

    private MetadataAttribute attribute;
    private PropertySupport[] attributeProperties;

    public MetadataAttributePropertySetter(MetadataAttribute attribute) {
        this.attribute = attribute;
        List<PropertySupport> attributePropertyList = new ArrayList<>();
        final int type = attribute.getDataType();
        switch (type) {
            case ProductData.TYPE_INT32:
                attributePropertyList.add(new IntegerProperty("Value"));
                break;
            case ProductData.TYPE_UINT32:
                attributePropertyList.add(new IntegerProperty("Value"));
                break;
            case ProductData.TYPE_FLOAT64:
                attributePropertyList.add(new DoubleProperty("Value"));
                break;
            default:
                attributePropertyList.add(new StringProperty("Value"));
        }
        PropertySupport.ReadOnly<String> typeProperty =
                new PropertySupport.ReadOnly<String>("Type", String.class, "Type", null) {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                return ProductData.getTypeString(attribute.getDataType());
            }
        };
        attributePropertyList.add(typeProperty);
        String unit = attribute.getUnit();
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            PropertySupport.ReadOnly<String> unitProperty =
                    new PropertySupport.ReadOnly<String>("Unit", String.class, "Unit", null) {
                @Override
                public String getValue() throws IllegalAccessException, InvocationTargetException {
                    return attribute.getUnit();
                }
            };
            attributePropertyList.add(unitProperty);
        }
        String description = attribute.getDescription();
        if (StringUtils.isNotNullAndNotEmpty(description)) {
            PropertySupport.ReadOnly<String> descriptionProperty =
                    new PropertySupport.ReadOnly<String>("Description", String.class, "Description", null) {
                        @Override
                        public String getValue() throws IllegalAccessException, InvocationTargetException {
                            return attribute.getDescription();
                        }
                    };
            attributePropertyList.add(descriptionProperty);
        }
        attributeProperties = attributePropertyList.toArray(new PropertySupport[attributePropertyList.size()]);
    }

    public PropertySupport[] getAttributeProperties() {
        return attributeProperties;
    }

    public class IntegerProperty extends PropertySupport.ReadOnly<Integer> {

        public IntegerProperty(String name) {
            super(name, Integer.class, name, null);
        }

        @Override
        public Integer getValue() throws IllegalAccessException, InvocationTargetException {
            return attribute.getData().getElemInt();
        }
    }

    public class DoubleProperty extends PropertySupport.ReadOnly<Double> {

        public DoubleProperty(String name) {
            super(name, Double.class, name, null);
        }

        @Override
        public Double getValue() throws IllegalAccessException, InvocationTargetException {
            return attribute.getData().getElemDouble();
        }
    }

    public class StringProperty extends PropertySupport.ReadOnly<String> {

        public StringProperty(String name) {
            super(name, String.class, name, null);
        }

        @Override
        public String getValue() throws IllegalAccessException, InvocationTargetException {
            return attribute.getData().getElemString();
        }
    }

}
