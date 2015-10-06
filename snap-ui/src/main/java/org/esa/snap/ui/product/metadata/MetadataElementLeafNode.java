package org.esa.snap.ui.product.metadata;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

import java.awt.Image;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class MetadataElementLeafNode extends AbstractNode {

    private MetadataTableLeaf leaf;

    public MetadataElementLeafNode(MetadataTableLeaf leaf) {
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
    public Image getIcon(int type) {
        return new EmptyImage();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
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
        final int type = leaf.getDataType();
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

    public class IntegerProperty extends PropertySupport.ReadOnly<Integer> {

        public IntegerProperty(String name) {
            super(name, Integer.class, name, null);
        }

        @Override
        public Integer getValue() throws IllegalAccessException, InvocationTargetException {
            return leaf.getData().getElemInt();
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

}
